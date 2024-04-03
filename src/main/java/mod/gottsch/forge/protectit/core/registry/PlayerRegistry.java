package mod.gottsch.forge.protectit.core.registry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import mod.gottsch.forge.gottschcore.enums.IRarity;
import mod.gottsch.forge.protectit.core.player.PlayerIdentification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 27, 2024
 *
 */
public class PlayerRegistry {

    private static final Map<UUID, PlayerIdentification> BY_UUID = Maps.newHashMap();
    private static final Map<String, PlayerIdentification> BY_NAME = Maps.newHashMap();

    public static void register(PlayerIdentification playerId) {
        BY_UUID.put(playerId.getUuid(), playerId);
        BY_NAME.put(playerId.getName().trim().toLowerCase(), playerId);
    }

    public static Optional<PlayerIdentification> get(UUID uuid) {
        Optional<PlayerIdentification> result = Optional.empty();
        if (BY_UUID.containsKey(uuid)) {
            result = Optional.of(BY_UUID.get(uuid));
        }
        return result;
    }

    public static Optional<PlayerIdentification> get(String name) {
        Optional<PlayerIdentification> result = Optional.empty();
        if (BY_NAME.containsKey(name.toLowerCase())) {
            result = Optional.of(BY_NAME.get(name.toLowerCase()));
        }
        return result;
    }

    public static void clear() {
        BY_NAME.clear();
        BY_UUID.clear();
    }
}
