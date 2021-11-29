package com.festp;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class StretchTarget {
	private static final int BOTTOM_SAMPLING_RADIUS = 2;
	private static final int BOTTOM_SAMPLING_UNIT = 3;
	
	private Vector3i pos;
	private Vector3i bottomCenter;
	private World world;
	private int bottomY;
	
	public StretchTarget(Block b)
	{
		pos = new Vector3i(b);
		world = b.getWorld();
		recalcBottomY();
		bottomCenter = new Vector3i(pos.x, bottomY, pos.z);
	}
	
	public Vector3i getPos()
	{
		return pos;
	}
	
	public Vector3i getBottomCenter()
	{
		return bottomCenter;
	}
	
	public Block getBlock()
	{
		return world.getBlockAt(pos.x, pos.y, pos.z);
	}
	
	public World getWorld()
	{
		return world;
	}

	public int getBottomY() {
		return bottomY;
	}
	
	private void recalcBottomY()
	{
		Block b = getBlock();
		b = b.getRelative(BlockFace.DOWN);
		while (b.getType().isAir() && b.getY() > world.getMinHeight())
		{
			b = b.getRelative(BlockFace.DOWN);
		}
		int x = b.getX();
		int y = b.getY();
		int z = b.getZ();
		int sumY = 0;
		int sumCount = 0;
		for (int vx = -BOTTOM_SAMPLING_RADIUS; vx <= BOTTOM_SAMPLING_RADIUS; vx++)
		{
			for (int vz = -BOTTOM_SAMPLING_RADIUS; vz <= BOTTOM_SAMPLING_RADIUS; vz++)
			{
				if (vx == 0 && vz == 0)
					continue;
				int dx;
				if (vx >= 0)
					dx = vx * (vx + 1) / 2;
				else
					dx = vx * (-vx + 1) / 2;
				int dz;
				if (vz >= 0)
					dz = vz * (vz + 1) / 2;
				else
					dz = vz * (-vz + 1) / 2;
				dx *= BOTTOM_SAMPLING_UNIT;
				dz *= BOTTOM_SAMPLING_UNIT;
				if (world.isChunkLoaded((x + dx) >> 4, (z + dz) >> 4))
				{
					sumY += getBottomY(x + dx, y, z + dz);
					sumCount++;
				}
			}
		}
		bottomY = sumY / sumCount;
		if (bottomY == pos.y)
			bottomY -= 1;
	}
	
	private int getBottomY(int x, int startY, int z)
	{
		// test if Chunk is loaded
		Block b = world.getBlockAt(x, startY, z);
		while (b.getType().isAir() && b.getY() > world.getMinHeight())
		{
			b = b.getRelative(BlockFace.DOWN);
		}
		return b.getY();
	}
}
