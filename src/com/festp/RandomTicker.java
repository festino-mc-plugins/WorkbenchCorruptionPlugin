package com.festp;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
	private int regularChunkTicks = 1;
	
	public RandomTicker()
	{
		//chunkTicks = new int[1]; // TODO remember all chunks
		chunkTicks = 0;
		random = new Random();
		rSeed = random.nextInt();
		WORKBENCH_DATA = CraftMagicNumbers.getBlock(Material.CRAFTING_TABLE, (byte) 0);
		BEDROCK_DATA = CraftMagicNumbers.getBlock(Material.BEDROCK, (byte) 0);
	}
	
	public void setRandomSectionTicks(int rts)
	{
		randomSectionTicks = rts;
	}
	
	// use method #2 from https://www.spigotmc.org/threads/methods-for-changing-massive-amount-of-blocks-up-to-14m-blocks-s.395868/
	public void tick()
	{
		for (World w : Bukkit.getWorlds())
		{
			Set<Long> loadedChunks = new HashSet<>();
			for (Chunk c : w.getLoadedChunks())
				loadedChunks.add( (((long) c.getZ()) << 32) + c.getX());

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
			
			/*for (Chunk c : w.getLoadedChunks())
			{
				tickChunkRegularly(c, nmsWorld);
			}*/

			while (!blocksToGrow.isEmpty())
			{
				Vector3i to = blocksToGrow.remove();
				if (loadedChunks.contains((((long) to.z >> 4) << 32) + (to.x >> 4)))
				{
					Block toBlock = w.getBlockAt(to.x, to.y, to.z);
					Material toMaterial = toBlock.getType();
					if (!toMaterial.isAir() && toMaterial != Material.CRAFTING_TABLE && toMaterial != Material.BEDROCK)
						blocksToSet.add(to);
				}
			}
			
			while (!blocksToSet.isEmpty())
			{
				Vector3i to = blocksToSet.remove();
				Block toBlock = w.getBlockAt(to.x, to.y, to.z);
				toBlock.setType(Material.CRAFTING_TABLE);
			}
			loadedChunks.clear();
		}
		chunkTicks++;
	}
	
	private void tickChunkRandomly(net.minecraft.world.level.chunk.Chunk nmsChunk, net.minecraft.world.level.World nmsWorld,
			ConcurrentLinkedQueue<Vector3i> blocksToGrow, ConcurrentLinkedQueue<Vector3i> blocksToSet)
	{
	    net.minecraft.world.level.chunk.ChunkSection[] chunksections = nmsChunk.getSections();
	    int baseZ = ((int) (nmsChunk.getPos().pair() >> 32)) << 4;
	    int baseX = ((int) nmsChunk.getPos().pair()) << 4;
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
						//System.out.println(2 + " " + ibd + " " + (net.minecraft.world.level.block.Block.getCombinedId(ibd) >> 12));
						/*Block to = w.getChunkAt(c.getX() + dx, c.getZ() + dz).getBlock(x, y, z);*/
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
}
