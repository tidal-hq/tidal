package net.tidalhq.tidal.feature.impl;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.Npc;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.config.BooleanOption;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.config.EnumOption;
import net.tidalhq.tidal.config.IntOption;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.notification.Notification;
import net.tidalhq.tidal.state.BuffState;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.PlayerUtil;

import java.util.List;

public class AutoBoosterCookieFeature extends Feature implements MacroLifecycleHook {
    private enum AcquisitionState {
        IDLE,
        WARPING_TO_HUB,
        WALKING_TO_BAZAAR,
        AT_BAZAAR,
        DONE
    }

    private final EnumOption<BoosterCookieSource> source = new EnumOption<>(
            "source",
            "Cookie Source",
            "Where to obtain the cookie from",
            BoosterCookieSource.class,
            BoosterCookieSource.BAZAAR_NO_COOKIE
    );

    private final IntOption reapplyBeforeExpiry = new IntOption(
            "reapply_before_expiry",
            "Early Reapply (seconds)",
            "Re-apply the cookie this many seconds before the buff expires to avoid downtime",
            30,
            0,
            300
    );

    private final BooleanOption pauseIfUnavailable = new BooleanOption(
            "pause_if_unavailable",
            "Pause if Unavailable",
            "Pause the macro if a cookie cannot be obtained from the configured source",
            true
    );

    @Override
    public List<ConfigOption<?>> getOptions() {
        return List.of(source, reapplyBeforeExpiry, pauseIfUnavailable);
    }

    private AcquisitionState acquisitionState = AcquisitionState.IDLE;
    private boolean acquisitionFailed         = false;

    public AutoBoosterCookieFeature(FeatureContext ctx) {
        super(ctx);
    }

    @Override public String getId()          { return "auto_booster_cookie"; }
    @Override public String getName()        { return "Auto Booster Cookie"; }
    @Override public String getDescription() { return "Automatically re-up Booster Cookie from a selected source."; }
    @Override public Category getCategory()  { return Category.MISC; }

    @Override
    public void onTick() {
        if (ctx.gameState().getCookieBuffState() == BuffState.ACTIVE) {
            // @TODO: early re-apply using reapplyBeforeExpiry
            acquisitionFailed = false;
            resetState();

            return;
        }

        boolean success = acquire();
        if (!success && acquisitionState == AcquisitionState.IDLE) {
            if (!acquisitionFailed) {
                acquisitionFailed = true;
                ctx.notifier().send(
                        "[" + getName() + "] could not obtain Booster Cookie from " + source.get().getName(),
                        Notification.NotificationLevel.WARNING
                );
            }
        }
    }

    @Override
    public boolean onBeforeMacroStart(Macro macro) {
        if (source.get().requiresCookie()
                && ctx.gameState().getCookieBuffState() != BuffState.ACTIVE) {
            ctx.notifier().danger(
                    "[" + getName() + "] source '" + source.get().getName()
                            + "' requires a Booster Cookie but none is active."
            );
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldPauseMacro(Macro macro) {
        boolean midAcquisition = acquisitionState == AcquisitionState.WARPING_TO_HUB
                || acquisitionState == AcquisitionState.WALKING_TO_BAZAAR
                || acquisitionState == AcquisitionState.AT_BAZAAR;
        return midAcquisition || (acquisitionFailed && pauseIfUnavailable.get());
    }

    @Override
    public void onMacroPaused(Macro macro) {
        if (acquisitionFailed) {
            ctx.notifier().danger("[" + getName() + "] macro paused — Booster Cookie unavailable from " + source.get().getName());
        } else {
            ctx.notifier().info("[" + getName() + "] macro paused — obtaining Booster Cookie (" + acquisitionState + ")");
        }
    }

    private boolean acquire() {
        return switch (source.get()) {
            case INVENTORY          -> applyFromInventory();
            case BACKPACK           -> applyFromBackpack();
            case BAZAAR_NO_COOKIE,
                 BAZAAR -> purchaseFromBazaar();
        };
    }

    private boolean purchaseFromBazaar() {
        return switch (acquisitionState) {
            case IDLE -> {
                if (ctx.gameState().getCurrentLocation() == Location.HUB) {
                    if (ctx.worldAccessor().isChunkLoaded(Npc.BAZAAR_AGENT.getBlockPos())) {
                        startWalkingToBazaar();
                    }
                } else {
                    acquisitionState = AcquisitionState.WARPING_TO_HUB;
                    PlayerUtil.warp(Location.HUB);
                }
                yield true;
            }
            case WARPING_TO_HUB -> {
                if (ctx.gameState().getCurrentLocation() == Location.HUB) {
                    startWalkingToBazaar();
                }
                yield true;
            }
            case WALKING_TO_BAZAAR -> true;
            case AT_BAZAAR -> {
                boolean bought = openBazaarAndBuy();
                if (bought) {
                    acquisitionState = AcquisitionState.DONE;
                } else {
                    acquisitionFailed = true;
                    resetState();
                }
                yield bought;
            }
            case DONE -> {
                resetState();
                yield true;
            }
        };
    }

    private void startWalkingToBazaar() {
        acquisitionState = AcquisitionState.WALKING_TO_BAZAAR;

    }

    private void resetState() {
        acquisitionState  = AcquisitionState.IDLE;
    }

    private boolean applyFromInventory() {
        // TODO: scan hotbar/inventory for Booster Cookie item and right-click
        return false;
    }

    private boolean applyFromBackpack() {
        // TODO: open backpack GUI, locate Booster Cookie, click
        return false;
    }

    private boolean openBazaarAndBuy() {
        // TODO: send /bazaar, wait for GUI, search Booster Cookie, click BUY
        return false;
    }
}