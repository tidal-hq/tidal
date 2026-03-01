package net.tidalhq.tidal;


public enum Category {
    FARMING("Farming"),
    MISC("Misc"),
    HUD("Hud");


    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}