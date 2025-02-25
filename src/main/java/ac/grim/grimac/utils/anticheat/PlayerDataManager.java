package ac.grim.grimac.utils.anticheat;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.floodgate.FloodgateUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.GeyserUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public class PlayerDataManager {
    private final ConcurrentHashMap<User, GrimPlayer> playerDataMap = new ConcurrentHashMap<>();
    public final Collection<User> exemptUsers = Collections.synchronizedCollection(new HashSet<>());

    public GrimPlayer getPlayer(final Player player) {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18) && MultiLibUtil.isExternalPlayer(player)) return null;

        // Is it safe to interact with this, or is this internal PacketEvents code?
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        return playerDataMap.get(user);
    }

    public boolean shouldCheck(User user, boolean alreadyChecked) {
        if (exemptUsers.contains(user)) return false;
        if (!alreadyChecked && !ChannelHelper.isOpen(user.getChannel())) return false;

        if (!alreadyChecked && user.getUUID() != null) {
            // Geyser players don't have Java movement
            // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
            if (GeyserUtil.isGeyserPlayer(user.getUUID()) || FloodgateUtil.isFloodgatePlayer(user.getUUID())) {
                exemptUsers.add(user);
                return false;
            }

            // Geyser formatted player string
            // This will never happen for Java players, as the first character in the 3rd group is always 4 (xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxxx)
            if (user.getUUID().toString().startsWith("00000000-0000-0000-0009")) {
                exemptUsers.add(user);
                return false;
            }
        }

        return true;
    }

    @Nullable
    public GrimPlayer getPlayer(final User user) {
        return playerDataMap.get(user);
    }

    public void addUser(final User user) {
        if (shouldCheck(user, false)) {
            GrimPlayer player = new GrimPlayer(user);
            playerDataMap.put(user, player);
        }
    }

    public void remove(final User player) {
        playerDataMap.remove(player);
    }

    public Collection<GrimPlayer> getEntries() {
        return playerDataMap.values();
    }

    public int size() {
        return playerDataMap.size();
    }
}
