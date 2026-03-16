package net.tidalhq.tidal.feature.impl;

import net.tidalhq.tidal.requirement.Requirement;
import net.tidalhq.tidal.requirement.RequirementSet;
import net.tidalhq.tidal.state.BuffState;
import net.tidalhq.tidal.state.CompositeGameStateView;

public enum GodPotionSource {

    INVENTORY(
            "Inventory",
            "Use a God Potion already in your inventory"
    ),
    AUCTION_HOUSE_COMMAND(
            "Auction House (/ah)",
            "Open auction house via /ah and buy a God Potion"
    ),
    AUCTION_HOUSE_PHYSICAL(
            "Auction House (NPC)",
            "Walk to the Auction Master NPC and buy a God Potion"
    ),
    BITS_SHOP(
            "Bits Shop (Elizabeth)",
            "Walk to Elizabeth and buy a God Potion with bits"
    );

    private final String name;
    private final String description;

    GodPotionSource(String name, String description) {
        this.name        = name;
        this.description = description;
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }

    public RequirementSet requirements(CompositeGameStateView gameState) {
        return switch (this) {
            case INVENTORY, BITS_SHOP -> RequirementSet.EMPTY;
            case AUCTION_HOUSE_COMMAND, AUCTION_HOUSE_PHYSICAL -> RequirementSet.of(
                    Requirement.of("Active Booster Cookie (required to access Auction House)",
                            () -> gameState.getCookieBuffState() == BuffState.ACTIVE)
            );
        };
    }
}