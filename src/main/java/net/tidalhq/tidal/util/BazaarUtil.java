package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.tidalhq.tidal.Tidal;

public class BazaarUtil {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static final int SLOT_SEARCH     = 45;
    private static final int SLOT_BUY_NOW    = 10;
    private static final int SLOT_CUSTOM_QTY = 16;
    private static final int SLOT_CONFIRM    = 13;
    private static final int SLOT_SELL_NOW   = 32;

    public static GuiInteraction buy(String itemName, int quantity) {
        return GuiInteraction.begin()
                .waitFor("Bazaar main",     BazaarUtil::isBazaarMain,     s -> clickSlot(s, SLOT_SEARCH))
                .waitFor("Search sign",     s -> s instanceof AbstractSignEditScreen,
                        s -> SignInput.setAndConfirm(itemName))
                .waitFor("Search results",  s -> findItemSlot(s, itemName) != -1,
                        s -> clickSlot(s, findItemSlot(s, itemName)))
                .waitFor("Buy Instantly",   s -> hasSlotNamed(s, "Buy Instantly"),
                        s -> clickSlot(s, findSlotNamed(s, "Buy Instantly")))
                .waitFor("Custom qty",      s -> hasSlotNamed(s, "Custom Amount"),
                        s -> clickSlot(s, findSlotNamed(s, "Custom Amount")))
                .waitFor("Quantity sign",   s -> s instanceof AbstractSignEditScreen,
                        s -> SignInput.setAndConfirm(String.valueOf(quantity)))
                .waitFor("Confirm",         s -> hasSlotNamed(s, "Custom Amount"),
                        s -> clickSlot(s, findSlotNamed(s, "Custom Amount")));
    }

    public static GuiInteraction sell(String itemName, int quantity) {
        return GuiInteraction.begin()
                .waitFor("Bazaar index",    BazaarUtil::isBazaarMain,    s -> clickSlot(s, SLOT_SEARCH))
                .waitFor("Search sign",     s -> s instanceof AbstractSignEditScreen,
                        s -> SignInput.setAndConfirm(itemName))
                .waitFor("Search results",  s -> findItemSlot(s, itemName) != -1,
                        s -> clickSlot(s, findItemSlot(s, itemName)))
                .waitFor("Sell Instantly",  s -> hasSlotNamed(s, "Sell Instantly"));
    }

    private static boolean isBazaarMain(Screen screen) {
        if (!(screen instanceof HandledScreen<?>)) return false;
        String title = screen.getTitle().getString();
        return title.contains("Bazaar") && !title.contains("Confirm");
    }

    private static int findItemSlot(Screen screen, String itemName) {
        if (!(screen instanceof HandledScreen<?> hs)) return -1;
        String lower = itemName.toLowerCase();
        for (Slot slot : hs.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (slot.getStack().getName().getString().toLowerCase().contains(lower)) return slot.getIndex();
        }
        return -1;
    }

    private static boolean hasSlotNamed(Screen screen, String name) {
        return findSlotNamed(screen, name) != -1;
    }

    private static int findSlotNamed(Screen screen, String name) {
        if (!(screen instanceof HandledScreen<?> hs)) return -1;
        String lower = name.toLowerCase();
        for (Slot slot : hs.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (slot.getStack().getName().getString().toLowerCase().contains(lower)) return slot.getIndex();
        }
        return -1;
    }

    public static void clickSlot(Screen screen, int slotIndex) {
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (client.interactionManager == null || client.player == null) return;
        var slots = hs.getScreenHandler().slots;
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        client.interactionManager.clickSlot(
                hs.getScreenHandler().syncId, slotIndex, 0, SlotActionType.PICKUP, client.player);
    }
}