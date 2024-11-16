package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.events.FlagEvent;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import github.scarsz.configuralize.DynamicConfig;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check implements AbstractCheck {
    protected final GrimPlayer player;

    public double violations;
    private double decay;
    private double setbackVL;

    private String checkName;
    private String configName;
    private String alternativeName;

    private boolean experimental;
    @Setter
    private boolean isEnabled;

    private @Getter
    @Setter boolean autoFix = true;

    @Override
    public boolean isExperimental() {
        return experimental;
    }

    public Check(final GrimPlayer player) {
        this.player = player;

        final Class<?> checkClass = this.getClass();

        if (checkClass.isAnnotationPresent(CheckData.class)) {
            final CheckData checkData = checkClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.configName = checkData.configName();
            // Fall back to check name
            if (this.configName.equals("DEFAULT")) this.configName = this.checkName;
            this.decay = checkData.decay();
            this.setbackVL = checkData.setback();
            this.alternativeName = checkData.alternativeName();
            this.experimental = checkData.experimental();
        }

        reload();
    }

    public boolean shouldModifyPackets() {
        return isEnabled && autoFix && !player.disableGrim;
    }

    @SafeVarargs
    public final boolean flagAndAlert(Pair<String, Object>... verbose) {
        if (flag()) {
            alert(verbose);
            return true;
        }

        return false;
    }

    public final boolean flag() {
        if (player.disableGrim || (experimental && !GrimAPI.INSTANCE.getConfigManager().isExperimentalChecks()))
            return false; // Avoid calling event if disabled

        FlagEvent event = new FlagEvent(player, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        player.punishmentManager.handleViolation(this);

        violations++;
        return true;
    }

    public final boolean flagWithSetback() {
        if (flag()) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final void reward() {
        violations = Math.max(0, violations - decay);
    }

    public void reload() {
        decay = getConfig().getDoubleElse(configName + ".decay", decay);
        setbackVL = getConfig().getDoubleElse(configName + ".setbackvl", setbackVL);

        if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final boolean alert(Pair<String, Object>... verboseEntries) {
        Pair<String, String>[] serializedEntries = Arrays.stream(verboseEntries)
                .map(entry -> {
                    if (entry.second() instanceof String) {
                        return entry;
                    }

                    String value = entry.second().toString();
                    return new Pair<>(entry.first(), value);
                }).toArray(Pair[]::new);

        return player.punishmentManager.handleAlert(player, this, serializedEntries);
    }

    public DynamicConfig getConfig() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig();
    }

    public boolean setbackIfAboveSetbackVL() {
        if (getViolations() > setbackVL && shouldModifyPackets()) {
            // 1 second late, we will force the player to sync again.
            if (player.getTransactionPing() >= 1000) {
                player.getSetbackTeleportUtil().executeForceResync();
                return true;
            } else {
                return player.getSetbackTeleportUtil().executeViolationSetback();
            }
        }

        return false;
    }

    public boolean isAboveSetbackVl() {
        return getViolations() > setbackVL;
    }

    public String formatOffset(double offset) {
        return offset > 0.001 ? String.format("%.5f", offset) : String.format("%.2E", offset);
    }

    public boolean isTransaction(PacketTypeCommon packetType) {
        return packetType == PacketType.Play.Client.PONG ||
                packetType == PacketType.Play.Client.WINDOW_CONFIRMATION;
    }

    @Override
    public String getDescription() {
        return getConfig().getStringElse("checks." + getConfigName() + ".description", "N/A");
    }

}
