package com.mineinabyss.plugin

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mineinabyss.geary.addon.GearyAddon
import com.mineinabyss.geary.addon.autoscan
import com.mineinabyss.geary.papermc.dsl.gearyAddon
import com.mineinabyss.geary.papermc.store.PrefabNamespaceMigrations
import com.mineinabyss.guilds.database.GuildJoinQueue
import com.mineinabyss.guilds.database.Guilds
import com.mineinabyss.guilds.database.Players
import com.mineinabyss.helpers.MessageQueue
import com.mineinabyss.idofront.commands.Command
import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.platforms.IdofrontPlatforms
import com.mineinabyss.idofront.plugin.getServiceOrNull
import com.mineinabyss.idofront.plugin.isPluginEnabled
import com.mineinabyss.idofront.plugin.registerService
import com.mineinabyss.idofront.time.ticks
import com.mineinabyss.mineinabyss.core.AbyssContext
import com.mineinabyss.mineinabyss.core.AbyssWorldManager
import com.mineinabyss.mineinabyss.core.MIAConfig
import com.mineinabyss.mineinabyss.core.MineInAbyssPlugin
import kotlinx.coroutines.delay
import net.milkbowl.vault.economy.Economy
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

class MineInAbyssPluginImpl : MineInAbyssPlugin() {
    override fun onLoad() {
        IdofrontPlatforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        saveDefaultConfig()

        var addon: GearyAddon? = null
        if (isPluginEnabled("Geary")) {
            PrefabNamespaceMigrations.migrations += listOf("looty" to "mineinabyss", "mobzy" to "mineinabyss")
            gearyAddon {
                addon = this
                autoscan("com.mineinabyss") {
                    components()
                }
            }
        }

        //TODO use Koin
        registerService<AbyssContext>(object : AbyssContext {
            override val econ = getServiceOrNull<Economy>("Vault")
            override val addonScope: GearyAddon
                get() = addon ?: error("Feature tried accessing Geary but it wasn't loaded")
            override val miaSubcommands = mutableListOf<Command.() -> Unit>()
            override val tabCompletions = mutableListOf<TabCompletion.() -> List<String>?>()
            override val db = Database.connect("jdbc:sqlite:" + dataFolder.path + "/data.db", "org.sqlite.JDBC")

            override val commandExecutor = object : IdofrontCommandExecutor(), TabCompleter {
                override val commands = commands(this@MineInAbyssPluginImpl) {
                    ("mineinabyss" / "mia")(desc = "The main command for Mine in Abyss") {
                        miaSubcommands.forEach { it() }
                    }
                }

                override fun onTabComplete(
                    sender: CommandSender,
                    command: org.bukkit.command.Command,
                    alias: String,
                    args: Array<String>
                ): List<String> {
                    val tab = TabCompletion(sender, command, alias, args)
                    return tabCompletions.mapNotNull { it(tab) }.flatten()
                }
            }
        })

        transaction(AbyssContext.db) {
            addLogger(StdOutSqlLogger)

            SchemaUtils.createMissingTablesAndColumns(Guilds, Players, GuildJoinQueue, MessageQueue)
        }

        launch {
            delay(1.ticks)

            val config = MIAConfigImpl()
            config.load()
            registerService<MIAConfig>(config)
            registerService<AbyssWorldManager>(AbyssWorldManagerImpl())
        }
    }
}
