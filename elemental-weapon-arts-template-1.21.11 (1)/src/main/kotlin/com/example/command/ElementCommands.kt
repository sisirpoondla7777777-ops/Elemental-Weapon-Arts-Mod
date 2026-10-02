// src/main/kotlin/com/example/command/ElementCommands.kt

package com.example.command

import com.example.ability.AbilityHelp
import com.example.ability.AbilityManager
import com.example.ability.AbilityResult
import com.example.ability.ElementalEnchantments
import com.example.element.AbilitySlot
import com.example.element.Element
import com.example.element.EssenceConfig
import com.example.element.PlayerElementData
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object ElementCommands {

    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        context: CommandBuildContext
    ) {
        dispatcher.register(
            Commands.literal("element")

                .then(
                    Commands.literal("choose")
                        .then(
                            Commands.argument(
                                "element",
                                StringArgumentType.word()
                            )
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(
                                        Element.entries.map { it.id },
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    chooseElement(
                                        ctx.source.playerOrException,
                                        StringArgumentType.getString(
                                            ctx,
                                            "element"
                                        )
                                    )
                                }
                        )
                )

                .then(
                    Commands.literal("set")
                        .requires { source ->
                            source.hasPermission(2)
                        }
                        .then(
                            Commands.argument(
                                "element",
                                StringArgumentType.word()
                            )
                                .suggests { _, builder ->
                                    SharedSuggestionProvider.suggest(
                                        Element.entries.map { it.id },
                                        builder
                                    )
                                }
                                .executes { ctx ->
                                    setElement(
                                        ctx.source.playerOrException,
                                        StringArgumentType.getString(
                                            ctx,
                                            "element"
                                        ),
                                        true
                                    )
                                }
                        )
                )

                .then(
                    Commands.literal("power1")
                        .executes {
                            useAbility(
                                it.source.playerOrException,
                                AbilitySlot.POWER1
                            )
                        }
                )

                .then(
                    Commands.literal("power2")
                        .executes {
                            useAbility(
                                it.source.playerOrException,
                                AbilitySlot.POWER2
                            )
                        }
                )

                .then(
                    Commands.literal("power3")
                        .executes {
                            useAbility(
                                it.source.playerOrException,
                                AbilitySlot.POWER3
                            )
                        }
                )

                .then(
                    Commands.literal("conduit")
                        .executes {
                            useAbility(
                                it.source.playerOrException,
                                AbilitySlot.CONDUIT
                            )
                        }
                )

                .then(
                    Commands.literal("status")
                        .executes {
                            status(it.source)
                        }
                )

                .then(
                    Commands.literal("help")
                        .executes { ctx ->
                            val player =
                                ctx.source.playerOrException

                            val element =
                                PlayerElementData.getElement(player)

                            if (element == null) {
                                player.sendSystemMessage(
                                    Component.literal(
                                        "Choose an element first: " +
                                            "/element choose <fire|water|earth|wind|shadow|light>"
                                    )
                                )
                                0
                            } else {
                                sendAbilityGuide(
                                    player,
                                    element
                                )
                                1
                            }
                        }
                )
        )
    }

    private fun chooseElement(
        player: ServerPlayer,
        raw: String
    ): Int {
        if (PlayerElementData.getElement(player) != null) {
            player.sendSystemMessage(
                Component.literal(
                    "You have already chosen an element. " +
                        "Only an operator can change it with /element set."
                )
            )
            return 0
        }

        return setElement(
            player,
            raw,
            false
        )
    }

    private fun setElement(
        player: ServerPlayer,
        raw: String,
        adminChange: Boolean
    ): Int {
        val element =
            Element.fromId(raw)

        if (element == null) {
            player.sendSystemMessage(
                Component.literal(
                    "Unknown element '$raw'. Choose: " +
                        Element.entries.joinToString(", ") {
                            it.id
                        }
                )
            )
            return 0
        }

        PlayerElementData.setElement(
            player,
            element
        )

        player.sendSystemMessage(
            Component.literal(
                if (adminChange) {
                    "Element changed to ${element.id}."
                } else {
                    "You are now attuned to ${element.id}."
                }
            )
        )

        player.sendSystemMessage(
            Component.literal(
                "Conduit weapon: ${element.conduitWeaponName}."
            )
        )

        sendAbilityGuide(
            player,
            element
        )

        return 1
    }

    private fun useAbility(
        player: ServerPlayer,
        slot: AbilitySlot
    ): Int {
        return when (
            AbilityManager.use(
                player,
                slot
            )
        ) {
            AbilityResult.Success -> 1

            AbilityResult.NoElement -> {
                player.sendSystemMessage(
                    Component.literal(
                        "Choose an element first with " +
                            "/element choose <element>."
                    )
                )
                0
            }

            AbilityResult.OnCooldown -> {
                val remaining =
                    (
                        PlayerElementData.getCooldownEnd(
                            player,
                            slot
                        ) - player.level().gameTime
                    )
                        .coerceAtLeast(0L) / 20.0

                player.sendSystemMessage(
                    Component.literal(
                        "${slot.label()} is still recharging " +
                            "(${"%.1f".format(remaining)}s left)."
                    )
                )

                0
            }

            AbilityResult.NotEnoughEssence -> {
                player.sendSystemMessage(
                    Component.literal(
                        "Not enough Essence."
                    )
                )
                0
            }

            AbilityResult.WrongWeapon -> {
                val element =
                    PlayerElementData.getElement(player)

                player.sendSystemMessage(
                    Component.literal(
                        "You need your ${element?.conduitWeaponName ?: "conduit weapon"} " +
                            "in your main hand to use the Conduit."
                    )
                )
                0
            }
        }
    }

    private fun status(
        source: CommandSourceStack
    ): Int {
        val player =
            source.playerOrException

        val element =
            PlayerElementData.getElement(player)

        if (element == null) {
            source.sendSuccess(
                {
                    Component.literal(
                        "No element chosen."
                    )
                },
                false
            )

            return 1
        }

        val essence =
            PlayerElementData.getEssence(player)

        val enchantLevel =
            ElementalEnchantments.levelOnHeldItem(
                player,
                element
            )

        source.sendSuccess(
            {
                Component.literal(
                    "Element: ${element.id}\n" +
                        "Conduit: ${element.conduitWeaponName}\n" +
                        "Essence: ${essence.toInt()}/" +
                        EssenceConfig.MAX_ESSENCE.toInt() + "\n" +
                        "Conduit enchantment: " +
                        "Level $enchantLevel"
                )
            },
            false
        )

        AbilitySlot.entries.forEach { slot ->
            val remaining =
                (
                    PlayerElementData.getCooldownEnd(
                        player,
                        slot
                    ) - player.level().gameTime
                )
                    .coerceAtLeast(0L) / 20.0

            source.sendSuccess(
                {
                    Component.literal(
                        "${slot.label()}: " +
                            if (remaining <= 0.0) {
                                "READY"
                            } else {
                                "${"%.1f".format(remaining)}s"
                            }
                    )
                },
                false
            )
        }

        return 1
    }

    private fun sendAbilityGuide(
        player: ServerPlayer,
        element: Element
    ) {
        player.sendSystemMessage(
            Component.literal(
                "${element.id.replaceFirstChar { it.uppercase() }} abilities"
            )
        )

        AbilitySlot.entries.forEach { slot ->
            val help =
                AbilityHelp.forAbility(
                    element,
                    slot
                )

            val command =
                when (slot) {
                    AbilitySlot.POWER1 ->
                        "/element power1"

                    AbilitySlot.POWER2 ->
                        "/element power2"

                    AbilitySlot.POWER3 ->
                        "/element power3"

                    AbilitySlot.CONDUIT ->
                        "/element conduit"
                }

            val requirement =
                if (slot == AbilitySlot.CONDUIT) {
                    " Requires ${element.conduitWeaponName}."
                } else {
                    ""
                }

            player.sendSystemMessage(
                Component.literal(
                    "${slot.label()} — " +
                        "${help.name}: " +
                        "${help.description}. " +
                        "$command$requirement"
                )
            )
        }
    }

    private fun AbilitySlot.label(): String =
        when (this) {
            AbilitySlot.POWER1 -> "Power 1"
            AbilitySlot.POWER2 -> "Power 2"
            AbilitySlot.POWER3 -> "Power 3"
            AbilitySlot.CONDUIT -> "Conduit"
        }
}
