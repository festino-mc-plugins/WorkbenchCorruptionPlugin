package com.festp;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

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
	private Random random;
	//private int[] chunkTicks;
	private int chunkTicks;
	private int randomSectionTicks = 3;
	private int regularChunkTicks = 1;
	
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
	
	// use method #2 from https://www.spigotmc.org/threads/methods-for-changing-massive-amount-of-blocks-up-to-14m-blocks-s.395868/
	@SuppressWarnings("deprecation")
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

			Set<Long> loadedChunks = new HashSet<>();
			for (Chunk c : w.getLoadedChunks())
				loadedChunks.add(((long) c.getZ()) << 32 + c.getX());
			
			net.minecraft.world.level.World nmsWorld = ((CraftWorld) w).getHandle();
			for (Chunk c : w.getLoadedChunks())
			{
			    net.minecraft.world.level.chunk.Chunk nmsChunk = nmsWorld.getChunkAt(c.getX(), c.getZ());
				for (int section = minY; section < maxY; section++)
				{
					//if (c.isSectionEmpty(section))
					//	continue;
					
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
						
						BlockPosition bp = new BlockPosition(x, y, z);
						IBlockData ibd = nmsChunk.getType(bp);
						Material m = CraftMagicNumbers.getMaterial(ibd).getItemType(); // Found at https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/block/data/CraftBlockData.java
						if (m ==  Material.CRAFTING_TABLE) // TODO use nms crafting_table material
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

							int dx = x >> 4;
							int dz = z >> 4;
						    net.minecraft.world.level.chunk.Chunk nmsChunkTo;
							if (dx == 0 && dz == 0) {
								nmsChunkTo = nmsChunk;
							} else {
								Long chunk = ((long) c.getZ() + dz) << 32 + c.getX() + dx;
								if (loadedChunks.contains(chunk)) {
									nmsChunkTo = nmsWorld.getChunkAt(c.getX() + dx, c.getZ() + dz);
									x -= dx << 4;
									z -= dz << 4;
								} else {
									continue;
								}
							}

							bp = new BlockPosition(x, y, z);
							ibd = nmsChunkTo.getType(bp);
							m = CraftMagicNumbers.getMaterial(ibd).getItemType();
							Material toMaterial = CraftMagicNumbers.getMaterial(ibd).getItemType();
							
							if (!toMaterial.isAir() && toMaterial != Material.CRAFTING_TABLE && toMaterial != Material.BEDROCK) {
								Block to = w.getChunkAt(c.getX() + dx, c.getZ() + dz).getBlock(x, y, z);
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
					Block b = c.getBlock(x, y, z);
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
			loadedChunks.clear();
		}
	}
}
