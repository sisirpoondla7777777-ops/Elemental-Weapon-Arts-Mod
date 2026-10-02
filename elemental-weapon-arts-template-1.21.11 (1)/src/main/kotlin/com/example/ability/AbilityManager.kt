// src/main/kotlin/com/example/ability/AbilityManager.kt

package com.example.ability

import com.example.element.AbilitySlot
import com.example.element.Element
import com.example.element.PlayerElementData
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

enum class AbilityResult {
    Success,
    NoElement,
    OnCooldown,
    NotEnoughEssence,
    WrongWeapon
}

object AbilityManager {

    private const val POWER_1_COST = 10.0f
    private const val POWER_2_COST = 15.0f
    private const val POWER_3_COST = 20.0f
    private const val CONDUIT_COST = 30.0f

    private const val POWER_1_COOLDOWN = 60L
    private const val POWER_2_COOLDOWN = 120L
    private const val POWER_3_COOLDOWN = 200L
    private const val CONDUIT_COOLDOWN = 300L

    fun baseCost(slot: AbilitySlot): Float = when (slot) {
        AbilitySlot.POWER1 -> POWER_1_COST
        AbilitySlot.POWER2 -> POWER_2_COST
        AbilitySlot.POWER3 -> POWER_3_COST
        AbilitySlot.CONDUIT -> CONDUIT_COST
    }

    fun baseCooldown(slot: AbilitySlot): Long = when (slot) {
        AbilitySlot.POWER1 -> POWER_1_COOLDOWN
        AbilitySlot.POWER2 -> POWER_2_COOLDOWN
        AbilitySlot.POWER3 -> POWER_3_COOLDOWN
        AbilitySlot.CONDUIT -> CONDUIT_COOLDOWN
    }

    fun discountedCost(player: ServerPlayer, element: Element, slot: AbilitySlot): Float {
        val level = ElementalEnchantments.levelOnHeldItem(player, element)
        return baseCost(slot) * (1f - level * 0.05f)
    }

    fun discountedCooldown(player: ServerPlayer, element: Element, slot: AbilitySlot): Long {
        val level = ElementalEnchantments.levelOnHeldItem(player, element)
        return (baseCooldown(slot) * (1.0 - level * 0.05)).toLong()
    }

    fun use(player: ServerPlayer, slot: AbilitySlot): AbilityResult {
        val element = PlayerElementData.getElement(player)
            ?: return AbilityResult.NoElement

        if (
            slot == AbilitySlot.CONDUIT &&
            !element.isConduitWeapon(player.mainHandItem.item)
        ) {
            return AbilityResult.WrongWeapon
        }

        val now = player.level().gameTime
        val cooldownEnd = PlayerElementData.getCooldownEnd(player, slot)

        if (now < cooldownEnd) {
            return AbilityResult.OnCooldown
        }

        val cost = discountedCost(player, element, slot)
        val essence = PlayerElementData.getEssence(player)

        if (essence < cost) {
            return AbilityResult.NotEnoughEssence
        }

        PlayerElementData.setEssence(player, essence - cost)

        PlayerElementData.setCooldownEnd(
            player,
            slot,
            now + discountedCooldown(player, element, slot)
        )

        execute(player, element, slot)

        return AbilityResult.Success
    }

    private fun execute(
        player: ServerPlayer,
        element: Element,
        slot: AbilitySlot
    ) {
        when (element) {
            Element.FIRE -> executeFire(player, slot)
            Element.WATER -> executeWater(player, slot)
            Element.EARTH -> executeEarth(player, slot)
            Element.WIND -> executeWind(player, slot)
            Element.SHADOW -> executeShadow(player, slot)
            Element.LIGHT -> executeLight(player, slot)
        }
    }

