package me.itzjustsamu.doublejump;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JumpListener implements Listener {
    private final DoubleJump plugin;
    private final Map<UUID, Boolean> Jumped = new HashMap<>();
    private final Map<Player, Long> Cooldown_Map = new HashMap<>();

    public JumpListener(DoubleJump plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        player.setAllowFlight(!Cooldown_Map.containsKey(player) || System.currentTimeMillis() >= Cooldown_Map.get(player));
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (Cooldown_Map.containsKey(player) && System.currentTimeMillis() < Cooldown_Map.get(player)) {
            if (!Jumped.getOrDefault(player.getUniqueId(), false)) {
                event.setCancelled(true);
                player.setAllowFlight(false);
            }
        } else {
            doubleJump(player);
        }
    }

    private void doubleJump(Player player) {
        double jumpHeight = 1.0; // Example jump height value
        double upwardVelocity = 0.5; // Example upward velocity value

        Cooldown_Map.put(player, System.currentTimeMillis() + Cooldown_Time.getValue());

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(jumpHeight);
        velocity.setY(upwardVelocity); // Apply upward force

        player.setVelocity(velocity);
        player.setAllowFlight(true);
        player.setFlying(false);
        player.setFallDistance(0); // Prevent fall damage

        // Allow player to move after jump
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.setAllowFlight(false), 20L);

        long remainingTime = (Cooldown_Map.get(player) - System.currentTimeMillis()) / 1000L;
        sendActionBar(player, remainingTime);

        if (player.getLocation().getY() % 1 != 0) {
            Jumped.put(player.getUniqueId(), true);
        } else {
            Jumped.put(player.getUniqueId(), false);
        }
    }

    @EventHandler
    public void onSneak(final PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        if (player.getLocation().getY() % 1 == 0 && Cooldown_Map.get(player) != null && System.currentTimeMillis() >= Cooldown_Map.get(player)) {
            Cooldown_Map.put(player, System.currentTimeMillis());
            player.setVelocity(new Vector());
        }
    }

    private void sendActionBar(Player player, long remainingTime) {
        if (isLegacyVersion()) {
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
                        String actionBarMessage = ChatColor.translateAlternateColorCodes('&', Cooldown_Message.getValue())
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

            @Override
            public void run() {
                if (timeLeft > 0) {
                    String actionBarMessage = ChatColor.translateAlternateColorCodes('&', Cooldown_Message.getValue())
                            .replace("{remaining_time}", String.valueOf(timeLeft));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));
                    timeLeft--;
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                    cancel(); // Stop the task when the cooldown ends
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Update every second
    }

    private boolean isLegacyVersion() {
        String[] versionParts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        try {
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);
            return major == 1 && minor < 14; // ActionBar was introduced in 1.14
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // Unable to determine version, assume modern
            return false;
        }
    }

    private static class Cooldown_Message {
        static String getValue() {
            // Implement the logic to get the cooldown message
            return "&aCooldown: {remaining_time}s"; // Placeholder
        }
    }

    private static class Cooldown_Time {
        static long getValue() {
            // Implement the logic to get the cooldown time value
            return 5000L; // Placeholder
        }
    }
}
