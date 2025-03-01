package ac.grim.grimac.manager;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.alerts.AlertManager;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.Getter;
import org.bukkit.entity.Player;

public class AlertManagerImpl implements AlertManager {
    @Getter
    private final Set<Player> enabledAlerts = new CopyOnWriteArraySet<>(new HashSet<>());
    @Getter
    private final Set<Player> enabledVerbose = new CopyOnWriteArraySet<>(new HashSet<>());

    @Override
    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.contains(player);
    }

    @Override
    public void toggleAlerts(Player player) {
        if (!enabledAlerts.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-enabled", "%prefix% &fAlerts enabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
            enabledAlerts.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("alerts-disabled", "%prefix% &fAlerts disabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
        }
    }

    @Override
    public boolean hasVerboseEnabled(Player player) {
        return enabledVerbose.contains(player);
    }

    @Override
    public void toggleVerbose(Player player) {
        if (!enabledVerbose.remove(player)) {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-enabled", "%prefix% &fVerbose enabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
            enabledVerbose.add(player);
        } else {
            String alertString = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("verbose-disabled", "%prefix% &fVerbose disabled");
            alertString = MessageUtil.format(alertString);
            player.sendMessage(alertString);
        }
    }

    public void handlePlayerQuit(Player player) {
        enabledAlerts.remove(player);
        enabledVerbose.remove(player);
    }
}