    private fun executeFire(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    12.0,
                    5.0f,
                    0.8,
                    ParticleTypes.FLAME
                ) { target ->
                    target.setRemainingFireTicks(80)
                }
            }

            AbilitySlot.POWER2 -> {
                dash(player, 1.5)
                trail(player, ParticleTypes.FLAME, 12)
            }

            AbilitySlot.POWER3 -> {
                player.addEffect(
                    MobEffectInstance(
                        MobEffects.FIRE_RESISTANCE,
                        200,
                        0
                    )
                )

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        160,
                        1
                    )
                )

                area(
                    player,
                    4.0
                ).forEach { target ->
                    target.setRemainingFireTicks(100)
                    pushAway(player, target, 0.45)
                }

                particles(
                    player,
                    ParticleTypes.FLAME,
                    35,
                    2.5
                )
            }

            AbilitySlot.CONDUIT -> {
                arcAttack(
                    player,
                    7.0f,
                    5.0,
                    ParticleTypes.FLAME
                ) { target ->
                    target.setRemainingFireTicks(120)
                }
            }
        }
    }

    private fun executeWater(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    12.0,
                    5.0f,
                    1.0,
                    ParticleTypes.SPLASH
                ) { target ->
                    pushAway(player, target, 0.8)
                }
            }

            AbilitySlot.POWER2 -> {
                dash(player, 1.35)
                trail(player, ParticleTypes.SPLASH, 14)
            }

            AbilitySlot.POWER3 -> {
                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        200,
                        1
                    )
                )

                area(player, 5.0).forEach { target ->
                    pushAway(player, target, 1.0)
                }

                particles(
                    player,
                    ParticleTypes.SPLASH,
                    45,
                    2.5
                )
            }

            AbilitySlot.CONDUIT -> {
                rayAttack(
                    player,
                    14.0,
                    10.0f,
                    1.6,
                    ParticleTypes.SPLASH
                ) { target ->
                    pushAway(player, target, 1.5)
                }
            }
        }
    }

    private fun executeEarth(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    10.0,
                    6.0f,
                    1.2,
                    ParticleTypes.POOF
                ) { target ->
                    pushAway(player, target, 1.0)
                }
            }

            AbilitySlot.POWER2 -> {
                player.setDeltaMovement(
                    player.deltaMovement.x,
                    1.15,
                    player.deltaMovement.z
                )
                player.hurtMarked = true

                area(player, 4.0).forEach { target ->
                    pushAway(player, target, 0.9)
                }

                particles(
                    player,
                    ParticleTypes.BLOCK,
                    30,
                    2.0
                )
            }

            AbilitySlot.POWER3 -> {
                player.addEffect(
                    MobEffectInstance(
                        MobEffects.RESISTANCE,
                        220,
                        1
                    )
                )

                particles(
                    player,
                    ParticleTypes.POOF,
                    40,
                    2.5
                )
            }

            AbilitySlot.CONDUIT -> {
                area(player, 6.0).forEach { target ->
                    damage(player, target, 9.0f)
                    launch(target, 1.4)
                }

                particles(
                    player,
                    ParticleTypes.POOF,
                    80,
                    4.0
                )
            }
        }
    }

    private fun executeWind(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    14.0,
                    4.0f,
                    2.0,
                    ParticleTypes.CLOUD
                ) { target ->
                    pushAway(player, target, 1.8)
                }
            }

            AbilitySlot.POWER2 -> {
                dash(player, 1.7)

                player.addEffect(
                    MobEffectInstance(
                        MobEffects.SLOW_FALLING,
                        100,
                        0
                    )
                )

                trail(player, ParticleTypes.CLOUD, 18)
            }

            AbilitySlot.POWER3 -> {
                area(player, 6.0).forEach { target ->
                    pushAway(player, target, 1.8)
                }

                particles(
                    player,
                    ParticleTypes.CLOUD,
                    55,
                    3.5
                )
            }

            AbilitySlot.CONDUIT -> {
                rayAttack(
                    player,
                    20.0,
                    8.0f,
                    3.0,
                    ParticleTypes.CLOUD
                ) { target ->
                    pushAway(player, target, 3.0)
                }
            }
        }
    }

    private fun executeShadow(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    12.0,
                    5.0f,
                    0.6,
                    ParticleTypes.SMOKE
                ) { target ->
                    target.addEffect(
                        MobEffectInstance(
                            MobEffects.BLINDNESS,
                            80,
                            0
                        )
                    )
                }
            }

            AbilitySlot.POWER2 -> {
                blink(player, 6.0)
                particles(
                    player,
                    ParticleTypes.PORTAL,
                    35,
                    1.5
                )
            }

            AbilitySlot.POWER3 -> {
                player.addEffect(
                    MobEffectInstance(
                        MobEffects.INVISIBILITY,
                        200,
                        0
                    )
                )

                particles(
                    player,
                    ParticleTypes.SMOKE,
                    35,
                    2.0
                )
            }

            AbilitySlot.CONDUIT -> {
                rayAttack(
                    player,
                    18.0,
                    9.0f,
                    0.8,
                    ParticleTypes.SOUL
                ) { target ->
                    target.addEffect(
                        MobEffectInstance(
                            MobEffects.BLINDNESS,
                            100,
                            0
                        )
                    )

                    target.addEffect(
                        MobEffectInstance(
                            MobEffects.WEAKNESS,
                            140,
                            0
                        )
                    )
                }
            }
        }
    }

    private fun executeLight(player: ServerPlayer, slot: AbilitySlot) {
        when (slot) {
            AbilitySlot.POWER1 -> {
                rayAttack(
                    player,
                    12.0,
                    5.0f,
                    0.8,
                    ParticleTypes.END_ROD
                ) { target ->
                    if (target.type().is(net.minecraft.tags.EntityTypeTags.UNDEAD)) {
                        damage(player, target, 4.0f)
                    }
                }
            }

            AbilitySlot.POWER2 -> {
                dash(player, 1.5)
                trail(player, ParticleTypes.END_ROD, 16)
            }

            AbilitySlot.POWER3 -> {
                player.addEffect(
                    MobEffectInstance(
                        MobEffects.ABSORPTION,
                        200,
                        1
                    )
                )

                area(player, 5.0).forEach { target ->
                    pushAway(player, target, 1.2)
                }

                particles(
                    player,
                    ParticleTypes.END_ROD,
                    45,
                    3.0
                )
            }

            AbilitySlot.CONDUIT -> {
                arcAttack(
                    player,
                    11.0f,
                    5.5,
                    ParticleTypes.END_ROD
                )
            }
        }
    }

    private fun rayAttack(
        player: ServerPlayer,
        range: Double,
        damage: Float,
        knockback: Double,
        particle: net.minecraft.core.particles.ParticleOptions,
        afterHit: (LivingEntity) -> Unit = {}
    ) {
        val level = player.level()
        val start = player.eyePosition
        val direction = player.lookAngle.normalize()
        val end = start.add(direction.scale(range))

        var closest: LivingEntity? = null
        var closestDistance = Double.MAX_VALUE

        val box = player.boundingBox
            .expandTowards(direction.scale(range))
            .inflate(1.0)

        for (entity in level.getEntitiesOfClass(
            LivingEntity::class.java,
            box
        )) {
            if (entity === player || !entity.isAlive) continue

            val distance = player.distanceTo(entity)

            if (distance > range) continue

            val hit = entity.boundingBox.clip(start, end)

            if (hit.isPresent && distance < closestDistance) {
                closest = entity
                closestDistance = distance
            }
        }

        spawnLineParticles(
            level,
            start,
            if (closest != null) closest!!.position().add(0.0, closest!!.bbHeight / 2.0, 0.0) else end,
            particle
        )

        closest?.let {
            damage(player, it, damage)
            pushAway(player, it, knockback)
            afterHit(it)
        }
    }

    private fun arcAttack(
        player: ServerPlayer,
        damage: Float,
        radius: Double,
        particle: net.minecraft.core.particles.ParticleOptions,
        afterHit: (LivingEntity) -> Unit = {}
    ) {
        val direction = player.lookAngle.normalize()
        val center = player.position().add(direction.scale(2.0))

        area(
            player,
            radius
        ).forEach { target ->
            val toTarget = target.position()
                .subtract(player.position())
                .normalize()

            if (direction.dot(toTarget) > 0.15) {
                damage(player, target, damage)
                pushAway(player, target, 1.2)
                afterHit(target)
            }
        }

        particles(
            player,
            particle,
            80,
            radius
        )
    }

    private fun damage(
        player: ServerPlayer,
        target: LivingEntity,
        amount: Float
    ) {
        target.hurtServer(
            player.level(),
            player.damageSources().playerAttack(player),
            amount
        )
    }

    private fun area(
        player: ServerPlayer,
        radius: Double
    ): List<LivingEntity> {
        return player.level().getEntitiesOfClass(
            LivingEntity::class.java,
            player.boundingBox.inflate(radius)
        ).filter {
            it !== player && it.isAlive
        }
    }

    private fun pushAway(
        player: ServerPlayer,
        target: LivingEntity,
        strength: Double
    ) {
        val direction = target.position()
            .subtract(player.position())

        val horizontal = Vec3(
            direction.x,
            0.0,
            direction.z
        )

        if (horizontal.lengthSqr() <= 0.0001) return

        val normalized = horizontal.normalize()

        target.setDeltaMovement(
            normalized.x * strength,
            0.35,
            normalized.z * strength
        )

        target.hurtMarked = true
    }

    private fun launch(
        target: LivingEntity,
        strength: Double
    ) {
        target.setDeltaMovement(
            target.deltaMovement.x,
            strength,
            target.deltaMovement.z
        )

        target.hurtMarked = true
    }

    private fun dash(
        player: ServerPlayer,
        strength: Double
    ) {
        val direction = player.lookAngle.normalize()

        player.setDeltaMovement(
            direction.x * strength,
            player.deltaMovement.y.coerceAtLeast(0.15),
            direction.z * strength
        )

        player.hurtMarked = true
    }

    private fun blink(
        player: ServerPlayer,
        distance: Double
    ) {
        val direction = player.lookAngle.normalize()
        val start = player.position()
        val desired = start.add(
            direction.x * distance,
            direction.y * distance,
            direction.z * distance
        )

        val destination = findSafeBlinkPosition(
            player,
            start,
            desired
        )

        player.teleportTo(
            destination.x,
            destination.y,
            destination.z
        )
    }

    private fun findSafeBlinkPosition(
        player: ServerPlayer,
        start: Vec3,
        desired: Vec3
    ): Vec3 {
        val level = player.level()
        val steps = 12

        var safe = start

        for (i in 1..steps) {
            val progress = i.toDouble() / steps
            val position = start.lerp(desired, progress)

            if (
                level.noCollision(
                    player,
                    player.boundingBox.move(
                        position.x - start.x,
                        position.y - start.y,
                        position.z - start.z
                    )
                )
            ) {
                safe = position
            } else {
                break
            }
        }

        return safe.add(0.0, 0.05, 0.0)
    }

    private fun trail(
        player: ServerPlayer,
        particle: net.minecraft.core.particles.ParticleOptions,
        count: Int
    ) {
        particles(
            player,
            particle,
            count,
            1.5
        )
    }

    private fun particles(
        player: ServerPlayer,
        particle: net.minecraft.core.particles.ParticleOptions,
        count: Int,
        radius: Double
    ) {
        val level = player.level()

        if (level is ServerLevel) {
            level.sendParticles(
                particle,
                player.x,
                player.y + 1.0,
                player.z,
                count,
                radius,
                radius / 2.0,
                radius,
                0.05
            )
        }
    }

    private fun spawnLineParticles(
        level: ServerLevel,
        start: Vec3,
        end: Vec3,
        particle: net.minecraft.core.particles.ParticleOptions
    ) {
        val distance = start.distanceTo(end)
        val steps = (distance * 4.0).toInt().coerceIn(4, 80)

        for (i in 0..steps) {
            val progress = i.toDouble() / steps
            val point = start.lerp(end, progress)

            level.sendParticles(
                particle,
                point.x,
                point.y,
                point.z,
                1,
                0.0,
                0.0,
                0.0,
                0.0
            )
        }
    }
}
