package com.festp;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

//TODO print time
//TODO phase system
//TODO phases
public class Main extends JavaPlugin implements Listener
{
	private static final String SEP = System.getProperty("file.separator");
	public static final String PATH = "plugins" + SEP + "WorkbenchCorruption" + SEP;

	PhaseFeature[] features0 = new PhaseFeature[] {};
	PhaseFeature[] features1 = new PhaseFeature[] {PhaseFeature.GROWTH};
	PhaseFeature[] features2 = new PhaseFeature[] {PhaseFeature.GROWTH, PhaseFeature.ENDERMAN, PhaseFeature.REPLACE_MOB, PhaseFeature.CHUNK_PREINFECTION};
	PhaseFeature[] features3 = new PhaseFeature[] {PhaseFeature.GROWTH, PhaseFeature.ENDERMAN, PhaseFeature.REPLACE_MOB, PhaseFeature.CHUNK_PREINFECTION,
			PhaseFeature.HUNT_STRETCHING, PhaseFeature.REPLACE_PLAYER, PhaseFeature.INFECT_PLAYER, PhaseFeature.EXECUTE_PLAYER};
	int dayTicks = 24 * 60 * 60 * 20;
	Phase phase0 = new LinearPhase(features0, Integer.MAX_VALUE, 1.0, 1.0);
	Phase phase1 = new LinearPhase(features1, dayTicks, 1.0, 50.0);
	Phase phase2 = new LinearPhase(features1, dayTicks, 50.0, 50.0);
	Phase phase3 = new LinearPhase(features2, dayTicks, 50.0, 50.0);
	Phase phase4 = new LinearPhase(features3, dayTicks, 50.0, 50.0);
	Phase[] phases = new Phase[] { phase0, phase1, phase2, phase3, phase4 };
	final Phaser phaser = new Phaser(phases);
	final Metrics metrics = new Metrics();
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		getServer().getPluginManager().registerEvents(phaser, this);
		
    	CommandWorker command_worker = new CommandWorker(phaser, metrics);
    	getCommand(CommandWorker.MAIN_COMMAND).setExecutor(command_worker);
    	getCommand(CommandWorker.MAIN_COMMAND).setTabCompleter(command_worker);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
			new Runnable() {
				public void run() {
					long t1 = System.nanoTime();
					phaser.tick();
					long t2 = System.nanoTime();
					
					metrics.addMeasurement(t2 - t1);
				}
			}, 0L, 1L);
	}
}
