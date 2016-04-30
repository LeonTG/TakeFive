package com.leontg77.takefive.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.google.common.collect.ImmutableList;
import com.leontg77.takefive.Main;

/**
 * Main listener class.
 * 
 * @author LeonTG77
 */
public class MainListener implements Listener {
	private final Map<Location, Inventory> chestLocs = new HashMap<Location, Inventory>();
	private final Map<Inventory, Integer> clicked = new HashMap<Inventory, Integer>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void on(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Location loc = player.getLocation();
		
		Block block = loc.getBlock();
		block.setType(Material.CHEST);
		
		Inventory inv = Bukkit.createInventory(null, 54, player.getName() + "'s death loot");
		
		for (ItemStack item : event.getDrops()) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			
			inv.addItem(item);
		}
		
		event.getDrops().clear();
		
		chestLocs.put(block.getLocation(), inv);
		clicked.put(inv, 0);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		
		if (action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		Block block = event.getClickedBlock();
		
		if (block == null) {
			return;
		}
		
		Location loc = block.getLocation();
		
		if (!chestLocs.containsKey(loc)) {
			return;
		}
		
		Inventory inv = chestLocs.get(loc);
		player.openInventory(inv);
		
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(InventoryClickEvent event) {
		Player player = (Player) event.getWhoClicked();
		Inventory inv = event.getClickedInventory();
		
		if (!clicked.containsKey(inv)) {
			return;
		}
		
		ItemStack current = event.getCurrentItem();
		
		if (current == null || current.getType() == Material.AIR) {
			return;
		}
		
		for (ItemStack drop : player.getInventory().addItem(current.clone()).values()) {
			player.getWorld().dropItem(player.getLocation(), drop).setVelocity(new Vector(0, 0.5, 0));
		}
		
		event.setCurrentItem(new ItemStack(Material.AIR));
		event.setCancelled(true);
		
		int clicks = clicked.get(inv);
		clicks++;
		
		clicked.put(inv, clicks);
		
		if (clicks == 5) {
			Location loc = null;
			
			for (Entry<Location, Inventory> entry : chestLocs.entrySet()) {
				if (entry.getValue().getTitle().equals(inv.getTitle())) {
					loc = entry.getKey();
				}
			}
			
			if (loc == null) { // safety
				return;
			}
			
			player.sendMessage(Main.PREFIX + "You have taken 5 items, removing the loot.");
			loc.getBlock().setType(Material.AIR);

			for (HumanEntity hum : ImmutableList.copyOf(inv.getViewers())) {
				hum.closeInventory();
			}
			
			chestLocs.remove(loc);
			clicked.remove(inv);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		
		if (block == null) {
			return;
		}
		
		Location loc = block.getLocation();
		
		if (!chestLocs.containsKey(loc)) {
			return;
		}
		
		player.sendMessage(Main.PREFIX + "You cannot break the loot chest.");
		event.setCancelled(true);
	}
}