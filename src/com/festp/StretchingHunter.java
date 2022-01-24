package com.festp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class StretchingHunter {
	private final static int MAX_CHUNK_TARGETS = 8;
	private final static int MELEE_MOVES = 16;
	private final static int FAR_MOVES = 16;
	private final static int BOTTOM_RADIUS = 48;
	private final static int MELEE_THICKNESS = 6;
	private final static int FAR_THICKNESS = 6;
	private Random random = new Random();

	Map<World, Map<Long, List<StretchTarget>>> allTargets = new HashMap<>();

	private void tryAddWorlds()
	{
		for (World w : Bukkit.getWorlds())
		{
			if (!allTargets.containsKey(w))
				allTargets.put(w, new HashMap<>());
		}
	}
	
	public void tick()
	{
		tryAddWorlds();
	}

	
	public void huntBlocks(World w, Set<Long> loadedChunks)
	{
		Map<Long, List<StretchTarget>> worldTargets = allTargets.get(w);
		
		if (worldTargets == null)
			return;
		
		updateTargets(worldTargets, loadedChunks);
		
		randomNewTargets(w, worldTargets, loadedChunks);
		
		for (List<StretchTarget> targets : worldTargets.values())
		{
			for (StretchTarget target : targets)
			{
				stretch(target, loadedChunks);
			}
		}
	}
	
	private void updateTargets(Map<Long, List<StretchTarget>> worldTargets, Set<Long> loadedChunks)
	{
		List<Long> removed = new ArrayList<>();
		List<Block> moved = new ArrayList<>();
		for (Entry<Long, List<StretchTarget>> entry : worldTargets.entrySet())
		{
			if (!loadedChunks.contains(entry.getKey()))
			{
				removed.add(entry.getKey());
				continue;
			}
			
			List<StretchTarget> targets = entry.getValue();
			for (int i = targets.size() - 1; i >= 0; i--)
			{
				StretchTarget target = targets.get(i);
				Block block = target.getBlock();
				
				Material m = block.getType();
				if (!RandomTicker.isGrowable(m))
				{
					targets.remove(i);
					if (m == Material.CRAFTING_TABLE)
						continue;
					Vector3i[] directions = new Vector3i[] {
							new Vector3i(1, 0, 0), new Vector3i(-1, 0, 0), new Vector3i(0, 0, 1), new Vector3i(0, 0, -1), new Vector3i(0, 1, 0), new Vector3i(0, -1, 0)
					};
					Utils.shuffleArray(directions);
					for (Vector3i dir : directions)
					{
						Block newTarget = block.getRelative(dir.x, dir.y, dir.z);
						if (!loadedChunks.contains(Utils.worldToLong(newTarget.getX(), newTarget.getZ())))
							continue;
						if (!canAddTarget(worldTargets, newTarget))
							continue;
						
						m = newTarget.getType();
						if (!RandomTicker.isGrowable(m))
							continue;
						
						moved.add(newTarget);
						break;
					}
				}
			}
		}
		for (Long l : removed)
		{
			worldTargets.remove(l);
		}
		for (Block newTarget : moved)
		{
			addTarget(worldTargets, new StretchTarget(newTarget));
		}
	}
	
	private void randomNewTargets(World w, Map<Long, List<StretchTarget>> worldTargets, Set<Long> loadedChunks)
	{
		for (Long l : loadedChunks)
		{
			if (!isEmpty(worldTargets, l)) // max 1 random target
				continue;
			
			for (int i = 0; i < 3; i++) // random amount
			{
				int y = w.getMinHeight() + random.nextInt(w.getMaxHeight() - w.getMinHeight());
				if (y == w.getMinHeight())
					continue;
				int x = random.nextInt(16);
				int z = random.nextInt(16);
				Block b = Utils.longToChunk(w, l).getBlock(x, y, z);
				if (RandomTicker.isGrowable(b.getType()))
				{
					Block bDown = b.getRelative(BlockFace.DOWN);
					if (!bDown.getType().isAir())
						continue;

					addTarget(worldTargets, new StretchTarget(b));
					break;
				}
			}
		}
	}
	
	private void stretch(StretchTarget target, Set<Long> loadedChunks)
	{
		Vector3i targetPos = target.getPos();
		for (int m = 0; m < MELEE_MOVES; m++)
		{
			int rx = random.nextInt(MELEE_THICKNESS) - random.nextInt(MELEE_THICKNESS);
			int rz = random.nextInt(MELEE_THICKNESS) - random.nextInt(MELEE_THICKNESS);
			if (rx != 0 || rz != 0)
			{
				int ry = random.nextInt(target.getPos().y - target.getBottomY() + MELEE_THICKNESS * 2) - MELEE_THICKNESS;
				int y = targetPos.y - ry;
				if (y < target.getWorld().getMinHeight())
					continue;
				int x = targetPos.x + rx;
				int z = targetPos.z + rz;
				
				if (!loadedChunks.contains(Utils.worldToLong(x, z)))
					continue;
				
				Block b = target.getWorld().getBlockAt(x, y, z);
				if (b.getType() == Material.CRAFTING_TABLE)
				{
					int absX = Math.abs(rx);
					int absZ = Math.abs(rz);
					int d = absX + absZ;
					int dx, dz;
					if (random.nextInt(d) < absX) {
						dx = -rx / absX;
						dz = 0;
					} else {
						dx = 0;
						dz = -rz / absZ;
					}
					
					Block to = b.getRelative(dx, 0, dz);
					if (!to.getType().isAir()) {
						if (y <= targetPos.y)
							to = b.getRelative(0, 1, 0);
						else
							to = b.getRelative(0, -1, 0);
						if (!to.getType().isAir()) {
							continue;
						}
					}
					boolean found = false;
					for (int checkDx = -1; checkDx <= 1; checkDx++)
						for (int checkDy = -1; checkDy <= 1; checkDy++)
							for (int checkDz = -1; checkDz <= 1; checkDz++)
							{
								if (checkDx * checkDy * checkDz == 0 && (checkDx | checkDy | checkDz) != 0)
								{
									Block check = to.getRelative(checkDx, checkDy, checkDz);
									if (check != b && check.getType() == Material.CRAFTING_TABLE)
									{
										found = true;
										break;
									}
								}
							}
					if (found)
					{
						//System.out.println("Move " + b + " to " + to);
						b.setType(Material.AIR);
						to.setType(Material.CRAFTING_TABLE);
					}
				}
			}
		}
		Vector3i center = target.getBottomCenter();
		for (int m = 0; m < FAR_MOVES; m++)
		{
			int rx = random.nextInt(BOTTOM_RADIUS) - random.nextInt(BOTTOM_RADIUS);
			int rz = random.nextInt(BOTTOM_RADIUS) - random.nextInt(BOTTOM_RADIUS);
			if (rx != 0 || rz != 0)
			{
				int ry = random.nextInt(FAR_THICKNESS * 2) - FAR_THICKNESS;
				int y = center.y + FAR_THICKNESS - ry;
				if (y < target.getWorld().getMinHeight())
					continue;
				int x = center.x + rx;
				int z = center.z + rz;
				
				if (!loadedChunks.contains(Utils.worldToLong(x, z)))
					continue;
				
				Block b = target.getWorld().getBlockAt(x, y, z);
				if (b.getType() == Material.CRAFTING_TABLE)
				{
					int absX = Math.abs(rx);
					int absZ = Math.abs(rz);
					int d = absX + absZ;
					int dx, dz;
					if (random.nextInt(d) < absX) {
						dx = -rx / absX;
						dz = 0;
					} else {
						dx = 0;
						dz = -rz / absZ;
					}
					
					Block to = b.getRelative(dx, 0, dz);
					if (!to.getType().isAir()) {
						to = b.getRelative(0, 1, 0);
						if (!to.getType().isAir()) {
							continue;
						}
					}
					boolean found = false;
					for (int checkDx = -1; checkDx <= 1; checkDx++)
						for (int checkDy = -1; checkDy <= 1; checkDy++)
							for (int checkDz = -1; checkDz <= 1; checkDz++)
							{
								if (checkDx * checkDy * checkDz == 0 && (checkDx | checkDy | checkDz) != 0)
								{
									Block check = to.getRelative(checkDx, checkDy, checkDz);
									if (check != b && check.getType() == Material.CRAFTING_TABLE)
									{
										found = true;
										break;
									}
								}
							}
					if (!found)
					{
						to = b.getRelative(0, -1, 0);
						if (to.getType().isAir()) {
							found = true;
						}
					}
					if (found)
					{
						b.setType(Material.AIR);
						to.setType(Material.CRAFTING_TABLE);
					}
				}
			}
		}
	}
	
	private void addTarget(Map<Long, List<StretchTarget>> worldTargets, StretchTarget target)
	{
		long l = Utils.chunkToLong(target.getBlock().getChunk());
		if (!worldTargets.containsKey(l))
			worldTargets.put(l, new ArrayList<>());
		worldTargets.get(l).add(target);
	}
	
	private boolean isEmpty(Map<Long, List<StretchTarget>> worldTargets, Long l)
	{
		return !worldTargets.containsKey(l) || worldTargets.get(l).size() == 0;
	}
	
	private boolean canAddTarget(Map<Long, List<StretchTarget>> worldTargets, Block newTarget)
	{
		Long l = Utils.chunkToLong(newTarget.getChunk());
		return !worldTargets.containsKey(l) || worldTargets.get(l).size() < MAX_CHUNK_TARGETS;
	}
}
