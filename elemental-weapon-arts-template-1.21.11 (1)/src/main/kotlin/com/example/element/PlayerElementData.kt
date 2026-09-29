package com.example.element

import com.example.ElementalWeaponArts
import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.world.entity.player.Player

object PlayerElementData {

    val ELEMENT: AttachmentType<String> =
        AttachmentRegistry.create(ElementalWeaponArts.id("element")) {
            it.persistent(Codec.STRING)
        }

    val ESSENCE: AttachmentType<Float> =
        AttachmentRegistry.create(ElementalWeaponArts.id("essence")) {
            it.persistent(Codec.FLOAT)
                .initializer { EssenceConfig.MAX_ESSENCE }
        }

    val COOLDOWN_POWER1: AttachmentType<Long> =
        cooldownAttachment("cooldown_power1")

    val COOLDOWN_POWER2: AttachmentType<Long> =
        cooldownAttachment("cooldown_power2")

    val COOLDOWN_POWER3: AttachmentType<Long> =
        cooldownAttachment("cooldown_power3")

    val COOLDOWN_CONDUIT: AttachmentType<Long> =
        cooldownAttachment("cooldown_conduit")

    private fun cooldownAttachment(path: String): AttachmentType<Long> =
        AttachmentRegistry.create(ElementalWeaponArts.id(path)) {
            it.persistent(Codec.LONG)
                .initializer { 0L }
        }

    fun getElement(player: Player): Element? {
        val raw = player.getAttachedOrCreate(ELEMENT) { "" }
        return Element.fromId(raw)
    }

    fun setElement(player: Player, element: Element) {
        player.setAttached(ELEMENT, element.id)

        player.setAttached(
            ESSENCE,
            EssenceConfig.MAX_ESSENCE
        )

        player.setAttached(COOLDOWN_POWER1, 0L)
        player.setAttached(COOLDOWN_POWER2, 0L)
        player.setAttached(COOLDOWN_POWER3, 0L)
        player.setAttached(COOLDOWN_CONDUIT, 0L)
    }

    fun getEssence(player: Player): Float =
        player.getAttachedOrCreate(ESSENCE) {
            EssenceConfig.MAX_ESSENCE
        }

    fun setEssence(player: Player, value: Float) {
        player.setAttached(
            ESSENCE,
            value.coerceIn(
                0f,
                EssenceConfig.MAX_ESSENCE
            )
        )
    }

    fun cooldownAttachmentFor(
        slot: AbilitySlot
    ): AttachmentType<Long> =
        when (slot) {
            AbilitySlot.POWER1 -> COOLDOWN_POWER1
            AbilitySlot.POWER2 -> COOLDOWN_POWER2
            AbilitySlot.POWER3 -> COOLDOWN_POWER3
            AbilitySlot.CONDUIT -> COOLDOWN_CONDUIT
        }

    fun getCooldownEnd(
        player: Player,
        slot: AbilitySlot
    ): Long =
        player.getAttachedOrCreate(
            cooldownAttachmentFor(slot)
        ) {
            0L
        }

    fun setCooldownEnd(
        player: Player,
        slot: AbilitySlot,
        worldTime: Long
    ) {
        player.setAttached(
            cooldownAttachmentFor(slot),
            worldTime
        )
    }
}

enum class AbilitySlot {
    POWER1,
    POWER2,
    POWER3,
    CONDUIT
}

object EssenceConfig {

    const val MAX_ESSENCE: Float = 100f

    const val REGEN_PER_SECOND: Float = 2f
}
