package com.festp;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;

public class Phaser implements Listener {
	private static final int ENDERMAN_MAX_COUNT = 32;
	private static final int ENDERMAN_MAX_ATTEMPTS = 16;
	private static final int ENDERMAN_DIST_MIN = 2 * 16;
	private static final int ENDERMAN_DIST_MAX = 8 * 16;
	private static final int ENDERMAN_DIST_DESPAWN = 6 * 16;
	
	private boolean enabled = false;
	Random random = new Random();
	List<Enderman> endermen = new ArrayList<>();
	
	double baseTickspeed = 3;
	double tickSupply = 0;
	RandomTicker randomTicker = new RandomTicker();
	EntityInfector infector = new EntityInfector();

	Phase[] phases;
	long phaseTicks = 0;
	int phaseIndex = 0;
	Phase currentPhase = null;
	EnumMap<PhaseFeature, Boolean> features;
	
	ConcurrentLinkedQueue<ChunkInfo> newChunks = new ConcurrentLinkedQueue<>();
	
	public Phaser(Phase[] phases)
	{
		setPhases(phases);
	}
	
	public void setPhases(Phase[] phases)
	{
		this.phases = phases;
		setPhase(0, 0);
	}
	
	public boolean setPhase(int index, double timePercent)
	{
		if (index < 0 || phases.length <= index)
			return false;
		phaseIndex = index;
		currentPhase = phases[phaseIndex];
		features = currentPhase.getFeatures();
		phaseTicks = (int)Math.floor(currentPhase.getDuration() * timePercent);
		return true;
	}
	
	public void tick()
	{
		if (!enabled)
			return;
		
		populateNewChunks(4);
		
		double actual = getActualTickspeed();
		tickSupply += actual;
		int ticks = (int)Math.floor(tickSupply);
		tickSupply -= ticks;
		if (features.containsKey(PhaseFeature.GROWTH) || features.containsKey(PhaseFeature.HUNT_STRETCHING))
		{
			randomTicker.setRandomSectionTicks(ticks);
			randomTicker.setFeatures(features.containsKey(PhaseFeature.GROWTH), features.containsKey(PhaseFeature.HUNT_STRETCHING));
			if (ticks > 0)
				randomTicker.tick();
		}
		
		if (features.containsKey(PhaseFeature.ENDERMAN))
		{
			if (random.nextInt(16) == 0)
			{
				for (int i = endermen.size() - 1; i >= 0; i--)
				{
					if (!endermen.get(i).isValid())
						endermen.remove(i);
				}
				if (endermen.size() < ENDERMAN_MAX_COUNT)
				{
					// try to spawn near random player (2-8 chunks)
					// on any solid block(light?)
					Player[] players = new Player[Bukkit.getOnlinePlayers().size()];
					if (players.length > 0)
					{
						int i = 0;
						for (Player p : Bukkit.getOnlinePlayers())
						{
							players[i] = p;
							i++;
						}
						Player player = players[random.nextInt(players.length)];
						Location playerLoc = player.getLocation();
						if (playerLoc.getWorld().getEnvironment() != Environment.THE_END)
						{
							for (int j = 0; j < ENDERMAN_MAX_ATTEMPTS; j++)
								if (trySpawnEnderman(playerLoc))
									break;
						}
					}
				}
				else
				{
					Enderman e = endermen.get(0);
					double minDist = ENDERMAN_DIST_MAX;
					for (Player p : e.getWorld().getPlayers()) {
						double dist = e.getLocation().distance(p.getLocation());
						if (minDist > dist)
							minDist = dist;
					}
					if (minDist > ENDERMAN_DIST_DESPAWN)
						e.remove();
				}
			}
		}

		if (features.containsKey(PhaseFeature.REPLACE_MOB))
		{
			if (random.nextInt(16) == 0)
			{
				infector.replaceMobItems();
			}
		}
		if (features.containsKey(PhaseFeature.REPLACE_PLAYER))
		{
			if (random.nextInt(256) == 0) // 16 => half per 30s
			{
				infector.replacePlayerItems(true);
			}
		}
		
		if (features.containsKey(PhaseFeature.INFECT_PLAYER))
		{
			infector.infectPlayers();
		}
		if (features.containsKey(PhaseFeature.EXECUTE_PLAYER))
		{
			// TODO
			if (random.nextInt(16) == 0)
			{
				//
			}
		}
			
		phaseTicks++;
		if (phaseTicks > currentPhase.getDuration())
		{
			phaseTicks = 0;
			setPhase(phaseIndex + 1, 0);
		}
		
		// TODO save state
	}

