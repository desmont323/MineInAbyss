package com.mineinabyss.anticheese

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.block.AnvilDamagedEvent
import com.mineinabyss.helpers.handleCurse
import com.mineinabyss.hubstorage.isInHub
import com.mineinabyss.idofront.messaging.error
import com.mineinabyss.mineinabyss.core.layer
import dev.geco.gsit.api.GSitAPI
import dev.geco.gsit.api.event.EntityGetUpSitEvent
import dev.geco.gsit.api.event.EntitySitEvent
import dev.geco.gsit.api.event.PreEntitySitEvent
import org.bukkit.Material
import org.bukkit.block.Dispenser
import org.bukkit.block.data.Directional
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.potion.PotionEffectType

class AntiCheeseListener : Listener {

    @EventHandler
    fun BlockSpreadEvent.onSculkSpread() {
        if (block.type == Material.SCULK || block.type == Material.SCULK_VEIN) isCancelled = true
    }

    @EventHandler
    fun AnvilDamagedEvent.onDamageAnvilOrth() {
        if ((viewers.firstOrNull() as? Player)?.isInHub() == true) isCancelled = true
    }

    @EventHandler
    fun BlockPlaceEvent.preventPlacement() {
        if (player.location.layer?.blockBlacklist?.contains(blockPlaced.type) == true) {
            player.error("You may not place this block on this layer.")
            isCancelled = true
        }
    }

    @EventHandler
    fun EntityPotionEffectEvent.onPlayerHit() {
        val player = entity as? Player ?: return
        if (cause != Cause.PLUGIN && cause != Cause.COMMAND) {
            if (newEffect?.type == PotionEffectType.DAMAGE_RESISTANCE) {
                isCancelled = true
                player.error("The <b>Resistance Effect</b> has been disabled")
            } else if (newEffect?.type == PotionEffectType.SLOW_FALLING) {
                isCancelled = true
                player.error("<b>Slow Falling</b> has been disabled")
            }
        }
        else if (cause == Cause.MILK) {
            isCancelled = true
            player.error("<b>Milk</b> has been disabled")
        }
    }

    @EventHandler
    fun BlockDispenseEvent.preventBackpackPlace() {
        if (MaterialTags.SHULKER_BOXES.isTagged(item)) {
            val inv = (block.state as Dispenser).inventory.contents ?: return
            val relative = block.getRelative((block.blockData as Directional).facing)
            if (relative.isSolid || !relative.isReplaceable) return

            inv.forEach {
                if (it != item) return@forEach
                it.subtract(1)
            }

            isCancelled = true
            block.world.dropItemNaturally(relative.location, item)
        }
    }

    @EventHandler
    fun EntityDamageByEntityEvent.cancelMinecartTNT() {
        if (damager is ExplosiveMinecart) isCancelled = true
    }

    // Cancels moving entities with fishing rods in Orth
    @EventHandler
    fun PlayerFishEvent.cancelBlockGrief() {
        if (caught?.type != EntityType.PLAYER && player.isInHub()) isCancelled = true
    }
}

class GSitListener : Listener {

    // Cancels pistons if a player is riding it via a GSit Seat
    @EventHandler
    fun BlockPistonExtendEvent.seatMovedByPiston() {
        if (GSitAPI.getSeats(blocks).isNotEmpty()) isCancelled = true
    }

    @EventHandler
    fun PreEntitySitEvent.onSitMidair() {
        if ((entity as? Player ?: return).fallDistance >= 4.0) isCancelled = true
    }

    @EventHandler
    fun EntitySitEvent.handleCurseOnSitting() {
        val player = (entity as? Player ?: return)
        handleCurse(player, seat.location.toBlockLocation(), player.location)
    }

    @EventHandler
    fun EntityGetUpSitEvent.handleCurseOnSitting() {
        val player = (entity as? Player ?: return)
        handleCurse(player, player.location, seat.location.toBlockLocation())
    }
}

