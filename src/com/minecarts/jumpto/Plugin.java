package com.minecarts.jumpto;

import java.util.logging.Level;
import java.text.MessageFormat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;


public class Plugin extends JavaPlugin implements Listener {
    protected static final Pattern COORDS = Pattern.compile("\\n(-?\\d+)(?:,\\s*(-?\\d+))?,\\s*(-?\\d+)$");
    protected int delay = 3;
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        log("Version {0} enabled.", getDescription().getVersion());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(Jump.check(player)) {
            player.sendMessage(ChatColor.AQUA + "Jump cancelled. " + ChatColor.GRAY + "You moved! Please try again.");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.useInteractedBlock() == Result.DENY) return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        BlockState state = event.getClickedBlock().getState();
        if(state instanceof Sign) {
            jumpFrom(event.getPlayer(), (Sign) state);
        }
    }
    
    public void jumpFrom(Player player, Sign sign) {
        String content = StringUtils.join(sign.getLines(), "\n");
        Location location = parseLocation(content, player.getWorld());
        if(location != null) {
            log("{0} jumping from sign at {1}", player.getName(), sign.getLocation());
            jumpTo(player, location.add(.5, 0, .5));
        }
    }
    
    public void jumpTo(Player player, Location to) {
        if(!player.hasPermission("jumpto.jump")) {
            player.sendMessage(ChatColor.GRAY + "You do not have permission to use this jump sign.");
            return;
        }
        
        log("{0} jumping to {1}", player.getName(), to);
        Jump.schedule(this, player, to, 20 * delay);
        player.sendMessage(ChatColor.GRAY + "Jumping to " + to.getBlockX() + ", " + to.getBlockY() + ", " + to.getBlockZ() + " in " + delay + " seconds... " + ChatColor.AQUA + "don't move!");
    }
    
    
    public static Location parseLocation(String coords, World defaultWorld) {
        Matcher matcher = COORDS.matcher(coords);
        if(!matcher.find()) return null;
        
        String x = matcher.group(1);
        String y = matcher.group(2);
        String z = matcher.group(3);
        
        return (y == null)
                ? defaultWorld.getHighestBlockAt(Integer.parseInt(x), Integer.parseInt(z)).getLocation()
                : new Location(defaultWorld, Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
    }
    
    
    protected static void sendMessage(Player player, String message) {
        player.sendMessage(MessageFormat.format("{3}<{2}JUMP{3}>{1} {0}", message, ChatColor.RESET, ChatColor.AQUA, ChatColor.DARK_AQUA));
    }
    
    
    public void log(String message) {
        log(Level.INFO, message);
    }
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }
    public void log(String message, Object... args) {
        log(MessageFormat.format(message, args));
    }
    public void log(Level level, String message, Object... args) {
        log(level, MessageFormat.format(message, args));
    }
    
    public void debug(String message) {
        log(Level.FINE, message);
    }
    public void debug(String message, Object... args) {
        debug(MessageFormat.format(message, args));
    }
}