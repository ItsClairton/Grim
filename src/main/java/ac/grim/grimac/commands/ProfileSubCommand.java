package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.anticheat.MultiLibUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import org.bukkit.command.CommandSender;

@CommandAlias("ac")
public class ProfileSubCommand extends BaseCommand {

    @Subcommand("profile")
    @CommandPermission("ac.profile")
    @CommandCompletion("@players")
    public void onConsoleDebug(CommandSender sender, OnlinePlayer target) {
        // Short circuit due to minimum java requirements for MultiLib
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18) && MultiLibUtil.isExternalPlayer(target.getPlayer())) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-this-server", "%prefix% &cThis player isn't on this server!");
            sender.sendMessage(MessageUtil.format(alertString));
            return;
        }

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(target.getPlayer());
        if (grimPlayer == null) {
            String message = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-found", "%prefix% &cPlayer is exempt or offline!");
            sender.sendMessage(MessageUtil.format(message));
            return;
        }

        for (String message : GrimAPI.INSTANCE.getConfigManager().getConfig().getStringList("profile")) {
            message = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(grimPlayer, message, true);
            sender.sendMessage(message);
        }
    }

}
