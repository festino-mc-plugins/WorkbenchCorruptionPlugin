package com.festp;

import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld; // TODO use reflection
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

public class RandomTicker {
	private final IBlockData WORKBENCH_DATA, BEDROCK_DATA;
	private Random random;
	private int rSeed;
	//private int[] chunkTicks;
	private int chunkTicks;
	private int randomSectionTicks = 3;
	private int regularChunkTicks = 0;
	private final static int MAX_CHUNK_TARGETS = 6;
	private final static int MELEE_MOVES = 16;
	private final static int FAR_MOVES = 16;
	private final static int BOTTOM_RADIUS = 32;
	private final static int MELEE_THICKNESS = 6;
	private final static int FAR_THICKNESS = 6;

	Map<World, Map<Long, List<StretchTarget>>> allTargets = new HashMap<>();
	
	private boolean isGrowing = false, isMoving = false;
	
	public RandomTicker()
	{
		//chunkTicks = new int[1]; // TODO remember all chunks for regular ticks
		chunkTicks = 0;
		random = new Random();
		rSeed = random.nextInt();
		WORKBENCH_DATA = CraftMagicNumbers.getBlock(Material.CRAFTING_TABLE, (byte) 0);
		BEDROCK_DATA = CraftMagicNumbers.getBlock(Material.BEDROCK, (byte) 0);
	}
	
	private void tryAddWorlds()
	{
		for (World w : Bukkit.getWorlds())
		{
			if (!allTargets.containsKey(w))
				allTargets.put(w, new HashMap<>());
		}
	}
	
	public void setRandomSectionTicks(int rts)
	{
		randomSectionTicks = rts;
	}
	
	public void setFeatures(boolean isGrowing, boolean isMoving)
	{
		this.isGrowing = isGrowing;
		this.isMoving = isMoving;
	}
	
	public void tick()
	{
		tryAddWorlds();
		for (World w : Bukkit.getWorlds())
		{
			if (w.getEnvironment() == Environment.THE_END /*&& isEndDisabled*/)
				continue;

			Set<Long> loadedChunks = new HashSet<>();
			for (Chunk c : w.getLoadedChunks())
				loadedChunks.add(Utils.chunkToLong(c));
			
			if (isGrowing)
				growWorld(w, loadedChunks);
			
			reduceLoadedChunks(loadedChunks, 1);

			if (isMoving)
				huntBlocks(w, loadedChunks);
			
			loadedChunks.clear();
		}
		
		chunkTicks++;
	}
	
