package ac.grim.grimac.checks.impl.post;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.*;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@CheckData(name = "Post")
public class PostCheck extends Check implements PacketCheck, PostPredictionCheck {

    private int postPacketId = -1;

    // Due to 1.9+ missing the idle packet, we must queue flags
    private final ObjectArrayList<PacketTypeCommon> flags;

    private boolean sentFlying = false;
    private int isExemptFromSwingingCheck = -1;

    private final IntArrayList availableTypes = new IntArrayList();

    public PostCheck(GrimPlayer player) {
        super(player);

        this.flags = player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)
                ? new ObjectArrayList<>()
                : null;

        final var clientVersion = player.getClientVersion();

        availableTypes.add(ANIMATION.getId(clientVersion));
        availableTypes.add(PLAYER_ABILITIES.getId(clientVersion));
        availableTypes.add(INTERACT_ENTITY.getId(clientVersion));
        availableTypes.add(PLAYER_BLOCK_PLACEMENT.getId(clientVersion));
        availableTypes.add(USE_ITEM.getId(clientVersion));
        availableTypes.add(PLAYER_DIGGING.getId(clientVersion));
        availableTypes.add(ENTITY_ACTION.getId(clientVersion));

        if (clientVersion.isOlderThan(ClientVersion.V_1_13)) {
            availableTypes.add(CLICK_WINDOW.getId(clientVersion));
        }

        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_8)) {
            availableTypes.add(HELD_ITEM_CHANGE.getId(clientVersion));
        }

    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (flags == null || flags.isEmpty()) {
            return;
        }

        if (!player.isTickingReliablyFor(3)) {
            return;
        }

        final var iterator = flags.iterator();
        while (iterator.hasNext()) {
            final var packetType = iterator.next();
            flagAndAlert(new Pair<>("packet", packetType.getName()));

            iterator.remove();
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_ANIMATION) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayServerEntityAnimation.class,
                () -> new WrapperPlayServerEntityAnimation(event));

        if (packet.getEntityId() != player.entityID) {
            return;
        }

        final var animationType = packet.getType();
        if (animationType != EntityAnimationType.SWING_MAIN_ARM && animationType != EntityAnimationType.SWING_OFF_HAND) {
            return;
        }

        isExemptFromSwingingCheck = player.lastTransactionSent.get();
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        final var packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            // Don't count teleports or duplicates as movements
            if (player.packetStateData.lastPacketWasTeleport || player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
                return;
            }

            postPacketId = -1;
            sentFlying = true;
            return;
        }

        if (isTransaction(packetType) && player.packetStateData.lastTransactionPacketWasValid) {
            if (sentFlying && postPacketId != -1) {
                final var flaggedType = PacketType.Play.Client.getById(player.getClientVersion(), postPacketId);
                if (flaggedType == null) {
                    throw new IllegalStateException("not found packet with id " + postPacketId);
                }

                if (flags == null) {
                    flagAndAlert(new Pair<>("packet", flaggedType.getName()));
                } else {
                    flags.add(flaggedType);
                }
            }

            postPacketId = -1;
            sentFlying = false;
            return;
        }

        if (!sentFlying) {
            return;
        }

        if (postPacketId != -1) {
            return;
        }

        final var packetId = event.getPacketId();
        if (!availableTypes.contains(packetId)) {
            return;
        }

        final var clientVersion = player.getClientVersion();

        if (ANIMATION.equals(packetType)) {
            if ((clientVersion.isNewerThanOrEquals(ClientVersion.V_1_9) // ViaVersion delays animations for 1.8 clients
                    || PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_8_8)) // when on 1.9+ servers
                    && clientVersion.isOlderThan(ClientVersion.V_1_13) // 1.13 clicking inventory causes weird animations
                    && isExemptFromSwingingCheck < player.lastTransactionReceived.get()) {
                postPacketId = packetId;
            }

            return;
        }

        if (ENTITY_ACTION.equals(packetType)) {
            if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_9) || lastWrapper(event,
                    WrapperPlayClientEntityAction.class,
                    () -> new WrapperPlayClientEntityAction(event)).getAction() != Action.START_FLYING_WITH_ELYTRA) {

                // https://github.com/GrimAnticheat/Grim/issues/824
                if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_19_3) && player.compensatedEntities
                        .getSelf()
                        .getRiding() != null) {
                    return;
                }

                postPacketId = packetId;
            }

            return;
        }

        postPacketId = packetId;
    }

}
