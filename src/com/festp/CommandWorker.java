package com.festp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class CommandWorker implements CommandExecutor, TabCompleter {
	public static final String MAIN_COMMAND = "event";
	private final Phaser phaser;
	private final Metrics metrics;

	public CommandWorker(Phaser phaser, Metrics metrics) {
		this.phaser = phaser;
		this.metrics = metrics;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args)
	{
		if (!sender.isOp())
			return false;
		
		if (args.length == 0)
			return false;
		
		if (args[0].equalsIgnoreCase("randomTickSpeed")) {
			if (args.length == 1) {
				sender.sendMessage(ChatColor.GREEN + "Base tickspeed: " + phaser.getBaseTickspeed());
				sender.sendMessage(ChatColor.GREEN + "Actual tickspeed: " + phaser.getActualTickspeed());
			} else {
				try {
					double tickspeed = Double.parseDouble(args[1]);
					if (tickspeed < 0)
						tickspeed = 0;
					phaser.setBaseTickspeed(tickspeed);
					sender.sendMessage(ChatColor.GREEN + "New base tickspeed: " + tickspeed);
				} catch (Exception ex) {
					
				}
			}
		}
		if (args[0].equalsIgnoreCase("metrics")) {
			if (args.length == 1)
			{
				double avg = metrics.getAverageTickSec();
				sender.sendMessage(ChatColor.GREEN + "Time to tick: " + String.format("%.6f", avg) + "s (" + String.format("%.2f", avg * 20 * 100) + "% of tick)");
			}
			else
			{
				if (args[1].equalsIgnoreCase("stretching")) {
					Player p = (Player)sender;
					int stretching = phaser.getStretchingTargetCount(p.getWorld());
					int loaded = p.getWorld().getLoadedChunks().length;
					sender.sendMessage(ChatColor.GREEN + "Stretching targets: " + stretching + " (" + String.format("%.2f", stretching * 100.0 / loaded) + "% of loaded)");
					List<StretchTarget> targets = phaser.getStretchingTargets(p.getLocation().getChunk());
					if (targets == null || targets.size() == 0)
					{
						sender.sendMessage(ChatColor.GREEN + "No targets in chunk ");
					}
					else
					{
						String info = "";
						for (StretchTarget target : targets)
						{
							if (!info.isEmpty())
								info += ", ";
							info += "(" + target.getBlock().getX() + ", " + target.getBlock().getY() + ", " + target.getBlock().getZ() + ")";
							info += " {BottomY: " + target.getBottomY() + "}";
						}
						sender.sendMessage(ChatColor.GREEN + "" + targets.size() + " targets in chunk: " + info);
					}
				}
			}
		}
		if (args[0].equalsIgnoreCase("testspeed")) {
			int N = Integer.parseInt(args[1]);
			int x = 0, y = 0, z = 0;
			long t1 = System.nanoTime();
			for (int i = -N; i < N; i++)
			{
				int dir = i % 6;
				if (dir < 0)
					dir += 6;
				int sign = (dir & 0x1) * 2 - 1;
				x += sign * (1 - (((dir >> 1) & 0x1) & ((dir >> 2) & 0x1)));
				y += sign * ((dir >> 1) & 0x1);
				z += sign * ((dir >> 2) & 0x1);
			}
			long t2 = System.nanoTime();
			double dt1 = (t2 - t1) / 1000000000.0;
			sender.sendMessage(ChatColor.GREEN + "Time 1 (unbranched): " + dt1); // ~ 5.3s for N = 1000000000
			t1 = System.nanoTime();
			for (int i = -N; i < N; i++)
			{
				int dir = i % 6;
				if (dir < 0)
					dir += 6;
				if (dir == 0)
					x--;
				else if (dir == 1)
					x++;
				else if (dir == 2)
					y--;
				else if (dir == 3)
					y++;
				else if (dir == 4)
					z--;
				else if (dir == 5)
					z++;
			}
			t2 = System.nanoTime();
			double dt2 = (t2 - t1) / 1000000000.0;
			sender.sendMessage(ChatColor.GREEN + "Time 2 ( branched ): " + dt2); // ~ 3.4s for N = 1000000000
			boolean found = false;
			for (int i = -N; i < N; i++)
			{
				int dir = i % 6;
				if (dir < 0) {
					found = true;
					break;
				}
			}
			sender.sendMessage(ChatColor.GREEN + "Negative found " + found + ", x+y+z = " + (x + y + z));
		}
		if (args[0].equalsIgnoreCase("main")) {
			if (args.length == 1) {
				boolean enabled = phaser.isEnabled();
				sender.sendMessage(ChatColor.GREEN + "State: " + (enabled ? "ON" : "OFF"));
			} else {
				if (args[1].equalsIgnoreCase("restart")) {
					phaser.setEnabled(true);
					phaser.setPhase(0, 0);
					sender.sendMessage(ChatColor.GREEN + "New state: ON");
					sender.sendMessage(ChatColor.GREEN + "Full restart...");
				}
				if (args[1].equalsIgnoreCase("pause")) {
					phaser.setEnabled(false);
					sender.sendMessage(ChatColor.GREEN + "New state: OFF");
				}
				if (args[1].equalsIgnoreCase("continue")) {
					phaser.setEnabled(true);
					sender.sendMessage(ChatColor.GREEN + "New state: ON");
				}
			}
		}
		if (args[0].equalsIgnoreCase("pause")) {
			
		}
		if (args[0].equalsIgnoreCase("phase")) {
			if (args.length == 1)
			{
				sender.sendMessage(ChatColor.RED + "Need more args");
				return false;
			}
			
			if (args[1].equalsIgnoreCase("start"))
			{
				if (args[2].length() > 0 && Character.isDigit(args[2].charAt(0)))
				{
					int phase = Integer.parseInt(args[2]);
					if (phase >= phaser.phases.length) {
						sender.sendMessage(ChatColor.RED + "Invalid phase number");
						return false;
					}
					phaser.setPhase(phase, 0);
					sender.sendMessage(ChatColor.GREEN + "Starting phase #" + phase);
					return true;
				}
			}
			if (args[1].equalsIgnoreCase("time"))
			{
				double time = phaser.phaseTicks / (double) phaser.currentPhase.getDuration();
				sender.sendMessage(ChatColor.GREEN + "Phase #" + phaser.phaseIndex + ": " + String.format("%.2f", time * 100) + "%");
				return true;
			}
			if (args[1].length() > 0 && Character.isDigit(args[1].charAt(0)))
			{
				int phase = Integer.parseInt(args[1]);
				if (phase >= phaser.phases.length) {
					sender.sendMessage(ChatColor.RED + "Invalid phase number");
					return false;
				}
				sender.sendMessage(ChatColor.GREEN + "Phase #" + phase + " info:");
				sender.sendMessage(ChatColor.GREEN + "Duration: " + String.format("%.2f", phaser.phases[phase].getDuration() / 20.0) + "s or " + String.format("%.2f", phaser.phases[phase].getDuration() / (20.0 * 60 * 60)) + "h");
				int size = 0;
				for (Entry<PhaseFeature, Boolean> entry : phaser.phases[phase].getFeatures().entrySet())
					if (entry.getValue())
						size++;
				String[] stringFeatures = new String[size];
				int i = 0;
				for (Entry<PhaseFeature, Boolean> entry : phaser.phases[phase].getFeatures().entrySet())
					if (entry.getValue())
					{
						stringFeatures[i] = entry.getKey().name();
						i++;
					}
				String features = String.join(", ", stringFeatures);
				sender.sendMessage(ChatColor.GREEN + "Features: " + features);
				return true;
			}
		}
		
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> options = new ArrayList<>();
		if (!sender.isOp())
			return options;
		
		if (args.length <= 1) {
			options.add("randomTickSpeed");
			options.add("metrics");
			options.add("main");
			options.add("phase");
		}
		if (args.length == 2) {
			if (args[0].equalsIgnoreCase("randomTickSpeed")) {
				options.add("0");
				options.add("vanilla");
			}
			if (args[0].equalsIgnoreCase("main")) {
				options.add("restart");
				options.add("pause");
				options.add("continue");
			}
			if (args[0].equalsIgnoreCase("phase")) {
				options.add("start");
				options.add("time"); // table
				options.add("0");
				options.add("1");
			}
			if (args[0].equalsIgnoreCase("metrics")) {
				options.add("stretching");
			}
		}
		if (args.length >= 3) {
			if (args[0].equalsIgnoreCase("phase")) {
				if (args[1].equalsIgnoreCase("start") && args.length == 3) {
					options.add("0");
					options.add("1");
				}
				if (args[1].length() == 1 && Character.isDigit(args[1].charAt(0))) {
					int phase = Integer.parseInt(args[1]);
					if (phase < phaser.phases.length) {
						if (args.length == 3) {
							// info
						}
						else {
							// settings
						}
					}
				}
			}
		}
		return options;
	}
}
