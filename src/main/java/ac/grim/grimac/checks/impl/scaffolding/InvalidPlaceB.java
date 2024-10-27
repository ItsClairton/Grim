package ac.grim.grimac.checks.impl.scaffolding;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.BlockPlaceCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.BlockPlace;
import ac.grim.grimac.utils.data.Pair;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

@CheckData(name = "InvalidPlaceB")
public class InvalidPlaceB extends BlockPlaceCheck {

    private final boolean legacyServer = PacketEvents.getAPI().getServerManager()
            .getVersion()
            .isOlderThanOrEquals(ServerVersion.V_1_8);

    public InvalidPlaceB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onBlockPlace(final BlockPlace place) {
        if (legacyServer && place.getFaceId() == 255) {
            return;
        }

        if (place.getFaceId() >= 0 && place.getFaceId() <= 5) {
            return;
        }

        if (!flagAndAlert(new Pair<>("direction", place.getFaceId()))) {
            return;
        }

        if (!shouldModifyPackets() || !shouldCancel()) {
            return;
        }

        place.resync();
    }
    
}
