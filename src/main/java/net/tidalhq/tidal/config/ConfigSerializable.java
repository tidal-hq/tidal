package net.tidalhq.tidal.config;

import net.tidalhq.tidal.registry.Registerable;

import java.util.Map;

/**
 * Interface describing something serializable to config file, necessary for use with generic {@link net.tidalhq.tidal.registry.Registry}.
 * Avoids coupling {@link ConfigSerializer} to any one object.
 */
public interface ConfigSerializable extends Registerable {
    String getId();

    Map<String, String> serialize();

    void deserialize(Map<String, String> values);
}
