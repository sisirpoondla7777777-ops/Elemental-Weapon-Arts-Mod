package com.example.ability

import com.example.element.AbilitySlot
import com.example.element.Element
import com.example.element.PlayerElementData
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

data class AbilityConfig(
    val essenceCost: Float,
    val cooldownTicks: Long
)

object AbilityConfigs {

    val CONFIG =
        mapOf(

            AbilitySlot.POWER1 to
                AbilityConfig(
                    10f,
                    3 * 20L
                ),

            AbilitySlot.POWER2 to
                AbilityConfig(
                    15f,
                    6 * 20L
                ),

            AbilitySlot.POWER3 to
                AbilityConfig(
                    20f,
                    10 * 20L
                ),

            AbilitySlot.CONDUIT to
                AbilityConfig(
                    30f,
                    15 * 20L
                )
        )
}

sealed class AbilityResult {

    data object Success : AbilityResult()

    data object NoElement : AbilityResult()

    data object OnCooldown : AbilityResult()

    data object NotEnoughEssence : AbilityResult()

    data object WrongWeapon : AbilityResult()
}

object AbilityManager {

    fun use(
        player: ServerPlayer,
        slot: AbilitySlot
    ): AbilityResult {

        val element =
            PlayerElementData.getElement(player)
                ?: return AbilityResult.NoElement

        if (
            slot == AbilitySlot.CONDUIT &&
            !element.isConduitWeapon(
                player.mainHandItem.item
            )
        ) {
            return AbilityResult.WrongWeapon
        }

        val level =
            player.level()

        val now =
            level.gameTime

        val cooldownEnd =
            PlayerElementData.getCooldownEnd(
                player,
                slot
            )

        if (now < cooldownEnd) {
            return AbilityResult.OnCooldown
        }

        val base =
            AbilityConfigs.CONFIG.getValue(slot)

        val enchantLevel =
            ElementalEnchantments.levelOnHeldItem(
                player,
                element
            )

        val reduction =
            1f - enchantLevel * 0.05f

        val cost =
            base.essenceCost * reduction

        val essence =
            PlayerElementData.getEssence(player)

        if (essence < cost) {
            return AbilityResult.NotEnoughEssence
        }

        PlayerElementData.setEssence(
            player,
            essence - cost
        )

        val cooldownTicks =
            (
                base.cooldownTicks * reduction
            )
                .toLong()
                .coerceAtLeast(1L)

        PlayerElementData.setCooldownEnd(
            player,
            slot,
            now + cooldownTicks
        )

        runEffect(
            player,
            level,
            element,
            slot
        )

        return AbilityResult.Success
    }

    private fun runEffect(
        player: ServerPlayer,
        level: ServerLevel,
        element: Element,
        slot: AbilitySlot
    ) {

        player.swing(
            net.minecraft.world.InteractionHand.MAIN_HAND,
            true
        )

        when (element) {

            Element.FIRE ->
                FireAbilities.run(
                    player,
                    level,
                    slot
                )

            Element.WATER ->
                WaterAbilities.run(
                    player,
                    level,
                    slot
                )

            Element.EARTH ->
                EarthAbilities.run(
                    player,
                    level,
                    slot
                )

            Element.WIND ->
                WindAbilities.run(
                    player,
                    level,
                    slot
                )

            Element.SHADOW ->
                ShadowAbilities.run(
                    player,
                    level,
                    slot
                )

            Element.LIGHT ->
                LightAbilities.run(
                    player,
                    level,
                    slot
                )
        }
    }

    fun entitiesInFrontOf(
        player: ServerPlayer,
        range: Double
    ): List<LivingEntity> {

        val look =
            player.lookAngle.normalize()

        val box =
            player.boundingBox.inflate(range)

        return player.level()
            .getEntitiesOfClass(
                LivingEntity::class.java,
                box
            ) { entity ->

                entity !== player &&
                    entity !is ArmorStand &&
                    entity.distanceTo(player) <= range &&
                    Vec3(
                        entity.x - player.x,
                        0.0,
                        entity.z - player.z
                    )
                        .normalize()
                        .dot(
                            Vec3(
                                look.x,
                                0.0,
                                look.z
                            ).normalize()
                        ) > 0.5
            }
    }

    fun entitiesAround(
        player: ServerPlayer,
        radius: Double
    ): List<LivingEntity> {

        val box =
            player.boundingBox.inflate(radius)

        return player.level()
            .getEntitiesOfClass(
                LivingEntity::class.java,
                box
            ) {
                it !== player
            }
    }

    fun pushAwayFrom(
        player: ServerPlayer,
        target: LivingEntity,
        strength: Double
    ) {

        val dx =
            target.x - player.x

        val dz =
            target.z - player.z

        val distance =
            maxOf(
                0.1,
                kotlin.math.sqrt(
                    dx * dx + dz * dz
                )
            )

        target.push(
            dx / distance * strength,
            0.35 * strength,
            dz / distance * strength
        )
    }

    fun dash(
        player: ServerPlayer,
        strength: Double
    ) {

        val look =
            player.lookAngle.normalize()

        player.push(
            look.x * strength,
            0.15,
            look.z * strength
        )
    }

    fun applyEffect(
        entity: LivingEntity,
        effect: net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>,
        seconds: Int,
        amplifier: Int = 0
    ) {

        entity.addEffect(
            MobEffectInstance(
                effect,
                seconds * 20,
                amplifier
            )
        )
    }
}

/* ---------------- FIRE ---------------- */

