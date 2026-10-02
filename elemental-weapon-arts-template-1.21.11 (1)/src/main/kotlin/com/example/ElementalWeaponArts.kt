// src/main/kotlin/com/example/ElementalWeaponArts.kt

package com.example

import com.example.command.ElementCommands
import com.example.element.EssenceConfig
import com.example.element.PlayerElementData
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object ElementalWeaponArts : ModInitializer {

    const val MOD_ID = "elemental-weapon-arts"

    private val LOGGER =
        LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        LOGGER.info(
            "Elemental Weapon Arts initializing"
        )

        CommandRegistrationCallback.EVENT.register {
                dispatcher,
                context,
                _ ->
            ElementCommands.register(
                dispatcher,
                context
            )
        }

        var tickCounter = 0

        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++

            if (tickCounter >= 20) {
                tickCounter = 0

                server.playerList.players.forEach { player ->
                    val essence =
                        PlayerElementData.getEssence(player)

                    if (
                        essence <
                        EssenceConfig.MAX_ESSENCE
                    ) {
                        PlayerElementData.setEssence(
                            player,
                            essence +
                                EssenceConfig.REGEN_PER_SECOND
                        )
                    }
                }
            }

            if (tickCounter % 5 == 0) {
                server.playerList.players.forEach { player ->
                    sendActionBar(player)
                }
            }
        }
    }

    private fun sendActionBar(
        player: net.minecraft.server.level.ServerPlayer
    ) {
        val essence =
            PlayerElementData.getEssence(player).toInt()

        val element =
            PlayerElementData.getElement(player)
                ?: return

        val now =
            player.level().gameTime

        val cooldowns =
            com.example.element.AbilitySlot.entries.joinToString("  ") { slot ->
                val remaining =
                    (
                        PlayerElementData.getCooldownEnd(
                            player,
                            slot
                        ) - now
                    )
                        .coerceAtLeast(0L)

                val name =
                    when (slot) {
                        com.example.element.AbilitySlot.POWER1 -> "P1"
                        com.example.element.AbilitySlot.POWER2 -> "P2"
                        com.example.element.AbilitySlot.POWER3 -> "P3"
                        com.example.element.AbilitySlot.CONDUIT -> "C"
                    }

                if (remaining == 0L) {
                    "$name READY"
                } else {
                    "$name ${"%.1f".format(remaining / 20.0)}s"
                }
            }

        player.sendSystemMessage(
            Component.literal(
                "${element.id.uppercase()}  " +
                    "Essence $essence/100  |  $cooldowns"
            ),
            true
        )
    }

    fun id(
        path: String
    ): Identifier =
        Identifier.fromNamespaceAndPath(
            MOD_ID,
            path
        )
}
