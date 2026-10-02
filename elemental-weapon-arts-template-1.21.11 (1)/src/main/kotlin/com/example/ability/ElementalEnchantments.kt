// src/main/kotlin/com/example/ability/ElementalEnchantments.kt

package com.example.ability

import com.example.ElementalWeaponArts
import com.example.element.Element
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

object ElementalEnchantments {

    private fun keyFor(
        element: Element
    ): ResourceKey<Enchantment> =
        ResourceKey.create(
            Registries.ENCHANTMENT,
            ElementalWeaponArts.id(element.enchantmentPath)
        )

    fun levelOnHeldItem(
        player: ServerPlayer,
        element: Element
    ): Int {
        val registry =
            player.level()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)

        val holder: Holder<Enchantment> =
            registry
                .get(keyFor(element))
                .orElse(null)
                ?: return 0

        return EnchantmentHelper.getItemEnchantmentLevel(
            holder,
            player.mainHandItem
        )
    }

    fun discountMultiplier(level: Int): Double =
        (1.0 - (level.coerceIn(0, 3) * 0.05))
}
