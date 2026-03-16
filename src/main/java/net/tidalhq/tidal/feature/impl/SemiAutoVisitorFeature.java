package net.tidalhq.tidal.feature.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.config.BooleanOption;
import net.tidalhq.tidal.config.ConfigOption;
import net.tidalhq.tidal.event.Subscribe;
import net.tidalhq.tidal.event.impl.ClientEndTickEvent;
import net.tidalhq.tidal.feature.Feature;
import net.tidalhq.tidal.feature.FeatureContext;
import net.tidalhq.tidal.feature.MacroLifecycleHook;
import net.tidalhq.tidal.macro.Macro;
import net.tidalhq.tidal.util.BazaarUtil;
import net.tidalhq.tidal.util.GuiInteraction;
import net.tidalhq.tidal.util.InventoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemiAutoVisitorFeature extends Feature implements MacroLifecycleHook {

    private static final Pattern ITEM_PATTERN = Pattern.compile("^(.+?)\\s*x([\\d,]+)$");

    private record RequiredItem(String name, int quantity) {}

    private enum State {
        IDLE, READING_OFFER, BUYING, WAITING_TO_REOPEN, ACCEPTING, REFUSING, DONE
    }

    private final BooleanOption autoBuy = new BooleanOption(
            "auto_buy", "Auto Buy Missing Items",
            "Buy missing items from /bz before accepting", true);

    private final BooleanOption refuseIfCantBuy = new BooleanOption(
            "refuse_if_cant_buy", "Refuse if Can't Buy",
            "Refuse the offer if items can't be obtained", true);

    @Override
    public List<ConfigOption<?>> getOptions() { return List.of(autoBuy, refuseIfCantBuy); }

    private State          state          = State.IDLE;
    private GuiInteraction buyInteraction = null;
    private List<RequiredItem> required   = new ArrayList<>();
    private String         visitorName    = "";
    private int            waitTicks      = 0;
    private static final int REOPEN_WAIT  = 40;

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public SemiAutoVisitorFeature(FeatureContext ctx) { super(ctx); }

    @Override public String getId()          { return "visitor_macro"; }
    @Override public String getName()        { return "Visitor Macro"; }
    @Override public String getDescription() { return "Semi-auto visitor handling, click visitor to trigger."; }
    @Override public Category getCategory()  { return Category.MISC; }

    @Override public boolean shouldPauseMacro(Macro macro) { return state != State.IDLE && state != State.DONE; }
    @Override public void onMacroPaused(Macro macro)       { log().info("handling visitor: " + visitorName); }
    @Override public void onMacroResumed(Macro macro)      { net.tidalhq.tidal.util.PlayerUtil.setToolForCrop(macro.getTargetCrop()); }

    @Subscribe
    public void onClientEndTick(ClientEndTickEvent event) {
        if (!isEnabled()) return;
        if (client.player == null) return;

        if (buyInteraction != null) buyInteraction.tick();

        switch (state) {
            case IDLE -> {
                if (client.currentScreen instanceof HandledScreen<?> hs) {
                    if (isVisitorScreen(hs)) {
                        visitorName = hs.getTitle().getString();
                        transitionTo(State.READING_OFFER);
                    }
                }
            }

            case READING_OFFER -> {
                if (!(client.currentScreen instanceof HandledScreen<?> hs)) {
                    transitionTo(State.IDLE);
                    return;
                }

                required = parseRequirements(hs);

                if (required.isEmpty()) {
                    transitionTo(State.ACCEPTING);
                    return;
                }

                List<RequiredItem> missing = getMissing(required);
                if (missing.isEmpty()) {
                    transitionTo(State.ACCEPTING);
                } else if (autoBuy.get()) {
                    client.setScreen(null);
                    startBuying(missing);
                } else if (refuseIfCantBuy.get()) {
                    transitionTo(State.REFUSING);
                } else {
                    transitionTo(State.IDLE);
                }
            }

            case BUYING -> {}

            case WAITING_TO_REOPEN -> {
                if (++waitTicks >= REOPEN_WAIT) {
                    waitTicks = 0;
                    log().info("items bought — click " + visitorName + " again to accept");
                    transitionTo(State.IDLE);
                }
            }

            case ACCEPTING -> {
                if (!(client.currentScreen instanceof HandledScreen<?> hs)) {
                    transitionTo(State.IDLE);
                    return;
                }
                InventoryUtil.clickSlot(client.currentScreen, "Accept Offer");
                log().info("accepted offer from " + visitorName);
                transitionTo(State.DONE);
            }

            case REFUSING -> {
                if (!(client.currentScreen instanceof HandledScreen<?> hs)) {
                    transitionTo(State.IDLE);
                    return;
                }
                InventoryUtil.clickSlot(client.currentScreen, "Refuse Offer");
                log().info("refused offer from " + visitorName);
                transitionTo(State.DONE);
            }

            case DONE -> {
                if (client.currentScreen == null) transitionTo(State.IDLE);
            }
        }
    }

    private void startBuying(List<RequiredItem> missing) {
        if (missing.isEmpty()) {
            transitionTo(State.WAITING_TO_REOPEN);
            return;
        }

        RequiredItem first = missing.get(0);
        List<RequiredItem> rest = missing.subList(1, missing.size());

        transitionTo(State.BUYING);
        if (client.player != null) client.player.networkHandler.sendChatCommand("bz");

        buyInteraction = BazaarUtil.buy(first.name(), first.quantity())
                .onDone(() -> {
                    client.setScreen(null);
                    buyInteraction = null;
                    if (rest.isEmpty()) {
                        transitionTo(State.WAITING_TO_REOPEN);
                    } else {
                        startBuying(rest);
                    }
                })
                .onFail(reason -> {
                    buyInteraction = null;
                    log().warning("couldn't buy " + first.name() + ": " + reason);
                    if (refuseIfCantBuy.get()) transitionTo(State.REFUSING);
                    else transitionTo(State.IDLE);
                })
                .start();
    }

    private List<RequiredItem> parseRequirements(HandledScreen<?> screen) {
        List<RequiredItem> items = new ArrayList<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (!slot.getStack().getName().getString().contains("Accept Offer")) continue;

            var lore = slot.getStack().get(DataComponentTypes.LORE);
            if (lore == null) continue;

            boolean inRequiredSection = false;
            for (Text line : lore.lines()) {
                String text = line.getString().trim();
                if (text.contains("Items Required:")) { inRequiredSection = true; continue; }
                if (inRequiredSection) {
                    if (text.isEmpty() || text.contains("Rewards:") || text.contains("Missing")) break;
                    Matcher m = ITEM_PATTERN.matcher(text);
                    if (m.matches()) {
                        String name = m.group(1).trim();
                        int qty = Integer.parseInt(m.group(2).replace(",", ""));
                        items.add(new RequiredItem(name, qty));
                    }
                }
            }
            break;
        }
        return items;
    }

    private List<RequiredItem> getMissing(List<RequiredItem> required) {
        List<RequiredItem> missing = new ArrayList<>();
        for (RequiredItem req : required) {
            int have = countInInventory(req.name());
            if (have < req.quantity()) missing.add(new RequiredItem(req.name(), req.quantity() - have));
        }
        return missing;
    }

    private int countInInventory(String itemName) {
        if (client.player == null) return 0;
        String lower = itemName.toLowerCase();
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(lower))
                total += stack.getCount();
        }
        return total;
    }

    private static boolean isVisitorScreen(HandledScreen<?> screen) {
        boolean hasAccept = false;
        boolean hasRefuse = false;
        for (var slot : screen.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            String name = slot.getStack().getName().getString();
            if (name.contains("Accept Offer")) hasAccept = true;
            if (name.contains("Refuse Offer")) hasRefuse = true;
            if (hasAccept && hasRefuse) return true;
        }
        return false;
    }

    private void transitionTo(State next) { state = next; }
}