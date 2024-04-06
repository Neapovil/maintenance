package com.github.neapovil.maintenance;

import org.bukkit.plugin.java.JavaPlugin;

public final class Maintenance extends JavaPlugin
{
    private static Maintenance instance;

    @Override
    public void onEnable()
    {
        instance = this;
    }

    public static Maintenance instance()
    {
        return instance;
    }
}
