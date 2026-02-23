package net.minex.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minex.logic.BreachLogic;

public class BreachSwapIndicatorClient implements ClientModInitializer {
    private int tickCounter = 0;
    private int lastKnownSlot = -1;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            int currentSlot = client.player.getInventory().selectedSlot;
            
            if (lastKnownSlot == -1) {
                lastKnownSlot = currentSlot;
            }
            
            if (currentSlot != lastKnownSlot) {
                 BreachLogic.checkSwap(currentSlot, lastKnownSlot, client);
                 lastKnownSlot = currentSlot;
            }
            
            BreachLogic.tick(client);
        });
        
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                 if (player instanceof net.minecraft.client.network.ClientPlayerEntity) {
                     BreachLogic.onAttack((net.minecraft.client.network.ClientPlayerEntity) player);
                 }
            }
            return net.minecraft.util.ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            renderOverlay(drawContext);
        });
        
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("breachswaphud")
                .executes(context -> {
                    BreachLogic.enabled = !BreachLogic.enabled;
                    context.getSource().sendFeedback(Text.literal("Breach HUD " + (BreachLogic.enabled ? "Enabled" : "Disabled")).formatted(BreachLogic.enabled ? Formatting.GREEN : Formatting.RED));
                    return 1;
                }));
        });
    }

    private void renderOverlay(DrawContext context) {
        if (!BreachLogic.enabled) return;
        if (!BreachLogic.isSuccessActive()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        if (BreachLogic.enablePerfectSwapText) {
            String text = "Perfect Swap";
            int color = BreachLogic.isPerfectSwap() ? BreachLogic.COLOR_PERFECT : BreachLogic.COLOR_GOOD;
            
            TextRenderer textRenderer = client.textRenderer;
            int textWidth = textRenderer.getWidth(text);
            int x = (width - textWidth) / 2;
            
            float progress = BreachLogic.getAnimationProgress();
            
            int startY = height - 70;
            int offset = (int) (progress * 15);
            int y = startY - offset;

            int alpha = 255;
            if (progress < 0.2f) {
                alpha = (int) ((progress / 0.2f) * 255);
            } else if (progress > 0.7f) {
                alpha = (int) ((1.0f - (progress - 0.7f) / 0.3f) * 255);
            }
            
            if (alpha < 5) alpha = 5;
            
            color = 0xA020F0;
            color = (color & 0x00FFFFFF) | (alpha << 24);

            float scale = 1.0f;
            if (progress < 0.2f) {
                scale = 0.8f + (progress / 0.2f) * 0.4f;
            } else if (progress < 0.4f) {
                scale = 1.2f - ((progress - 0.2f) / 0.2f) * 0.2f;
            }

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(x + textWidth / 2.0f, y + textRenderer.fontHeight / 2.0f, 0);
            matrices.scale(scale, scale, 1.0f);
            matrices.translate(-(x + textWidth / 2.0f), -(y + textRenderer.fontHeight / 2.0f), 0);
            
            context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
            
            matrices.pop();
        }
    }
}
