package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAC;
import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.math.GrimMath;
import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.configuralize.Language;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ConfigManager {
    @Getter
    private final DynamicConfig config;
    @Getter
    private final File configFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "config.yml");
    @Getter
    private final File messagesFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "messages.yml");
    @Getter
    private final File discordFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "discord.yml");
    @Getter
    private final File punishFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "punishments.yml");
    @Getter
    private int maxPingTransaction = 60; // This is just a really hot variable so cache it.
    @Getter
    private boolean ignoreDuplicatePacketRotation = false;

    @Getter
    private boolean experimentalChecks = false;

    private final List<Pattern> ignoredClientPatterns = new ArrayList<>();

    public ConfigManager() {
        upgrade();

        // load config
        GrimAPI.INSTANCE.getPlugin().getDataFolder().mkdirs();
        config = new DynamicConfig();
        config.addSource(GrimAC.class, "config", getConfigFile());
        config.addSource(GrimAC.class, "messages", getMessagesFile());
        config.addSource(GrimAC.class, "discord", getDiscordFile());
        config.addSource(GrimAC.class, "punishments", getPunishFile());

        reload();
    }

    public void reload() {
        config.setLanguage(Language.EN);

        try {
            config.saveAllDefaults(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save default config files", e);
        }

        try {
            config.loadAll();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config", e);
        }

        final int configuredMaxTransactionTime = config.getIntElse("max-transaction-time", 60);
        maxPingTransaction = (int) GrimMath.clamp(configuredMaxTransactionTime, 1, 180);
        if (maxPingTransaction != configuredMaxTransactionTime) {
            LogUtil.warn("Detected invalid max-transaction-time! This setting is clamped between 1 and 180 to prevent issues. " +
                    "Changed: " + configuredMaxTransactionTime + " -> " + maxPingTransaction);
            LogUtil.warn("Attempting to disable or set this too high can result in memory usage issues.");
        }

        ignoredClientPatterns.clear();
        for (String string : config.getStringList("client-brand.ignored-clients")) {
            try {
                ignoredClientPatterns.add(Pattern.compile(string));
            } catch (PatternSyntaxException e) {
                throw new RuntimeException("Failed to compile client pattern", e);
            }
        }
        experimentalChecks = config.getBooleanElse("experimental-checks", false);
        ignoreDuplicatePacketRotation = config.getBooleanElse("ignore-duplicate-packet-rotation", false);
    }

    public boolean isIgnoredClient(String brand) {
        for (Pattern pattern : ignoredClientPatterns) {
            if (pattern.matcher(brand).find()) return true;
        }
        return false;
    }

    private void upgrade() {
        File config = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "config.yml");
        if (config.exists()) {
            try {
                String configString = new String(Files.readAllBytes(config.toPath()));

                int configVersion = configString.indexOf("config-version: ");

                if (configVersion != -1) {
                    String configStringVersion = configString.substring(configVersion + "config-version: ".length());
                    configStringVersion = configStringVersion.substring(0, !configStringVersion.contains("\n") ? configStringVersion.length() : configStringVersion.indexOf("\n"));
                    configStringVersion = configStringVersion.replaceAll("\\D", "");

                    configVersion = Integer.parseInt(configStringVersion);
                    // TODO: Do we have to hardcode this?
                    configString = configString.replaceAll("config-version: " + configStringVersion, "config-version: 9");
                    Files.write(config.toPath(), configString.getBytes());

                    upgradeModernConfig(config, configString, configVersion);
                } else {
                    removeLegacyTwoPointOne(config);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void upgradeModernConfig(File config, String configString, int configVersion) throws IOException {
        if (configVersion < 1) {
            addMaxPing(config, configString);
        }
        if (configVersion < 2) {
            addMissingPunishments();
        }
        if (configVersion < 3) {
            addBaritoneCheck();
        }
        if (configVersion < 4) {
            newOffsetNewDiscordConf(config, configString);
        }
        if (configVersion < 5) {
            fixBadPacketsAndAdjustPingConfig(config, configString);
        }
        if (configVersion < 6) {
            addSuperDebug(config, configString);
        }
        if (configVersion < 7) {
            removeAlertsOnJoin(config, configString);
        }
        if (configVersion < 8) {
            addPacketSpamThreshold(config, configString);
        }
        if (configVersion < 9) {
            newOffsetHandlingAntiKB(config, configString);
        }
    }

    private void removeLegacyTwoPointOne(File config) throws IOException {
        // If config doesn't have config-version, it's a legacy config
        Files.move(config.toPath(), new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "config-2.1.old.yml").toPath());
    }

    private void addMaxPing(File config, String configString) throws IOException {
        configString += """
                
                
                
                # How long should players have until we keep them for timing out? Default = 2 minutes
                max-ping: 120""";

        Files.write(config.toPath(), configString.getBytes());
    }

    // TODO: Write conversion for this... I'm having issues with windows new lines
    private void addMissingPunishments() {
        File config = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "punishments.yml");
        String configString;
        if (config.exists()) {
            try {
                configString = new String(Files.readAllBytes(config.toPath()));

                // If it works, it isn't stupid.  Only replace it if it exactly matches the default config.
                int commentIndex = configString.indexOf("  # As of 2.2.2 these are just placeholders, there are no Killaura/Aim/Autoclicker checks other than those that");
                if (commentIndex != -1) {

                    configString = configString.substring(0, commentIndex);
                    configString += """
                              Combat:
                                remove-violations-after: 300
                                checks:
                                  - "Killaura"
                                  - "Aim"
                                commands:
                                  - "20:40 [alert]"
                              # As of 2.2.10, there are no AutoClicker checks and this is a placeholder. 2.3 will include AutoClicker checks.
                              Autoclicker:
                                remove-violations-after: 300
                                checks:
                                  - "Autoclicker"
                                commands:
                                  - "20:40 [alert]"
                            """;
                }

                Files.write(config.toPath(), configString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void fixBadPacketsAndAdjustPingConfig(File config, String configString) {
        try {
            configString = configString.replaceAll("max-ping: \\d+", "max-transaction-time: 60");
            Files.write(config.toPath(), configString.getBytes());
        } catch (IOException ignored) {
        }

        File punishConfig = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "punishments.yml");
        String punishConfigString;
        if (punishConfig.exists()) {
            try {
                punishConfigString = new String(Files.readAllBytes(punishConfig.toPath()));
                punishConfigString = punishConfigString.replace("command:", "commands:");
                Files.write(punishConfig.toPath(), punishConfigString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void addBaritoneCheck() {
        File config = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "punishments.yml");
        String configString;
        if (config.exists()) {
            try {
                configString = new String(Files.readAllBytes(config.toPath()));
                configString = configString.replace("      - \"EntityControl\"\n", "      - \"EntityControl\"\n      - \"Baritone\"\n      - \"FastBreak\"\n");
                Files.write(config.toPath(), configString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void newOffsetNewDiscordConf(File config, String configString) throws IOException {
        configString = configString.replace("threshold: 0.0001", "threshold: 0.001"); // 1e-5 -> 1e-4 default flag level
        configString = configString.replace("threshold: 0.00001", "threshold: 0.001"); // 1e-6 -> 1e-4 antikb flag
        Files.write(config.toPath(), configString.getBytes());

        File discordFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "discord.yml");

        if (discordFile.exists()) {
            try {
                String discordString = new String(Files.readAllBytes(discordFile.toPath()));
                discordString += """
                        
                        embed-color: "#00FFFF"
                        violation-content:
                          - "**Player**: %player%"
                          - "**Check**: %check%"
                          - "**Violations**: %violations%"
                          - "**Client Version**: %version%"
                          - "**Brand**: %brand%"
                          - "**Ping**: %ping%"
                          - "**TPS**: %tps%"
                        """;
                Files.write(discordFile.toPath(), discordString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void addSuperDebug(File config, String configString) throws IOException {
        // The default config didn't have this change
        configString = configString.replace("threshold: 0.0001", "threshold: 0.001"); // 1e-5 -> 1e-4 default flag level
        if (!configString.contains("experimental-checks")) {
            configString += """
                    
                    
                    # Enables experimental checks
                    experimental-checks: false
                    
                    """;
        }
        configString += """
                
                verbose:
                  print-to-console: false
                """;
        Files.write(config.toPath(), configString.getBytes());

        File messageFile = new File(GrimAPI.INSTANCE.getPlugin().getDataFolder(), "messages.yml");
        if (messageFile.exists()) {
            try {
                String messagesString = new String(Files.readAllBytes(messageFile.toPath()));
                messagesString += """
                        
                        
                        upload-log: "%prefix% &fUploaded debug to: %url%"
                        upload-log-start: "%prefix% &fUploading log... please wait"
                        upload-log-not-found: "%prefix% &cUnable to find that log"
                        upload-log-upload-failure: "%prefix% &cSomething went wrong while uploading this log, see console for more info"
                        """;
                Files.write(messageFile.toPath(), messagesString.getBytes());
            } catch (IOException ignored) {
            }
        }
    }

    private void removeAlertsOnJoin(File config, String configString) throws IOException {
        configString = configString.replaceAll("  # Should players with grim\\.alerts permission automatically enable alerts on join\\?\r?\n  enable-on-join: (?:true|false)\r?\n", ""); // en
        configString = configString.replaceAll("  # 管理员进入时是否自动开启警告？\r?\n  enable-on-join: (?:true|false)\r?\n", ""); // zh
        Files.write(config.toPath(), configString.getBytes());
    }

    private void addPacketSpamThreshold(File config, String configString) throws IOException {
        configString += """
                
                # Grim sometimes cancels illegal packets such as with timer, after X packets in a second cancelled, when should
                # we simply kick the player? This is required as some packet limiters don't count packets cancelled by grim.
                packet-spam-threshold: 150
                """;
        Files.write(config.toPath(), configString.getBytes());
    }

    private void newOffsetHandlingAntiKB(File config, String configString) throws IOException {
        configString = configString.replaceAll("  # How much of an offset is \"cheating\"\r?\n  # By default this is 1e-5, which is safe and sane\r?\n  # Measured in blocks from the correct movement\r?\n  threshold: 0.001\r?\n  setbackvl: 3",
                "  # How much should we multiply total advantage by when the player is legit\n" +
                        "  setback-decay-multiplier: 0.999\n" +
                        "  # How large of an offset from the player's velocity should we create a violation for?\n" +
                        "  # Measured in blocks from the possible velocity\n" +
                        "  threshold: 0.001\n" +
                        "  # How large of a violation in a tick before the player gets immediately setback?\n" +
                        "  # -1 to disable\n" +
                        "  immediate-setback-threshold: 0.1\n" +
                        "  # How large of an advantage over all ticks before we start to setback?\n" +
                        "  # -1 to disable\n" +
                        "  max-advantage: 1\n" +
                        "  # This is to stop the player from gathering too many violations and never being able to clear them all\n" +
                        "  max-ceiling: 4"
        );
        Files.write(config.toPath(), configString.getBytes());
    }
}
