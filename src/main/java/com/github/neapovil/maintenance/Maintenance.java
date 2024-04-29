package com.github.neapovil.maintenance;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

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
import io.papermc.paper.util.MCUtil;
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

        this.load();

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("maintenance")
                .withPermission("maintenance.command.reload")
                .withArguments(new LiteralArgument("reload"))
                .executes((sender, args) -> {
                    this.load();
                })
                .register();

        new CommandAPICommand("maintenance")
                .withPermission("maintenance.command.set")
                .withArguments(new LiteralArgument("set"))
                .withArguments(new BooleanArgument("bool"))
                .executes((sender, args) -> {
                    final boolean bool = (boolean) args.get("bool");

                    this.configResource.enabled = bool;

                    this.save().whenComplete((result, ex) -> {
                        if (ex == null)
                        {
                            sender.sendMessage("Maintenance status changed to: " + bool);
                        }
                    });
                })
                .register();
    }

    public static Maintenance instance()
    {
        return instance;
    }

    private CompletableFuture<String> load()
    {
        final Core core = Core.instance();
        return core.loadResource(this, this.configPath).whenCompleteAsync((result, ex) -> {
            if (result != null)
            {
                this.configResource = this.gson.fromJson(result, ConfigResource.class);
            }
        }, MCUtil.MAIN_EXECUTOR);
    }

    private CompletableFuture<Void> save()
    {
        final Core core = Core.instance();
        final String string = this.gson.toJson(this.configResource);
        return core.saveResource(this.configPath, string);
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
