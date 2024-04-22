package com.github.neapovil.maintenance;

import java.nio.file.Path;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.github.neapovil.core.Core;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Maintenance extends JavaPlugin implements Listener
{
    private static Maintenance instance;
    public ConfigResource configResource;
    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public final Path configPath = this.getDataFolder().toPath().resolve("config.json");

    @Override
    public void onEnable()
    {
        instance = this;

        final Core core = Core.instance();

        core.loadResource(this, this.configPath).whenComplete((result, ex) -> {
            if (ex == null)
            {
                this.configResource = this.gson.fromJson(result, ConfigResource.class);
            }
            else
            {
                ex.printStackTrace();
            }
        });

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("maintenance")
                .withPermission("maintenance.command.reload")
                .withArguments(new LiteralArgument("reload"))
                .executes((sender, args) -> {
                    core.loadResource(this, this.configPath).whenComplete((result, ex) -> {
                        if (ex == null)
                        {
                            this.configResource = this.gson.fromJson(result, ConfigResource.class);
                            sender.sendMessage("Config reloaded");
                        }
                        else
                        {
                            sender.sendRichMessage("<red>Unable to reload config: " + ex.getMessage());
                            this.getLogger().severe(ex.getMessage());
                        }
                    });
                })
                .register();

        new CommandAPICommand("maintenance")
                .withPermission("maintenance.command.set")
                .withArguments(new LiteralArgument("set"))
                .withArguments(new BooleanArgument("bool"))
                .executes((sender, args) -> {
                    final boolean bool = (boolean) args.get("bool");

                    this.configResource.enabled = bool;
                    final String string = this.gson.toJson(this.configResource);

                    core.saveResource(this.configPath, string).whenComplete((result, ex) -> {
                        if (ex == null)
                        {
                            sender.sendMessage("Maintenance status changed to: " + bool);
                        }
                        else
                        {
                            sender.sendRichMessage("<red>Unable to toggle status: " + ex.getMessage());
                            this.getLogger().severe(ex.getMessage());
                        }
                    });
                })
                .register();
    }

    public static Maintenance instance()
    {
        return instance;
    }

    @EventHandler
    private void onPaperServerListPing(PaperServerListPingEvent event)
    {
        if (this.configResource.enabled)
        {
            event.setVersion(this.configResource.versionMessage);
            event.setProtocolVersion(0);
        }
    }

    @EventHandler
    private void onPlayerLogin(PlayerLoginEvent event)
    {
        if (!this.configResource.enabled)
        {
            return;
        }

        if (event.getPlayer().hasPermission("maintenance.bypass"))
        {
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, MiniMessage.miniMessage().deserialize(this.configResource.kickMessage));
    }

    class ConfigResource
    {
        public boolean enabled;
        public String kickMessage;
        public String versionMessage;
    }
}
