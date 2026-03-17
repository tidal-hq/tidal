package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class BazaarUtil {

    private static final int SLOT_SEARCH = 45;

    public static GuiInteraction buy(String itemName, int quantity) {
        return GuiInteraction.begin()
                .waitFor("Bazaar main",    BazaarUtil::isBazaarMain,                s -> InventoryUtil.clickSlot(s, SLOT_SEARCH),          3, 6)
                .waitFor("Search sign",    s -> s instanceof AbstractSignEditScreen, s -> SignInput.setAndConfirm(itemName),                8, 18)
                .waitFor("Search results", s -> InventoryUtil.hasSlot(s, itemName),  s -> InventoryUtil.clickSlot(s, itemName),            4, 8)
                .waitFor("Buy Instantly",  s -> InventoryUtil.hasSlot(s, "Buy Instantly"),  s -> InventoryUtil.clickSlot(s, "Buy Instantly"), 3, 7)
                .waitFor("Custom qty",     s -> InventoryUtil.hasSlot(s, "Custom Amount"),  s -> InventoryUtil.clickSlot(s, "Custom Amount"), 2, 5)
                .waitFor("Quantity sign",  s -> s instanceof AbstractSignEditScreen, s -> SignInput.setAndConfirm(String.valueOf(quantity)), 6, 14)
                .waitFor("Confirm",        s -> InventoryUtil.hasSlot(s, "Custom Amount"),  s -> { InventoryUtil.clickSlot(s, "Custom Amount"); MinecraftClient.getInstance().setScreen(null); }, 2, 4)
                .onDone(() -> MinecraftClient.getInstance().setScreen(null));
    }

    public static GuiInteraction sell(String itemName, int quantity) {
        return GuiInteraction.begin()
                .waitFor("Bazaar main",    BazaarUtil::isBazaarMain,                s -> InventoryUtil.clickSlot(s, SLOT_SEARCH),          3, 6)
                .waitFor("Search sign",    s -> s instanceof AbstractSignEditScreen, s -> SignInput.setAndConfirm(itemName),                8, 18)
                .waitFor("Search results", s -> InventoryUtil.hasSlot(s, itemName),  s -> InventoryUtil.clickSlot(s, itemName),            4, 8)
                .waitFor("Sell Instantly", s -> InventoryUtil.hasSlot(s, "Sell Instantly"), s -> { InventoryUtil.clickSlot(s, "Sell Instantly"); MinecraftClient.getInstance().setScreen(null); }, 2, 4);
    }

    private static boolean isBazaarMain(Screen screen) {
        if (!(screen instanceof HandledScreen<?>)) return false;
        String title = screen.getTitle().getString();
        return title.contains("Bazaar") && !title.contains("Confirm");
    }
}