package com.elyther.eai;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class NPCManager {

    private final EAI plugin;

    private final NamespacedKey npcKey;

    private Entity npc;

    private BukkitTask movementTask;

    private final Random random =
            new Random();

    public NPCManager(EAI plugin) {

        this.plugin = plugin;

        npcKey =
                new NamespacedKey(
                        plugin,
                        "eai_npc"
                );
    }

    public void createNPC(
            Player player
    ) {

        removeNPC();

        Location location =
                player.getLocation()
                        .clone();

        Entity entity =
                location.getWorld()
                        .spawnEntity(
                                location,
                                EntityType.VILLAGER
                        );

        if (!(entity instanceof Villager villager)) {

            entity.remove();

            return;
        }

        npc = villager;

        villager.setCustomName(
                color(
                        plugin.getConfig()
                                .getString(
                                        "npc.name",
                                        "&b&lElyther AI"
                                )
                )
        );

        villager.setCustomNameVisible(
                true
        );

        villager.setAI(true);

        villager.setInvulnerable(
                true
        );

        villager.setCollidable(
                false
        );

        villager.setSilent(
                true
        );

        villager.getPersistentDataContainer()
                .set(
                        npcKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        saveNPC();

        startMovement();

        player.sendMessage(
                color(
                        "&aEAI NPC yaradıldı."
                )
        );
    }

    public void removeNPC() {

        stopMovement();

        if (npc != null
                && !npc.isDead()) {

            npc.remove();
        }

        npc = null;

        plugin.getConfig()
                .set(
                        "npc.world",
                        ""
                );

        plugin.saveConfig();
    }

    public void loadNPC() {

        if (!plugin.getConfig()
                .getBoolean(
                        "npc.enabled",
                        true
                )) {

            return;
        }

        String worldName =
                plugin.getConfig()
                        .getString(
                                "npc.world",
                                ""
                        );

        if (worldName == null
                || worldName.isBlank()) {

            return;
        }

        World world =
                Bukkit.getWorld(
                        worldName
                );

        if (world == null) {

            plugin.getLogger()
                    .warning(
                            "EAI NPC world tapılmadı: "
                                    + worldName
                    );

            return;
        }

        double x =
                plugin.getConfig()
                        .getDouble(
                                "npc.x"
                        );

        double y =
                plugin.getConfig()
                        .getDouble(
                                "npc.y"
                        );

        double z =
                plugin.getConfig()
                        .getDouble(
                                "npc.z"
                        );

        float yaw =
                (float) plugin.getConfig()
                        .getDouble(
                                "npc.yaw"
                        );

        float pitch =
                (float) plugin.getConfig()
                        .getDouble(
                                "npc.pitch"
                        );

        Location location =
                new Location(
                        world,
                        x,
                        y,
                        z,
                        yaw,
                        pitch
                );

        Entity entity =
                world.spawnEntity(
                        location,
                        EntityType.VILLAGER
                );

        if (!(entity instanceof Villager villager)) {

            entity.remove();

            return;
        }

        npc = villager;

        villager.setCustomName(
                color(
                        plugin.getConfig()
                                .getString(
                                        "npc.name",
                                        "&b&lElyther AI"
                                )
                )
        );

        villager.setCustomNameVisible(
                true
        );

        villager.setAI(true);

        villager.setInvulnerable(
                true
        );

        villager.setCollidable(
                false
        );

        villager.setSilent(
                true
        );

        villager.getPersistentDataContainer()
                .set(
                        npcKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        startMovement();
    }

    public void saveNPC() {

        if (npc == null
                || npc.isDead()
                || npc.getLocation()
                .getWorld() == null) {

            return;
        }

        Location location =
                npc.getLocation();

        plugin.getConfig()
                .set(
                        "npc.world",
                        location.getWorld()
                                .getName()
                );

        plugin.getConfig()
                .set(
                        "npc.x",
                        location.getX()
                );

        plugin.getConfig()
                .set(
                        "npc.y",
                        location.getY()
                );

        plugin.getConfig()
                .set(
                        "npc.z",
                        location.getZ()
                );

        plugin.getConfig()
                .set(
                        "npc.yaw",
                        location.getYaw()
                );

        plugin.getConfig()
                .set(
                        "npc.pitch",
                        location.getPitch()
                );

        plugin.saveConfig();
    }

    private void startMovement() {

        stopMovement();

        if (!plugin.getConfig()
                .getBoolean(
                        "npc.movement.enabled",
                        true
                )) {

            return;
        }

        long interval =
                plugin.getConfig()
                        .getLong(
                                "npc.movement.interval-seconds",
                                5
                        );

        movementTask =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                plugin,
                                this::moveRandomly,
                                40L,
                                Math.max(
                                        20L,
                                        interval * 20L
                                )
                        );
    }

    private void stopMovement() {

        if (movementTask != null) {

            movementTask.cancel();

            movementTask = null;
        }
    }

    private void moveRandomly() {

        if (npc == null
                || npc.isDead()) {

            return;
        }

        if (!(npc instanceof Mob mob)) {
            return;
        }

        int radius =
                plugin.getConfig()
                        .getInt(
                                "npc.movement.radius",
                                8
                        );

        Location current =
                mob.getLocation();

        double x =
                current.getX()
                        + (
                        random.nextDouble() * 2
                                - 1
                ) * radius;

        double z =
                current.getZ()
                        + (
                        random.nextDouble() * 2
                                - 1
                ) * radius;

        int highest =
                current.getWorld()
                        .getHighestBlockYAt(
                                (int) x,
                                (int) z
                        );

        Location target =
                new Location(
                        current.getWorld(),
                        x,
                        highest + 1,
                        z
                );

        mob.getPathfinder()
                .moveTo(
                        target,
                        1.0
                );
    }

    public boolean isNPC(
            Entity entity
    ) {

        if (entity == null) {
            return false;
        }

        return entity
                .getPersistentDataContainer()
                .has(
                        npcKey,
                        PersistentDataType.BYTE
                );
    }

    private String color(
            String text
    ) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
