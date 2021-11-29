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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EntityInfector {
	private Random random = new Random();
	private static final int N = 100; // >= max eq+inv slots
	private static final int EQUIPMENT_SLOTS_COUNT = EquipmentSlot.values().length;
	private static final PotionEffectType INFECTED_EFFECT = PotionEffectType.UNLUCK;
	private static final int INFECTION_DURATION_LVL2 = 20 * 60 * 5;
	private static final int DURATION_INCREASING = 3;
	
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
					if (e instanceof Player)
						continue;
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
	
	public void replacePlayerItems(boolean onlyInfected)
	{
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (onlyInfected && isInfected(p))
				continue;
			
			if (isTouchingWorkbench(p))
			{
				replaceItemRandomly(p);
			}
		}
	}

	public void infectPlayers() {
		for (Player p : Bukkit.getOnlinePlayers())
		{
			if (isTouchingWorkbench(p))
			{
				if (!p.hasPotionEffect(INFECTED_EFFECT))
				{
					p.addPotionEffect(new PotionEffect(INFECTED_EFFECT, DURATION_INCREASING, 0, true, true));
				}
				else
				{
					int dur = p.getPotionEffect(INFECTED_EFFECT).getDuration() + DURATION_INCREASING; // TODO set deeper on second(reduce timer flickering)
					int lvl = 0;
					if (dur >= INFECTION_DURATION_LVL2)
						lvl = 1;
					p.addPotionEffect(new PotionEffect(INFECTED_EFFECT, dur, lvl, true, true));
				}
			}
		}
	}
	
	public boolean isInfected(Player p)
	{
		return !p.hasPotionEffect(INFECTED_EFFECT) || p.getPotionEffect(INFECTED_EFFECT).getAmplifier() < 1;
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
