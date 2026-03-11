package net.tidalhq.tidal.feature.impl;

public enum BoosterCookieSource {
    BACKPACK(
            "Backpack",
            "Use a Booster Cookie stored in a backpack",
            false
    ),

    INVENTORY(
            "Inventory",
            "Use a Booster Cookie already in your inventory",
            false
    ),

    BAZAAR_NO_COOKIE(
            "Bazaar (physical)",
            "Travel to bazaar and purchase booster cookie",
            false
    ),

    BAZAAR(
            "Bazaar (command)",
            "Use /bz and purchase a booster cookie (can only be done before current expires)",
            true
    )
    ;

    private final String name;
    private final String description;
    private final boolean requiresCookie;

    BoosterCookieSource(String name, String description, boolean requiresCookie) {
        this.name = name;
        this.description = description;
        this.requiresCookie = requiresCookie;
    }

    public String getName()  { return name; }
    public String getDescription()  { return description; }
    public boolean requiresCookie() { return requiresCookie; }
}
