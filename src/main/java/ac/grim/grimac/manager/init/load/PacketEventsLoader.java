package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.PEVersions;
import com.github.retrooper.packetevents.util.TimeStampMode;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PacketEventsLoader implements Initable {

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void start() {
        LogUtil.info("Loading PacketEvents v" + PEVersions.RAW + "...");

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(GrimAPI.INSTANCE.getPlugin()));

        final var serverVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        final var candidateVersions = Arrays.stream(ClientVersion.values())
                .filter(version -> version.isOlderThanOrEquals(serverVersion))
                .toList()
                .toArray(new ClientVersion[0]);

        PacketEvents.getAPI().getSettings()
                .fullStackTrace(true)
                .kickOnPacketException(true)
                .checkForUpdates(false)
                .reEncodeByDefault(false)
                .debug(false)
                .bStats(false)
                .blockStateVersions(candidateVersions)
                .timeStampMode(TimeStampMode.NONE);

        PacketEvents.getAPI().load();
    }

}
