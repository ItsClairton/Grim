package ac.grim.grimac.checks.impl.inventory;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenHorseWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import lombok.Getter;

public class InventoryHandler extends Check implements PacketCheck {

    private @Getter int windowId = -1;

    private final boolean legacyClient;
    private @Getter boolean sentClose;

    public InventoryHandler(GrimPlayer player) {
        super(player);

        this.legacyClient = player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_11_1);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_STATUS) {
            final var wrapper = lastWrapper(event,
                    WrapperPlayClientClientStatus.class,
                    () -> new WrapperPlayClientClientStatus(event));

            if (wrapper.getAction() == WrapperPlayClientClientStatus.Action.OPEN_INVENTORY_ACHIEVEMENT) {
                if (!legacyClient) {
                    return;
                }

                setWindowId(0);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
            if (legacyClient) {
                return;
            }

            final var wrapper = lastWrapper(event,
                    WrapperPlayClientClickWindow.class,
                    () -> new WrapperPlayClientClickWindow(event));

            setWindowId(wrapper.getWindowId());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            setWindowId(-1);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.OPEN_WINDOW) {
            final var window = lastWrapper(event,
                    WrapperPlayServerOpenWindow.class,
                    () -> new WrapperPlayServerOpenWindow(event));

            // Thanks mojang, another disgraceful desync
            // when the player clicks on the "X" of the beacon
            // the client does not send a packet informing.
            if(window.getType() == 8 || "minecraft:beacon".equals(window.getLegacyType())) {
                return;
            }

            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> setWindowId(window.getContainerId()));
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            final var wrapper = lastWrapper(event,
                    WrapperPlayServerOpenHorseWindow.class,
                    () -> new WrapperPlayServerOpenHorseWindow(event));

            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> setWindowId(wrapper.getWindowId()));
        }

        if (event.getPacketType() == PacketType.Play.Server.CLOSE_WINDOW) {
            player.sendTransaction();
            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> setWindowId(-1));
        }
    }

    public void closeInventory() {
        if (sentClose) {
            return;
        }

        player.user.sendPacketSilently(new WrapperPlayServerCloseWindow(windowId));
        player.sendTransaction();
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
            this.sentClose = true;
            this.windowId = -1;
        });
    }

    public void handleRespawn() {
        setWindowId(-1);
    }

    private void setWindowId(int windowId) {
        this.sentClose = false;
        this.windowId = windowId;
    }

}
