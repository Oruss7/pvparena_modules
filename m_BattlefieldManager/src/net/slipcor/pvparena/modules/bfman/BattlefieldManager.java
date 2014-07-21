package net.slipcor.pvparena.modules.bfman;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.commands.AbstractArenaCommand;
import net.slipcor.pvparena.commands.CommandTree;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Debug;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.loadables.ArenaModule;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BattlefieldManager extends ArenaModule {
    private String loaded;
    private boolean changed;

    public BattlefieldManager() {
        super("BattlefieldManager");
        debug = new Debug(691);
    }

    @Override
    public String version() {
        return "v1.3.0.515";
    }

    @Override
    public boolean checkCommand(final String s) {
        return arena.getEveryone().size() < 1 && ("!bm".equals(s) || s.startsWith("battlefieldm"));
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("battlefieldmanager");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("!bm");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        final CommandTree<String> result = new CommandTree<String>(null);
        result.define(new String[]{"clear"});
        result.define(new String[]{"update"});
        result.define(new String[]{"save"});
        return result;
    }

    @Override
    public void commitCommand(final CommandSender sender, final String[] args) {
        // !bm | show the currently loaded battle definitions
        // !bm [name] | load definition [name]
        // !bm clear | start defining a new definition
        // !bm update | update loaded definition with corrections/additions
        // !bm save [name] | save to definition [name]
        if (!PVPArena.hasAdminPerms(sender)
                && !PVPArena.hasCreatePerms(sender, arena)) {
            arena.msg(
                    sender,
                    Language.parse(MSG.ERROR_NOPERM,
                            Language.parse(MSG.ERROR_NOPERM_X_ADMIN)));
            return;
        }

        if (!AbstractArenaCommand.argCountValid(sender, arena, args, new Integer[]{0, 1, 2})) {
            return;
        }

        if (args == null || args.length < 1) {
            // !bm -> show status!

            if (loaded == null) {
                arena.msg(sender, "No battle definition loaded!");
            } else {
                arena.msg(sender, "Loaded definition: " + ChatColor.GREEN
                        + loaded);
            }

            if (changed) {
                arena.msg(sender, ChatColor.RED + "There unsaved changes!");
            } else {
                arena.msg(sender, "No unsaved changes!");
            }

            return;
        }

        if (args.length == 1) {
            if ("clear".equals(args[0])) {
                // !bm update | update loaded definition with corrections/additions

                if (loaded == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

                    return;
                }

                arena.getSpawns().clear();
                arena.getBlocks().clear();
                arena.getRegions().clear();

                changed = false;
                loaded = null;
                return;
            }
            if (args[0].equals("update")) {
                // !bm update | update loaded definition with corrections/additions

                if (loaded == null) {
                    arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

                    return;
                }
                /*
				if (!changed) {
					arena.msg(sender, Language.parse(MSG.ERROR_ERROR, "No definition loaded!"));

					return;
				}
				*/
                for (PASpawn spawn : arena.getSpawns()) {
                    arena.getArenaConfig().setManually(
                            "spawns." + encrypt(spawn.getName(), loaded),
                            Config.parseToString(spawn.getLocation()));
                }

                for (PABlock block : arena.getBlocks()) {
                    arena.getArenaConfig().setManually(
                            "spawns." + encrypt(block.getName(), loaded),
                            Config.parseToString(block.getLocation()));
                }

                changed = false;
                return;
            }
            // !bm [name] | load definition [name]
            Set<String> keys = arena.getArenaConfig().getKeys("spawns");

            if (keys == null) {
                return;
            }

            arena.getSpawns().clear();
            arena.getBlocks().clear();

            for (final String key : keys) {
                if (key.startsWith(loaded + "->")) {
                    final String value = (String) arena.getArenaConfig().getUnsafe("spawns." + key);
                    try {
                        final PABlockLocation loc = Config.parseBlockLocation(value);

                        final String[] split = ((String) arena.getArenaConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        arena.addBlock(new PABlock(loc, newKey));
                    } catch (final IllegalArgumentException e) {
                        final PALocation loc = Config.parseLocation(value);

                        final String[] split = ((String) arena.getArenaConfig().getUnsafe("spawns." + key)).split(">");
                        final String newKey = StringParser.joinArray(StringParser.shiftArrayBy(split, 1), "");
                        arena.addSpawn(new PASpawn(loc, newKey));
                    }
                }
            }

            keys = arena.getArenaConfig().getKeys("arenaregion");

            if (keys == null) {
                return;
            }

            arena.getRegions().clear();

            for (final String key : keys) {
                if (key.startsWith(loaded + "->")) {
                    arena.addRegion(Config.parseRegion(arena, arena.getArenaConfig().getYamlConfiguration(), key));
                }
            }

            return;
        }

        // !bm save [name] | save to definition [name]

        for (final PASpawn spawn : arena.getSpawns()) {
            arena.getArenaConfig().setManually(
                    "spawns." + encrypt(spawn.getName(), args[1]),
                    Config.parseToString(spawn.getLocation()));
        }

        for (final PABlock block : arena.getBlocks()) {
            arena.getArenaConfig().setManually(
                    "spawns." + encrypt(block.getName(), args[1]),
                    Config.parseToString(block.getLocation()));
        }

        changed = false;
        loaded = args[1];
    }

    private String encrypt(final String name, final String definition) {
        final StringBuilder buff = new StringBuilder(name);
        buff.append('-');

        for (final char c : definition.toCharArray()) {
            buff.append('>');
            buff.append(c);
        }

        return buff.toString();
    }

    @Override
    public boolean needsBattleRegion() {
        return true;
    }
/*
	@Override
	public void configParse(YamlConfiguration cfg) {
		
		for (ArenaClass c : arena.getClasses()) {
			if (cfg.get("modules.bettergears.levels." + c.getName()) == null)
				cfg.set("modules.bettergears.levels." + c.getName(),
						parseClassNameToDefaultProtection(c.getName()));
		}
		for (ArenaTeam t : arena.getTeams()) {
			if (cfg.get("modules.bettergears.colors." + t.getName()) == null)
				cfg.set("modules.bettergears.colors." + t.getName(),
						parseTeamColorStringToRGB(t.getColor().name()));
		}
		if (getColorMap().isEmpty()) {
			setup();
		}
	}
	
	@Override
	public void displayInfo(CommandSender sender) {
		for (ArenaTeam team : colorMap.keySet()) {
			Short[] colors = colorMap.get(team);
			sender.sendMessage(team.getName() + ": " +
					StringParser.joinArray(colors,""));
		}
		
		for (ArenaClass aClass : levelMap.keySet()) {
			sender.sendMessage(aClass.getName() + ": " + levelMap.get(aClass));
		}
	}

	void equip(ArenaPlayer ap) {

		short r = 0;
		short g = 0;
		short b = 0;
		
		if (getArena().isFreeForAll()) {
			r = (short) ((new Random()).nextInt(256));
			g = (short) ((new Random()).nextInt(256));
			b = (short) ((new Random()).nextInt(256));
		} else {
			r = getColorMap().get(ap.getArenaTeam())[0];
			g = getColorMap().get(ap.getArenaTeam())[1];
			b = getColorMap().get(ap.getArenaTeam())[2];
		}
		

		ItemStack[] isArmor = new ItemStack[4];
		isArmor[0] = new ItemStack(Material.LEATHER_HELMET, 1);
		isArmor[1] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
		isArmor[2] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
		isArmor[3] = new ItemStack(Material.LEATHER_BOOTS, 1);

		Color c = Color.fromBGR(b, g, r);
		for (int i = 0; i < 4; i++) {
			LeatherArmorMeta lam = (LeatherArmorMeta) isArmor[i].getItemMeta();
			
			lam.setColor(c);
			isArmor[i].setItemMeta(lam);
		}

		Short s = getLevelMap().get(ap.getArenaClass());
		
		if (s == null) {
			String autoClass = getArena().getArenaConfig().getString(CFG.READY_AUTOCLASS);
			ArenaClass ac = getArena().getClass(autoClass);
			s = getLevelMap().get(ac);
		}
		

		isArmor[0].addUnsafeEnchantment(
				Enchantment.PROTECTION_ENVIRONMENTAL, s);
		isArmor[0]
				.addUnsafeEnchantment(Enchantment.PROTECTION_EXPLOSIONS, s);
		isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_FALL, s);
		isArmor[0].addUnsafeEnchantment(Enchantment.PROTECTION_FIRE, s);
		isArmor[0]
				.addUnsafeEnchantment(Enchantment.PROTECTION_PROJECTILE, s);

		ap.get().getInventory().setHelmet(isArmor[0]);
		ap.get().getInventory().setChestplate(isArmor[1]);
		ap.get().getInventory().setLeggings(isArmor[2]);
		ap.get().getInventory().setBoots(isArmor[3]);
	}
	
	private Map<ArenaTeam, Short[]> getColorMap() {
		if (colorMap == null) {
			colorMap = new HashMap<ArenaTeam, Short[]>();
		}
		return colorMap;
	}
	
	private Map<ArenaClass, Short> getLevelMap() {
		if (levelMap == null) {
			levelMap = new HashMap<ArenaClass, Short>();
		}
		return levelMap;
	}

	@Override
	public void initiate(Player player) {
		if (getColorMap().isEmpty()) {
			setup();
		}
	}
	
	@Override
	public void lateJoin(Player player) {
		equip(ArenaPlayer.parsePlayer(player.getName()));
	}

	@Override
	public void reset(boolean force) {
		getColorMap().remove(arena);
		getLevelMap().remove(arena);
	}

	@Override
	public void parseStart() {

		if (getColorMap().isEmpty()) {
			setup();
		}
		// debug();

		for (ArenaPlayer ap : arena.getFighters()) {
			equip(ap);
		}
	}

	private Short parseClassNameToDefaultProtection(String name) {
		if (name.equals("Tank")) {
			return 10;
		} else if (name.equals("Swordsman")) {
			return 4;
		} else if (name.equals("Ranger")) {
			return 1;
		} else if (name.equals("Pyro")) {
			return 1;
		}
		return null;
	}

	public void parseRespawn(Player player, ArenaTeam team, DamageCause cause,
			Entity damager) {
		ArenaPlayer ap = ArenaPlayer.parsePlayer(player.getName());
		if (arena.getArenaConfig().getBoolean(CFG.PLAYER_REFILLINVENTORY)) {
			new EquipRunnable(ap, this);
		}
	}

	private Short[] parseRGBToShortArray(Object o) {
		Short[] result = new Short[3];
		result[0] = 255;
		result[1] = 255;
		result[2] = 255;

		debug.i("parsing RGB:");
		debug.i(String.valueOf(o));

		if (!(o instanceof String)) {
			return result;
		}

		String s = (String) o;

		if (s == null || s.equals("") || !s.contains(",")
				|| s.split(",").length < 3) {
			return result;
		}

		try {
			String[] split = s.split(",");
			result[0] = Short.parseShort(split[0]);
			result[1] = Short.parseShort(split[1]);
			result[2] = Short.parseShort(split[2]);
		} catch (Exception e) {
		}
		return result;
	}

	private String parseTeamColorStringToRGB(String name) {
		if (defaultColors == null) {
			defaultColors = new HashMap<String, String>();

			defaultColors.put("BLACK", "0,0,0");
			defaultColors.put("DARK_BLUE", "0,0,153");
			defaultColors.put("DARK_GREEN", "0,68,0");
			defaultColors.put("DARK_AQUA", "0,153,153");
			defaultColors.put("DARK_RED", "153,0,0");

			defaultColors.put("DARK_PURPLE", "153,0,153");
			defaultColors.put("GOLD", "0,0,0");
			defaultColors.put("GRAY", "153,153,153");
			defaultColors.put("DARK_GRAY", "68,68,68");
			defaultColors.put("BLUE", "0,0,255");
			defaultColors.put("GREEN", "0,255,0");

			defaultColors.put("AQUA", "0,255,255");
			defaultColors.put("RED", "255,0,0");
			defaultColors.put("LIGHT_PURPLE", "255,0,255");
			defaultColors.put("PINK", "255,0,255");
			defaultColors.put("YELLOW", "255,255,0");
			defaultColors.put("WHITE", "255,255,255");
		}

		String s = defaultColors.get(name);
		debug.i("team " + name + " : " + s);
		return s == null ? "255,255,255" : s;
	}

	private void printHelp(CommandSender sender) {
		arena.msg(sender, "/pa [arenaname] !bg [teamname]  | show team color");
		arena.msg(sender,
				"/pa [arenaname] !bg [teamname] color �c<R> �a<G> �9<B>�r | set color");
		arena.msg(sender,
				"/pa [arenaname] !bg [classname] | show protection level");
		arena.msg(sender,
				"/pa [arenaname] !bg [classname] level <level> | set protection level");
	}

	private void setup() {
		debug.i("Setting up BetterGears");

		for (ArenaClass c : arena.getClasses()) {
			Short s = 0;
			try {
				s = Short
						.valueOf(String.valueOf(arena.getArenaConfig()
								.getUnsafe(
										"modules.bettergears.levels."
												+ c.getName())));
				debug.i(c.getName() + " : " + s);
			} catch (Exception e) {
			}
			getLevelMap().put(c, s);
		}

		for (ArenaTeam t : arena.getTeams()) {
			Short[] s = parseRGBToShortArray(arena.getArenaConfig().getUnsafe(
					"modules.bettergears.colors." + t.getName()));
			getColorMap().put(t, s);
			debug.i(t.getName() + " : " + StringParser.joinArray(s, ","));
		}
	}
	*/
}
