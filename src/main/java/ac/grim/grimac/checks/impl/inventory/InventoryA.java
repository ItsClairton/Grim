package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "InventoryA")
public class InventoryA extends Check implements PacketCheck {

    public InventoryA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        final var handler = player.checkManager.getPacketCheck(InventoryHandler.class);
        if (handler.getWindowId() == -1) {
            return;
        }

        final var wrapper = lastWrapper(event,
                WrapperPlayClientInteractEntity.class,
                () -> new WrapperPlayClientInteractEntity(event));

        if (!handler.isSentClose()) {
            handler.closeInventory();
        } else {
            if (!flagAndAlert(new Pair<>("window-id", handler.getWindowId()),
                    new Pair<>("entity-id", wrapper.getEntityId()),
                    new Pair<>("action", wrapper.getAction()))) {
                return;
            }
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
    }

}
