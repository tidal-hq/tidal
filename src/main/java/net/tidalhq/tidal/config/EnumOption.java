package net.tidalhq.tidal.config;

import java.util.Arrays;
import java.util.List;

public class EnumOption<E extends Enum<E>> extends ConfigOption<E> {

    private final Class<E> enumClass;
    private final List<E> choices;

    public EnumOption(String key, String displayName, String description,
                      Class<E> enumClass, E defaultValue) {
        super(key, displayName, description, defaultValue);
        this.enumClass = enumClass;
        this.choices   = List.copyOf(Arrays.asList(enumClass.getEnumConstants()));
    }

    public List<E> getChoices() {
        return choices;
    }

    public void cycleNext() {
        int next = (choices.indexOf(get()) + 1) % choices.size();
        set(choices.get(next));
    }

    @Override
    public String serialize() {
        return get().name();
    }

    @Override
    public void deserialize(String raw) {
        try {
            set(Enum.valueOf(enumClass, raw));
        } catch (IllegalArgumentException e) {
            reset();
        }
    }

    @Override
    public OptionType getOptionType() {
        return OptionType.ENUM;
    }
}