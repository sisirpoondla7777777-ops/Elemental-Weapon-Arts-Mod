package com.example.ability

import com.example.element.AbilitySlot
import com.example.element.Element

data class AbilityHelp(val name: String, val description: String) {
    companion object {
        fun forAbility(element: Element, slot: AbilitySlot): AbilityHelp = when (element) {
            Element.FIRE -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Fire Blast", "hits and ignites enemies ahead")
                AbilitySlot.POWER2 -> AbilityHelp("Flame Dash", "dash forward in a burst of flame")
                AbilitySlot.POWER3 -> AbilityHelp("Flame Guard", "gain fire resistance and absorption; burn nearby enemies")
                AbilitySlot.CONDUIT -> AbilityHelp("Inferno Slash", "strike and ignite enemies in a short arc ahead")
            }
            Element.WATER -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Water Blast", "damage and push enemies ahead")
                AbilitySlot.POWER2 -> AbilityHelp("Water Flow", "dash forward with water particles")
                AbilitySlot.POWER3 -> AbilityHelp("Water Guard", "gain absorption and push nearby enemies away")
                AbilitySlot.CONDUIT -> AbilityHelp("Tidal Spear", "deal heavy damage and knock back enemies ahead")
            }
            Element.EARTH -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Earth Shot", "damage and knock back an enemy ahead")
                AbilitySlot.POWER2 -> AbilityHelp("Earth Burst", "launch yourself and knock back nearby enemies")
                AbilitySlot.POWER3 -> AbilityHelp("Stone Guard", "gain strong resistance briefly")
                AbilitySlot.CONDUIT -> AbilityHelp("Seismic Strike", "slam the ground to damage and launch nearby enemies")
            }
            Element.WIND -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Wind Shot", "damage and strongly knock back enemies ahead")
                AbilitySlot.POWER2 -> AbilityHelp("Wind Step", "dash forward and slow your fall")
                AbilitySlot.POWER3 -> AbilityHelp("Wind Guard", "push nearby enemies away")
                AbilitySlot.CONDUIT -> AbilityHelp("Gale Shot", "hit distant enemies ahead with a powerful knockback")
            }
            Element.SHADOW -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Shadow Bolt", "damage and briefly blind enemies ahead")
                AbilitySlot.POWER2 -> AbilityHelp("Shadow Step", "blink forward a short distance")
                AbilitySlot.POWER3 -> AbilityHelp("Shadow Veil", "become invisible briefly")
                AbilitySlot.CONDUIT -> AbilityHelp("Shadow Bolt", "damage, blind, and weaken enemies at range")
            }
            Element.LIGHT -> when (slot) {
                AbilitySlot.POWER1 -> AbilityHelp("Light Bolt", "damage enemies ahead, with extra damage to undead")
                AbilitySlot.POWER2 -> AbilityHelp("Light Step", "dash forward in a burst of light")
                AbilitySlot.POWER3 -> AbilityHelp("Radiant Guard", "gain absorption and push nearby enemies away")
                AbilitySlot.CONDUIT -> AbilityHelp("Radiant Cleave", "deal heavy damage to enemies ahead")
            }
        }
    }
}
