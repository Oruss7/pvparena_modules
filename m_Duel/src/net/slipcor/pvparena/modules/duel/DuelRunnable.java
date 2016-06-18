package net.slipcor.pvparena.modules.duel;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.commands.PAI_Ready;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import org.bukkit.Bukkit;

class DuelRunnable implements Runnable {
    private final DuelManager dm;
    private final String hoster;
    private final String player;
    private final Debug debug = new Debug(77);

    public DuelRunnable(final DuelManager dm, final String h, final String p) {
        this.dm = dm;
        this.player = p;
        this.hoster = h;
        debug.i("DuelRunnable constructor", hoster);

        final PAG_Join cmd = new PAG_Join();
        cmd.commit(dm.getArena(), Bukkit.getPlayer(hoster), new String[0]);
        cmd.commit(dm.getArena(), Bukkit.getPlayer(player), new String[0]);
        dm.getArena().broadcast(Language.parse(MSG.MODULE_DUEL_STARTING));
    }

    /**
     * the run method, commit arena end
     */
    @Override
    public void run() {
        debug.i("DuelRunnable commiting", hoster);
        if (!"none".equals(dm.getArena().getArenaConfig().getString(CFG.READY_AUTOCLASS))
                && dm.getArena().getArenaConfig().getBoolean(CFG.MODULES_DUEL_FORCESTART)) {
            final PAI_Ready cmd = new PAI_Ready();
            try {
                if (dm.getArena().equals(ArenaPlayer.parsePlayer(hoster).getArena()) &&
                        dm.getArena().equals(ArenaPlayer.parsePlayer(player).getArena())) {
                    cmd.commit(dm.getArena(), Bukkit.getPlayer(hoster), new String[0]);
                    cmd.commit(dm.getArena(), Bukkit.getPlayer(player), new String[0]);
                    dm.getArena().countDown();
                } else if (dm.getArena().getFighters().size() > 0){
                    dm.getArena().reset(false);
                }
            } catch (Exception e) {
                if (dm.getArena().getFighters().size() > 0){
                    dm.getArena().reset(false);
                }
            }
        }
    }
}
