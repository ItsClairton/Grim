package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.data.Pair;

@CheckData(name = "InvalidPlaceA")
public class InvalidPlaceA extends BlockPlaceCheck {

    public InvalidPlaceA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final var cursor = place.getCursor();
        if (cursor == null) {
            return;
        }

        if (Float.isFinite(cursor.getX()) && Float.isFinite(cursor.getY()) && Float.isFinite(cursor.getZ())) {
            return;
        }

        if (!flagAndAlert(new Pair<>("cursor", cursor))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

}
