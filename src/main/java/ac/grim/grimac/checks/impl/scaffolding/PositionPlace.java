package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Collections;

@CheckData(name = "PositionPlace")
public class PositionPlace extends BlockPlaceCheck {

    private final boolean hasIdlePacket = player.getClientVersion().isOlderThan(ClientVersion.V_1_9);

    public PositionPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final var material = place.getMaterial();
        if (material == StateTypes.FIRE || material == StateTypes.SCAFFOLDING) {
            return;
        }

        final var combined = getCombinedBox(place);
        final var eyePositions = getEyePositions();

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(combined)) {
            return;
        }

        final var flag = switch (place.getDirection()) {
            case NORTH -> // Z- face
                    eyePositions.minZ > combined.minZ;
            case SOUTH -> // Z+ face
                    eyePositions.maxZ < combined.maxZ;
            case EAST -> // X+ face
                    eyePositions.maxX < combined.maxX;
            case WEST -> // X- face
                    eyePositions.minX > combined.minX;
            case UP -> // Y+ face
                    eyePositions.maxY < combined.maxY;
            case DOWN -> // Y- face
                    eyePositions.minY > combined.minY;
            default -> false;
        };

        if (!flag) {
            return;
        }

        if (!flagAndAlert(new Pair<>("material", material), new Pair<>("direction", place.getDirection()))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

    private SimpleCollisionBox getEyePositions() {
        // Alright, now that we have the most optimal positions for each place
        // Please note that minY may be lower than maxY, this is INTENTIONAL!
        // Each position represents the best case scenario to have clicked
        //
        // We will now calculate the most optimal position for the player's head to be in
        final var minEyeHeight = Collections.min(player.getPossibleEyeHeights());
        final var maxEyeHeight = Collections.max(player.getPossibleEyeHeights());

        final var movementThreshold = !player.packetStateData.didLastMovementIncludePosition || !hasIdlePacket
                ? player.getMovementThreshold() :
                0;

        final var eyePositions = new SimpleCollisionBox(
                player.x, player.y + minEyeHeight, player.z,
                player.x, player.y + maxEyeHeight, player.z);

        eyePositions.expand(movementThreshold);
        return eyePositions;
    }

}
