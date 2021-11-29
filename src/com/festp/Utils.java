package com.festp;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.World;

import io.netty.util.internal.ThreadLocalRandom;

public class Utils {
	
	public static long chunkToLong(Chunk c)
	{
		return chunkToLong(c.getX(), c.getZ());
	}
	
	public static long chunkToLong(int x, int z)
	{
		// https://github.com/Bukkit/mc-dev/blob/c1627dc9cc7505581993eb0fa15597cb36e94244/net/minecraft/server/ChunkCoordIntPair.java#L14
		return (long) x & 0xFFFFFFFF | ((long) z & 0xFFFFFFFF) << 32;
	}
	
	public static long worldToLong(int x, int z)
	{
		return chunkToLong(x >> 4, z >> 4);
	}
	
	public static int getChunkX(long l)
	{
		return (int) (l & 0xFFFFFFFF);
	}
	
	public static int getChunkZ(long l)
	{
		return (int) ((l >> 32) & 0xFFFFFFFF);
	}
	
	public static Chunk longToChunk(World w, long l)
	{
		return w.getChunkAt(getChunkX(l), getChunkZ(l));
	}
	
	/*public static boolean isChunkLoaded(World w, long l)
	{
	<< 4 ???
		return w.isChunkLoaded(getChunkX(l), getChunkZ(l));
	}*/
	
	// https://stackoverflow.com/a/1520212
	public static void shuffleArray(Object[] ar)
	{
	    Random rnd = ThreadLocalRandom.current();
	    for (int i = ar.length - 1; i > 0; i--)
	    {
	        int index = rnd.nextInt(i + 1);
	        // Simple swap
	        Object a = ar[index];
	        ar[index] = ar[i];
	        ar[i] = a;
	    }
	}
}
