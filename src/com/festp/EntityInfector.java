package com.festp;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class EntityInfector {
	private Random random = new Random();
	private static final int N = 100; // >= max eq+inv slots
	private static final int EQUIPMENT_SLOTS_COUNT = EquipmentSlot.values().length;
	
	public EntityInfector()
	{
		
	}
	
	public void replaceMobItems()
	{
		for (World w : Bukkit.getWorlds())
		{
			/*for (Entity e : w.getEntitiesByClasses(InventoryHolder.class))
			{
				if (isTouchingWorkbench(e))
				{
					replaceItemRandomly((InventoryHolder) e);
				}
			}*/
			for (Entity e : w.getEntities())
			{
				if (e instanceof LivingEntity)
				{
					if (isTouchingWorkbench(e))
					{
						int r = random.nextInt(N);
						if (r < EQUIPMENT_SLOTS_COUNT) {
							EntityEquipment eq = ((LivingEntity) e).getEquipment();
							EquipmentSlot eqSlot = EquipmentSlot.values()[r];
							eq.setItem(eqSlot, tryReplace(eq.getItem(eqSlot)));
						}
						else if (e instanceof InventoryHolder)
						{
							replaceItem((InventoryHolder) e, N - 1 - r);
						}
					}
				}
				else if (e instanceof InventoryHolder)
				{
					if (isTouchingWorkbench(e))
					{
						replaceItemRandomly((InventoryHolder) e);
					}
				}
			}
		}
	}
	
	public void replacePlayerItems()
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			// check delay???
			if (isTouchingWorkbench(p))
			{
				replaceItemRandomly(p);
			}
		}
	}
	
	private boolean isTouchingWorkbench(Entity e)
	{
		Block legs = e.getLocation().getBlock();
		int tall = (int) (e.getLocation().getY() + e.getHeight()) - legs.getY();
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				for (int h = -1; h <= tall; h++)
					if (legs.getRelative(dx, h, dz).getType() == Material.CRAFTING_TABLE)
						return true;
		return false;
	}
	
	private void replaceItemRandomly(InventoryHolder holder)
	{
		ItemStack[] items = holder.getInventory().getContents();
		int slot = random.nextInt(items.length);
		replace(items, slot);
	}
	
	private void replaceItem(InventoryHolder holder, int slot)
	{
		ItemStack[] items = holder.getInventory().getContents();
		if (slot >= items.length)
			return;
		replace(items, slot);
	}
	
	private void replace(ItemStack[] items, int index)
	{
		ItemStack item = items[index];
		tryReplace(item);
	}
	
	/** Modifies original item*/
	private ItemStack tryReplace(ItemStack item)
	{
		if (item != null && item.getType() != Material.AIR && item.getType() != Material.CRAFTING_TABLE)
		{
			// may be if (random.nextInt(item.getAmount()) == 0)
			item.setType(Material.CRAFTING_TABLE);
		}
		return item;
	}
}
