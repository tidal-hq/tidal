package net.tidalhq.tidal.feature.impl;

import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.*;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.notification.Notification;
import net.tidalhq.tidal.state.BuffState;

import java.util.List;

public class AutoGodPotFeature extends Feature implements MacroLifecycleHook {
    private final EnumOption<GodPotSource> source = new EnumOption<>(
            "source",
            "Potion Source",
            "Where to obtain the God Potion from",
            GodPotSource.class,
            GodPotSource.INVENTORY
    );

    private final IntOption maxAhPrice = new IntOption(
            "max_ah_price",
            "Max AH Price",
            "Maximum coins to spend on the Auction House per potion (in thousands)",
            500,
            1,
            10_000
    );

    private final IntOption reapplyBeforeExpiry = new IntOption(
            "reapply_before_expiry",
            "Early Reapply (seconds)",
            "Re-apply the potion this many seconds before the buff expires to avoid downtime",
            30,
            0,
            300
    );

    private final BooleanOption pauseIfUnavailable = new BooleanOption(
            "pause_if_unavailable",
            "Pause if Unavailable",
            "Pause the macro if a potion cannot be obtained from the configured source",
            true
    );

    @Override
    public List<ConfigOption<?>> getOptions() {
        return List.of(source, maxAhPrice, reapplyBeforeExpiry, pauseIfUnavailable);
    }

    private boolean acquisitionFailed = false;

    public AutoGodPotFeature(FeatureContext ctx) {
        super(ctx);
    }

    @Override public String getId()          { return "auto_god_pot"; }
    @Override public String getDisplayName() { return "Auto God Pot"; }
    @Override public Category getCategory()  { return Category.FARMING; }

    @Override
    public void onTick() {
        if (ctx.gameState().getGodPotionState() == BuffState.ACTIVE) {
            acquisitionFailed = false;
            return;
        }

        boolean success = acquirePotion();
        if (!success) {
            if (!acquisitionFailed) {
                acquisitionFailed = true;
                ctx.notifier().send(
                        "Could not obtain God Potion from: " + source.get().getDisplayName(),
                        Notification.NotificationLevel.WARNING
                );
            }
        } else {
            acquisitionFailed = false;
        }
    }

    @Override
    public boolean onBeforeMacroStart(Macro macro) {
        if (source.get().requiresCookie()
                && ctx.gameState().getCookieBuffState() != BuffState.ACTIVE) {
            ctx.notifier().danger(
                    "AutoGodPot: source '" + source.get().getDisplayName()
                            + "' requires a Booster Cookie but none is active."
            );
            return false;
        }
        return true;
    }

    @Override
    public boolean shouldPauseMacro(Macro macro) {
        return acquisitionFailed && pauseIfUnavailable.get();
    }

    @Override
    public void onMacroPaused(Macro macro) {
        ctx.notifier().danger("Macro paused - God Potion unavailable from " + source.get().getDisplayName());
    }

    @Override
    public void onMacroResumed(Macro macro) {
        ctx.notifier().info("God Potion obtained - resuming macro.");
    }

    private boolean acquirePotion() {
        return switch (source.get()) {
            case INVENTORY      -> applyFromInventory();
            case BACKPACK       -> applyFromBackpack();
            case BITS_SHOP      -> purchaseFromBitsShop();
            case AH_NO_COOKIE,
                 AH_WITH_COOKIE -> purchaseFromAH(maxAhPrice.get() * 1000L);
        };
    }

    private boolean applyFromInventory() {
        // TODO: scan inventory for God Potion item ID, select slot, right-click
        return false;
    }

    private boolean applyFromBackpack() {
        // TODO: open backpack GUI, locate God Potion, click it
        return false;
    }

    private boolean purchaseFromBitsShop() {
        // TODO: open /bits shop, locate God Potion listing, purchase
        return false;
    }

    private boolean purchaseFromAH(long maxPrice) {
        // TODO: open /ah, search "God Potion", filter BIN, check price <= maxPrice, purchase
        return false;
    }
}