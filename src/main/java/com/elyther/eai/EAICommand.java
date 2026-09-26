package com.elyther.eai;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EAICommand
        implements CommandExecutor, TabCompleter {

    private final EAI plugin;

    public EAICommand(EAI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {

            help(sender);

            return true;
        }

        String sub =
                args[0].toLowerCase();

        switch (sub) {

            case "message":
            case "msg":
            case "ask":

                if (!(sender instanceof Player player)) {

                    sender.sendMessage(
                            "Bu komanda yalnız oyunçu üçündür."
                    );

                    return true;
                }

                if (args.length < 2) {

                    player.sendMessage(
                            color(
                                    "&c/eai message <sual>"
                            )
                    );

                    return true;
                }

                String question =
                        String.join(
                                " ",
                                java.util.Arrays.copyOfRange(
                                        args,
                                        1,
                                        args.length
                                )
                        );

                plugin.getAIManager()
                        .ask(
                                player,
                                question
                        );

                return true;

            case "create":

                if (!sender.hasPermission(
                        "eai.admin"
                )) {

                    sender.sendMessage(
                            color(
                                    "&cİcazən yoxdur."
                            )
                    );

                    return true;
                }

                if (!(sender instanceof Player player)) {

                    sender.sendMessage(
                            "Bu komanda yalnız oyunçu üçündür."
                    );

                    return true;
                }

                plugin.getNPCManager()
                        .createNPC(player);

                return true;

            case "remove":

                if (!sender.hasPermission(
                        "eai.admin"
                )) {

                    sender.sendMessage(
                            color(
                                    "&cİcazən yoxdur."
                            )
                    );

                    return true;
                }

                plugin.getNPCManager()
                        .removeNPC();

                sender.sendMessage(
                        color(
                                "&aEAI NPC silindi."
                        )
                );

                return true;

            case "reload":

                if (!sender.hasPermission(
                        "eai.admin"
                )) {

                    sender.sendMessage(
                            color(
                                    "&cİcazən yoxdur."
                            )
                    );

                    return true;
                }

                plugin.reloadConfig();

                sender.sendMessage(
                        color(
                                "&aEAI config yeniləndi."
                        )
                );

                return true;

            default:

                help(sender);

                return true;
        }
    }

    private void help(
            CommandSender sender
    ) {

        sender.sendMessage(
                color("&8&m----------------------")
        );

        sender.sendMessage(
                color("&b&lEAI")
        );

        sender.sendMessage(
                color(
                        "&f/eai message <sual>"
                                + " &7- AI ilə danış"
                )
        );

        if (sender.hasPermission(
                "eai.admin"
        )) {

            sender.sendMessage(
                    color(
                            "&f/eai create"
                                    + " &7- NPC yarat"
                    )
            );

            sender.sendMessage(
                    color(
                            "&f/eai remove"
                                    + " &7- NPC sil"
                    )
            );

            sender.sendMessage(
                    color(
                            "&f/eai reload"
                                    + " &7- Config yenilə"
                    )
            );
        }

        sender.sendMessage(
                color("&8&m----------------------")
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        List<String> result =
                new ArrayList<>();

        if (args.length == 1) {

            result.add("message");
            result.add("ask");

            if (sender.hasPermission(
                    "eai.admin"
            )) {

                result.add("create");
                result.add("remove");
                result.add("reload");
            }
        }

        return result;
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
