package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtil {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static int findInInventory(String itemName) {
        if (client.player == null) return -1;
        String lower = itemName.toLowerCase();
        var inv = client.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getName().getString().toLowerCase().contains(lower)) return i;
        }
        return -1;
    }

    public static int moveToHotbar(int invSlot) {
        if (client.player == null || client.interactionManager == null) return -1;
        if (invSlot < 0) return -1;
        if (invSlot < 9) return invSlot;

        int targetHotbar = findEmptyHotbarSlot();

        client.interactionManager.clickSlot(
                client.player.playerScreenHandler.syncId,
                invSlot,
                targetHotbar,
                SlotActionType.SWAP,
                client.player
        );

        return targetHotbar;
    }

    public static int findEmptyHotbarSlot() {
        if (client.player == null) return 8;
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isEmpty()) return i;
        }
        return 8;
    }

    public static void selectHotbarSlot(int slot) {
        if (client.player == null || slot < 0 || slot > 8) return;
        client.player.getInventory().setSelectedSlot(slot);
    }

    public static int findSlot(Screen screen, String itemName) {
        if (!(screen instanceof HandledScreen<?> hs)) return -1;
        String lower = itemName.toLowerCase();
        for (Slot slot : hs.getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) continue;
            if (slot.getStack().getName().getString().toLowerCase().contains(lower)) return slot.getIndex();
        }
        return -1;
    }

    public static boolean hasSlot(Screen screen, String itemName) {
        return findSlot(screen, itemName) != -1;
    }

    public static void clickSlot(Screen screen, int slotIndex) {
        if (!(screen instanceof HandledScreen<?> hs)) return;
        if (client.interactionManager == null || client.player == null) return;
        var slots = hs.getScreenHandler().slots;
        if (slotIndex < 0 || slotIndex >= slots.size()) return;
        client.interactionManager.clickSlot(
                hs.getScreenHandler().syncId,
                slotIndex,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }

    public static void clickSlot(Screen screen, String itemName) {
        int slot = findSlot(screen, itemName);
        if (slot != -1) clickSlot(screen, slot);
    }
}