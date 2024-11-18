package ac.grim.grimac.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

@Getter
@AllArgsConstructor
public enum UserClient {

    LUNAR(Pattern.compile("^lunarclient:v(\\d+)\\.(\\d+)\\.(\\d+)-(\\d{2})(\\d{2})$"),
            "Lunar Client",
            true),

    BADLION(Pattern.compile("^badlion$"),
            "Badlion Client",
            true),

    CM_CLIENT(Pattern.compile("^cmclient:[a-f0-9]{7}$"),
            "CMClient",
            false),

    FABRIC(Pattern.compile("^fabric$"),
            "Fabric",
            false),

    VANILLA(Pattern.compile("^vanilla$"),
            "Minecraft Launcher",
            false),

    UNKNOWN(null,
            null,
            false);

    private final Pattern pattern;
    private final String displayName;
    private final @Accessors(fluent = true) boolean flySpeed;

    public static UserClient fromString(String brand) {
        for (final var client : UserClient.values()) {
            final var pattern = client.getPattern();
            if (pattern == null || !pattern.matcher(brand).matches()) {
                continue;
            }

            return client;
        }

        return UserClient.UNKNOWN;
    }

}
