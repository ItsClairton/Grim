package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;

@CheckData(name = "BadPacketsQ")
public class BadPacketsQ extends Check implements PacketCheck {

    public BadPacketsQ(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != Client.ENTITY_ACTION) {
            return;
        }

        // Checking if player replied first transaction, this fix falses with lagging players.
        if (player.lastTransactionReceived.get() == 0) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientEntityAction.class,
                () -> new WrapperPlayClientEntityAction(event));

        if (!(packet.getJumpBoost() < 0 || packet.getJumpBoost() > 100
                || packet.getEntityId() != player.entityID
                || (packet.getAction() != Action.START_JUMPING_WITH_HORSE && packet.getJumpBoost() != 0))) {
            return;
        }

        if (!flagAndAlert(new Pair<>("boost", packet.getJumpBoost()),
                new Pair<>("action", packet.getAction()),
                new Pair<>("entity-id", packet.getEntityId()))) {
            return;
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
    }

}
