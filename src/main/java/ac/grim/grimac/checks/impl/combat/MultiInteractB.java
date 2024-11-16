package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@CheckData(name = "MultiInteractB", experimental = true)
public class MultiInteractB extends Check implements PostPredictionCheck {

    // Due to 1.9+ missing the idle packet, we must queue flags
    private final @SuppressWarnings("rawtypes") ObjectArrayList<Pair[]> flags;

    private Vector3f lastPosition;
    private boolean hasInteracted;

    public MultiInteractB(final GrimPlayer player) {
        super(player);

        this.flags = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                ? new ObjectArrayList<>()
                : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            if (flags == null || player.packetStateData.lastPacketWasTeleport) {
                return;
            }

            hasInteracted = false;
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientInteractEntity.class,
                () -> new WrapperPlayClientInteractEntity(event));

        final var position = new WrapperPlayClientInteractEntity(event).getTarget().orElse(null);
        if (position == null) {
            return;
        }

        if (!hasInteracted || !position.equals(lastPosition)) {
            lastPosition = position;
            hasInteracted = true;
            return;
        }

        final var details = new Pair[]{
                new Pair<>("current-position", position),
                new Pair<>("last-position", lastPosition)
        };

        if (flags != null) {
            flags.add(details);
            return;
        }

        if (!flagAndAlert(details)) {
            return;
        }

        if (!shouldModifyPackets()) {
            return;
        }

        event.setCancelled(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (flags == null) {
            return;
        }

        hasInteracted = false;

        if (player.skippedTickInActualMovement || !predictionComplete.isChecked()) {
            flags.clear();
            return;
        }

        final var iterator = flags.iterator();
        while (iterator.hasNext()) {
            flagAndAlert(iterator.next());

            iterator.remove();
        }
    }

}
