package ac.grim.grimac.checks.impl.visual;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.Enchantment;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;

import java.util.Collections;

@CheckData(name = "EquipmentIndicator", experimental = true)
public class EquipmentIndicator extends Check implements PacketCheck {

    private final ClientVersion serverVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

    public EquipmentIndicator(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (!isEnabled() || !shouldModifyPackets()) {
            return;
        }

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) {
            return;
        }

        final var packet = lastWrapper(event,
                WrapperPlayServerEntityEquipment.class,
                () -> new WrapperPlayServerEntityEquipment(event));

        if (packet.getEntityId() == player.entityID) {
            return;
        }

        final var entity = player.compensatedEntities.getEntity(packet.getEntityId());
        if (entity == null || entity.getType() != EntityTypes.PLAYER) {
            return;
        }

        var needEncoding = false;
        for (Equipment equipment : packet.getEquipment()) {
            final var item = equipment.getItem();
            if (item.getType() == ItemTypes.AIR) {
                continue;
            }

            if (item.getAmount() > 1) {
                item.setAmount(1);
                needEncoding = true;
            }

            if (item.getDamageValue() > 0) {
                item.setDamageValue(0);
                needEncoding = true;
            }

            final var enchantments = item.getEnchantments(serverVersion);
            if (enchantments.isEmpty()) {
                continue;
            }

            item.setEnchantments(Collections.singletonList(new Enchantment(EnchantmentTypes.UNBREAKING, 1)),
                    serverVersion);

            needEncoding = true;
        }

        if (!needEncoding) {
            return;
        }

        event.markForReEncode(true);
    }

}
