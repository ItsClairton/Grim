package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.data.Pair;
import ac.grim.grimac.utils.nmsutil.Materials;
import com.github.retrooper.packetevents.protocol.player.GameMode;

@CheckData(name = "AirLiquidPlace")
public class AirLiquidPlace extends BlockPlaceCheck {

    public AirLiquidPlace(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (player.gamemode == GameMode.CREATIVE) {
            return;
        }

        final var blockPos = place.getPlacedAgainstBlockLocation();
        final var placeAgainst = player.compensatedWorld.getStateTypeAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());

        if (!placeAgainst.isAir() && !Materials.isNoPlaceLiquid(placeAgainst)) {
            return;
        }

        if (!flagAndAlert(new Pair<>("material", place.getMaterial()), new Pair<>("place-against", placeAgainst))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }

    @Override
    public void reload() {
        super.reload();
        this.cancelVL = getConfig().getIntElse(getConfigName() + ".cancelVL", 0);
    }

}
