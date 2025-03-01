package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "GroundSpoof", configName = "GroundSpoof", setback = 5, decay = 0.01)
public class NoFallB extends Check implements PostPredictionCheck {

    public NoFallB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Don't check players in spectator
        if (player.gamemode == GameMode.SPECTATOR && PacketEvents.getAPI().getServerManager()
                .getVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_8)) {
            return;
        }

        // And don't check this long list of ground exemptions
        if (player.exemptOnGround() || !predictionComplete.isChecked()) {
            return;
        }

        // Don't check if the player was on a ghost block
        if (player.getSetbackTeleportUtil().blockOffsets) {
            return;
        }

        // Viaversion sends wrong ground status... (doesn't matter but is annoying)
        if (player.packetStateData.lastPacketWasTeleport) {
            return;
        }

        boolean invalid = player.clientClaimsLastOnGround != player.onGround;
        if (!invalid) {
            return;
        }

        if (shouldModifyPackets()) {
            player.checkManager.getNoFall().flipPlayerGroundStatus = true;
        }

        if (player.getSetbackTeleportUtil().insideUnloadedChunk() || player.getSetbackTeleportUtil().blockOffsets) {
            return;
        }

        if (!flagWithSetback()) {
            return;
        }

        alert(new Pair<>("server", player.onGround), new Pair<>("client", player.clientClaimsLastOnGround));
    }

}
