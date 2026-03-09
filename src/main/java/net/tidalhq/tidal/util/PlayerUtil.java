package net.tidalhq.tidal.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.tidalhq.tidal.Crop;
import net.tidalhq.tidal.Tidal;
import net.tidalhq.tidal.state.Location;

public class PlayerUtil {

    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void warp(Location to) {
        if (client.player == null || client.getNetworkHandler() == null) {
            Tidal.LOGGER.warn("[PlayerUtil] Cannot warp: not connected");
            return;
        }
        client.getNetworkHandler().sendChatMessage("/warp " + to.name());
    }

    public static int toolForCrop(Crop crop) {
        if (crop == null || client.player == null) return -1;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack == null || stack.isEmpty()) continue;

            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data == null) continue;

            NbtCompound nbt = data.copyNbt();
            if (!nbt.contains("id")) continue;

            String id = nbt.getString("id", "");
            if (id.isEmpty()) continue;

            switch (crop) {
                case NETHER_WART:           if (id.contains("HOE_WARTS"))    return slot; break;
                case CARROT:                if (id.contains("HOE_CARROT"))   return slot; break;
                case WHEAT:                 if (id.contains("HOE_WHEAT"))    return slot; break;
                case POTATO:                if (id.contains("HOE_POTATO"))   return slot; break;
                case SUGAR_CANE:            if (id.contains("HOE_CANE"))     return slot; break;
                case CACTUS:                if (id.contains("CACTUS_KNIFE")) return slot; break;
                case MUSHROOM:              if (id.contains("FUNGI_CUTTER") || id.contains("DAEDALUS_AXE")) return slot; break;
                case PUMPKIN_MELON_UNKNOWN: if (id.contains("_DICER"))       return slot; break;
                case MELON:                 if (id.contains("MELON_DICER"))  return slot; break;
                case PUMPKIN:               if (id.contains("PUMPKIN_DICER")) return slot; break;
                case COCOA_BEANS:           if (id.contains("COCO_CHOPPER")) return slot; break;
            }
        }

        return -1;
    }

    public static boolean setToolForCrop(Crop crop) {
        if (crop == null || client.player == null) return false;
        int id = toolForCrop(crop);
        if (id == -1) return false;
        client.player.getInventory().setSelectedSlot(id);
        return true;
    }

    public static int vacuumIndex() {
        if (client.player == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack == null || stack.isEmpty()) continue;

            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data == null) continue;

            NbtCompound nbt = data.copyNbt();
            if (!nbt.toString().toLowerCase().contains("vacuum")) continue;

            return slot;
        }
        return -1;
    }

    public static boolean swapToVacuum() {
        if (client.player == null) return false;
        int index = vacuumIndex();
        if (index == -1) return false;
        client.player.getInventory().setSelectedSlot(index);
        return true;
    }
}