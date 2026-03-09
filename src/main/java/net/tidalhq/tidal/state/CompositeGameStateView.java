package net.tidalhq.tidal.state;

public class CompositeGameStateView {

    private final ServerState serverState;
    private final TablistState tablistState;

    public CompositeGameStateView(ServerState serverState, TablistState tablistState) {
        this.serverState = serverState;
        this.tablistState = tablistState;
    }

    public boolean isConnectedToHypixel() {
        return serverState.isConnectedToHypixel();
    }

    public Location getCurrentLocation() {
        return tablistState.getCurrentLocation();
    }

    public int getPestsCount() {
        return tablistState.getPestsCount();
    }

    public BuffState getGodPotionState() {
        return tablistState.getGodPotionState();
    }

    public BuffState getCookieBuffState() {
        return tablistState.getCookieBuffState();
    }
}