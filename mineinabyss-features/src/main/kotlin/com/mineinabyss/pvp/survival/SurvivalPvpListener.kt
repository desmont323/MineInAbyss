package com.mineinabyss.pvp.survival

import com.mineinabyss.components.playerData
import com.mineinabyss.deeperworld.event.PlayerDescendEvent
import com.mineinabyss.idofront.messaging.error
import com.mineinabyss.mineinabyss.core.layer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class SurvivalPvpListener : Listener {
    @EventHandler
    fun PlayerDescendEvent.onEnterPvPLayer() {
        if (toSection.layer?.hasPvpDefault == true && !player.playerData.pvpStatus) {
            player.error("PVP is always enabled below this point.")
        }
    }
}
