package com.festp;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class RandomTicker {
	//private int[] chunkTicks;
	private int chunkTicks;
	private int randomSectionTicks = 3;
	private int regularChunkTicks = 1;
	private Random random;
	
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
			int minY = w.getMinHeight() / 16;
			int maxY = w.getMaxHeight() / 16;
			int sectionHeight = maxY - minY;
			int times = chunkTicks / (16 * 16 * sectionHeight * 16);
			for (Chunk c : w.getLoadedChunks())
			{
				for (int section = minY; section < maxY; section++)
				{
					for (int i = 0; i < randomSectionTicks; i++)
					{
						int xyz = random.nextInt();
						int x = xyz & 0x0F;
						xyz >>= 4;
						int z = xyz & 0x0F;
						xyz >>= 4;
						int y = xyz & 0x0F;
						xyz >>= 4;
						Block b = c.getBlock(x, y + 16 * section, z);
						if (b.getType() == Material.CRAFTING_TABLE)
						{
							// TODO improve code
							int dir = xyz % 6;
							if (dir < 0)
								dir = -dir;
							Block to = null;
							if (dir == 0)
								to = b.getRelative(0, 0, 1);
							else if (dir == 1)
								to = b.getRelative(0, 0, -1);
							else if (dir == 2)
								to = b.getRelative(0, 1, 0);
							else if (dir == 3)
								to = b.getRelative(0, -1, 0);
							else if (dir == 4)
								to = b.getRelative(1, 0, 0);
							else if (dir == 5)
								to = b.getRelative(-1, 0, 0);
							if (!to.getType().isAir())
								to.setType(Material.CRAFTING_TABLE);
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
							dir = -dir;
						// TODO improve code
						Block to = null;
						if (dir == 0)
							to = b.getRelative(0, 0, 1);
						else if (dir == 1)
							to = b.getRelative(0, 0, -1);
						else if (dir == 2)
							to = b.getRelative(0, 1, 0);
						else if (dir == 3)
							to = b.getRelative(0, -1, 0);
						else if (dir == 4)
							to = b.getRelative(1, 0, 0);
						else if (dir == 5)
							to = b.getRelative(-1, 0, 0);
						if (!to.getType().isAir())
							to.setType(Material.CRAFTING_TABLE);
					}
					chunkTicks++;
				}
			}
		}
	}
}
