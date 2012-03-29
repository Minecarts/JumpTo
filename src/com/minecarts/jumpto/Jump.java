package com.minecarts.jumpto;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Player;
import org.bukkit.Location;


public class Jump {
    protected static final Map<Player, Jump> jumps = new HashMap<Player, Jump>();
    
    protected final Player player;
    protected final Location from;
    protected final Location to;
    protected int task;
    
    protected Jump(Plugin plugin, Player player, Location to, int delay) {
        this.player = player;
        this.from = player.getLocation();
        this.to = to.clone();
        this.task = Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, new Runnable() {
            public void run() {
                synchronized(jumps) {
                    if(!playerHasMoved()) {
                        Location to = Jump.this.to.clone();
                        Location from = Jump.this.player.getLocation();
                        to.setPitch(from.getPitch());
                        to.setYaw(from.getYaw());
                        
                        Jump.this.player.teleport(to);
                        jumps.remove(Jump.this.player);
                    }
                }
            }
        }, delay);
    }
    
    public static Jump schedule(Plugin plugin, Player player, Location to, int delay) {
        Jump old = jumps.get(player);
        if(old != null) old.cancel();
        
        Jump jump = new Jump(plugin, player, to, delay);
        jumps.put(player, jump);
        
        return jump;
    }
    
    public static boolean check(Player player) {
        return check(player, player.getLocation());
    }
    public static boolean check(Player player, Location location) {
        Jump jump = jumps.get(player);
        if(jump == null) return false;
        if(jump.isCancelled()) return false;
        
        if(jump.playerHasMoved(location)) {
            jump.cancel();
            return true;
        }
        
        return false;
    }
    
    public boolean isCancelled() {
        return task < 0;
    }
    public void cancel() {
        if(isCancelled()) return;
        Bukkit.getScheduler().cancelTask(task);
        task = -1;
        jumps.remove(player);
    }
    
    public boolean playerHasMoved() {
        return playerHasMoved(player.getLocation());
    }
    public boolean playerHasMoved(Location at) {
        if(at.getBlockX() != from.getBlockX()) return true;
        if(at.getBlockZ() != from.getBlockZ()) return true;
        return false;
    }
    
}
