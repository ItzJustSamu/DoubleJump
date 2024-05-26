package me.itzjustsamu.doublejump;

import org.bukkit.plugin.java.JavaPlugin;

public class DoubleJump extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new JumpListener(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
