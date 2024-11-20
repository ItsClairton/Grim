package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(name = "InventoryC")
public class InventoryC extends Check implements PacketCheck {

    public InventoryC(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) {
            return;
        }

        final var wrapper = lastWrapper(event,
                WrapperPlayClientClickWindow.class,
                () -> new WrapperPlayClientClickWindow(event));

        final var handler = player.checkManager.getPacketCheck(InventoryHandler.class);

        if (wrapper.getWindowId() == handler.getWindowId()) {
            return;
        }

        if (player.getSetbackTeleportUtil().hasAcceptedSpawnTeleport && !flagAndAlert(
                new Pair<>("window-id", wrapper.getWindowId()),
                new Pair<>("expected-window-id", handler.getWindowId()),
                new Pair<>("button", wrapper.getButton()),
                new Pair<>("slot", wrapper.getSlot()))) {
            return;
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
    }

}
