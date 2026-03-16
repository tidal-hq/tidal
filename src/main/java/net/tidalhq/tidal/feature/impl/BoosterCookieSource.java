package net.tidalhq.tidal.feature.impl;

import net.tidalhq.tidal.requirement.Requirement;
import net.tidalhq.tidal.requirement.RequirementSet;
import net.tidalhq.tidal.state.BuffState;

public enum BoosterCookieSource {

    BACKPACK(
            "Backpack",
            "Use a Booster Cookie stored in a backpack"
    ),
    INVENTORY(
            "Inventory",
            "Use a Booster Cookie already in your inventory"
    ),
    BAZAAR_PHYSICAL(
            "Bazaar (physical)",
            "Travel to bazaar NPC and purchase a Booster Cookie"
    ),
    BAZAAR_COMMAND(
            "Bazaar (command)",
            "Use /bz to purchase a Booster Cookie, requires an active cookie to access the AH/Bazaar"
    );

    private final String name;
    private final String description;

    BoosterCookieSource(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }


    public RequirementSet requirements(net.tidalhq.tidal.state.CompositeGameStateView gameState) {
        return switch (this) {
            case INVENTORY, BACKPACK, BAZAAR_PHYSICAL -> RequirementSet.EMPTY;
            case BAZAAR_COMMAND -> RequirementSet.of(
                    Requirement.of("Active Booster Cookie (needed to access /bz)",
                            () -> gameState.getCookieBuffState() == BuffState.ACTIVE)
            );
        };
    }
}