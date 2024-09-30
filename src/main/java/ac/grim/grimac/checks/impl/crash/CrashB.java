package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "CrashB")
public class CrashB extends Check implements PacketCheck {

    public CrashB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            return;
        }

        if (player.gamemode == GameMode.CREATIVE) {
            return;
        }

        player.getSetbackTeleportUtil().executeViolationSetback();
        event.setCancelled(true);
        flagAndAlert(); // Could be transaction split, no need to setback though
    }

}
