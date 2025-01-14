package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTabComplete;

@CheckData(name = "CrashH")
public class CrashH extends Check implements PacketCheck {

    public CrashH(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.TAB_COMPLETE) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientTabComplete.class,
                () -> new WrapperPlayClientTabComplete(event));

        final var text = packet.getText();
        final var length = text.length();

        // general length limit
        if (length > 256) {
            if (shouldModifyPackets()) {
                event.setCancelled(true);
            }
            flagAndAlert(new Pair<>("cause", "length"), new Pair<>("length", length));
            return;
        }

        // paper's patch
        final int index;
        if (text.length() > 64 && ((index = text.indexOf(' ')) == -1 || index >= 64)) {
            if (shouldModifyPackets()) {
                event.setCancelled(true);
            }

            flagAndAlert(new Pair<>("cause", "space"), new Pair<>("length", length));
        }
    }

}
