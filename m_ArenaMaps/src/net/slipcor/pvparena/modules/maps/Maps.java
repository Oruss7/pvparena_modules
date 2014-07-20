package net.slipcor.pvparena.modules.maps;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Maps extends ArenaModule {
    private HashSet<String> mappings = new HashSet<String>();
    private HashSet<MapItem> items = new HashSet<MapItem>();
    private boolean setup = false;

    public Maps() {
        super("ArenaMaps");
    }

    @Override
    public String version() {
        return "v1.3.0.495";
    }

    @Override
    public boolean checkCommand(String s) {
        return s.equals("!map") || s.equals("arenamaps");
    }

    @Override
    public List<String> getMain() {
        return Arrays.asList("arenamaps");
    }

    @Override
    public List<String> getShort() {
        return Arrays.asList("!map");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        CommandTree<String> result = new CommandTree<String>(null);
        result.define(new String[]{"align", "true"});
        result.define(new String[]{"align", "false"});
        result.define(new String[]{"lives", "true"});
        result.define(new String[]{"lives", "false"});
        result.define(new String[]{"players", "true"});
        result.define(new String[]{"players", "false"});
        result.define(new String[]{"spawns", "true"});
        result.define(new String[]{"spawns", "false"});
        return result;
    }

    @Override
    public void commitCommand(CommandSender sender, String[] args) {
        // !map align
        // !map lives
        // !map players
        // !map spawns


        if (!PVPArena.hasAdminPerms(sender)
                && !(PVPArena.hasCreatePerms(sender, arena))) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{2})) {
            return;
        }

        if (args[0].equals("!map") || args[0].equals("arenamaps")) {
            CFG c = null;
            if (args[1].equals("align")) {
                c = CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER;
            }
            if (args[1].equals("lives")) {
                c = CFG.MODULES_ARENAMAPS_SHOWLIVES;
            }
            if (args[1].equals("players")) {
                c = CFG.MODULES_ARENAMAPS_SHOWPLAYERS;
            }
            if (args[1].equals("spawns")) {
                c = CFG.MODULES_ARENAMAPS_SHOWSPAWNS;

            }
            if (c == null) {

                arena.msg(sender, Language.parse(MSG.ERROR_ARGUMENT, args[1], "align | lives | players | spawns"));
                return;
            }
            boolean b = arena.getArenaConfig().getBoolean(c);
            arena.getArenaConfig().set(c, !b);
            arena.getArenaConfig().save();
            arena.msg(sender, Language.parse(MSG.SET_DONE, c.getNode(), String.valueOf(!b)));
        }
    }

    @Override
    public void displayInfo(CommandSender sender) {
        sender.sendMessage(StringParser.colorVar("playerAlign",
                arena.getArenaConfig().getBoolean(
                        CFG.MODULES_ARENAMAPS_ALIGNTOPLAYER)) + "||" +
                StringParser.colorVar("showLives",
                        arena.getArenaConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWLIVES)) + "||" +
                StringParser.colorVar("showPlayers",
                        arena.getArenaConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWPLAYERS)) + "||" +
                StringParser.colorVar("showSpawns",
                        arena.getArenaConfig().getBoolean(
                                CFG.MODULES_ARENAMAPS_SHOWSPAWNS)));
    }

    public HashSet<MapItem> getItems() {
        return items;
    }

    void trySetup() {
        if (setup)
            return;
        Bukkit.getPluginManager().registerEvents(new MapListener(this), PVPArena.instance);
        setup = true;
    }

    @Override
    public void parseJoin(CommandSender sender, ArenaTeam team) {
        trySetup();
        Player player = (Player) sender;
        HashSet<String> maps;
        if (mappings.isEmpty()) {
            maps = new HashSet<String>();
            prepareSpawnLocations();
        } else {
            maps = mappings;
        }

        maps.add(player.getName());

        items.add(new MapItem(player, team.getColor()));
        mappings = maps;
    }

    private void prepareSpawnLocations() {
        if (!items.isEmpty()) {
            items.clear();
            // recalculate, in case admin added stuff
        }

        HashSet<MapItem> locations = new HashSet<MapItem>();

        for (ArenaTeam team : arena.getTeams()) {
            for (PASpawn spawn : arena.getSpawns()) {
                if (spawn.getName().contains(team.getName())) {
                    locations.add(new MapItem(new PABlockLocation(spawn.getLocation().toLocation()), team.getColor()));
                }
            }

            for (ArenaGoal goal : arena.getGoals()) {
                if (goal.getName().contains("Flag")) {
                    for (PASpawn spawn : arena.getSpawns()) {
                        if (spawn.getName().startsWith(team.getName() + "flag")) {
                            locations.add(new MapItem(new PABlockLocation(spawn.getLocation().toLocation()), team.getColor()));
                        }
                    }
                }
            }
        }
        items = locations;
    }

    @Override
    public void reset(boolean force) {
        mappings.remove(arena.getName());
    }

    @Override
    public void parseRespawn(final Player player, ArenaTeam team, DamageCause cause, Entity damager) {
        if (player == null) {
            return;
        }
        if (!arena.hasPlayer(player)) {
            return;
        }

        class RunLater implements Runnable {

            @Override
            public void run() {
                Short value = MyRenderer.getId(player.getName());
                player.getInventory().addItem(new ItemStack(Material.MAP, 1, value));
                mappings.add(player.getName());
                if (value != Short.MIN_VALUE) {
                    MapView map = Bukkit.getMap(value);

                    MapRenderer mr = new MyRenderer(Maps.this);
                    map.addRenderer(mr);
                }
            }
        }
        Bukkit.getScheduler().runTaskLater(PVPArena.instance, new RunLater(), 5L);
    }

    @Override
    public void parseStart() {

        if (mappings.isEmpty()) {
            return;
        }
        for (String playerName : mappings) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null) {
                continue;
            }
            if (!arena.hasPlayer(player)) {
                continue;
            }
            Short value = MyRenderer.getId(playerName);
            player.getInventory().addItem(new ItemStack(Material.MAP, 1, value));
            mappings.add(player.getName());
            if (value != Short.MIN_VALUE) {
                MapView map = Bukkit.getMap(value);

                MapRenderer mr = new MyRenderer(this);
                map.addRenderer(mr);
            }
        }
    }

    public boolean hasCustomMap(String sPlayer) {
        return mappings.contains(sPlayer);
    }
}
