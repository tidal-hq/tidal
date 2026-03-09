package net.tidalhq.tidal.config;

public class BooleanOption extends ConfigOption<Boolean> {

    public BooleanOption(String key, String displayName, String description, boolean defaultValue) {
        super(key, displayName, description, defaultValue);
    }

    public void toggle() {
        set(!get());
    }

    @Override
    public String serialize() {
        return get().toString();
    }

    @Override
    public void deserialize(String raw) {
        set(Boolean.parseBoolean(raw));
    }

    @Override
    public OptionType getOptionType() {
        return OptionType.BOOLEAN;
    }
}
