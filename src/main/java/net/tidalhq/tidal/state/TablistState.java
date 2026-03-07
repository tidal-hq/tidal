package net.tidalhq.tidal.state;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.PestSpawnedEvent;
import net.tidalhq.tidal.event.impl.PlayerListFooterSetEvent;
import net.tidalhq.tidal.event.impl.PlayerListHeaderSetEvent;
import net.tidalhq.tidal.event.impl.PlayerListUpdateEvent;
import net.tidalhq.tidal.util.TablistUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TablistState {
    private static final TablistState instance = new TablistState();

    private static final Pattern AREA_PATTERN       = Pattern.compile("Area:\\s(.+)");
    private static final Pattern PEST_COUNT_PATTERN = Pattern.compile("Alive:\\s*(\\d+)");

    private String footer;

    private Collection<PlayerListEntry> currentEntries;

    private BuffState godPotionState = BuffState.UNKNOWN;
    public BuffState getGodPotionState() { return godPotionState; }
    private BuffState cookieBuffState = BuffState.UNKNOWN;
    public BuffState getCookieBuffState() { return cookieBuffState; }

    private Location currentLocation = Location.UNKNOWN;
    public Location getCurrentLocation() { return currentLocation; }

    private int pestsCount = 0;
    public int getPestsCount() { return pestsCount; }

    private final List<Consumer<String>> entryParsers = List.of(
            this::updateLocation,
            this::updatePestCount
    );

    private TablistState() {
        EventBus.getInstance().register(this);
    }

    public static TablistState getInstance() { return instance; }

    private Stream<String> getEntryStrings() {
        return currentEntries.stream()
                .filter(Objects::nonNull)
                .map(PlayerListEntry::getDisplayName)
                .filter(Objects::nonNull)
                .map(Text::getString)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim);
    }

    @Subscribe
    public void onPlayerListUpdate(PlayerListUpdateEvent event) {
        this.currentEntries = event.getEntries();
        if (this.currentEntries == null) return;
        getEntryStrings().forEach(this::updateFromEntry);
    }

    @Subscribe
    public void onPlayerListHeaderSet(PlayerListHeaderSetEvent event) {
        updateFromHeader(TablistUtil.splitLines(event.getHeaderContent()));
    }

    @Subscribe
    public void onPlayerListFooterSet(PlayerListFooterSetEvent event) {
        this.footer = event.getFooterContent();
        updateFromFooter(TablistUtil.splitLines(footer));
    }

    private void updateFromEntry(String entry) {
        entryParsers.forEach(parser -> parser.accept(entry));
    }

    private void updateLocation(String entry) {
        TablistUtil.extract(entry, AREA_PATTERN)
                .flatMap(area -> Arrays.stream(Location.values())
                        .filter(loc -> loc.getName().equalsIgnoreCase(area))
                        .findFirst())
                .ifPresent(loc -> this.currentLocation = loc);
    }

    private void updatePestCount(String entry) {
        if (!entry.contains("Alive:")) return;

        TablistUtil.extract(entry, PEST_COUNT_PATTERN)
                .map(Integer::parseInt)
                .ifPresent(count -> {
                    int previous = this.pestsCount;
                    this.pestsCount = count;
                    if (count > previous) {
                        EventBus.getInstance().post(new PestSpawnedEvent(previous, count));
                    }
                });
    }

    private void updateFromHeader(String[] lines) {
    }

    private void updateFromFooter(String[] lines) {
        this.godPotionState = TablistUtil.findLine(lines, "You have a God Potion active!")
                .map(x -> BuffState.ACTIVE)
                .orElse(BuffState.UNKNOWN);

        this.cookieBuffState = TablistUtil.findLine(lines, "Not Active")
                .map(x -> BuffState.NOT_ACTIVE)
                .orElse(BuffState.ACTIVE);
    }
}