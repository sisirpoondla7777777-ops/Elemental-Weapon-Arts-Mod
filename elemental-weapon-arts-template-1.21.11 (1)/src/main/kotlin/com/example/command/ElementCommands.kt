package com.example.command

import com.example.ability.AbilityManager
import com.example.ability.AbilityResult
import com.example.ability.AbilityHelp
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
                    Commands.literal("set")
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

                                    val player =
                                        ctx.source.playerOrException

                                    val raw =
                                        StringArgumentType.getString(
                                            ctx,
                                            "element"
                                        )

                                    val element =
                                        Element.fromId(raw)

                                    if (element == null) {
                                        ctx.source.sendFailure(
                                            Component.literal(
                                                "Unknown element '$raw'. " +
                                                    "Choose one of: " +
                                                    Element.entries.joinToString {
                                                        it.id
                                                    }
                                            )
                                        )

                                        return@executes 0
                                    }

                                    PlayerElementData.setElement(
                                        player,
                                        element
                                    )

                                    ctx.source.sendSuccess(
                                        {
                                            Component.literal(
                                                "You are now attuned to " +
                                                    "${element.id.replaceFirstChar { c -> c.uppercase() }}. " +
                                                    "Your conduit weapon is the " +
                                                    "${element.conduitWeaponName}."
                                            )
                                        },
                                        true
                                    )

                                    sendAbilityGuide(player, element)

                                    1
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
                            val player = ctx.source.playerOrException
                            val element = PlayerElementData.getElement(player)
                            if (element == null) {
                                player.sendSystemMessage(
                                    Component.literal("Choose an element first: /element set <fire|water|earth|wind|shadow|light>")
                                )
                                0
                            } else {
                                sendAbilityGuide(player, element)
                                1
                            }
                        }
                )
        )
    }

    private fun useAbility(
        player: ServerPlayer,
        slot: AbilitySlot
    ): Int {

        return when (
            AbilityManager.use(player, slot)
        ) {

            AbilityResult.Success -> 1

            AbilityResult.NoElement -> {
                player.sendSystemMessage(
                    Component.literal(
                        "You haven't chosen an element yet. " +
                            "Use /element set <element>."
                    )
                )
                0
            }

            AbilityResult.OnCooldown -> {
                val remaining = (PlayerElementData.getCooldownEnd(player, slot) - player.level().gameTime)
                    .coerceAtLeast(0L) / 20.0
                player.sendSystemMessage(Component.literal("${slot.label()} is still recharging (${"%.1f".format(remaining)}s left)."))
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
                        "You need your conduit weapon " +
                            "(${element?.conduitWeaponName ?: "?"}) " +
                            "in hand to use your Conduit Power."
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
                        "No element chosen. " +
                            "Use /element set <element>."
                    )
                },
                false
            )

            return 1
        }

        val essence =
            PlayerElementData.getEssence(player)

        val level =
            ElementalEnchantments.levelOnHeldItem(
                player,
                element
            )

        source.sendSuccess(
            {
                Component.literal(
                    "Element: ${element.id} | " +
                        "Conduit: ${element.conduitWeaponName} | " +
                        "Essence: ${essence.toInt()}/" +
                        "${EssenceConfig.MAX_ESSENCE.toInt()} | " +
                        "Enchant level: $level\n" +
                        AbilitySlot.entries.joinToString(" | ") { slot ->
                            val remaining = (PlayerElementData.getCooldownEnd(player, slot) - player.level().gameTime)
                                .coerceAtLeast(0L) / 20.0
                            "${slot.label()}: ${if (remaining <= 0.0) "READY" else "%.1fs".format(remaining)}"
                        }
                )
            },
            false
        )

        return 1
    }

    private fun sendAbilityGuide(player: ServerPlayer, element: Element) {
        player.sendSystemMessage(Component.literal("Your ${element.id.replaceFirstChar { it.uppercase() }} abilities (Essence costs: 10 / 15 / 20 / 30; cooldowns: 3 / 6 / 10 / 15s):"))
        AbilitySlot.entries.forEach { slot ->
            val help = AbilityHelp.forAbility(element, slot)
            val command = when (slot) {
                AbilitySlot.POWER1 -> "/element power1"
                AbilitySlot.POWER2 -> "/element power2"
                AbilitySlot.POWER3 -> "/element power3"
                AbilitySlot.CONDUIT -> "/element conduit"
            }
            val requirement = if (slot == AbilitySlot.CONDUIT) " (hold ${element.conduitWeaponName})" else ""
            player.sendSystemMessage(Component.literal("${slot.label()} — ${help.name}: ${help.description} [$command]$requirement"))
        }
        player.sendSystemMessage(Component.literal("Use /element status to check Essence and cooldowns. The action bar also shows them during play. Use /element help to show this again."))
    }

    private fun AbilitySlot.label(): String = when (this) {
        AbilitySlot.POWER1 -> "Power 1"
        AbilitySlot.POWER2 -> "Power 2"
        AbilitySlot.POWER3 -> "Power 3"
        AbilitySlot.CONDUIT -> "Conduit"
    }
}
