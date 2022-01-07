package com.mineinabyss.helpers

import androidx.compose.runtime.Composable
import com.mineinabyss.guiy.components.Item
import com.mineinabyss.guiy.modifiers.Modifier
import com.mineinabyss.idofront.items.editItemMeta
import de.erethon.headlib.HeadLib
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object TitleItem {
    fun of(name: String, vararg lore: String) = ItemStack(Material.PAPER).editItemMeta {
        setDisplayName(name)
        setLore(lore.toList())
        setCustomModelData(1)
    }
}

@Composable
fun Text(name: String, vararg lore: String, modifier: Modifier = Modifier) {
    Item(TitleItem.of(name, *lore), modifier)
}

fun OfflinePlayer?.head(title: String, vararg lore: String): ItemStack {
    this ?: return HeadLib.WOODEN_QUESTION_MARK.toItemStack()

    return ItemStack(Material.PLAYER_HEAD).editItemMeta {
        if (this is SkullMeta) {
            setDisplayName(title)
            setLore(lore.toList())
            owningPlayer = this@head
        }
    }
}
