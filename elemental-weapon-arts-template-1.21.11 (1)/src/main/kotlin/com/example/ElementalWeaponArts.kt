package com.example

import com.example.command.ElementCommands
import com.example.element.AbilitySlot
import com.example.element.EssenceConfig
import com.example.element.PlayerElementData
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

object ElementalWeaponArts : ModInitializer {

    const val MOD_ID: String = "elemental-weapon-arts"

    private val LOGGER =
        LoggerFactory.getLogger(MOD_ID)

    private val elementDataInit =
        PlayerElementData

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

            if (tickCounter >= 20) tickCounter = 0
            if (tickCounter % 5 == 0) {

                for (
                    player: ServerPlayer
                    in server.playerList.players
                ) {

                    val current =
                        PlayerElementData.getEssence(
                            player
                        )

                    if (tickCounter == 0 &&
                        current <
                        EssenceConfig.MAX_ESSENCE
                    ) {

                        PlayerElementData.setEssence(
                            player,
                            current +
                                EssenceConfig.REGEN_PER_SECOND
                        )
                    }

                    val element = PlayerElementData.getElement(player) ?: continue
                    val now = player.level().gameTime
                    val cooldowns = AbilitySlot.entries.joinToString("  ") { slot ->
                        val end = PlayerElementData.getCooldownEnd(player, slot)
                        val remaining = (end - now).coerceAtLeast(0L) / 20.0
                        val shortName = when (slot) {
                            AbilitySlot.POWER1 -> "P1"
                            AbilitySlot.POWER2 -> "P2"
                            AbilitySlot.POWER3 -> "P3"
                            AbilitySlot.CONDUIT -> "C"
                        }
                        "$shortName ${if (remaining <= 0.0) "READY" else "%.1fs".format(remaining)}"
                    }
                    val essence = PlayerElementData.getEssence(player).toInt()
                    player.sendSystemMessage(
                        Component.literal("Essence $essence/${EssenceConfig.MAX_ESSENCE.toInt()}  |  $cooldowns"),
                        true
                    )
                }
            }
        }
    }

    fun id(path: String): Identifier =
        Identifier.fromNamespaceAndPath(
            MOD_ID,
            path
        )
}
