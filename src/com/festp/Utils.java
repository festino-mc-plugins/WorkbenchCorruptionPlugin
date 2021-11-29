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
		return (((long) z) << 32) + x;
	}
	
	public static long worldToLong(int x, int z)
	{
		return chunkToLong(x >> 4, z >> 4);
	}
	
	public static int getChunkX(long l)
	{
		return (int) l;
	}
	
	public static int getChunkZ(long l)
	{
		return (int) (l >> 32);
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
