package com.festp;

import org.bukkit.World;
import org.bukkit.block.Block;

public class StretchTarget {
	private Vector3i pos;
	private World world;
	
	public StretchTarget(Block b)
	{
		pos = new Vector3i(b);
		world = b.getWorld();
	}
	
	public Vector3i getPos()
	{
		return pos;
	}
	
	public World getWorld()
	{
		return world;
	}
}
