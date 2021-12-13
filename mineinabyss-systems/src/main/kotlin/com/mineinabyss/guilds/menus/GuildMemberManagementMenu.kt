package com.mineinabyss.guilds.menus

import androidx.compose.runtime.Composable
import com.mineinabyss.guiy.components.Grid
import com.mineinabyss.guiy.components.Item
import com.mineinabyss.guiy.components.canvases.Chest
import com.mineinabyss.guiy.guiyPlugin
import com.mineinabyss.guiy.inventory.GuiyOwner
import com.mineinabyss.guiy.inventory.guiy
import com.mineinabyss.guiy.modifiers.Modifier
import com.mineinabyss.guiy.modifiers.clickable
import com.mineinabyss.idofront.font.NegativeSpace
import com.mineinabyss.idofront.items.editItemMeta
import com.mineinabyss.mineinabyss.data.Players
import com.mineinabyss.mineinabyss.extensions.getGuildLevel
import com.mineinabyss.mineinabyss.extensions.getGuildRank
import com.mineinabyss.mineinabyss.extensions.hasGuildRequest
import com.mineinabyss.mineinabyss.extensions.invitePlayerToGuild
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

@Composable
fun GuiyOwner.GuildMemberManagementMenu(player: Player) {
    Chest(
        listOf(player), "${NegativeSpace.of(18)}${ChatColor.WHITE}:guild_member_management_menu:",
        player.getGuildLevel() + 2, onClose = { exit() }) {
        ManageGuildMembersButton(player, Modifier.at(1, 1))
        InviteToGuildButton(player, Modifier.at(7,0))
        ManageGuildJoinRequestsButton(player, Modifier.at(8,0))
        PreviousMenuButton(player, Modifier.at(2, player.getGuildLevel() + 1))
    }
}

@Composable
fun ManageGuildMembersButton(player: Player, modifier: Modifier) {
    val players = transaction {
        val playerRow = Players.select {
            Players.playerUUID eq player.uniqueId
        }.single()

        val guildId = playerRow[Players.guildId]

        Players.select {
            (Players.guildId eq guildId) and
            (Players.playerUUID neq player.uniqueId)
        }.map { row ->
            Pair(row[Players.guildRank], Bukkit.getOfflinePlayer(row[Players.playerUUID]))
        }
    }
    Grid(5, player.getGuildLevel(), modifier){
        players.sortedBy { it.first }.forEach { (rank, member) ->
            val skull = ItemStack(Material.PLAYER_HEAD).editItemMeta {
                if (this is SkullMeta) {
                    setDisplayName("${ChatColor.GOLD}${ChatColor.ITALIC}${member.name}")
                    lore = listOf(
                        "${ChatColor.YELLOW}${ChatColor.BOLD}Guild Rank: ${ChatColor.YELLOW}${ChatColor.ITALIC}${member.getGuildRank()}",
                    )
                    owningPlayer = member
                }
            }

            Item(skull, Modifier.clickable {
                player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
                guiy { GuildMemberAction(player, member) }
            })
        }
    }

}

@Composable
fun InviteToGuildButton(player: Player, modifier: Modifier){
    val guildInvitePaper = ItemStack(Material.PAPER).editItemMeta {
        setDisplayName("${ChatColor.BLUE}${ChatColor.ITALIC}Playername")
        setCustomModelData(1)
    }
    Grid(1,1,modifier.clickable {
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
        AnvilGUI.Builder()
            .title(":guild_invite:")
            .itemLeft(guildInvitePaper)
            .plugin(guiyPlugin)
            .onClose { guiy { GuildMemberManagementMenu(player) } }
            .onComplete { player, invitedPlayer: String ->
                player.invitePlayerToGuild(invitedPlayer)
                AnvilGUI.Response.close()
            }
            .open(player)
    }) {
        Item(ItemStack(Material.PAPER).editItemMeta {
            setDisplayName("${ChatColor.YELLOW}Invite Player to Guild")
        })
    }

}

@Composable
fun ManageGuildJoinRequestsButton(player: Player, modifier: Modifier) {
    if (player.hasGuildRequest()) {
        Item(ItemStack(Material.PAPER).editItemMeta {
            setDisplayName("${ChatColor.DARK_GREEN}Manage Guild Join Requests")
            /* Icon that notifies player there are new invites */
        }, modifier.clickable {
        player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
        guiy { GuildJoinRequestsMenu(player) }
        })
    }
    else {
        Item(ItemStack(Material.PAPER).editItemMeta {
            setDisplayName("${ChatColor.DARK_GREEN}${ChatColor.STRIKETHROUGH}Manage Guild Join Requests")
            /* Custom Icon for "darkerened" out icon indicating no invites */
        }, modifier.clickable {
            player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f)
            guiy { GuildMemberManagementMenu(player) }
        })
    }

}

