package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Ray;
import ac.grim.grimac.utils.nmsutil.ReachUtils;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CheckData(name = "RotationPlace")
public class RotationPlace extends BlockPlaceCheck {

    // If the player flags once, force them to play legit, or we will cancel the tick before.
    private byte flagBuffer;

    // Ignore post-check if pre-check flag player
    private boolean ignorePost;

    public RotationPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (!place.isBlock() || place.getMaterial() == StateTypes.SCAFFOLDING) {
            return;
        }

        if (player.gamemode == GameMode.SPECTATOR) {
            // you don't send flying packets when spectating entities
            return;
        }

        if (flagBuffer == 0) {
            return;
        }

        if (didRayTraceHit(place)) {
            return;
        }

        if (!flagAndAlert(new Pair<>("stage", "pre-flying"),
                new Pair<>("placed-material", place.getMaterial()),
                new Pair<>("placed-pos", place.getPlacedBlockPos()),
                new Pair<>("against-material", place.getPlacedAgainstMaterial()),
                new Pair<>("against-pos", place.getPlacedAgainstBlockLocation()))) {
            return;
        }

        ignorePost = true;

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

    @Override
    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (!place.isBlock() || place.getMaterial() == StateTypes.SCAFFOLDING) {
            return;
        }

        if (player.gamemode == GameMode.SPECTATOR) {
            // you don't send flying packets when spectating entities
            return;
        }

        if (ignorePost) {
            ignorePost = false;
            return;
        }

        if (didRayTraceHit(place)) {
            if (flagBuffer > 0) {
                flagBuffer--;
            }

            return;
        }

        if (!flagAndAlert(new Pair<>("stage", "post-flying"),
                new Pair<>("placed-material", place.getMaterial()),
                new Pair<>("placed-pos", place.getPlacedBlockPos()),
                new Pair<>("against-material", place.getPlacedAgainstMaterial()),
                new Pair<>("against-pos", place.getPlacedAgainstBlockLocation()))) {
            return;
        }

        flagBuffer = 10;
    }

    private boolean didRayTraceHit(BlockPlace place) {
        final var box = new SimpleCollisionBox(place.getPlacedAgainstBlockLocation());

        List<Vector3f> possibleLookDirs = new ObjectArrayList<>(Arrays.asList(
                new Vector3f(player.xRot, player.yRot, 0),
                new Vector3f(player.lastXRot, player.yRot, 0)
        ));

        final var possibleEyeHeights = player.getPossibleEyeHeights();

        var minEyeHeight = Double.MAX_VALUE;
        var maxEyeHeight = Double.MIN_VALUE;

        for (final var height : possibleEyeHeights) {
            minEyeHeight = Math.min(minEyeHeight, height);
            maxEyeHeight = Math.max(maxEyeHeight, height);
        }

        final var eyePositions = new SimpleCollisionBox(
                player.x, player.y + minEyeHeight, player.z,
                player.x, player.y + maxEyeHeight, player.z);

        eyePositions.expand(player.getMovementThreshold());

        // If the player is inside a block, then they can ray trace through the block and hit the other side of the block
        if (eyePositions.isIntersected(box)) {
            return true;
        }

        // End checking if the player is in the block

        // 1.9+ players could be a tick behind because we don't get skipped ticks
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            possibleLookDirs.add(new Vector3f(player.lastXRot, player.lastYRot, 0));
        }

        // 1.7 players do not have any of these issues! They are always on the latest look vector
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
            possibleLookDirs = Collections.singletonList(new Vector3f(player.xRot, player.yRot, 0));
        }

        final var distance = player.compensatedEntities
                .getSelf()
                .getAttributeValue(Attributes.PLAYER_BLOCK_INTERACTION_RANGE);

        for (final var d : possibleEyeHeights) {
            for (final var lookDir : possibleLookDirs) {
                // x, y, z are correct for the block placement even after post tick because of code elsewhere
                final var starting = new Vector3d(player.x, player.y + d, player.z);

                // xRot and yRot are a tick behind
                final var trace = new Ray(player, starting.getX(), starting.getY(), starting.getZ(),
                        lookDir.getX(), lookDir.getY());

                final var intercept = ReachUtils.calculateIntercept(box,
                        trace.getOrigin(),
                        trace.getPointAtDistance(distance));

                if (intercept.first() == null) {
                    continue;
                }

                return true;
            }
        }

        return false;
    }

}
