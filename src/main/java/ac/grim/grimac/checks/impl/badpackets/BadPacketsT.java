package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsT")
public class BadPacketsT extends Check implements PacketCheck {

    // 1.7 and 1.8 seem to have different hitbox "expansion" values than 1.9+
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872458702
    // https://github.com/GrimAnticheat/Grim/pull/1274#issuecomment-1872533497
    private final boolean hasLegacyExpansion = player.getClientVersion().isOlderThan(ClientVersion.V_1_9);
    private final double maxHorizontalDisplacement = 0.3001 + (hasLegacyExpansion ? 0.1 : 0);
    private final double minVerticalDisplacement = -0.0001 - (hasLegacyExpansion ? 0.1 : 0);
    private final double maxVerticalDisplacement = 1.8001 + (hasLegacyExpansion ? 0.1 : 0);

    public BadPacketsT(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayClientInteractEntity.class,
                () -> new WrapperPlayClientInteractEntity(event));

        packet.getTarget().ifPresent(targetVector -> {
            final var packetEntity = player.compensatedEntities.getEntity(packet.getEntityId());
            // Don't continue if the compensated entity hasn't been resolved
            if (packetEntity == null) {
                return;
            }

            // Make sure our target entity is actually a player (Player NPCs work too)
            if (!EntityTypes.PLAYER.equals(packetEntity.getType())) {
                // We can't check for any entity that is not a player
                return;
            }

            // Perform the interaction vector check
            // TODO:
            //  27/12/2023 - Dynamic values for more than just one entity type?
            //  28/12/2023 - Player-only is fine
            //  30/12/2023 - Expansions differ in 1.9+
            final var scale = (float) packetEntity.getAttributeValue(Attributes.GENERIC_SCALE);
            if (targetVector.y > (minVerticalDisplacement * scale) && targetVector.y < (maxVerticalDisplacement * scale)
                    && Math.abs(targetVector.x) < (maxHorizontalDisplacement * scale)
                    && Math.abs(targetVector.z) < (maxHorizontalDisplacement * scale)) {
                return;
            }

            // We could pretty much ban the player at this point
            flagAndAlert(
                    new Pair<>("target-vector-x", String.format("%.5f", targetVector.x)),
                    new Pair<>("target-vector-y", String.format("%.5f", targetVector.y)),
                    new Pair<>("target-vector-z", String.format("%.5f", targetVector.z))
            );
        });
    }

}