	public void setEnabled(boolean isEnabled)
	{
		enabled = isEnabled;
	}
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public double getBaseTickspeed()
	{
		return baseTickspeed;
	}
	
	public double getActualTickspeed()
	{
		if (!enabled)
			return 0;
		return baseTickspeed * currentPhase.getSpeedMultiplier(phaseTicks);
	}
	
	public void setBaseTickspeed(double tickspeed)
	{
		baseTickspeed = tickspeed;
	}
	
	private boolean trySpawnEnderman(Location center) {
		int dx = random.nextInt(2 * ENDERMAN_DIST_MAX + 1) - ENDERMAN_DIST_MAX;
		int dy = random.nextInt(2 * ENDERMAN_DIST_MAX + 1) - ENDERMAN_DIST_MAX;
		int dz = random.nextInt(2 * ENDERMAN_DIST_MAX + 1) - ENDERMAN_DIST_MAX;
		if (Math.abs(dx) > ENDERMAN_DIST_MIN && Math.abs(dy) > ENDERMAN_DIST_MIN && Math.abs(dz) > ENDERMAN_DIST_MIN)
		{
			Location l = center.add(dx, dy, dz);
			Block b = l.getBlock();
			l = b.getLocation().add(0.5, 0, 0.5);
			if (b.isPassable() && b.getRelative(0, -1, 0).getType().isSolid()
					&& b.getRelative(0, 1, 0).isPassable() && b.getRelative(0, 2, 0).isPassable())
			{
				Enderman e = l.getWorld().spawn(l, Enderman.class, (enderman) -> {
		 			enderman.setCarriedMaterial(new ItemStack(Material.CRAFTING_TABLE).getData());
		        });
				l.getWorld().playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 3.0f, 1.0f);
				endermen.add(e);
				return true;
			}
		}
		return false;
	}
	
	@EventHandler
	public void onChunkPopulated(ChunkPopulateEvent event)
	{
		if (!features.containsKey(PhaseFeature.CHUNK_PREINFECTION))
			return;
		
		newChunks.add(new ChunkInfo(event.getChunk()));
	}
	
	private void populateNewChunks(int count)
	{
		for (int i = 0; i < count; i++)
		{
			if (newChunks.isEmpty())
				break;
			ChunkInfo chunk = newChunks.remove();
			World world = chunk.getWorld();
			int minX = chunk.getX() << 4;
			int minZ = chunk.getZ() << 4;
			int maxX = minX + 16;
			int maxZ = minZ + 16;
			for (int x = minX; x < maxX; x++) {
				for (int z = minZ; z < maxZ; z++) {
					Block b = world.getHighestBlockAt(x, z);
					Block b1 = b.getRelative(0, 1, 0);
					while (!b1.getType().isAir()) {
						b = b1;
						b1 = b1.getRelative(0, 1, 0);
					}
					b.setType(Material.CRAFTING_TABLE, false);
					b = b.getRelative(0, -1, 0);
					if (!b.getType().isAir())
						b.setType(Material.CRAFTING_TABLE, false); // depth == 2
					//if (x % 4 == 1 && z % 4 == 1) // may be use chunk x and z to "random"
					if ((3 * x + z) % 17 == 0)
					{
						for (int y = b.getY() - 4; y >= 6; y -= 4)
						{
							b = b.getRelative(0, -4, 0);
							if (!b.getType().isAir())
								b.setType(Material.CRAFTING_TABLE, false);
						}
					}
				}
			}
		}
	}

	public int getStretchingTargetCount(World world) {
		int res = 0;
		for (Entry<World, Map<Long, List<StretchTarget>>> worldTargets : randomTicker.hunter.allTargets.entrySet())
		{
			if (worldTargets.getKey() != world)
				continue;
			for (List<StretchTarget> targets : worldTargets.getValue().values())
				res += targets.size();
		}
		return res;
	}

	public List<StretchTarget> getStretchingTargets(Chunk c) {
		for (Entry<World, Map<Long, List<StretchTarget>>> worldTargets : randomTicker.hunter.allTargets.entrySet())
		{
			if (worldTargets.getKey() != c.getWorld())
				continue;
			if (!worldTargets.getValue().containsKey(Utils.chunkToLong(c)))
				return null;
			return worldTargets.getValue().get(Utils.chunkToLong(c));
		}
		return null;
	}
}
