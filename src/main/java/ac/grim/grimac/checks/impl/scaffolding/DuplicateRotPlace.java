package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Materials;

@CheckData(name = "DuplicateRotPlace", experimental = true)
public class DuplicateRotPlace extends BlockPlaceCheck {

    private boolean rotated = true;

    private float lastRotateYaw = Float.MIN_VALUE;
    private float lastPlacedYaw = Float.MIN_VALUE;
    private float lastPlacedDeltaYaw = Float.MIN_VALUE;

    private byte buffer = 0;

    public DuplicateRotPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(RotationUpdate rotation) {
        lastRotateYaw = rotation.getTo().getYaw();

        rotated = true;
    }

    @Override
    public void onBlockPlace(BlockPlace place) {
        if (!rotated || !place.isBlock()) {
            return;
        }

        if (lastPlacedYaw == Float.MIN_VALUE) {
            lastPlacedYaw = lastRotateYaw;
            rotated = false;
            return;
        }

        final var deltaYaw = Math.abs(lastPlacedYaw - lastRotateYaw);

        lastPlacedYaw = lastRotateYaw;
        rotated = false;

        if (lastPlacedDeltaYaw == Float.MIN_VALUE) {
            lastPlacedDeltaYaw = deltaYaw;
            return;
        }

        if (deltaYaw != lastPlacedDeltaYaw) {
            lastPlacedDeltaYaw = deltaYaw;
            buffer = 0;
            return;
        }

        if (Materials.isClientSideInteractable(place.getPlacedAgainstMaterial())) {
            lastPlacedDeltaYaw = deltaYaw;
            buffer = 0;
            return;
        }

        if (place.isReplaceClicked()) {
            lastPlacedDeltaYaw = deltaYaw;
            buffer = 0;
            return;
        }

        final var existingState = place.getExistingBlockData();
        if (!existingState.getType().isAir()) {
            lastPlacedDeltaYaw = deltaYaw;
            buffer = 0;
            return;
        }

        final var pos = place.getPlacedBlockPos();
        if (player.boundingBox.isIntersected(new SimpleCollisionBox(pos))) {
            lastPlacedDeltaYaw = deltaYaw;
            buffer = 0;
            return;
        }

        buffer++;

        if (buffer < 5) {
            return;
        }

        if (!flagAndAlert(new Pair<>("material", place.getMaterial()),
                new Pair<>("place-against", place.getPlacedAgainstMaterial()),
                new Pair<>("existing-block", existingState))) {
            return;
        }

        if (!shouldModifyPackets()) {
            return;
        }

        place.resync();
    }

}
