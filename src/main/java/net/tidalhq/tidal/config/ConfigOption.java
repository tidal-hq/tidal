package net.tidalhq.tidal.config;

import java.util.function.Consumer;

public abstract class ConfigOption<T> {

    private final String key;
    private final String displayName;
    private final String description;
    private final T defaultValue;

    private T value;
    private Consumer<T> onChange;

    protected ConfigOption(String key, String displayName, String description, T defaultValue) {
        this.key          = key;
        this.displayName  = displayName;
        this.description  = description;
        this.defaultValue = defaultValue;
        this.value        = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        if (newValue == null) throw new IllegalArgumentException("Option value must not be null");
        if (!isValid(newValue)) throw new IllegalArgumentException("Invalid value for option '" + key + "': " + newValue);
        T previous = this.value;
        this.value = newValue;
        if (onChange != null && !newValue.equals(previous)) {
            onChange.accept(newValue);
        }
    }

    public void reset() {
        set(defaultValue);
    }

    protected boolean isValid(T value) {
        return true;
    }

    public abstract String serialize();

    public abstract void deserialize(String raw);

    public abstract OptionType getOptionType();

    public ConfigOption<T> onChange(Consumer<T> listener) {
        this.onChange = listener;
        return this;
    }
    public String getKey()         { return key; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public T getDefaultValue()     { return defaultValue; }
    public boolean isDefault()     { return value.equals(defaultValue); }

    @Override
    public String toString() {
        return key + "=" + serialize();
    }
}