package net.tidalhq.tidal.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.tidalhq.tidal.mixin.PlayerListHudAccessor;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistState {
    private static TablistState instance;
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");

    private String lastHeader;
    private String lastFooter;

    private String currentHeader;
    private String currentFooter;

    private Collection<PlayerListEntry> currentPlayerList;

    private BuffState godPotionState = BuffState.UNKNOWN;
    public BuffState getGodPotionState() { return godPotionState; }
    private BuffState cookieBuffState  = BuffState.UNKNOWN;
    public BuffState getCookieBuffState() { return cookieBuffState; }

    private Location currentLocation = Location.UNKNOWN;
    public Location getCurrentLocation() { return currentLocation; }
    private Location lastLocation = Location.UNKNOWN;


    public static TablistState getInstance() {
        if (instance == null) {
            instance = new TablistState();
        }

        return instance;
    }

    public void onPlayerListUpdate() {
        capturePlayerList();

        updateLocation();
    }

    public void onTablistUpdate() {
        // When an update is triggered from mixin, use accessors to get private fields on PlayerListHud and store results
        captureHeaderAndFooter();

        updateBuffs();
    }

    private void captureHeaderAndFooter() {
        if (client.getNetworkHandler() == null) return;


        PlayerListHudAccessor accessor =
                (PlayerListHudAccessor) client.inGameHud.getPlayerListHud();

        Text header = accessor.getHeader();
        Text footer = accessor.getFooter();

        this.lastHeader = this.currentHeader;
        this.lastFooter = this.currentFooter;
        this.currentHeader = header != null ? header.getString() : "";
        this.currentFooter = footer != null ? footer.getString() : "";
    }

    private void capturePlayerList() {
        if (client.getNetworkHandler() == null) return;

        this.currentPlayerList = client.getNetworkHandler().getListedPlayerListEntries();

    }

    private void updateLocation() {
        if (this.currentPlayerList == null) return;

        for (PlayerListEntry entry : this.currentPlayerList) {
            if (entry == null) continue;

            Text displayName = entry.getDisplayName();
            if (displayName == null) continue;

            String entryString = displayName.getString();
            if (entryString == null) continue;

            entryString = entryString.trim();
            if (entryString.isEmpty()) continue;

            Matcher matcher = areaPattern.matcher(entryString);
            if (!matcher.find()) continue;

            String area = matcher.group(1).trim();

            for (Location loc : Location.values()) {
                if (!area.equalsIgnoreCase(loc.getName())) continue;

                this.lastLocation = this.currentLocation;
                this.currentLocation = loc;
                return;
            }
        }
    }
    private void updateBuffs() {

        boolean cookieInactive = false;
        for (String line : currentFooter.split("\n")) {
            if (line.isEmpty()) continue;

            if (line.contains("You have a God Potion active!")) {
                this.godPotionState = BuffState.ACTIVE;

                // TODO: auto god pot
                break;
            }

            if (line.contains("Not Active")) {
                cookieInactive = true;
                this.cookieBuffState = BuffState.NOT_ACTIVE;
                break;
            }
        }

        if (!cookieInactive) {
            this.cookieBuffState = BuffState.ACTIVE;
        }
    }

    private TablistState() {
    }


}
