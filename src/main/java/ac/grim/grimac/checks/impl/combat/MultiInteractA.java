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
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@CheckData(name = "MultiInteractA", experimental = true)
public class MultiInteractA extends Check implements PostPredictionCheck {

    // Due to 1.9+ missing the idle packet, we must queue flags
    private final @SuppressWarnings("rawtypes") ObjectArrayList<Pair[]> flags;

    private int lastEntityId;
    private boolean lastSneaking;

    private boolean hasInteracted;

    public MultiInteractA(final GrimPlayer player) {
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

        final var entityId = packet.getEntityId();
        final var sneaking = packet.isSneaking().orElse(false);

        if (!hasInteracted || lastEntityId == entityId) {
            lastEntityId = entityId;
            lastSneaking = sneaking;
            hasInteracted = true;
            return;
        }

        final var details = new Pair[]{
                new Pair<>("last-entity-id", lastEntityId),
                new Pair<>("current-entity-id", entityId),
                new Pair<>("last-sneaking", lastSneaking),
                new Pair<>("current-sneaking", sneaking)
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

        if (!player.isTickingReliablyFor(3)) {
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
