package net.tidalhq.tidal.config;

public class StringOption extends ConfigOption<String> {

    private final int maxLength;

    public StringOption(String key, String displayName, String description,
                        String defaultValue, int maxLength) {
        super(key, displayName, description, defaultValue);
        this.maxLength = maxLength;
    }

    public StringOption(String key, String displayName, String description, String defaultValue) {
        this(key, displayName, description, defaultValue, 256);
    }

    public int getMaxLength() { return maxLength; }

    @Override
    protected boolean isValid(String value) {
        return value.length() <= maxLength;
    }

    @Override
    public String serialize() {
        return get();
    }

    @Override
    public void deserialize(String raw) {
        set(raw);
    }

    @Override
    public OptionType getOptionType() {
        return OptionType.STRING;
    }
}