package net.tidalhq.tidal.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.PlayerListFooterSetEvent;
import net.tidalhq.tidal.event.impl.PlayerListHeaderSetEvent;
import net.tidalhq.tidal.event.impl.PlayerListUpdateEvent;
import net.tidalhq.tidal.mixin.PlayerListHudAccessor;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TablistState {
    private static TablistState instance;
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private final Pattern areaPattern = Pattern.compile("Area:\\s(.+)");

    private String header;
    private String footer;

    private Collection<PlayerListEntry> currentEntries;

    private BuffState godPotionState = BuffState.UNKNOWN;
    public BuffState getGodPotionState() { return godPotionState; }
    private BuffState cookieBuffState  = BuffState.UNKNOWN;
    public BuffState getCookieBuffState() { return cookieBuffState; }

    private Location currentLocation = Location.UNKNOWN;
    public Location getCurrentLocation() { return currentLocation; }

    private TablistState() {
        EventBus.getInstance().register(this);
    }

    public static TablistState getInstance() {
        if (instance == null) {
            instance = new TablistState();
        }

        return instance;
    }

    @Subscribe
    public void onPlayerListUpdate(PlayerListUpdateEvent event) {
        this.currentEntries = event.getEntries();

        updateLocation();
    }

    @Subscribe
    public void onPlayerListHeaderSet(PlayerListHeaderSetEvent event) {
        this.header = event.getHeaderContent();

        updateBuffs();
    }

    @Subscribe
    public void onPlayerListFooterSet(PlayerListFooterSetEvent event) {
        this.footer = event.getFooterContent();

        updateBuffs();
    }

    private void updateLocation() {
        if (this.currentEntries == null) return;

        for (PlayerListEntry entry : this.currentEntries) {
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

                this.currentLocation = loc;
                return;
            }
        }
    }
    private void updateBuffs() {

        boolean cookieInactive = false;
        for (String line : footer.split("\n")) {
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


}
