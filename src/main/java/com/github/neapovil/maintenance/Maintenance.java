package com.github.neapovil.maintenance;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Maintenance extends JavaPlugin implements Listener
{
    private static Maintenance instance;
    private ConfigResource configResource;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable()
    {
        instance = this;

        this.load().exceptionally(e -> {
            if (e != null)
            {
                e.printStackTrace();
            }
            return null;
        });

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("maintenance")
                .withPermission("maintenance.command.reload")
                .withArguments(new LiteralArgument("reload"))
                .executes((sender, args) -> {
                    this.load().handle((a, b) -> {
                        if (b == null)
                        {
                            sender.sendMessage("Config reloaded");
                        }
                        else
                        {
                            sender.sendRichMessage("<red>Unable to reload config: " + b.getMessage());
                            this.getLogger().severe(b.getMessage());
                        }
                        return null;
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

                    this.save().handle((a, b) -> {
                        if (b == null)
                        {
                            sender.sendMessage("Maintenance status changed to: " + bool);
                        }
                        else
                        {
                            sender.sendRichMessage("<red>Unable to toggle status: " + b.getMessage());
                            this.getLogger().severe(b.getMessage());
                        }
                        return null;
                    });
                })
                .register();
    }

    public static Maintenance instance()
    {
        return instance;
    }

    public CompletableFuture<Void> load()
    {
        return CompletableFuture.runAsync(() -> {
            this.saveResource("config.json", false);
            try
            {
                final String string = Files.readString(this.getDataFolder().toPath().resolve("config.json"));
                this.configResource = this.gson.fromJson(string, ConfigResource.class);
            }
            catch (IOException e)
            {
                throw new CompletionException(e);
            }
        });
    }

    public CompletableFuture<Void> save()
    {
        return CompletableFuture.runAsync(() -> {
            final String string = this.gson.toJson(this.configResource);
            try
            {
                Files.write(this.getDataFolder().toPath().resolve("config.json"), string.getBytes());
            }
            catch (IOException e)
            {
                throw new CompletionException(e);
            }
        });
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
