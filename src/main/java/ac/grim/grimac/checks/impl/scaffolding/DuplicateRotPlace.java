package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.data.Pair;

@CheckData(name = "DuplicateRotPlace", experimental = true)
public class DuplicateRotPlace extends BlockPlaceCheck {

    private float deltaX, deltaY;

    private double deltaDotsX;
    private boolean rotated = false;

    public DuplicateRotPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        deltaX = rotationUpdate.getDeltaXRotABS();
        deltaY = rotationUpdate.getDeltaYRotABS();
        deltaDotsX = rotationUpdate.getProcessor().deltaDotsX;
        rotated = true;
    }

    private float lastPlacedDeltaX;
    private double lastPlacedDeltaDotsX;

    public void onPostFlyingBlockPlace(BlockPlace place) {
        if (rotated) {
            if (deltaX > 2) {
                final var xDiff = Math.abs(deltaX - lastPlacedDeltaX);

                if (xDiff < 0.0001) {
                    final var xDiffDots = Math.abs(deltaDotsX - lastPlacedDeltaDotsX);
                    flagAndAlert(new Pair<>("x", xDiff), new Pair<>("x-dots", xDiffDots), new Pair<>("y-delta", deltaY));
                } else {
                    reward();
                }
            } else {
                reward();
            }

            this.lastPlacedDeltaX = deltaX;
            this.lastPlacedDeltaDotsX = deltaDotsX;
            rotated = false;
        }
    }


}
