// src/main/kotlin/com/example/element/Element.kt

package com.example.element

import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

enum class Element(
    val id: String,
    val enchantmentPath: String
) {
    FIRE("fire", "flame_conduit"),
    WATER("water", "tidal_conduit"),
    EARTH("earth", "terra_conduit"),
    WIND("wind", "gale_conduit"),
    SHADOW("shadow", "shadow_conduit"),
    LIGHT("light", "radiant_conduit");

    fun isConduitWeapon(item: Item): Boolean =
        when (this) {
            FIRE ->
                item == Items.WOODEN_SWORD ||
                item == Items.STONE_SWORD ||
                item == Items.GOLDEN_SWORD ||
                item == Items.IRON_SWORD ||
                item == Items.DIAMOND_SWORD ||
                item == Items.NETHERITE_SWORD

            WATER ->
                item == Items.TRIDENT

            EARTH ->
                item == Items.MACE

            WIND ->
                item == Items.BOW

            SHADOW ->
                item == Items.CROSSBOW

            LIGHT ->
                item == Items.WOODEN_AXE ||
                item == Items.STONE_AXE ||
                item == Items.GOLDEN_AXE ||
                item == Items.IRON_AXE ||
                item == Items.DIAMOND_AXE ||
                item == Items.NETHERITE_AXE
        }

    val conduitWeaponName: String
        get() =
            when (this) {
                FIRE -> "sword"
                WATER -> "trident"
                EARTH -> "mace"
                WIND -> "bow"
                SHADOW -> "crossbow"
                LIGHT -> "axe"
            }

    companion object {
        fun fromId(id: String): Element? =
            entries.firstOrNull {
                it.id.equals(id, ignoreCase = true)
            }
    }
}
