package net.slipcor.pvparena.modules.eventactions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAA_Edit;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.loadables.ArenaModule;

public class EventActions extends ArenaModule {
	private boolean setup = false;
	
	public EventActions() {
		super("EventActions");
	}
	
	@Override
	public String version() {
		return "v1.0.1.90";
	}
	
	@Override
	public boolean checkCommand(String s) {
		return s.toLowerCase().equals("setpower");
	}
	
	@Override
	public void commitCommand(CommandSender sender, String[] args) {
		Arena a = PAA_Edit.activeEdits.get(sender.getName()+"_power");
		
		if (a == null) {
			PAA_Edit.activeEdits.put(sender.getName()+"_power", arena);
			arena.msg(sender, Language.parse(MSG.SPAWN_SET_START, "power"));
		} else {
			PAA_Edit.activeEdits.remove(sender.getName()+"_power");
			arena.msg(sender, Language.parse(MSG.SPAWN_SET_DONE, "power"));
		}
	}

	
	@Override
	public void configParse(YamlConfiguration config) {
		if (setup)
			return;
		Bukkit.getPluginManager().registerEvents(new PAListener(this), PVPArena.instance);
		setup = true;
	}

	protected void catchEvent(String string, Player p, Arena a) {
		
		if (a == null || !a.equals(arena)) {
			return;
		}
		
		if (a.getArenaConfig().getUnsafe("event." + string) == null) {
			return;
		}
		
		List<String> items = a.getArenaConfig().getStringList("event." + string, new ArrayList<String>());
		
		for (String item : items) {
			
			if (p != null) {
				item = item.replace("%player%", p.getName());
			}
			
			item = item.replace("%arena%", a.getName());
			item = ChatColor.translateAlternateColorCodes('&', item);
			
			String[] split = item.split("<=>");
			if (split.length != 2) {
				PVPArena.instance.getLogger().warning("[PE] skipping: [" + a.getName() + "]:event." + string + "=>" + item);
				continue;
			}
			/*
			items.add("cmd<=>deop %player%");
			items.add("pcmd<=>me joins %arena%");
			items.add("brc<=>Join %arena%!");
			items.add("power<=>power1");
			items.add("switch<=>switch1");
			items.add("msg<=>Welcome to %arena%!");
			 */
			if (split[0].equalsIgnoreCase("cmd")) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), split[1]);
			} else if (split[0].equalsIgnoreCase("pcmd")) {
				p.performCommand(split[1]);
			} else if (split[0].equalsIgnoreCase("brc")) {
				Bukkit.broadcastMessage(split[1]);
			} else if (split[0].equalsIgnoreCase("power")) {
				PABlockLocation loc = new PABlockLocation(SpawnManager.getCoords(a, split[1]).toLocation());
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.instance, new EADelay(loc), 1L);
				
			} else if (split[0].equalsIgnoreCase("msg") && p != null) {
				p.sendMessage(split[1]);
			}
		}
	}
}