private object FireAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.PLAYERS,
                    1f,
                    1f
                )

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        8.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            4f
                        )

                        it.igniteForSeconds(4f)
                    }
            }

            AbilitySlot.POWER2 -> {

                AbilityManager.dash(
                    player,
                    1.4
                )

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.FLAME
                )
            }

            AbilitySlot.POWER3 -> {

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE,
                        8 * 20
                    )
                )

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        8 * 20
                    )
                )

                AbilityManager
                    .entitiesAround(
                        player,
                        3.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            3f
                        )

                        it.igniteForSeconds(2f)
                    }
            }

            AbilitySlot.CONDUIT -> {

                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.PLAYERS,
                    1f,
                    0.7f
                )

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        4.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            9f
                        )

                        it.igniteForSeconds(5f)

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.6
                        )
                    }
            }
        }
}

/* ---------------- WATER ---------------- */

private object WaterAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        8.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            3f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.9
                        )
                    }

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.SPLASH
                )
            }

            AbilitySlot.POWER2 -> {

                AbilityManager.dash(
                    player,
                    1.3
                )

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.BUBBLE
                )
            }

            AbilitySlot.POWER3 -> {

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        8 * 20
                    )
                )

                AbilityManager
                    .entitiesAround(
                        player,
                        3.5
                    )
                    .forEach {

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.8
                        )
                    }
            }

            AbilitySlot.CONDUIT -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        5.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            10f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            1.1
                        )
                    }

                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.TRIDENT_THROW.value(),
                    SoundSource.PLAYERS,
                    1f,
                    1f
                )
            }
        }
}

/* ---------------- EARTH ---------------- */

private object EarthAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        8.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            4f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.6
                        )
                    }
            }

            AbilitySlot.POWER2 -> {

                AbilityManager
                    .entitiesAround(
                        player,
                        3.5
                    )
                    .forEach {

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.9
                        )
                    }

                player.push(
                    0.0,
                    0.5,
                    0.0
                )

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.CRIT
                )
            }

            AbilitySlot.POWER3 -> {

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.RESISTANCE,
                        8 * 20,
                        1
                    )
                )
            }

            AbilitySlot.CONDUIT -> {

                level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS,
                    0.6f,
                    1.4f
                )

                AbilityManager
                    .entitiesAround(
                        player,
                        4.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            11f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            1.2
                        )
                    }
            }
        }
}

/* ---------------- WIND ---------------- */

private object WindAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        10.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            3f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            1.4
                        )
                    }
            }

            AbilitySlot.POWER2 -> {

                AbilityManager.dash(
                    player,
                    1.6
                )

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        4 * 20
                    )
                )
            }

            AbilitySlot.POWER3 -> {

                AbilityManager
                    .entitiesAround(
                        player,
                        4.0
                    )
                    .forEach {

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            1.1
                        )
                    }
            }

            AbilitySlot.CONDUIT -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        10.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            8f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            1.8
                        )
                    }
            }
        }
}

/* ---------------- SHADOW ---------------- */

private object ShadowAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        8.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            4f
                        )

                        AbilityManager.applyEffect(
                            it,
                            MobEffects.BLINDNESS,
                            2
                        )
                    }

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.SMOKE
                )
            }

            AbilitySlot.POWER2 -> {

                val look =
                    player.lookAngle.normalize()

                player.teleportTo(
                    player.x + look.x * 5,
                    player.y,
                    player.z + look.z * 5
                )

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.SQUID_INK
                )
            }

            AbilitySlot.POWER3 -> {

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.INVISIBILITY,
                        5 * 20
                    )
                )
            }

            AbilitySlot.CONDUIT -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        12.0
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            10f
                        )

                        AbilityManager.applyEffect(
                            it,
                            MobEffects.BLINDNESS,
                            4
                        )

                        AbilityManager.applyEffect(
                            it,
                            MobEffects.WEAKNESS,
                            5
                        )
                    }
            }
        }
}

/* ---------------- LIGHT ---------------- */

private object LightAbilities {

    fun run(
        player: ServerPlayer,
        level: ServerLevel,
        slot: AbilitySlot
    ) =
        when (slot) {

            AbilitySlot.POWER1 -> {

                AbilityManager
                    .entitiesInFrontOf(
                        player,
                        8.0
                    )
                    .forEach {

                        val undead =
                            it.type.`is`(
                                net.minecraft.tags.EntityTypeTags.UNDEAD
                            )

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            if (undead) 6f else 4f
                        )
                    }

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.END_ROD
                )
            }

            AbilitySlot.POWER2 -> {

                AbilityManager.dash(
                    player,
                    1.4
                )

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.GLOW
                )
            }

            AbilitySlot.POWER3 -> {

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        8 * 20
                    )
                )

                AbilityManager
                    .entitiesAround(
                        player,
                        3.5
                    )
                    .forEach {

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.9
                        )
                    }
            }

            AbilitySlot.CONDUIT -> {

                AbilityManager
                    .entitiesAround(
                        player,
                        3.5
                    )
                    .forEach {

                        it.hurtServer(
                            level,
                            player.damageSources()
                                .playerAttack(player),
                            10f
                        )

                        AbilityManager.pushAwayFrom(
                            player,
                            it,
                            0.7
                        )
                    }

                spreadParticles(
                    level,
                    player,
                    ParticleTypes.END_ROD
                )
            }
        }
}

private fun spreadParticles(
    level: ServerLevel,
    player: ServerPlayer,
    particle: ParticleOptions
) {

    val look =
        player.lookAngle.normalize()

    level.sendParticles(
        particle,
        player.x + look.x,
        player.y + player.eyeHeight * 0.5,
        player.z + look.z,
        12,
        0.3,
        0.3,
        0.3,
        0.02
    )
}
