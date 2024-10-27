package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;

@CheckData(name = "MultiPlace", experimental = true)
public class MultiPlace extends BlockPlaceCheck {

    private final ObjectArrayFIFOQueue<Pair<String, Object>[]> flags;

    private boolean hasPlaced;
    private BlockFace lastFace;
    private Vector3f lastCursor;
    private Vector3i lastPos;

    public MultiPlace(GrimPlayer player) {
        super(player);
        this.flags = player.getClientVersion().isNewerThan(ClientVersion.V_1_8) ? new ObjectArrayFIFOQueue<>() : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBlockPlace(final BlockPlace place) {
        final var face = place.getDirection();
        final var cursor = place.getCursor();
        final var pos = place.getPlacedAgainstBlockLocation();

        if (hasPlaced && (face != lastFace || !cursor.equals(lastCursor) || !pos.equals(lastPos))) {
            final var verbose = new Pair[]{
                    new Pair<>("face", face),
                    new Pair<>("last-face", lastFace),
                    new Pair<>("cursor", cursor),
                    new Pair<>("position", pos),
                    new Pair<>("last-position", lastPos)
            };

            if (flags == null) {
                if (flagAndAlert(verbose) && shouldModifyPackets() && shouldCancel()) {
                    place.resync();
                }
            } else {
                flags.enqueue(verbose);
            }
        }

        lastFace = face;
        lastCursor = cursor;
        lastPos = pos;
        hasPlaced = true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())
                && !player.packetStateData.lastPacketWasTeleport
                && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            hasPlaced = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked() || player.skippedTickInActualMovement || flags == null) {
            return;
        }

        while (!flags.isEmpty()) {
            flagAndAlert(flags.dequeue());
        }
    }

}
