package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;

public class AuctionHouseUtil {

    private static final int SLOT_SEARCH     = 48;
    private static final int SLOT_SORT       = 50;
    private static final int SLOT_BIN_FILTER = 52;
    private static final int SLOT_BUY_NOW    = 31;

    public static GuiInteraction buyBin(String itemName, Set<Integer> soldSlots) {
        return GuiInteraction.begin()
                .waitFor("AH lobby",      s -> isTitled(s, "Auction House") && !isResultsScreen(s) && !isTitled(s, "Browser"),
                        s -> InventoryUtil.clickSlot(s, "Auctions Browser"))
                .waitFor("AH browser",    s -> isTitled(s, "Auctions Browser"),
                        s -> InventoryUtil.clickSlot(s, SLOT_SEARCH))
                .waitFor("Search sign",   s -> s instanceof AbstractSignEditScreen,
                        s -> SignInput.setAndConfirm(itemName))
                .waitFor("Sort lowest",   s -> isResultsScreen(s) && isSortedLowestPrice(s),
                        s -> InventoryUtil.clickSlot(s, SLOT_SORT))
                .waitFor("BIN filter",    s -> isResultsScreen(s) && isBinOnly(s),
                        s -> InventoryUtil.clickSlot(s, SLOT_BIN_FILTER))
                .waitFor("Select item",   s -> isResultsScreen(s) && findBuyableSlot(s, itemName, soldSlots) != -1,
                        s -> InventoryUtil.clickSlot(s, findBuyableSlot(s, itemName, soldSlots)))
                .waitFor("Buy now",       s -> isTitled(s, "BIN Auction View"),
                        s -> InventoryUtil.clickSlot(s, SLOT_BUY_NOW))
                .waitFor("Confirm",       s -> isTitled(s, "Confirm Purchase"),
                        s -> {InventoryUtil.clickSlot(s, "Confirm"); MinecraftClient.getInstance().setScreen(null);});
    }

    public static GuiInteraction buyBin(String itemName) {
        return buyBin(itemName, new HashSet<>());
    }

    private static boolean isTitled(Screen screen, String title) {
        if (!(screen instanceof HandledScreen<?>)) return false;
        return screen.getTitle().getString().contains(title);
    }

    private static boolean isResultsScreen(Screen screen) {
        if (!(screen instanceof HandledScreen<?>)) return false;
        return screen.getTitle().getString().startsWith("Auctions:");
    }

    private static boolean isSortedLowestPrice(Screen screen) {
        if (!(screen instanceof HandledScreen<?> hs)) return false;
        var slots = hs.getScreenHandler().slots;
        if (SLOT_SORT >= slots.size()) return false;
        var stack = slots.get(SLOT_SORT).getStack();
        if (stack.isEmpty()) return false;
        return getLore(stack).contains("Lowest Price");
    }

    private static boolean isBinOnly(Screen screen) {
        if (!(screen instanceof HandledScreen<?> hs)) return false;
        var slots = hs.getScreenHandler().slots;
        if (SLOT_BIN_FILTER >= slots.size()) return false;
        var stack = slots.get(SLOT_BIN_FILTER).getStack();
        if (stack.isEmpty()) return false;
        return getLore(stack).contains("BIN Only");
    }

    private static String getLore(net.minecraft.item.ItemStack stack) {
        var lore = stack.get(net.minecraft.component.DataComponentTypes.LORE);
        if (lore == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Text line : lore.lines()) sb.append(line.getString());
        return sb.toString();
    }

    public static int findBuyableSlot(Screen screen, String itemName, Set<Integer> soldSlots) {
        if (!(screen instanceof HandledScreen<?> hs)) return -1;
        String lower = itemName.toLowerCase();
        for (Slot slot : hs.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (!slot.getStack().getName().getString().toLowerCase().contains(lower)) continue;
            if (soldSlots.contains(slot.getIndex())) continue;
            return slot.getIndex();
        }
        return -1;
    }
}