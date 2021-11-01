package com.festp;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
			double avg = metrics.getAverageTickSec();
			sender.sendMessage(ChatColor.GREEN + "Base tickspeed: " + avg + "s (" + (avg * 20 * 100)+ "% of tick)");
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
		}
		if (args.length >= 3) {
			if (args[0].equalsIgnoreCase("phase")) {
				if (args[1].equalsIgnoreCase("start") && args.length == 3) {
					options.add("0");
					options.add("1");
				}
				if (args[1].length() == 1 && Character.isDigit(args[1].charAt(0))) {
					int phase = Integer.parseInt(args[1]);
					if (phase <= 3) {
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
