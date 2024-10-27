package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

@CheckData(name = "FabricatedPlace")
public class FabricatedPlace extends BlockPlaceCheck {

    public FabricatedPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        final var cursor = place.getCursor();
        if (cursor == null) {
            return;
        }

        final var placeAgainst = place.getPlacedAgainstMaterial();
        final var maxAllowed = Materials.isShapeExceedsCube(placeAgainst) || placeAgainst == StateTypes.LECTERN ? 1.5 : 1;
        final var minAllowed = 1 - maxAllowed;

        if (cursor.getX() >= minAllowed && cursor.getY() >= minAllowed && cursor.getZ() >= minAllowed
                && cursor.getX() <= maxAllowed && cursor.getY() <= maxAllowed && cursor.getZ() <= maxAllowed) {
            return;
        }

        if (!flagAndAlert(
                new Pair<>("material", place.getMaterial()),
                new Pair<>("place-against", placeAgainst),
                new Pair<>("cursor", cursor),
                new Pair<>("min-allowed", minAllowed),
                new Pair<>("max-allowed", maxAllowed))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

}
