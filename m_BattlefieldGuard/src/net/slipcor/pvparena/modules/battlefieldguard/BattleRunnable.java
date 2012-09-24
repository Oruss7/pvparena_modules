package net.slipcor.pvparena.modules.battlefieldguard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.slipcor.pvparena.api.PVPArenaAPI;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.managers.ArenaManager;

/**
 * custom runnable class
 * 
 * -
 * 
 * implements an own runnable class in order to commit a powerup spawn in the
 * arena it is running in
 * 
 * @author slipcor
 * 
 * @version v0.7.0
 * 
 */

public class BattleRunnable implements Runnable {
	private Debug db = new Debug(42);

	/**
	 * construct a powerup spawn runnable
	 * 
	 * @param a
	 *            the arena it's running in
	 */
	public BattleRunnable() {
		db.i("BattleRunnable constructor");
	}

	/**
	 * the run method, spawn a powerup
	 */
	@Override
	public void run() {
		db.i("BattleRunnable commiting");
		try {
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				ArenaPlayer ap = ArenaPlayer.parsePlayer(p.getName());
				
				String name = PVPArenaAPI.getArenaNameByLocation(p.getLocation());
				
				db.i("arena: " + String.valueOf(name));
				
				if (name == null || name.equals("")) {
					continue; // not physically in an arena
				}
				
				if (ap.getArena() == null || !ap.getArena().getName().equals(name)) {
					if (ap.getArena() != null) {
						ap.getArena().playerLeave(p, "exit");
						continue;
					}
					
					ArenaManager.getArenaByName(name).tpPlayerToCoordName(p, "exit");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
