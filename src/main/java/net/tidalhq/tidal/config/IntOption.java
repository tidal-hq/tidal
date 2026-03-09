package net.tidalhq.tidal.config;

public class IntOption extends ConfigOption<Integer> {

    private final int min;
    private final int max;

    public IntOption(String key, String displayName, String description,
                     int defaultValue, int min, int max) {
        super(key, displayName, description, defaultValue);
        if (min > max) throw new IllegalArgumentException("min must be <= max");
        this.min = min;
        this.max = max;
    }

    public int getMin() { return min; }
    public int getMax() { return max; }

    @Override
    protected boolean isValid(Integer value) {
        return value >= min && value <= max;
    }

    @Override
    public String serialize() {
        return get().toString();
    }

    @Override
    public void deserialize(String raw) {
        try {
            set(Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            reset();
        }
    }

    @Override
    public OptionType getOptionType() {
        return OptionType.INTEGER;
    }
}