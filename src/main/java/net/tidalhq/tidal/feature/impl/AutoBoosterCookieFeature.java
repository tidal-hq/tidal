package net.tidalhq.tidal.feature.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.Npc;
import net.tidalhq.tidal.config.BooleanOption;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.config.EnumOption;
import net.tidalhq.tidal.config.IntOption;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.LocationSanctionEvent;
import net.tidalhq.tidal.event.impl.MacroStoppedEvent;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.notification.Notification;
import net.tidalhq.tidal.state.BuffState;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.BazaarUtil;
import net.tidalhq.tidal.util.GuiInteraction;
import net.tidalhq.tidal.util.NpcInteraction;
import net.tidalhq.tidal.util.PlayerUtil;

import java.util.List;

public class AutoBoosterCookieFeature extends Feature implements MacroLifecycleHook {

    private enum AcquisitionState {
        IDLE,
        WARPING_TO_HUB,
        WAITING_FOR_CHUNK,
        WALKING_TO_BAZAAR,
        WAITING_FOR_BUY,
        DONE,
        FAILED
    }

    private final EnumOption<BoosterCookieSource> source = new EnumOption<>(
            "source", "Cookie Source", "Where to obtain the cookie from",
            BoosterCookieSource.class, BoosterCookieSource.BAZAAR_NO_COOKIE);

    private final IntOption reapplyBeforeExpiry = new IntOption(
            "reapply_before_expiry", "Early Reapply (seconds)",
            "Re-apply the cookie this many seconds before the buff expires to avoid downtime",
            30, 0, 300);

    private final BooleanOption pauseIfUnavailable = new BooleanOption(
            "pause_if_unavailable", "Pause if Unavailable",
            "Pause the macro if a cookie cannot be obtained from the configured source",
            true);

    @Override
    public List<ConfigOption<?>> getOptions() {
        return List.of(source, reapplyBeforeExpiry, pauseIfUnavailable);
    }

    private AcquisitionState acquisitionState = AcquisitionState.IDLE;
    private int              chunkWaitTicks       = 0;
    private static final int CHUNK_WAIT_MAX       = 100;

    private NpcInteraction bazaarInteraction = null;
    private GuiInteraction buyInteraction    = null;

    public AutoBoosterCookieFeature(FeatureContext ctx) { super(ctx); }

    @Override public String getId()          { return "auto_booster_cookie"; }
    @Override public String getName()        { return "Auto Booster Cookie"; }
    @Override public String getDescription() { return "Automatically re-up Booster Cookie from a selected source."; }
    @Override public Category getCategory()  { return Category.MISC; }

    @Override
    public void onTick() {
        BuffState cookieState = ctx.gameState().getCookieBuffState();
        if (cookieState == BuffState.ACTIVE || cookieState == BuffState.UNKNOWN) {
            if (acquisitionState != AcquisitionState.IDLE) resetState();
            return;
        }

        if (bazaarInteraction != null) bazaarInteraction.tick();
        if (buyInteraction    != null) buyInteraction.tick();

        if (acquisitionState == AcquisitionState.FAILED
                || acquisitionState == AcquisitionState.DONE) return;

        switch (source.get()) {
            case INVENTORY        -> applyFromInventory();
            case BACKPACK         -> applyFromBackpack();
            case BAZAAR_NO_COOKIE,
                 BAZAAR           -> purchaseFromBazaar();
        }
    }

    @Override
    public boolean onBeforeMacroStart(Macro macro) {
        if (source.get().requiresCookie()
                && ctx.gameState().getCookieBuffState() != BuffState.ACTIVE) {
            ctx.notifier().danger("[" + getName() + "] source '" + source.get().getName()
                    + "' requires a Booster Cookie but none is active.");
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldPauseMacro(Macro macro) {
        if (acquisitionState == AcquisitionState.IDLE || acquisitionState == AcquisitionState.DONE) return false;
        if (acquisitionState == AcquisitionState.FAILED) return pauseIfUnavailable.get();
        return true; // mid-acquisition
    }

    @Override
    public void onMacroPaused(Macro macro) {
        if (acquisitionState == AcquisitionState.FAILED) {
            ctx.notifier().danger("[" + getName() + "] macro paused — Booster Cookie unavailable from "
                    + source.get().getName());
        } else {
            ctx.notifier().info("[" + getName() + "] macro paused — obtaining Booster Cookie ("
                    + acquisitionState + ")");
        }
    }

    private void purchaseFromBazaar() {
        switch (acquisitionState) {

            case IDLE -> {
                Location current = ctx.gameState().getCurrentLocation();
                if (current == Location.UNKNOWN) return;
                if (current == Location.HUB) {
                    beginHubArrival();
                } else {
                    warpToHub();
                }
            }

            case WARPING_TO_HUB -> {
                if (ctx.gameState().getCurrentLocation() == Location.HUB) {
                    beginHubArrival();
                }
            }

            case WAITING_FOR_CHUNK -> {
                BlockPos bazaarPos = Npc.BAZAAR_AGENT.getBlockPos();
                if (ctx.worldAccessor().isChunkLoaded(bazaarPos)) {
                    startBazaarInteraction(bazaarPos);
                } else if (++chunkWaitTicks >= CHUNK_WAIT_MAX) {
                    fail("bazaar chunk didn't load after " + CHUNK_WAIT_MAX + " ticks");
                }
            }

            case WALKING_TO_BAZAAR, WAITING_FOR_BUY -> {}

            case DONE -> {}

            case FAILED -> {}
        }
    }

    private void warpToHub() {
        EventBus.getInstance().post(new LocationSanctionEvent(Location.HUB,
                getName() + " warping to hub for Booster Cookie"));
        acquisitionState = AcquisitionState.WARPING_TO_HUB;
        PlayerUtil.warp(Location.HUB);
    }

    private void beginHubArrival() {
        acquisitionState = AcquisitionState.WAITING_FOR_CHUNK;
        chunkWaitTicks   = 0;
    }

    private void startBazaarInteraction(BlockPos bazaarPos) {
        acquisitionState = AcquisitionState.WALKING_TO_BAZAAR;

        bazaarInteraction = NpcInteraction
                .create(bazaarPos, ctx.rotation())
                .onGui(
                        screen -> screen.getTitle().getString().contains("Bazaar"),
                        screen -> {
                            acquisitionState = AcquisitionState.WAITING_FOR_BUY;
                            buyInteraction   = BazaarUtil.buy("Booster Cookie", 1)
                                    .onDone(() -> {
                                        acquisitionState = AcquisitionState.DONE;
                                        stopSubInteractions();
                                        MinecraftClient.getInstance().setScreen(null);
                                    })
                                    .onFail(reason -> fail("bazaar GUI failed: " + reason))
                                    .start();
                        })
                .onFail(reason -> fail("NPC interaction failed: " + reason))
                .start();
    }

    private void fail(String reason) {
        acquisitionState = AcquisitionState.FAILED;
        ctx.notifier().send(
                "[" + getName() + "] could not obtain Booster Cookie: " + reason,
                Notification.NotificationLevel.WARNING);
        stopSubInteractions();
    }


    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) {
        resetState();
    }

    private void resetState() {
        acquisitionState = AcquisitionState.IDLE;
        chunkWaitTicks   = 0;
        stopSubInteractions();
    }

    private void stopSubInteractions() {
        if (bazaarInteraction != null) { bazaarInteraction.stop(); bazaarInteraction = null; }
        if (buyInteraction    != null) { buyInteraction.stop();    buyInteraction    = null; }
    }

    private void applyFromInventory() {}
    private void applyFromBackpack()  {}
}