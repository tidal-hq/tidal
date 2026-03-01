package net.tidalhq.tidal.state;

public enum Location {
    PRIVATE_ISLAND("Private Island"),
    HUB("Hub"),
    THE_PARK("The Park"),
    THE_FARMING_ISLANDS("The Farming Islands"),
    SPIDER_DEN("Spider's Den"),
    THE_END("The End"),
    CRIMSON_ISLE("Crimson Isle"),
    GOLD_MINE("Gold Mine"),
    DEEP_CAVERNS("Deep Caverns"),
    DWARVEN_MINES("Dwarven Mines"),
    CRYSTAL_HOLLOWS("Crystal Hollows"),
    JERRY_WORKSHOP("Jerry's Workshop"),
    DUNGEON_HUB("Dungeon Hub"),
    LIMBO("UNKNOWN"),
    LOBBY("PROTOTYPE"),
    GARDEN("Garden"),
    DUNGEON("Dungeon"),
    UNKNOWN(""),
    TELEPORTING("Teleporting");

    private final String name;

    Location(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
