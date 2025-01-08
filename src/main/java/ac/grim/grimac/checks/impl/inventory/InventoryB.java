package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.events.packets.patch.ResyncWorldUtil;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;

@CheckData(name = "InventoryB")
public class InventoryB extends Check implements PacketCheck {

    public InventoryB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            return;
        }

        final var handler = player.checkManager.getPacketCheck(InventoryHandler.class);
        if (handler.getWindowId() == -1) {
            return;
        }

        final var wrapper = lastWrapper(event,
                WrapperPlayClientPlayerBlockPlacement.class,
                () -> new WrapperPlayClientPlayerBlockPlacement(event));

        if (!handler.isSentClose()) {
            handler.closeInventory();
        } else {
            if (!flagAndAlert(new Pair<>("window-id", handler.getWindowId()),
                    new Pair<>("position", wrapper.getBlockPosition()),
                    new Pair<>("face", wrapper.getFace()))) {
                return;
            }
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
        ResyncWorldUtil.resyncPosition(player, wrapper.getBlockPosition());
    }

}
