package com.festp;

import org.bukkit.Chunk;
import org.bukkit.World;

public class ChunkInfo {
	private World world;
	private int x;
	private int z;
	
	public ChunkInfo(Chunk c)
	{
		world = c.getWorld();
		x = c.getX();
		z = c.getZ();
	}
	
	public ChunkInfo(World w, int chunkX, int chunkZ)
	{
		world = w;
		x = chunkX;
		z = chunkZ;
	}
	
	public World getWorld()
	{
		return world;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public Chunk getChunk()
	{
		return world.getChunkAt(x, z);
	}
}
