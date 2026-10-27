package im.client.client.screen.hud.impl;

import im.client.client.api.events.orbit.EventHandler;
import im.client.client.api.events.orbit.EventPriority;
import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.math.ScaleMath;
import im.client.client.utils.render.draw.RenderUtil;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.mojang.blaze3d.matrix.MatrixStack;

public class ArmorHudRenderer implements IRenderer {
    private static final ResourceLocation WIDGETS_TEX_PATH = new ResourceLocation("textures/gui/widgets.png");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void render(Render2DEvent event) {
        ScaleMath.scalePost();
        mc.getTextureManager().bindTexture(WIDGETS_TEX_PATH);
        MatrixStack matrix = event.getMatrix();
        int screenWidth = mw.getScaledWidth();
        int screenHeight = mw.getScaledHeight();
        int centerX = screenWidth / 2;

        HandSide handSide = mc.player.getPrimaryHand().opposite();
        boolean isRightHand = handSide.equals(HandSide.RIGHT) && !mc.player.getHeldItemOffhand().isEmpty();

        RenderUtil.start();

        int xPos1 = centerX + 91 + 7 + (isRightHand ? 29 : 0);
        int xPos2 = centerX + 141 - 2 + (isRightHand ? 29 : 0);
        int yPos = screenHeight - 22;

        AbstractGui.blit(matrix, xPos1, yPos, 0, 0, 0, 41, 22);
        AbstractGui.blit(matrix, xPos2, yPos, 0, 141, 0, 41, 22);

        //    RectHelper.drawNewHUDRect(matrix, xPos1 - 0.86F, yPos + 0.35F, 41 + 41, 20, 3);

        int xOffset = 0;
        for (ItemStack itemStack : mc.player.getArmorInventoryList()) {
            mc.ingameGUI.renderHotbarItem(xPos1 + 3 + xOffset, yPos + 2, event.getPartialTicks(), mc.player, itemStack);
            xOffset += 20;
        }

        RenderUtil.stop();
        ScaleMath.scalePre();
    }
}
