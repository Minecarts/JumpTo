package com.minecarts.jumpto;

import java.util.logging.Level;
import java.text.MessageFormat;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;


public class Plugin extends JavaPlugin implements Listener {
    protected static final Pattern COORDS = Pattern.compile("\\n\\s*(?:([\\w-]+)\\s*@\\s*)?(-?\\d+)(?:\\s*,\\s*(\\d+))?\\s*,\\s*(-?\\d+)\\s*$");
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
        if(Jump.isScheduled(player)) return;
        
        String content = StringUtils.join(sign.getLines(), "\n");
        Location location = parseLocation(content, player.getWorld());
        if(location == null) return;
        
        log("{0} jumping from sign at {1}", player.getName(), sign.getLocation());
        jumpTo(player, location);
    }
    
    public void jumpTo(Player player, Location to) {
        if(Jump.isScheduled(player)) return;
        
        if(!player.hasPermission("jumpto.jump")) {
            player.sendMessage(ChatColor.GRAY + "You do not have permission to use this jump sign.");
            return;
        }
        
        boolean canTeleport = false;
        if(to.getBlockY() >= to.getWorld().getMaxHeight()) {
            log("Loc higher than world max height: {0}", to);
            to.setY(to.getWorld().getMaxHeight() - 1);
            Block block = to.getBlock();
            log("Got max block: {0}", block);
            while(block.getY() > 0) {
                if(canTeleportTo(block)) {
                    canTeleport = true;
                    break;
                }
                block = block.getRelative(BlockFace.DOWN);
                log("Not safe, got new block: {0}", block);
            }
            to = block.getLocation();
        }
        else {
            Block block = to.getBlock();
            log("Block within max height: {0}", block);
            while(block.getY() < block.getWorld().getMaxHeight()) {
                if(canTeleportTo(block)) {
                    canTeleport = true;
                    break;
                }
                block = block.getRelative(BlockFace.UP);
                log("Not safe, got new block: {0}", block);
            }
            to = block.getLocation();
        }
        
        if(!canTeleport) {
            log("No safe location found for {0}", to);
            player.sendMessage(ChatColor.GRAY + "This jump sign's destination is not safe.");
            return;
        }
        
        
        to.add(.5, 0, .5);
        String destination = String.format("%s%d, %d, %d%s", ChatColor.DARK_AQUA, to.getBlockX(), to.getBlockY(), to.getBlockZ(), ChatColor.GRAY);
        if(!player.getWorld().equals(to.getWorld())) {
            if(!player.hasPermission("jumpto.crossworld")) {
                player.sendMessage(ChatColor.GRAY + "You do not have permission to use this jump sign.");
                return;
            }
            destination = String.format("%s%s%s @ %s", ChatColor.DARK_AQUA, to.getWorld().getName(), ChatColor.GRAY, destination);
        }
        
        Jump.schedule(this, player, to, 20 * delay);
        log("{0} jumping to {1}", player.getName(), to);
        player.sendMessage(String.format("%sJumping to %s in %d seconds... %sdon't move!", ChatColor.GRAY, destination, delay, ChatColor.AQUA));
    }
    
    
    public static Location parseLocation(String coords, World world) {
        Matcher matcher = COORDS.matcher(coords);
        if(!matcher.find()) return null;
        
        String worldName = matcher.group(1);
        if(worldName != null) {
            List<World> worlds = Bukkit.getWorlds();
            Iterator<World> i = worlds.iterator();
            while(i.hasNext()) {
                if(!i.next().getName().toLowerCase().contains(worldName.toLowerCase())) {
                    i.remove();
                }
            }
            
            if(worlds.size() == 1) {
                world = worlds.get(0);
            }
        }
        
        String x = matcher.group(2);
        String y = matcher.group(3);
        String z = matcher.group(4);
        
        return new Location(world, Integer.parseInt(x), y == null ? world.getMaxHeight() : Integer.parseInt(y), Integer.parseInt(z));
    }
    
    
    protected static void sendMessage(Player player, String message) {
        player.sendMessage(MessageFormat.format("{3}<{2}JUMP{3}>{1} {0}", message, ChatColor.RESET, ChatColor.AQUA, ChatColor.DARK_AQUA));
    }
    
    
    protected static boolean canTeleportTo(Block block) {
        // make sure we can pass through the block and the block above
        if(!canPassThrough(block)) return false;
        if(!canPassThrough(block.getRelative(BlockFace.UP))) return false;
        
        // make sure we can't pass through the block below
        if(canPassThrough(block.getRelative(BlockFace.DOWN))) return false;
        
        // make sure the block below is also safe
        switch(block.getRelative(BlockFace.DOWN).getType()) {
            case LAVA:
            case STATIONARY_LAVA:
            case WEB:
            case FIRE:
            case PORTAL:
            case ENDER_PORTAL:
                return false;
        }
        
        return true;
    }
    
    protected static boolean canPassThrough(Block block) {
        switch(block.getType()) {
            case AIR:
            case SAPLING:
            case WATER:
            case STATIONARY_WATER:
            //case LAVA:
            //case STATIONARY_LAVA:
            case POWERED_RAIL:
            case DETECTOR_RAIL:
            case WEB:
            case LONG_GRASS:
            case DEAD_BUSH:
            case YELLOW_FLOWER:
            case RED_ROSE:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case TORCH:
            //case FIRE:
            case CROPS:
            case SIGN_POST:
            case LADDER:
            case RAILS:
            case WALL_SIGN:
            case LEVER:
            case STONE_PLATE:
            case WOOD_PLATE:
            case REDSTONE_TORCH_OFF:
            case REDSTONE_TORCH_ON:
            case STONE_BUTTON:
            case SNOW:
            case SUGAR_CANE_BLOCK:
            // case PORTAL:
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON:
            case PUMPKIN_STEM:
            case MELON_STEM:
            case VINE:
            //case ENDER_PORTAL:
                return true;
            default:
                return false;
        }
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