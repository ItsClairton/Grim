package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.Direction;
import com.github.retrooper.packetevents.protocol.world.PaintingType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class PacketEntityPainting extends PacketEntity {

    private final @Nullable PaintingType paintingType;
    private final Direction direction;

    public PacketEntityPainting(GrimPlayer player, UUID uuid, double x, double y, double z, @Nullable PaintingType paintingType, Direction direction) {
        super(player, uuid, EntityTypes.PAINTING, x, y, z);
        this.paintingType = paintingType;
        this.direction = direction;
    }
}
