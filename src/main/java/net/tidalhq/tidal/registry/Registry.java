package net.tidalhq.tidal.registry;

import java.util.*;

/**
 * Generic registry for objects that implement {@link Registerable}.
 * Each object is stored by its ID (getId()).
 *
 * @param <T> the type of objects in this registry, must implement Registerable
 */
public class Registry<T extends Registerable> {
    private final Map<String, T> registeredObjects = new HashMap<>();

    /**
     * Registers an object using its getId() as the key.
     * If an object with the same ID exists, it is replaced.
     *
     * @param obj the object to register
     */
    public void put(T obj) {
        registeredObjects.put(obj.getId(), obj);
    }

    /**
     * Retrieves a registered object by its ID.
     *
     * @param id the ID of the object
     * @return optional containing the object if found
     */
    public Optional<T> get(String id) {
        return Optional.ofNullable(registeredObjects.get(id));
    }

    /**
     * Returns all registered objects as an unmodifiable collection.
     */
    public Collection<T> getRegisteredObjects() {
        return Collections.unmodifiableCollection(registeredObjects.values());
    }

    /**
     * Checks if a given ID is registered.
     */
    public boolean contains(String id) {
        return registeredObjects.containsKey(id);
    }

    /**
     * Removes a registered object by ID.
     *
     * @param id the ID of the object to remove
     * @return the removed object, or null if none was registered
     */
    public T remove(String id) {
        return registeredObjects.remove(id);
    }

    /**
     * Clears all registered objects.
     */
    public void clear() {
        registeredObjects.clear();
    }

    /**
     * Returns the number of registered objects.
     */
    public int size() {
        return registeredObjects.size();
    }
}
