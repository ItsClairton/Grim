package ac.grim.grimac.checks.impl.badpackets;

import static ac.grim.grimac.events.packets.patch.ResyncWorldUtil.resyncPosition;
import static ac.grim.grimac.utils.nmsutil.BlockBreakSpeed.getBlockDamage;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "BadPacketsZ", experimental = true)
public class BadPacketsZ extends Check implements PacketCheck {

    private boolean lastBlockWasInstantBreak = false;
    private Vector3i lastBlock, lastCancelledBlock, lastLastBlock = null;
    private final int exemptedY;

    private final boolean expectedSameY = player.getClientVersion().isOlderThan(ClientVersion.V_1_14_4);

    public BadPacketsZ(final GrimPlayer player) {
        super(player);

        ClientVersion clientVersion = player.getClientVersion();
        if (clientVersion.isOlderThan(ClientVersion.V_1_8)) {
            exemptedY = 255;
        } else {
            ServerVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion();
            exemptedY = serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14) ? -1 : 4095;
        }
    }

    // The client sometimes sends a wierd cancel packet
    private boolean shouldExempt(final Vector3i pos) {
        // lastLastBlock is always null when this happens, and lastBlock isn't
        if (lastLastBlock != null || lastBlock == null) {
            return false;
        }

        // on pre 1.14.4 clients, the YPos of this packet is always the same
        if (expectedSameY && pos.y != exemptedY) {
            return false;
        }

        // and if this block is not an instant break
        return expectedSameY || getBlockDamage(player, pos) < 1;
    }

    private String formatted(Vector3i vec) {
        return vec == null ? "N/A" : vec.x + ", " + vec.y + ", " + vec.z;
    }

    public void handle(PacketReceiveEvent event, WrapperPlayClientPlayerDigging dig) {
        if (dig.getAction() == DiggingAction.START_DIGGING) {
            final Vector3i pos = dig.getBlockPosition();

            lastBlockWasInstantBreak = getBlockDamage(player, pos) >= 1;
            lastCancelledBlock = null;
            lastLastBlock = lastBlock;
            lastBlock = pos;
            return;
        }

        if (dig.getAction() == DiggingAction.CANCELLED_DIGGING) {
            final Vector3i pos = dig.getBlockPosition();

            if (shouldExempt(pos)) {
                lastCancelledBlock = pos;
                lastLastBlock = null;
                lastBlock = null;
                return;
            }

            if (!pos.equals(lastBlock)) {
                // https://github.com/GrimAnticheat/Grim/issues/1512
                if (expectedSameY || (!lastBlockWasInstantBreak && pos.equals(lastCancelledBlock))) {
                    if (flagAndAlert(new Pair<>("action", "CANCELLED_DIGGING"),
                            new Pair<>("last-block", formatted(lastBlock)),
                            new Pair<>("position", formatted(pos)))) {
                        if (shouldModifyPackets()) {
                            event.setCancelled(true);
                            resyncPosition(player, pos);
                        }
                    }
                }
            }

            lastCancelledBlock = pos;
            lastLastBlock = null;
            lastBlock = null;
            return;
        }

        if (dig.getAction() == DiggingAction.FINISHED_DIGGING) {
            final Vector3i pos = dig.getBlockPosition();

            // when a player looks away from the mined block, they send a cancel, and if they look at it again, they don't send another start. (thanks mojang!)
            if (!pos.equals(lastCancelledBlock) && (!lastBlockWasInstantBreak || expectedSameY) && !pos.equals(lastBlock)) {
                if (flagAndAlert(new Pair<>("action", "FINISHED_DIGGING"),
                        new Pair<>("last-block", formatted(lastBlock)),
                        new Pair<>("position", formatted(pos)))) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        resyncPosition(player, pos);
                    }
                }
            }

            lastCancelledBlock = null;

            // 1.14.4+ clients don't send another start break in protected regions
            if (expectedSameY) {
                lastLastBlock = null;
                lastBlock = null;
            }
        }
    }

}
