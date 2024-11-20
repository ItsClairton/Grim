package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

public class InventoryD extends Check implements PacketCheck {

    public InventoryD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) {
            return;
        }

        final var wrapper = lastWrapper(event,
                WrapperPlayClientPlayerDigging.class,
                () -> new WrapperPlayClientPlayerDigging(event));

        if (wrapper.getAction() != DiggingAction.START_DIGGING) {
            return;
        }

        final var handler = player.checkManager.getPacketCheck(InventoryHandler.class);
        if (handler.getWindowId() == -1) {
            return;
        }

        if (player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport && !flagAndAlert(
                new Pair<>("window-id", handler.getWindowId()),
                new Pair<>("action", wrapper.getAction()),
                new Pair<>("position", wrapper.getBlockPosition()))) {
            return;
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
        ResyncWorldUtil.resyncPosition(player, wrapper.getBlockPosition());
    }

}
