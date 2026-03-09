package net.tidalhq.tidal;

import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.state.Location;

public enum Npc {
    ELIZABETH("Elizabeth", new BlockPos(-7, 79, 19), Location.HUB),
    AUCTION_MASTER("Auction Master", new BlockPos(-40, 73, -13), Location.HUB),
    BAZAAR_AGENT("Bazaar Agent", new BlockPos(-36, 73, -32), Location.HUB);

    private final String displayName;
    private final BlockPos pos;
    private final Location location;

    Npc(String displayName, BlockPos pos, Location location) {
        this.displayName = displayName;
        this.pos         = pos;
        this.location    = location;
    }

    public String getDisplayName() { return displayName; }
    public BlockPos getBlockPos()  { return pos; }
    public Location getLocation()  { return location; }
}