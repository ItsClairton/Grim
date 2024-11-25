package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@CheckData(name = "BadPacketsE")
public class BadPacketsE extends Check implements PacketCheck {

    private byte noReminderTicks;

    public BadPacketsE(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final var packet = event.getPacketType();

        if (packet == Client.PLAYER_POSITION_AND_ROTATION
                || packet == Client.PLAYER_POSITION
                || packet == Client.STEER_VEHICLE
                || packet == Client.VEHICLE_MOVE) {
            noReminderTicks = 0;
            return;
        }

        if (!WrapperPlayClientPlayerFlying.isFlying(packet)) {
            return;
        }

        if (noReminderTicks+1 <= 20) {
            noReminderTicks++;
            return;
        }

        flagAndAlert();
    }

    public void handleRespawn() {
        noReminderTicks = 0;
    }

}
