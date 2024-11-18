package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.player.UserClient;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import lombok.Getter;

public class ClientBrand extends Check implements PacketCheck {

    private final boolean legacyChannel;

    private @Getter String brand = "vanilla";
    private boolean hasBrand = false;

    public ClientBrand(GrimPlayer player) {
        super(player);

        legacyChannel = PacketEvents.getAPI().getServerManager()
                .getVersion()
                .isOlderThan(ServerVersion.V_1_13);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientPluginMessage.class,
                () -> new WrapperPlayClientPluginMessage(event));

        final var channel = packet.getChannelName();
        if (channel.equals(legacyChannel ? "MC|Brand" : "minecraft:brand")) {
            if (hasBrand) {
                return;
            }

            final var data = packet.getData();
            if (data.length > 64 || data.length == 0) {
                return;
            }

            final var dataWithoutPrefix = new byte[data.length - 1];
            System.arraycopy(data, 1, dataWithoutPrefix, 0, dataWithoutPrefix.length);

            brand = new String(dataWithoutPrefix).replace(" (Velocity)", "");
            player.setClient(UserClient.fromString(brand));

            /*if (legacyChannel) {
                sendForgeHandshake();
            }*/

            hasBrand = true;
            return;
        }

        /*if (channel.equals(legacyChannel ? "FML|HS" : "")) {
            List<UserMod> mods = new ArrayList<>();

            byte[] data = packet.getData();
            String modId = null;
            for (int index = 2; index < data.length; ) {
                int endIndex = index + data[index] + 1;

                String content = new String(Arrays.copyOfRange(data, index + 1, endIndex));
                if (modId == null) {
                    modId = content;
                } else {
                    if (!modId.equals("FML") && !modId.equals("Forge") && !modId.equals("mcp")) {
                        mods.add(new UserMod(modId, content));
                    }

                    modId = null;
                }

                index = endIndex;
            }

            if (!mods.isEmpty()) {
                player.getModList().addAll(mods);
            }

        }*/

    }

    /*private void sendForgeHandshake() {
        player.runNettyTaskInMs(() -> {
            player.user.writePacket(new WrapperPlayServerPluginMessage("FML|HS", new byte[] { -2, 0 }));
            player.user.writePacket(new WrapperPlayServerPluginMessage("FML|HS", new byte[] { 0, 2, 0, 0, 0, 0 }));
            player.user.writePacket(new WrapperPlayServerPluginMessage("FML|HS", new byte[] { 2, 0, 0, 0, 0 }));
        }, 100);
    }*/

}
