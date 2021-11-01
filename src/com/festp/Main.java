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

	final Phaser phaser = new Phaser(new Phase[] { new LinearPhase(72000, 1.0, 1.0) });
	final Metrics metrics = new Metrics();
	
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
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
