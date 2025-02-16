package com.mineinabyss.guilds

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.mineinabyss.chatty.components.chattyData
import com.mineinabyss.chatty.helpers.chattyConfig
import com.mineinabyss.chatty.helpers.getDefaultChat
import com.mineinabyss.chatty.listeners.RendererExtension
import com.mineinabyss.components.guilds.GuildMaster
import com.mineinabyss.geary.papermc.access.toGearyOrNull
import com.mineinabyss.guilds.database.GuildRank
import com.mineinabyss.guilds.extensions.getGuildChatId
import com.mineinabyss.guilds.extensions.getGuildName
import com.mineinabyss.guilds.extensions.getGuildRank
import com.mineinabyss.guilds.extensions.hasGuild
import com.mineinabyss.guilds.menus.GuildMainMenu
import com.mineinabyss.guiy.inventory.guiy
import com.mineinabyss.helpers.MessageQueue
import com.mineinabyss.helpers.MessageQueue.content
import com.mineinabyss.idofront.messaging.info
import com.mineinabyss.idofront.messaging.miniMsg
import com.mineinabyss.mineinabyss.core.AbyssContext
import com.mineinabyss.mineinabyss.core.mineInAbyss
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nl.rutgerkok.blocklocker.group.GroupSystem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.seconds

class GuildListener(private val feature: GuildFeature) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun AsyncChatEvent.onGuildChat() {
        val channelId = player.chattyData.channelId

        if ((!channelId.startsWith(player.getGuildName()) && channelId.endsWith(guildChannelId)) || channelId !in chattyConfig.channels.keys) {
            player.chattyData.channelId = getDefaultChat().key
            return
        }
        if (player.chattyData.channelId != player.getGuildChatId()) return

        viewers().clear()
        viewers().addAll(Bukkit.getOnlinePlayers().filter {
            it.getGuildName() == player.getGuildName() && it != player
        })
        viewers().forEach {
            RendererExtension().render(player, player.displayName(), message(), it)
        }
        viewers().clear()
    }

    @EventHandler
    fun PlayerInteractAtEntityEvent.onInteractGuildMaster() {
        rightClicked.toGearyOrNull()?.get<GuildMaster>() ?: return
        guiy { GuildMainMenu(player, feature, true) }
    }

    @EventHandler
    suspend fun PlayerJoinEvent.onJoin() {
        delay(1.seconds)
        withContext(mineInAbyss.asyncDispatcher) {
            transaction(AbyssContext.db) {
                MessageQueue.select { MessageQueue.playerUUID eq player.uniqueId }.forEach {
                    player.info(it[content].miniMsg())
                }
                MessageQueue.deleteWhere { MessageQueue.playerUUID eq player.uniqueId }
            }
            if (!player.hasGuild() && player.chattyData.channelId.endsWith(guildChannelId)) {
                player.chattyData.channelId = getDefaultChat().key
            }
            if (player.hasGuild() && player.chattyData.channelId.endsWith(guildChannelId))
                player.chattyData.channelId = player.getGuildChatId()
        }
    }
}

class GuildContainerSystem : GroupSystem() {

    override fun isInGroup(player: Player, guildName: String): Boolean {
        val name = player.getGuildName().replace(" ", "_")
        return name.equals(guildName, true) && player.hasGuild()
    }

    override fun isGroupLeader(player: Player, groupName: String): Boolean {
        val guild = player.hasGuild()
        if (!guild) return false

        return player.getGuildRank() == GuildRank.OWNER
    }
}
