package ac.grim.grimac.checks.impl.visual;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

@CheckData(name = "DamageIndicator", experimental = true)
public class DamageIndicator extends Check implements PacketCheck {

    public DamageIndicator(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!isEnabled() || !shouldModifyPackets()) {
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayServerEntityMetadata.class,
                () -> new WrapperPlayServerEntityMetadata(event));

        if (packet.getEntityId() == player.entityID) {
            return;
        }

        final var entity = player.compensatedEntities.getEntity(packet.getEntityId());
        if (entity == null || entity.getType() != EntityTypes.PLAYER) {
            return;
        }

        final var attributes = packet.getEntityMetadata();

        var needEncoding = false;
        for (EntityData attribute : attributes) {
            if (attribute.getIndex() != 6) {
                continue;
            }

            final var currentHealth = (Float) attribute.getValue();
            if (currentHealth <= 0 || currentHealth == 20) {
                continue;
            }

            attribute.setValue(20f);
            needEncoding = true;
            break;
        }

        if (!needEncoding) {
            return;
        }

        if (attributes.size() == 1) {
            event.setCancelled(true);
            return;
        }

        event.markForReEncode(true);
    }

}
