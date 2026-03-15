package net.tidalhq.tidal.state;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.*;
import net.tidalhq.tidal.util.TablistUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TablistState {

    private static final Pattern AREA_PATTERN           = Pattern.compile("Area:\\s(.+)");
    private static final Pattern PEST_COUNT_PATTERN     = Pattern.compile("Alive:\\s*(\\d+)");
    private static final Pattern INFESTED_PLOTS_PATTERN = Pattern.compile("Plots:\\s*([\\d,\\s]+)");

    private Collection<PlayerListEntry> currentEntries;

    private BuffState godPotionState  = BuffState.UNKNOWN;
    private BuffState cookieBuffState = BuffState.UNKNOWN;

    private Location      currentLocation = Location.UNKNOWN;
    private int           pestsCount      = 0;
    private List<Integer> infestedPlots   = new ArrayList<>();

    private final List<Consumer<String>> entryParsers = List.of(
            this::parseLocation,
            this::parsePestCount,
            this::parseInfestedPlots
    );

    public TablistState(EventBus eventBus) {
        eventBus.register(this);
    }

    public BuffState    getGodPotionState()  { return godPotionState; }
    public BuffState    getCookieBuffState() { return cookieBuffState; }
    public Location     getCurrentLocation() { return currentLocation; }
    public int          getPestsCount()      { return pestsCount; }
    public List<Integer> getInfestedPlots()  { return infestedPlots; }
    public boolean      inGarden()           { return currentLocation == Location.GARDEN; }

    @Subscribe
    public void onPlayerListUpdate(PlayerListUpdateEvent event) {
        this.currentEntries = event.getEntries();
        if (currentEntries == null) return;
        streamEntries().forEach(this::parseEntry);
    }

    @Subscribe
    public void onPlayerListHeaderSet(PlayerListHeaderSetEvent event) {
        parseHeader(TablistUtil.splitLines(event.getHeaderContent()));
    }

    @Subscribe
    public void onPlayerListFooterSet(PlayerListFooterSetEvent event) {
        parseFooter(TablistUtil.splitLines(event.getFooterContent()));
    }

    private Stream<String> streamEntries() {
        return currentEntries.stream()
                .filter(Objects::nonNull)
                .map(PlayerListEntry::getDisplayName)
                .filter(Objects::nonNull)
                .map(Text::getString)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim);
    }

    private void parseEntry(String entry) {
        entryParsers.forEach(p -> p.accept(entry));
    }

    private void parseLocation(String entry) {
        TablistUtil.extract(entry, AREA_PATTERN)
                .flatMap(area -> Arrays.stream(Location.values())
                        .filter(loc -> loc.getName().equalsIgnoreCase(area))
                        .findFirst())
                .ifPresent(loc -> this.currentLocation = loc);
    }

    private void parsePestCount(String entry) {
        if (!entry.contains("Alive:")) return;
        TablistUtil.extract(entry, PEST_COUNT_PATTERN)
                .map(Integer::parseInt)
                .ifPresent(count -> this.pestsCount = count);
    }

    private void parseInfestedPlots(String entry) {
        if (!entry.contains("Plots:")) return;
        TablistUtil.extract(entry, INFESTED_PLOTS_PATTERN).ifPresent(raw ->
                infestedPlots = Arrays.stream(raw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toList())
        );
    }

    private void parseHeader(String[] lines) {}

    private void parseFooter(String[] lines) {
        if (TablistUtil.findLine(lines, "God Potion").isPresent()) {
            godPotionState = TablistUtil.findLine(lines, "You have a God Potion active!")
                    .map(x -> BuffState.ACTIVE)
                    .orElse(BuffState.NOT_ACTIVE);
        } else {
            godPotionState = BuffState.UNKNOWN;
        }

        if (TablistUtil.findLine(lines, "Cookie Buff").isPresent()) {
            cookieBuffState = TablistUtil.findLine(lines, "Not Active")
                    .map(x -> BuffState.NOT_ACTIVE)
                    .orElse(BuffState.ACTIVE);
        } else {
            cookieBuffState = BuffState.UNKNOWN;
        }
    }
}