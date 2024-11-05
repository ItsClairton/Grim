package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("ac")
public class ToggleSubCommand extends BaseCommand {

    @Subcommand("toggle")
    @CommandPermission("ac.toggle")
    public void onReload(CommandSender sender, OnlinePlayer target) {
        final var player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getPlayer());
        if (player == null) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-found",
                    "%prefix% &cPlayer is exempt or offline!");

            sender.sendMessage(MessageUtil.format(message));
            return;
        }

        boolean newState = false;

        for (AbstractCheck abstractCheck : player.getChecks()) {
            if (abstractCheck instanceof Check check) {
                check.setAutoFix(!check.isAutoFix());;

                newState = check.isAutoFix();
            }
        }

        String message = MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig().getString("toggle-"
                + (newState ? "enabled" : "disabled")));

        if (message == null) {
            return;
        }

        sender.sendMessage(message.replace("%player%", player.getName()));
    }

}
