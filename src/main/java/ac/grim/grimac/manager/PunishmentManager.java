package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.events.AlertEvent;
import ac.grim.grimac.api.events.CommandExecuteEvent;
import ac.grim.grimac.api.events.PunishEvent;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.events.packets.ProxyAlertMessenger;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.data.Pair;
import github.scarsz.configuralize.DynamicConfig;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishmentManager {
    GrimPlayer player;
    List<PunishGroup> groups = new ArrayList<>();
    String experimentalSymbol = "*";

    public PunishmentManager(GrimPlayer player) {
        this.player = player;
        reload();
    }

    public void reload() {
        DynamicConfig config = GrimAPI.INSTANCE.getConfigManager().getConfig();
        List<String> punish = config.getStringListElse("Punishments", new ArrayList<>());
        experimentalSymbol = config.getStringElse("experimental-symbol", "*");

        try {
            groups.clear();

            // To support reloading
            for (AbstractCheck check : player.checkManager.allChecks.values()) {
                check.setEnabled(false);
            }

            for (Object s : punish) {
                LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) s;

                List<String> checks = (List<String>) map.getOrDefault("checks", new ArrayList<>());
                List<String> commands = (List<String>) map.getOrDefault("commands", new ArrayList<>());
                int removeViolationsAfter = (int) map.getOrDefault("remove-violations-after", 300);

                List<ParsedCommand> parsed = new ArrayList<>();
                List<AbstractCheck> checksList = new ArrayList<>();
                List<AbstractCheck> excluded = new ArrayList<>();
                for (String command : checks) {
                    command = command.toLowerCase(Locale.ROOT);
                    boolean exclude = false;
                    if (command.startsWith("!")) {
                        exclude = true;
                        command = command.substring(1);
                    }
                    for (AbstractCheck check : player.checkManager.allChecks.values()) { // o(n) * o(n)?
                        if (check.getCheckName() != null &&
                                (check.getCheckName().toLowerCase(Locale.ROOT).contains(command)
                                        || check.getAlternativeName().toLowerCase(Locale.ROOT).contains(command))) { // Some checks have equivalent names like AntiKB and AntiKnockback
                            if (exclude) {
                                excluded.add(check);
                            } else {
                                checksList.add(check);
                                check.setEnabled(true);
                            }
                        }
                    }
                    for (AbstractCheck check : excluded) checksList.remove(check);
                }

                for (String command : commands) {
                    String firstNum = command.substring(0, command.indexOf(":"));
                    String secondNum = command.substring(command.indexOf(":"), command.indexOf(" "));

                    int threshold = Integer.parseInt(firstNum);
                    int interval = Integer.parseInt(secondNum.substring(1));
                    String commandString = command.substring(command.indexOf(" ") + 1);

                    parsed.add(new ParsedCommand(threshold, interval, commandString));
                }

                groups.add(new PunishGroup(checksList, parsed, removeViolationsAfter));
            }
        } catch (Exception e) {
            LogUtil.error("Error while loading punishments.yml! This is likely your fault!");
            e.printStackTrace();
        }
    }

    @SafeVarargs
    private String replaceAlertPlaceholders(String original,
                                            PunishGroup group,
                                            Check check,
                                            String alertString,
                                            boolean hover,
                                            Pair<String, String>... verboseEntries) {
        // Streams are slow but this isn't a hot path... it's fine.
        String vl = group.violations.values().stream().filter((e) -> e == check).count() + "";

        String verbose;

        if (hover) {
            String format = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("hover-verbose-format",
                    "%key%: %value%");

            verbose = Arrays.stream(verboseEntries)
                    .map(detail -> format
                            .replace("%key%", StringUtils.capitalize(detail.first()))
                            .replace("%value%", detail.second()))
                    .collect(Collectors.joining("\n"));
        } else {
            String format = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alert-verbose-format",
                    "%key%: %value%");

            verbose = Arrays.stream(verboseEntries)
                    .map(detail -> format
                            .replace("%key%", StringUtils.capitalize(detail.first()))
                            .replace("%value%", detail.second()))
                    .collect(Collectors.joining(","));
        }

        original = MessageUtil.format(original
                .replace("[alert]", alertString)
                .replace("[proxy]", alertString)
                .replace("%check_name%", check.getCheckName())
                .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "")
                .replace("%vl%", vl)
                .replace("%verbose%", verbose)
                .replace("%description%", check.getDescription())
                .replace("%server%", Bukkit.getName())
        );

        original = GrimAPI.INSTANCE.getExternalAPI().replaceVariables(player, original, true);

        return original;
    }

    @SafeVarargs
    public final boolean handleAlert(GrimPlayer player, Check check, Pair<String, String>... verboseEntries) {
        String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getString("alerts-format");
        boolean sentDebug = false;

        // Check commands
        for (PunishGroup group : groups) {
            if (group.getChecks().contains(check)) {
                int violationCount = group.getViolations().size();
                for (ParsedCommand command : group.getCommands()) {
                    String cmd = replaceAlertPlaceholders(command.getCommand(), group, check, alertString, false, verboseEntries);

                    String baseHover = replaceAlertPlaceholders(command.getCommand(),
                            group,
                            check,
                            GrimAPI.INSTANCE.getConfigManager()
                                    .getConfig()
                                    .getStringElse("alerts-format-hover", ""),
                            true,
                            verboseEntries);

                    BaseComponent[] components = TextComponent.fromLegacyText(cmd);
                    for (BaseComponent component : components) {
                        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(baseHover)));
                    }

                    // Verbose that prints all flags
                    if (command.command.equals("[alert]")) {
                        sentDebug = true;

                        AlertEvent event = new AlertEvent(
                                player,
                                check,
                                Arrays.stream(verboseEntries).collect(Collectors.toMap(Pair::first, Pair::second)),
                                new ArrayList<>(GrimAPI.INSTANCE.getAlertManager().getEnabledVerbose()),
                                true);

                        if (!event.isCancelled()) {
                            for (Player receiver : event.getReceivers()) {
                                receiver.spigot().sendMessage(components);
                            }

                            if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("verbose.print-to-console", false)) {
                                LogUtil.console(String.format("%s Verbose: (%s).",
                                        cmd,
                                        Arrays.stream(verboseEntries)
                                                .map(vb -> vb.first() + ": " + vb.second())
                                                .collect(Collectors.joining(", ")))); // Print verbose to console
                            }
                        }
                    }

                    if (violationCount >= command.getThreshold()) {
                        // 0 means execute once
                        // Any other number means execute every X interval
                        boolean inInterval = command.getInterval() == 0 ? (command.executeCount == 0) : (violationCount % command.getInterval() == 0);
                        if (inInterval) {
                            CommandExecuteEvent executeEvent = new CommandExecuteEvent(player, check, cmd);
                            Bukkit.getPluginManager().callEvent(executeEvent);
                            if (executeEvent.isCancelled()) continue;

                            switch (command.command) {
                                case "[proxy]" -> {
                                    String proxyAlertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getString("alerts-format-proxy");
                                    proxyAlertString = replaceAlertPlaceholders(command.getCommand(), group, check, proxyAlertString, false, verboseEntries);
                                    ProxyAlertMessenger.sendPluginMessage(proxyAlertString);
                                }
                                case "[alert]" -> {
                                    AlertEvent event = new AlertEvent(
                                            player,
                                            check,
                                            Arrays.stream(verboseEntries).collect(Collectors.toMap(Pair::first, Pair::second)),
                                            new ArrayList<>(GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts()),
                                            false);

                                    Bukkit.getPluginManager().callEvent(event);

                                    if (!event.isCancelled()) {
                                        for (Player receiver : event.getReceivers()) {
                                            receiver.spigot().sendMessage(components);
                                        }
                                    }
                                }
                                case "[punish]" -> {
                                    PunishEvent event = new PunishEvent(player, check);
                                    Bukkit.getPluginManager().callEvent(event);
                                }
                                default ->
                                        FoliaScheduler.getGlobalRegionScheduler().run(GrimAPI.INSTANCE.getPlugin(), (dummy) ->
                                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                            }
                        }

                        command.setExecuteCount(command.getExecuteCount() + 1);
                    }
                }
            }
        }
        return sentDebug;
    }

    public void handleViolation(Check check) {
        for (PunishGroup group : groups) {
            if (group.getChecks().contains(check)) {
                long currentTime = System.currentTimeMillis();

                group.violations.put(currentTime, check);
                // Remove violations older than the defined time in the config
                group.violations.entrySet().removeIf(time -> currentTime - time.getKey() > group.removeViolationsAfter);
            }
        }
    }
}

class PunishGroup {
    @Getter
    List<AbstractCheck> checks;
    @Getter
    List<ParsedCommand> commands;
    @Getter
    HashMap<Long, Check> violations = new HashMap<>();
    @Getter
    int removeViolationsAfter;

    public PunishGroup(List<AbstractCheck> checks, List<ParsedCommand> commands, int removeViolationsAfter) {
        this.checks = checks;
        this.commands = commands;
        this.removeViolationsAfter = removeViolationsAfter * 1000;
    }
}

class ParsedCommand {
    @Getter
    int threshold;
    @Getter
    int interval;
    @Getter
    @Setter
    int executeCount;
    @Getter
    String command;

    public ParsedCommand(int threshold, int interval, String command) {
        this.threshold = threshold;
        this.interval = interval;
        this.command = command;
    }
}
