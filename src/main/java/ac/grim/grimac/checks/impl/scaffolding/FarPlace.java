package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.math.VectorUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import org.bukkit.util.Vector;

@CheckData(name = "FarPlace")
public class FarPlace extends BlockPlaceCheck {

    public FarPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (!place.isBlock() || place.getMaterial() == StateTypes.SCAFFOLDING) {
            return;
        }

        final var blockPos = place.getPlacedAgainstBlockLocation();

        var currentReach = Double.MAX_VALUE;

        final var possibleEyeHeights = player.getPossibleEyeHeights();
        for (final var d : possibleEyeHeights) {
            final var box = new SimpleCollisionBox(blockPos);
            final var eyes = new Vector(player.x, player.y + d, player.z);
            final var best = VectorUtils.cutBoxToVector(eyes, box);

            currentReach = Math.min(currentReach, eyes.distanceSquared(best));
        }

        var maxReach = player.compensatedEntities.getSelf().getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);

        final var threshold = player.getMovementThreshold();
        maxReach += Math.hypot(threshold, threshold);

        if (currentReach <= maxReach * maxReach) {
            return;
        }

        final var placeAgainst = place.getPlacedAgainstMaterial();
        if (!flagAndAlert(
                new Pair<>("material", place.getMaterial()),
                new Pair<>("place-against", placeAgainst),
                new Pair<>("current-reach", currentReach),
                new Pair<>("max-reach", maxReach * maxReach))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

}
