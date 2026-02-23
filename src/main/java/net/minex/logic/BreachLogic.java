package net.minex.logic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.item.AxeItem;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

public class BreachLogic {
    public static boolean enabled = true;
    public static int maxTrackingTicks = 4;
    public static int perfectSwapTicks = 1;
    public static boolean enablePerfectSwapText = true;
    public static boolean showHotbarAnimation = true;
    
    // Colors
    public static int COLOR_PERFECT = 0xA020F0;
    public static int COLOR_GOOD = 0x00FFFF;
    public static int COLOR_PREVIOUS = 0xFFA500;

    private static ItemStack lastMainHandItem = ItemStack.EMPTY;
    private static ItemStack previousTickItem = ItemStack.EMPTY;
    private static long lastAttackTime = 0;
    private static boolean trackingSwap = false;
    private static int ticksSinceAttack = 0;
    private static int successCooldown = 0;
    
    private static int lastSlotIndex = -1;
    private static int attackSlotIndex = -1;
    
    private static boolean successPending = false;
    private static long successDisplayStart = 0;
    private static boolean isPerfect = false;
    private static int successSlot = -1;
    private static int oldSlot = -1;

    private static long lastSlotChangeTime = 0;
    private static int lastSlotBeforeChange = -1;

    public static void checkSwap(int slotIndex, int oldSlotIndex, MinecraftClient client) {
        if (!enabled) return;
        
        lastSlotChangeTime = System.currentTimeMillis();
        lastSlotBeforeChange = oldSlotIndex;

        if (!trackingSwap) return;
        
        long timeSinceAttack = System.currentTimeMillis() - lastAttackTime;
        if (timeSinceAttack > maxTrackingTicks * 50L) {
            trackingSwap = false;
            return;
        }

        if (client.player != null) {
            ItemStack stack = client.player.getInventory().main.get(slotIndex);
            if (slotIndex != attackSlotIndex && checkBreachMace(stack, client)) {
                triggerSuccess(slotIndex, attackSlotIndex);
                trackingSwap = false;
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (!enabled) return;
        
        if (successCooldown > 0) {
            successCooldown--;
        }
        
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        ItemStack currentMainHand = player.getStackInHand(Hand.MAIN_HAND);
        int currentSlotIndex = player.getInventory().selectedSlot;

        if (trackingSwap) {
            ticksSinceAttack++;
            
            if (currentSlotIndex != attackSlotIndex && checkBreachMace(currentMainHand, client)) {
                triggerSuccess(currentSlotIndex, attackSlotIndex);
                trackingSwap = false;
                return;
            }

            if (ticksSinceAttack > maxTrackingTicks) {
                trackingSwap = false;
            }
        }
        
        previousTickItem = currentMainHand.copy();
    }
    
    public static void onAttack(ClientPlayerEntity player) {
        if (!enabled) return;
        
        if (successCooldown > 0) return;
        
        ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
        
        boolean isSwordOrAxe = (stack.getItem() instanceof SwordItem) || (stack.getItem() instanceof AxeItem);
        boolean isMace = checkBreachMace(stack, MinecraftClient.getInstance());
        
        if (!isSwordOrAxe && !isMace) {
            return;
        }
        
        lastAttackTime = System.currentTimeMillis();
        lastMainHandItem = stack.copy();
        ticksSinceAttack = 0;
        attackSlotIndex = player.getInventory().selectedSlot;

        // Check for instant swap
        if (isMace) {
            long timeSinceSwap = System.currentTimeMillis() - lastSlotChangeTime;
            if (timeSinceSwap < 150 && lastSlotBeforeChange != -1 && lastSlotBeforeChange != attackSlotIndex) {
                 triggerSuccess(attackSlotIndex, lastSlotBeforeChange);
                 return;
            }
        }
        
        if (isMace) return;

        trackingSwap = true;
    }

    private static boolean checkBreachMace(ItemStack stack, MinecraftClient client) {
        if (stack.getItem() != Items.MACE) return false;
        
        if (client.world == null) return false;
        
        Registry<Enchantment> registry = client.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> breachEntry = registry.getEntry(Enchantments.BREACH);
        
        if (breachEntry.isPresent()) {
            return EnchantmentHelper.getLevel(breachEntry.get(), stack) > 0;
        }
        
        return false;
    }

    private static void triggerSuccess(int currentSlot, int previousSlot) {
        successPending = true;
        successDisplayStart = System.currentTimeMillis();
        successSlot = currentSlot;
        oldSlot = previousSlot;
        isPerfect = true;
        
        successCooldown = 15;
    }

    public static boolean isSuccessActive() {
        if (!successPending) return false;
        long time = System.currentTimeMillis();
        if (time - successDisplayStart > 1000) {
            successPending = false;
            return false;
        }
        return true;
    }

    public static float getAnimationProgress() {
        if (!successPending) return 0f;
        long time = System.currentTimeMillis();
        long elapsed = time - successDisplayStart;
        
        // Return linear progress from 0.0 to 1.0 over 1000ms
        return Math.max(0f, Math.min(1f, elapsed / 1000f));
    }
    
    public static int getSuccessSlot() { return successSlot; }
    public static int getOldSlot() { return oldSlot; }
    public static boolean isPerfectSwap() { return isPerfect; }
}
