package net.tidalhq.tidal.feature;

import net.tidalhq.tidal.Category;

import java.util.*;

public class FeatureRegistry {
    private final Map<String, Feature> features = new LinkedHashMap<>();

    public void register(Feature feature) {
        features.put(feature.getId(), feature);
    }

    public Optional<Feature> get(String id) {
        return Optional.ofNullable(features.get(id));
    }

    public Collection<Feature> all() {
        return Collections.unmodifiableCollection(features.values());
    }

    public Collection<Feature> byCategory(Category category) {
        return features.values().stream()
                .filter(f -> f.getCategory() == category)
                .toList();
    }
}
