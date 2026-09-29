package com.example

import com.example.command.ElementCommands
import com.example.element.EssenceConfig
import com.example.element.PlayerElementData
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.resources.Identifier
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

            if (tickCounter >= 20) {

                tickCounter = 0

                for (
                    player: ServerPlayer
                    in server.playerList.players
                ) {

                    val current =
                        PlayerElementData.getEssence(
                            player
                        )

                    if (
                        current <
                        EssenceConfig.MAX_ESSENCE
                    ) {

                        PlayerElementData.setEssence(
                            player,
                            current +
                                EssenceConfig.REGEN_PER_SECOND
                        )
                    }
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
