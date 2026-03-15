package net.tidalhq.tidal.feature.impl;

import net.minecraft.client.MinecraftClient;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.Npc;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.config.BooleanOption;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.config.EnumOption;
import net.tidalhq.tidal.event.EventBus;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientReceiveGameMessageEvent;
import net.tidalhq.tidal.event.impl.LocationSanctionEvent;
import net.tidalhq.tidal.event.impl.MacroStoppedEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.notification.Notification;
import net.tidalhq.tidal.state.BuffState;
import net.tidalhq.tidal.state.Location;
import net.tidalhq.tidal.util.AuctionHouseUtil;
import net.tidalhq.tidal.util.GuiInteraction;
import net.tidalhq.tidal.util.InventoryUtil;
import net.tidalhq.tidal.util.NpcInteraction;
import net.tidalhq.tidal.util.PlayerUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoGodPotionFeature extends Feature implements MacroLifecycleHook {

    private enum Source { INVENTORY, AUCTION_HOUSE_COMMAND, AUCTION_HOUSE_PHYSICAL, BITS_SHOP }

    private enum AcquisitionState {
        IDLE,
        CHECKING_INVENTORY,
        WARPING_TO_HUB,
        WALKING_TO_NPC,
        WAITING_FOR_BUY,
        WAITING_FOR_CONFIRM,
        CONSUMING,
        DONE,
        FAILED
    }

    private final EnumOption<Source> source = new EnumOption<>(
            "source", "God Pot Source", "Where to obtain the God Potion from",
            Source.class, Source.AUCTION_HOUSE_COMMAND);

    private final BooleanOption pauseIfUnavailable = new BooleanOption(
            "pause_if_unavailable", "Pause if Unavailable",
            "Pause the macro if a God Potion cannot be obtained",
            true);

    @Override
    public List<ConfigOption<?>> getOptions() { return List.of(source, pauseIfUnavailable); }

    private AcquisitionState acquisitionState = AcquisitionState.IDLE;
    private NpcInteraction    npcInteraction   = null;
    private GuiInteraction    buyInteraction   = null;
    private GuiInteraction    consumeInteraction = null;
    private int               potHotbarSlot    = -1;
    private int               ahRetries        = 0;
    private final Set<Integer> soldSlots       = new HashSet<>();
    private static final int  AH_MAX_RETRIES   = 5;
    private Runnable          onHubArrival     = null;

    private int               stuckTicks       = 0;
    private static final int  STUCK_MAX        = 400; // ~20 seconds

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public AutoGodPotionFeature(FeatureContext ctx) { super(ctx); }

    @Override public String getId()          { return "auto_god_potion"; }
    @Override public String getName()        { return "Auto God Potion"; }
    @Override public String getDescription() { return "Automatically re-up God Potion from a selected source."; }
    @Override public Category getCategory()  { return Category.MISC; }

    @Override
    public void onTick() {
        BuffState state = ctx.gameState().getGodPotionState();
        if (state == BuffState.ACTIVE || state == BuffState.UNKNOWN) {
            if (acquisitionState != AcquisitionState.IDLE) resetState();
            return;
        }

        if (npcInteraction   != null) npcInteraction.tick();
        if (buyInteraction   != null) buyInteraction.tick();
        if (consumeInteraction != null) consumeInteraction.tick();

        if (acquisitionState == AcquisitionState.FAILED
                || acquisitionState == AcquisitionState.DONE
                || acquisitionState == AcquisitionState.WAITING_FOR_CONFIRM) return;

        if (++stuckTicks >= STUCK_MAX) {
            fail("stuck for " + STUCK_MAX + " ticks in state " + acquisitionState);
            return;
        }

        switch (acquisitionState) {
            case IDLE               -> transitionTo(AcquisitionState.CHECKING_INVENTORY);
            case CHECKING_INVENTORY -> checkInventoryFirst();
            case WARPING_TO_HUB     -> tickWarpingToHub();
            case WALKING_TO_NPC     -> {}
            case WAITING_FOR_BUY    -> tickWaitingForBuy();
            case CONSUMING          -> tickConsuming();
            case DONE, FAILED       -> {}
        }
    }

    private void checkInventoryFirst() {
        int slot = InventoryUtil.findInInventory("God Potion");
        if (slot != -1) {
            startConsuming(slot);
        } else {
            switch (source.get()) {
                case INVENTORY               -> fail("no God Potion in inventory");
                case AUCTION_HOUSE_COMMAND   -> startAhCommand();
                case AUCTION_HOUSE_PHYSICAL  -> startAhPhysical();
                case BITS_SHOP               -> startBitsShop();
            }
        }
    }

    private void startConsuming(int invSlot) {
        potHotbarSlot = InventoryUtil.moveToHotbar(invSlot);
        if (potHotbarSlot == -1) { fail("could not move God Potion to hotbar"); return; }
        transitionTo(AcquisitionState.CONSUMING);
    }

    private void tickConsuming() {
        if (consumeInteraction != null) return;
        InventoryUtil.selectHotbarSlot(potHotbarSlot);
        consumeInteraction = GuiInteraction.begin()
                .waitFor("Drink confirm", s -> InventoryUtil.hasSlot(s, "Drink God Potion"),
                        s -> InventoryUtil.clickSlot(s, "Drink God Potion"))
                .onDone(() -> {
                    transitionTo(AcquisitionState.DONE);
                    stopSubInteractions();
                })
                .onFail(reason -> fail("consume GUI failed: " + reason))
                .start();
        net.tidalhq.tidal.util.InputUtil.press(client.options.useKey);
    }

    private void startAhCommand() {
        Location current = ctx.gameState().getCurrentLocation();
        if (current == Location.UNKNOWN) return;
        if (current == Location.HUB) {
            openAhCommand();
        } else {
            warpToHub(this::openAhCommand);
        }
    }

    private void openAhCommand() {
        transitionTo(AcquisitionState.WAITING_FOR_BUY);
        if (client.player != null) client.player.networkHandler.sendChatCommand("ah");
    }

    private void startAhPhysical() {
        Location current = ctx.gameState().getCurrentLocation();
        if (current == Location.UNKNOWN) return;
        if (current == Location.HUB) {
            walkToAhAgent();
        } else {
            warpToHub(this::walkToAhAgent);
        }
    }

    private void walkToAhAgent() {
        transitionTo(AcquisitionState.WALKING_TO_NPC);
        npcInteraction = NpcInteraction
                .create(Npc.AUCTION_MASTER.getBlockPos(), ctx.rotation())
                .onGui(
                        screen -> screen.getTitle().getString().contains("Auction House"),
                        screen -> transitionTo(AcquisitionState.WAITING_FOR_BUY))
                .onFail(reason -> fail("Auction Agent NPC failed: " + reason))
                .start();
    }

    private void startBitsShop() {
        Location current = ctx.gameState().getCurrentLocation();
        if (current == Location.UNKNOWN) return;
        if (current == Location.HUB) {
            walkToElizabeth();
        } else {
            warpToHub(this::walkToElizabeth);
        }
    }

    private void walkToElizabeth() {
        transitionTo(AcquisitionState.WALKING_TO_NPC);
        npcInteraction = NpcInteraction
                .create(Npc.ELIZABETH.getBlockPos(), ctx.rotation())
                .onGui(
                        screen -> screen.getTitle().getString().contains("Community Shop"),
                        screen -> {
                            transitionTo(AcquisitionState.WAITING_FOR_BUY);
                            buyInteraction = GuiInteraction.begin()
                                    .waitFor("Bits Shop tab",
                                            s -> isBitsShop(s),
                                            s -> { if (!s.getTitle().getString().contains("Bits Shop")) InventoryUtil.clickSlot(s, "Bits Shop"); })
                                    .waitFor("Disable confirm",
                                            s -> isBitsShop(s),
                                            s -> disableConfirmIfEnabled(s))
                                    .waitFor("God Potion",
                                            s -> isBitsShop(s) && InventoryUtil.hasSlot(s, "God Potion"),
                                            s -> InventoryUtil.clickSlot(s, "God Potion"))
                                    .onDone(() -> {
                                        transitionTo(AcquisitionState.WAITING_FOR_CONFIRM);
                                        stopBuyInteraction();
                                    })
                                    .onFail(reason -> fail("bits shop GUI failed: " + reason))
                                    .start();
                        })
                .onFail(reason -> fail("Elizabeth NPC failed: " + reason))
                .start();
    }

    private static boolean isBitsShop(net.minecraft.client.gui.screen.Screen screen) {
        String title = screen.getTitle().getString();
        return title.contains("Bits Shop") || title.contains("Community Shop");
    }

    private static void disableConfirmIfEnabled(net.minecraft.client.gui.screen.Screen screen) {
        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen<?> hs)) return;
        for (var slot : hs.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (!slot.getStack().getName().getString().contains("Purchase Confirmation")) continue;
            var lore = slot.getStack().get(net.minecraft.component.DataComponentTypes.LORE);
            if (lore != null && lore.lines().stream()
                    .anyMatch(l -> l.getString().contains("Enabled"))) {
                InventoryUtil.clickSlot(screen, slot.getIndex());
            }
            return;
        }
    }

    private void tickWaitingForBuy() {
        if (buyInteraction != null) return;
        buyInteraction = AuctionHouseUtil.buyBin("God Potion", soldSlots)
                .onDone(() -> {
                    transitionTo(AcquisitionState.WAITING_FOR_CONFIRM);
                    stopBuyInteraction();
                })
                .onFail(reason -> {
                    Tidal.LOGGER.warn("[AutoGodPotion] AH attempt {} failed: {}", ahRetries + 1, reason);
                    stopBuyInteraction();
                    if (++ahRetries >= AH_MAX_RETRIES) {
                        fail("AH failed after " + AH_MAX_RETRIES + " attempts");
                    } else {
                        transitionTo(AcquisitionState.WAITING_FOR_BUY);
                    }
                })
                .start();
    }

    @Subscribe
    public void onChatMessage(ClientReceiveGameMessageEvent event) {
        String msg = event.getMessageContent();

        if (msg.startsWith("You purchased God Potion")) {
            if (client.currentScreen != null) client.setScreen(null);
            int slot = InventoryUtil.findInInventory("God Potion");
            if (slot != -1) {
                startConsuming(slot);
            } else {
                fail("purchased but couldn't find God Potion in inventory");
            }
            return;
        }

        if (acquisitionState == AcquisitionState.WAITING_FOR_CONFIRM
                && (msg.contains("NOT_FOUND_OR_ALREADY_CLAIMED")
                || msg.contains("There was an error with the auction house"))) {
            stopBuyInteraction();
            if (++ahRetries >= AH_MAX_RETRIES) {
                fail("AH: listing sold and retries exhausted");
            } else {
                transitionTo(AcquisitionState.WAITING_FOR_BUY);
            }
            return;
        }

        if (msg.startsWith("You bought God Potion!")) {
            if (client.currentScreen != null) client.setScreen(null);
            int slot = InventoryUtil.findInInventory("God Potion");
            if (slot != -1) {
                startConsuming(slot);
            } else {
                fail("bought from bits shop but couldn't find God Potion in inventory");
            }
        }
    }

    private void tickWarpingToHub() {
        if (ctx.gameState().getCurrentLocation() == Location.HUB && onHubArrival != null) {
            Runnable cb = onHubArrival;
            onHubArrival = null;
            cb.run();
        }
    }

    private void warpToHub(Runnable onArrival) {
        EventBus.getInstance().post(new LocationSanctionEvent(Location.HUB,
                getName() + " warping to hub for God Potion"));
        onHubArrival = onArrival;
        transitionTo(AcquisitionState.WARPING_TO_HUB);
        PlayerUtil.warp(Location.HUB);
    }

    private void transitionTo(AcquisitionState next) {
        acquisitionState = next;
        stuckTicks       = 0;
    }

    private void fail(String reason) {
        acquisitionState = AcquisitionState.FAILED;
        stuckTicks       = 0;
        ctx.notifier().send("[" + getName() + "] could not obtain God Potion: " + reason,
                Notification.NotificationLevel.WARNING);
        stopSubInteractions();
    }

    @Subscribe
    public void onMacroStopped(MacroStoppedEvent event) { resetState(); }

    private void resetState() {
        acquisitionState = AcquisitionState.IDLE;
        potHotbarSlot    = -1;
        ahRetries        = 0;
        stuckTicks       = 0;
        onHubArrival     = null;
        soldSlots.clear();
        stopSubInteractions();
    }

    private void stopBuyInteraction() {
        if (buyInteraction != null) { buyInteraction.stop(); buyInteraction = null; }
    }

    private void stopSubInteractions() {
        if (npcInteraction     != null) { npcInteraction.stop();     npcInteraction     = null; }
        if (consumeInteraction != null) { consumeInteraction.stop(); consumeInteraction = null; }
        stopBuyInteraction();
    }

    @Override
    public boolean shouldPauseMacro(Macro macro) {
        if (acquisitionState == AcquisitionState.IDLE || acquisitionState == AcquisitionState.DONE) return false;
        if (acquisitionState == AcquisitionState.FAILED) return pauseIfUnavailable.get();
        return true;
    }

    @Override
    public void onMacroPaused(Macro macro) {
        if (acquisitionState == AcquisitionState.FAILED) {
            ctx.notifier().danger("[" + getName() + "] macro paused — God Potion unavailable from " + source.get().name());
        } else {
            ctx.notifier().info("[" + getName() + "] macro paused — obtaining God Potion (" + acquisitionState + ")");
        }
    }
}