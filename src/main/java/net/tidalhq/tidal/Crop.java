package net.tidalhq.tidal;

public enum Crop {
    NONE("None"),
    CARROT("Carrot"),
    NETHER_WART("Nether Wart"),
    POTATO("Potato"),
    WHEAT("Wheat"),
    SUGAR_CANE("Sugar Cane"),
    MELON("Melon"),
    PUMPKIN("Pumpkin"),
    PUMPKIN_MELON_UNKNOWN("Pumpkin/Melon"),
    CACTUS("Cactus"),
    COCOA_BEANS("Cocoa Beans"),
    MUSHROOM("Mushroom"),
    MUSHROOM_ROTATE("Mushroom"),
    SUNFLOWER("Sunflower"),
    MOONFLOWER("Moonflower"),
    ROSE("Rose"),
    ;

    final String name;

    Crop(String name) {
        this.name = name;
    }
}
