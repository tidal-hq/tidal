package net.tidalhq.tidal.feature.impl;

public enum GodPotSource {

    BACKPACK(
            "Backpack",
            "Use a God Potion stored in a backpack",
            false
    ),

    INVENTORY(
            "Inventory",
            "Use a God Potion already in your inventory",
            false
    ),

    BITS_SHOP(
            "Bits Shop",
            "Purchase a God Potion from the Community Shop using Bits",
            false
    ),

    AH_NO_COOKIE(
            "Auction House",
            "Purchase a God Potion from the Auction House (no Cookie required)",
            false
    ),

    AH_WITH_COOKIE(
            "Auction House (Cookie)",
            "Purchase a God Potion from the Auction House (requires Booster Cookie)",
            true
    );

    private final String displayName;
    private final String description;
    private final boolean requiresCookie;

    GodPotSource(String displayName, String description, boolean requiresCookie) {
        this.displayName    = displayName;
        this.description    = description;
        this.requiresCookie = requiresCookie;
    }

    public String getDisplayName()  { return displayName; }
    public String getDescription()  { return description; }
    public boolean requiresCookie() { return requiresCookie; }
}