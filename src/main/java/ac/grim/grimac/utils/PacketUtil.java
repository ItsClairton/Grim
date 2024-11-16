package ac.grim.grimac.utils;

import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import java.util.function.Supplier;

public class PacketUtil {

    @SuppressWarnings("unchecked")
    public static <T extends PacketWrapper<?>> T lastWrapper(ProtocolPacketEvent event, Class<T> wrapper, Supplier<T> supplier) {
        final var lastWrapper = event.getLastUsedWrapper();
        if (lastWrapper != null) {
            return (T) lastWrapper;
        }

        return supplier.get();
    }

}
