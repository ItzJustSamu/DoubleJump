package me.itzjustsamu.doublejump;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class JumpListener implements Listener {
    private final HashMap<Player, Boolean> Cooldown = new HashMap<>();
    private final HashMap<Player, Long> Cooldown_Map = new HashMap<>();
    private final HashMap<Player, Boolean> Jumped = new HashMap<>();
    private final Plugin plugin;
    private final String cooldownMessage = "&cCooldown: {remaining_time} seconds";

    public JumpListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && Jumped.getOrDefault(player, false)) {
            event.setCancelled(true);
            Jumped.put(player, false);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        player.setAllowFlight(!Cooldown_Map.containsKey(player) || System.currentTimeMillis() >= Cooldown_Map.get(player));
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Check if the player is on cooldown for double jump
        if (Cooldown_Map.containsKey(player) && System.currentTimeMillis() < Cooldown_Map.get(player)) {
            // If the player hasn't double jumped, cancel the event and disable flight mode
            if (!Jumped.getOrDefault(player, false)) {
                event.setCancelled(true);
                player.setAllowFlight(false);
            }
        } else {
            doubleJump(player);
        }
    }

    private void doubleJump(Player player) {
        // Set cooldown for the double jump
        // Cooldown time in milliseconds
        long cooldownTime = 5000L;
        Cooldown_Map.put(player, System.currentTimeMillis() + cooldownTime);

        // Calculate the direction for the double jump
        Vector direction = player.getLocation().getDirection().normalize();

        // Multiply the normalized direction vector by the jump height to get the final velocity
        // Default jump height
        double jumpHeight = 1.5;
        Vector velocity = direction.multiply(jumpHeight);
        velocity.setY(jumpHeight); // Set vertical jump height

        player.setAllowFlight(true);

        // Set the player's velocity for the double jump
        player.setVelocity(velocity);

        // Schedule task to disable flight after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.setAllowFlight(false), 10L); // Delay of 0.5 seconds (10 ticks)

        // Display remaining cooldown time as an action bar message
        long remainingTime = (Cooldown_Map.get(player) - System.currentTimeMillis()) / 1000L;
        sendActionBar(player, remainingTime);

        // Set HasDoubleJumped to true only if the player is in the air
        if (player.getLocation().getY() % 1 != 0) {
            Jumped.put(player, true);
        } else {
            // Reset HasDoubleJumped to false if the player doesn't have a jump level
            Jumped.put(player, false);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (player.getLocation().getY() % 1 == 0 && Cooldown.get(player) != null && !Cooldown.get(player)) {
            Cooldown.put(player, true);
            player.setVelocity(new Vector());
        }
    }

    private void sendActionBar(Player player, long remainingTime) {
        if (isOldVersion()) {
            sendActionBarLegacy(player, remainingTime);
        } else {
            sendActionBarModern(player, remainingTime);
        }
    }

    private void sendActionBarLegacy(Player player, long remainingTime) {
        new BukkitRunnable() {
            long timeLeft = remainingTime;
            long ticks = 0;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    ticks++;
                    if (ticks % 100 == 0) { // 100 ticks = 5 seconds
                        String actionBarMessage = ChatColor.translateAlternateColorCodes('&', cooldownMessage)
                                .replace("{remaining_time}", String.valueOf(timeLeft));
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', actionBarMessage));
                        ticks = 0; // Reset ticks count
                    }
                    timeLeft--;
                } else {
                    player.sendMessage(""); // Clear the action bar
                    cancel(); // Stop the task when the cooldown ends
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Update every tick
    }

    private void sendActionBarModern(Player player, long remainingTime) {
        new BukkitRunnable() {
            long timeLeft = remainingTime;
            long ticks = 0;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    ticks++;
                    if (ticks % 20 == 0) { // 20 ticks = 1 second
                        String actionBarMessage = ChatColor.translateAlternateColorCodes('&', cooldownMessage)
                                .replace("{remaining_time}", String.valueOf(timeLeft));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
                        timeLeft--;
                    }
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Update every tick
    }

    public static boolean isOldVersion() {
        String[] packageNameParts = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
        return packageNameParts.length >= 4 && (packageNameParts[3].equals("v1_8_R3") || packageNameParts[3].startsWith("v1_8"));
    }
}
