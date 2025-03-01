package ac.grim.grimac.commands;

import ac.grim.grimac.predictionengine.MovementCheckRunner;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@CommandAlias("ac")
public class PerfSubCommand extends BaseCommand {

    @Subcommand("perf|performance")
    @CommandPermission("ac.performance")
    public void onPerformance(CommandSender sender) {
        double millis = MovementCheckRunner.predictionNanos / 1000000;
        double longMillis = MovementCheckRunner.longPredictionNanos / 1000000;

        sender.sendMessage(ChatColor.GRAY + "Milliseconds per prediction (avg. 500): " + ChatColor.WHITE + millis);
        sender.sendMessage(ChatColor.GRAY + "Milliseconds per prediction (avg. 20k): " + ChatColor.WHITE + longMillis);
    }

}
