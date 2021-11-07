package com.festp;

import org.bukkit.block.Block;

public class Vector3i {
	public final int x, y, z;
	
	public Vector3i(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3i(Block b)
	{
		this.x = b.getX();
		this.y = b.getY();
		this.z = b.getZ();
	}
}
