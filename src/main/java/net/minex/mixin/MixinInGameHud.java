package net.minex.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minex.logic.BreachLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void renderBreachIndicator(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!BreachLogic.enabled || !BreachLogic.showHotbarAnimation) return;
        if (!BreachLogic.isSuccessActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        int successSlot = BreachLogic.getSuccessSlot();
        int oldSlot = BreachLogic.getOldSlot();
        float progress = BreachLogic.getAnimationProgress();
        
        int startX = width / 2 - 91;
        int y = height - 22;

        if (successSlot >= 0 && successSlot < 9) {
            int x = startX + successSlot * 20;
            
            int color = BreachLogic.isPerfectSwap() ? BreachLogic.COLOR_PERFECT : 0x0000FF;
            
            int alpha = (int) ((1.0f - progress) * 100);
            if (alpha < 0) alpha = 0;
            
            context.fill(x, y, x + 20, y + 20, (alpha << 24) | (color & 0xFFFFFF));
            context.drawBorder(x, y, 20, 20, (alpha << 24) | (color & 0xFFFFFF));
        }
        
        if (oldSlot >= 0 && oldSlot < 9) {
            int x = startX + oldSlot * 20;
            int color = BreachLogic.COLOR_PREVIOUS;
            int alpha = (int) ((1.0f - progress) * 80);
            if (alpha < 0) alpha = 0;
             context.fill(x, y, x + 20, y + 20, (alpha << 24) | (color & 0xFFFFFF));
        }
    }
}