	private void huntBlocks(World w, Set<Long> loadedChunks)
	{
		Map<Long, List<StretchTarget>> worldTargets = allTargets.get(w);
		//update targets(move)
		List<Long> removed = new ArrayList<>();
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
				if (!isGrowable(m))
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
						if (!isGrowable(m))
							continue;
						//System.out.println(newTarget);
						addTarget(worldTargets, new StretchTarget(newTarget));
						break;
					}
				}
			}
		}
		for (Long l : removed)
		{
			worldTargets.remove(l);
		}
		
		//search new targets
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
				if (isGrowable(b.getType()))
				{
					Block bDown = b.getRelative(BlockFace.DOWN);
					if (!bDown.getType().isAir())
						continue;

					//if (w.getEnvironment() == Environment.NORMAL)
					//	System.out.println(b);

					//System.out.println(l + " " + Utils.worldToLong(b.getX(), b.getZ()) + " " + Utils.getChunkX(l) + " " + Utils.getChunkZ(l));
					//System.out.println(worldTargets.containsKey(Utils.worldToLong(b.getX(), b.getZ())) ? worldTargets.get(Utils.worldToLong(b.getX(), b.getZ())).size() : "no");
					addTarget(worldTargets, new StretchTarget(b));
					break;
				}
			}
		}
		
		//stretch
		for (List<StretchTarget> targets : worldTargets.values())
		{
			for (StretchTarget target : targets)
			{
				stretch(target, loadedChunks);
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

	private void growWorld(World w, Set<Long> loadedChunks)
	{

		final net.minecraft.world.level.World nmsWorld = ((CraftWorld) w).getHandle();
		final ConcurrentLinkedQueue<net.minecraft.world.level.chunk.Chunk> queue = new ConcurrentLinkedQueue<>();
		for (Chunk c : w.getLoadedChunks())
		{
			queue.add(nmsWorld.getChunkAt(c.getX(), c.getZ()));
		}

		ConcurrentLinkedQueue<Vector3i> blocksToGrow = new ConcurrentLinkedQueue<>();
		ConcurrentLinkedQueue<Vector3i> blocksToSet = new ConcurrentLinkedQueue<>();
		
		Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < threads.length; i++)
		{
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					while (true)
					{
						net.minecraft.world.level.chunk.Chunk c = null;
						synchronized(queue) {
						    if(!queue.isEmpty()) {
						        c = queue.poll();
						    }
						}
						if (c == null)
							break;
						else
							tickChunkRandomly(c, nmsWorld, blocksToGrow, blocksToSet);
					}
				}
			});
			threads[i].start();
		}
		for (int i = 0; i < threads.length; i++)
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		
		if (regularChunkTicks > 0)
		{
			for (Chunk c : w.getLoadedChunks())
			{
				tickChunkRegularly(c, nmsWorld);
			}
		}
		
		while (!blocksToGrow.isEmpty())
		{
			Vector3i to = blocksToGrow.remove();
			if (loadedChunks.contains(Utils.worldToLong(to.x, to.z)))
			{
				Block toBlock = w.getBlockAt(to.x, to.y, to.z);
				Material toMaterial = toBlock.getType();
				if (isGrowable(toMaterial))
					blocksToSet.add(to);
			}
		}
		
		while (!blocksToSet.isEmpty())
		{
			Vector3i to = blocksToSet.remove();
			Block toBlock = w.getBlockAt(to.x, to.y, to.z);
			toBlock.setType(Material.CRAFTING_TABLE);
		}
	}

	// use method #2 from https://www.spigotmc.org/threads/methods-for-changing-massive-amount-of-blocks-up-to-14m-blocks-s.395868/
	private void tickChunkRandomly(net.minecraft.world.level.chunk.Chunk nmsChunk, net.minecraft.world.level.World nmsWorld,
			ConcurrentLinkedQueue<Vector3i> blocksToGrow, ConcurrentLinkedQueue<Vector3i> blocksToSet)
	{
	    net.minecraft.world.level.chunk.ChunkSection[] chunksections = nmsChunk.getSections();
	    int baseZ = Utils.getChunkZ(nmsChunk.getPos().pair()) << 4;
	    int baseX = Utils.getChunkX(nmsChunk.getPos().pair()) << 4;
		for (int sectionIndex = 0; sectionIndex < nmsWorld.getSectionsCount(); sectionIndex++)
		{
			net.minecraft.world.level.chunk.ChunkSection section = chunksections[sectionIndex];
			if (section == null) // || !section.shouldTick()
				continue;
			
			for (int i = 0; i < randomSectionTicks; i++)
			{
				// https://github.com/Bukkit/mc-dev/blob/c1627dc9cc7505581993eb0fa15597cb36e94244/net/minecraft/server/WorldServer.java#L214
				rSeed = rSeed * 3 + 1013904223; // TODO synchronised?
				int xyz = rSeed >> 2;
				int x = xyz & 0x0F;
				xyz >>= 8;
				int z = xyz & 0x0F;
				xyz >>= 8;
				int y = xyz & 0x0F;
				xyz >>= 8;
				
				IBlockData ibd = section.getType(x, y, z);
				
				if (ibd == WORKBENCH_DATA)
				{
					y += section.getYPosition();
					int dir = random.nextInt(6);
					
					if (dir == 0)
						x--;
					else if (dir == 1)
						x++;
					else if (dir == 2)
						y--;
					else if (dir == 4)
						z--;
					else if (dir == 5)
						z++;
					else //if (dir == 3)
						y++;
					
					if (y < nmsWorld.getMinBuildHeight() || y >= nmsWorld.getMaxBuildHeight())
						continue;

					int dx = x >> 4;
					int dz = z >> 4;
				    net.minecraft.world.level.chunk.Chunk nmsChunkTo;
					if (dx == 0 && dz == 0) {
						nmsChunkTo = nmsChunk;
					} else {
						blocksToGrow.add(new Vector3i(baseX + x, y, baseZ + z));
						continue;
					}

					BlockPosition bp = new BlockPosition(x, y, z);
					ibd = nmsChunkTo.getType(bp);
					
					if (!ibd.isAir() && ibd != WORKBENCH_DATA && ibd != BEDROCK_DATA) {
						//System.out.println(baseX + " " + baseZ + " " + nmsChunkTo);
						blocksToSet.add(new Vector3i(baseX + x, y, baseZ + z));
					}
				}
			}
		}
	}

	private void tickChunkRegularly(Chunk c, net.minecraft.world.level.World nmsWorld)
	{
		int times = chunkTicks / (16 * 16 * nmsWorld.getSectionsCount() * 16);
		for (int i = 0; i < regularChunkTicks; i++)
		{
			int hor = (chunkTicks & 0x00FF);
			int direction = (chunkTicks & 0x0100) >> 8;
			int b1 = (chunkTicks & 0x0E00) >> 9;
			int section = (chunkTicks & 0xFFFFF000) >> 12;
			section = (section * 5 + b1 + direction + times % 11) % 16;
			section = nmsWorld.getMinSection() + section % nmsWorld.getSectionsCount();
			int y = (b1 + section * 8) * 2 + direction;
			if (direction == 1)
		        y = 256 - y;
			int xz = (hor * 55 + y * y + times % 17) % 256;
			int x = xz % 16;
			int z = xz / 16;
			Block b = c.getBlock(x, y, z);
			if (b.getType() == Material.CRAFTING_TABLE)
			{
				int dir = random.nextInt(6);
				
				Block to = null;
				if (dir == 0)
					to = b.getRelative(0, 0, -1);
				else if (dir == 1)
					to = b.getRelative(0, 0, 1);
				else if (dir == 2)
					to = b.getRelative(0, -1, 0);
				else if (dir == 3)
					to = b.getRelative(0, 1, 0);
				else if (dir == 4)
					to = b.getRelative(-1, 0, 0);
				else if (dir == 5)
					to = b.getRelative(1, 0, 0);
				
				// if "to" is not loaded

				Material toMaterial = to.getType();
				if (!toMaterial.isAir() && toMaterial != Material.CRAFTING_TABLE && toMaterial != Material.BEDROCK)
					to.setType(Material.CRAFTING_TABLE);
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
	
	private static boolean isGrowable(Material m)
	{
		return !(m.isAir() || m == Material.CRAFTING_TABLE || m == Material.BEDROCK);
	}
	
	private void reduceLoadedChunks(Set<Long> loadedChunks, int squareRadius)
	{
		if (squareRadius <= 0)
			return;
		Map<Long, Integer> map = new HashMap<Long, Integer>();
		for (Long l : loadedChunks)
		{
			int x = Utils.getChunkX(l);
			int z = Utils.getChunkZ(l);
			for (int i = -1; i <= 1; i++)
			{
				for (int j = -1; j <= 1; j++)
				{
					Long l2 = Utils.chunkToLong(x + i, z + j);
					if (map.containsKey(l2))
						map.put(l2, map.get(l2) + 1);
					else
						map.put(l2, 1);
				}
			}
		}
		loadedChunks.clear();
		for (Entry<Long, Integer> entry : map.entrySet())
		{
			if (entry.getValue() == 9)
				loadedChunks.add(entry.getKey());
		}
		reduceLoadedChunks(loadedChunks, squareRadius - 1);
	}
}
