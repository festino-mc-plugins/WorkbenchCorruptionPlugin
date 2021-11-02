package com.festp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class RandomTicker {
	private Random random;
	//private int[] chunkTicks;
	private int chunkTicks;
	private int randomSectionTicks = 3;
	private int regularChunkTicks = 1;
	Map<Integer, Map<Integer, ChunkSnapshot>> zMap = new HashMap<>();
	
	public RandomTicker()
	{
		//chunkTicks = new int[1]; // TODO remember all chunks
		chunkTicks = 0;
		random = new Random();
	}
	
	public void setRandomSectionTicks(int rts)
	{
		randomSectionTicks = rts;
	}
	
	public void tick()
	{
		for (World w : Bukkit.getWorlds())
		{
			int minYblock = w.getMinHeight();
			int maxYblock = w.getMaxHeight();
			int minY = minYblock / 16;
			int maxY = maxYblock / 16;
			int sectionHeight = maxY - minY;
			int times = chunkTicks / (16 * 16 * sectionHeight * 16);

			// Chunk#getChunkSnapshot() IS TOO SLOW
			//long t1 = System.nanoTime();
			Chunk[] loadedChunks = w.getLoadedChunks();
			for (Chunk cSlow : loadedChunks)
			{
				int chunkZ = cSlow.getZ();
				if (!zMap.containsKey(chunkZ))
					zMap.put(chunkZ, new HashMap<Integer, ChunkSnapshot>());
				int chunkX = cSlow.getX();
				zMap.get(chunkZ).put(chunkX, cSlow.getChunkSnapshot());
			}
			//long t2 = System.nanoTime();
			//System.out.println((t2 - t1) / 1000000000.0);
			
			for (Chunk cSlow : loadedChunks)
			{
				ChunkSnapshot c = zMap.get(cSlow.getZ()).get(cSlow.getX());
				for (int section = minY; section < maxY; section++)
				{
					if (c.isSectionEmpty(section))
						continue;
					
					for (int i = 0; i < randomSectionTicks; i++)
					{
						int xyz = random.nextInt();
						int x = xyz & 0x0F;
						xyz >>= 4;
						int z = xyz & 0x0F;
						xyz >>= 4;
						int y = xyz & 0x0F;
						xyz >>= 4;
						y += 16 * section;
						Material m = c.getBlockType(x, y, z);
						if (m == Material.CRAFTING_TABLE)
						{
							// TODO improve code
							int dir = xyz % 6;
							if (dir < 0)
								dir += 6;
							
							/*// 000, 001, 010, 011, 100, 101 - this implementation is slower
							int sign = (dir & 0x1) * 2 - 1;
							x += sign * (1 - (((dir >> 1) & 0x1) & ((dir >> 2) & 0x1)));
							y += sign * ((dir >> 1) & 0x1);
							z += sign * ((dir >> 2) & 0x1);*/
							
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
							
							if (y < minYblock || y >= maxYblock)
								continue;

							int dx = x >> 4;//(x - 15) / 16;
							int dz = z >> 4;//(z - 15) / 16;
							ChunkSnapshot c2;
							if (dx == 0 && dz == 0) {
								c2 = c;
							} else {
								//if (x < 0 || z < 0)
								//{
								//	System.out.println(x + " " + z + " " + dx + " " + dz);
								//}
								if (zMap.containsKey(cSlow.getZ() + dz)) { // TODO use tuples as keys?
									Map<Integer, ChunkSnapshot> xMap = zMap.get(cSlow.getZ() + dz);
									if (xMap.containsKey(cSlow.getX() + dx)) {
										c2 = xMap.get(cSlow.getX() + dx);
										x -= dx * 16;
										z -= dz * 16;
									} else {
										continue; // not loaded chunk
									}
								} else {
									continue; // not loaded chunk
								}
							}

							Material toMaterial = c2.getBlockType(x, y, z);
							
							if (!toMaterial.isAir() && toMaterial != Material.CRAFTING_TABLE && toMaterial != Material.BEDROCK) {
								Block to = w.getChunkAt(cSlow.getX() + dx, cSlow.getZ() + dz).getBlock(x, y, z);
								to.setType(Material.CRAFTING_TABLE);
							}
						}
					}
				}
				
				for (int i = 0; i < regularChunkTicks; i++)
				{
					int hor = (chunkTicks & 0x00FF);
					int direction = (chunkTicks & 0x0100) >> 8;
					int b1 = (chunkTicks & 0x0E00) >> 9;
					int section = (chunkTicks & 0xFFFFF000) >> 12;
					section = (section * 5 + b1 + direction + times % 11) % 16;
					section = minY + section % sectionHeight;
					int y = (b1 + section * 8) * 2 + direction;
					if (direction == 1)
				        y = 256 - y;
					int xz = (hor * 55 + y * y + times % 17) % 256;
					int x = xz % 16;
					int z = xz / 16;
					Block b = cSlow.getBlock(x, y, z);
					if (b.getType() == Material.CRAFTING_TABLE)
					{
						int dir = random.nextInt() % 6;
						if (dir < 0)
							dir += 6;
						
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
					chunkTicks++;
				}
			}
			
			//System.out.println(w.getLoadedChunks().length + " " + zMap.size());
			zMap.clear();
		}
	}
}
