package mod.gottsch.forge.protectit.core.player;

import java.util.UUID;

/**
 *
 * @author Mark Gottschling on Mar 16, 2024
 *
 */
public class PlayerIdentification {
    private UUID uuid;
    private String name;

    public PlayerIdentification(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "PlayerIdentification{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                '}';
    }
}
