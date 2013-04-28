package me.libraryaddict.Hungergames.Managers;

import me.libraryaddict.Hungergames.Types.AbilityListener;
import me.libraryaddict.Hungergames.Types.HungergamesApi;
import me.libraryaddict.Hungergames.Utilities.ClassGetter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User: Austin Date: 4/22/13 Time: 11:03 PM (c) lazertester
 */
public class AbilityManager {

    private HashMap<String, AbilityListener> abilities = new HashMap<String, AbilityListener>();
    private HashMap<String, List<String>> playerAbilities = new HashMap<String, List<String>>();
    private AbilityConfigManager abilityConfigManager;

    public AbilityManager(AbilityConfigManager abilityConfigManager) {
        this.abilityConfigManager = abilityConfigManager;
        initializeAllAbilitiesInPackage(HungergamesApi.getHungergames(), "me.libraryaddict.Hungergames.Abilities");
    }

    /**
     * 
     * @param Your plugin
     * @param Package name containing your abilities
     */
    public void initializeAllAbilitiesInPackage(JavaPlugin plugin, String packageName) {
        boolean saveConfig = false;
        Bukkit.getLogger().info(
                "[Hunger Games] Initializing all classes found in " + plugin.getName() + " in the " + packageName + " package");
        for (Class abilityClass : ClassGetter.getClassesForPackage(plugin, packageName)) {
            if (AbilityListener.class.isAssignableFrom(abilityClass)) {
                try {
                    Bukkit.getLogger().info("[HungerGames] Found ability " + abilityClass.getSimpleName());
                    AbilityListener abilityListener = (AbilityListener) abilityClass.newInstance();
                    final boolean modified = abilityListener.load(abilityConfigManager.getConfigSection(abilityClass
                            .getSimpleName()));
                    if (modified)
                        saveConfig = true;
                    if (abilityListener instanceof CommandExecutor && abilityListener.getCommand() != null) {
                        HungergamesApi.getHungergames().getCommand(abilityListener.getCommand())
                                .setExecutor((CommandExecutor) abilityListener);
                    }
                    abilities.put(abilityClass.getSimpleName(), abilityListener);
                } catch (Exception e) {
                    System.out.print("Error while loading ability : " + abilityClass.getSimpleName() + ", " + e.getMessage());
                }
            }
        }
        if (saveConfig)
            abilityConfigManager.save();
    }

    /**
     * 
     * @param Name of the ability
     * @param Ability listener
     */
    public void addAbility(String name, AbilityListener abilityListener) {
        abilities.put(name, abilityListener);
        Bukkit.getLogger().info("[HungerGames] Added ability: " + name);
    }

    /**
     * Register the abilitys in the plugin manager because the game started!
     */
    public void registerAbilityListeners() {
        for (AbilityListener abilityListener : abilities.values())
            Bukkit.getPluginManager().registerEvents(abilityListener, HungergamesApi.getHungergames());
    }

    public AbilityListener getAbility(String abilityName) {
        return abilities.get(abilityName);
    }

    public List<String> getPlayerAbilities(String name) {
        if (!playerAbilities.containsKey(name))
            playerAbilities.put(name, new ArrayList<String>());
        return playerAbilities.get(name);
    }

    public void unregisterPlayer(Player player) {
        List<String> abilitiesCopyList = new ArrayList<String>();
        abilitiesCopyList.addAll(getPlayerAbilities(player.getName()));
        for (String abilityName : abilitiesCopyList)
            unregisterPlayerAbility(player, abilityName);
        playerAbilities.remove(player.getName());
    }

    public void unregisterPlayerAbility(Player player, String abilityName) {
        final AbilityListener abilityListener = getAbility(abilityName);
        if (abilityListener != null)
            abilityListener.unregisterPlayer(player);
        getPlayerAbilities(player.getName()).remove(abilityName);
    }

    public void registerPlayerAbility(Player player, String abilityName) {
        final AbilityListener abilityListener = getAbility(abilityName);
        if (abilityListener != null) {
            abilityListener.registerPlayer(player);
        } else
            Bukkit.getLogger().info(
                    "[HungerGames] Tried to register " + player.getName() + " for the " + abilityName
                            + " ability but it does not exist.");
        getPlayerAbilities(player.getName()).add(abilityName);
    }

    public AbilityConfigManager getAbilityConfigManager() {
        return abilityConfigManager;
    }
}
