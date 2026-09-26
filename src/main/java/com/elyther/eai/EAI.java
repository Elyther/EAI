package com.elyther.eai;

import org.bukkit.plugin.java.JavaPlugin;

public final class EAI extends JavaPlugin {

    private AIManager aiManager;
    private NPCManager npcManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        aiManager = new AIManager(this);
        npcManager = new NPCManager(this);

        EAICommand command = new EAICommand(this);

        if (getCommand("eai") != null) {
            getCommand("eai").setExecutor(command);
            getCommand("eai").setTabCompleter(command);
        }

        npcManager.loadNPC();

        getLogger().info("EAI aktiv edildi.");
    }

    @Override
    public void onDisable() {

        if (npcManager != null) {
            npcManager.saveNPC();
        }

        getLogger().info("EAI deaktiv edildi.");
    }

    public AIManager getAIManager() {
        return aiManager;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }
}
