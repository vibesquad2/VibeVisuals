package im.client.client.screen.hud.impl;

import im.client.Client;
import im.client.client.api.interfaces.IWindow;
import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.player.MoveUtil;
import im.client.client.utils.player.PlayerUtil;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.font.Fonts;
import net.minecraft.client.gui.screen.ChatScreen;
import net.mojang.blaze3d.matrix.MatrixStack;

public class InformationRenderer implements IRenderer, IWindow {
    private float smoothY = -1.0F;
    private long lastTimeNs = 0L;
    private static final float SMOOTH_SPEED = 20.0F;

    public void render(Render2DEvent var1) {
        MatrixStack var2 = var1.getMatrix();
        double var10000 = Mathf.round(mc.player.getPosX(), 1);
        String var3 = var10000 + ", " + Mathf.round(mc.player.getPosY(), 1) + ", " + Mathf.round(mc.player.getPosZ(), 1);
        var10000 = MoveUtil.speedSqrt() * (double) 20.0F;
        String var4 = Mathf.round(var10000, 1) + " bps";
        var10000 = Client.inst().serverTps().getTPS();
        String var5 = Mathf.round(var10000, 1) + " tps";
        String var6 = PlayerUtil.getPing(mc.player) + " ms";
        float var7 = 8;
        float var8 = 7;
        boolean var9 = mc.currentScreen instanceof ChatScreen;
        float var10 = (float) mc.getMainWindow().getScaledHeight() - var7 - 15;
        if (var9) {
            var10 -= 10;
        }

        float var11 = this.computeDtSeconds();
        if (this.smoothY < 0.0F) {
            this.smoothY = var10;
        }

        this.smoothY = this.smoothTowards(this.smoothY, var10, var11, 20.0F);
        float var12 = this.smoothY;
        int var13 = ColorUtil.fade();
        int var14 = ColorUtil.getColor(200, 200, 200);
        int var15 = ColorUtil.getColor(90, 90, 90);
        Fonts.SFP_MEDIUM.draw(var2, "TPS: ", var8, var12, var13, var7);
        float var16 = var8 + Fonts.SFP_MEDIUM.getWidth("TPS: ", var7);
        Fonts.SFP_MEDIUM.draw(var2, var5, var16, var12, var14, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth(var5, var7);
        Fonts.SFP_MEDIUM.draw(var2, " | ", var16, var12, var15, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth(" | ", var7);
        Fonts.SFP_MEDIUM.draw(var2, "Ping: ", var16, var12, var13, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth("Ping: ", var7);
        Fonts.SFP_MEDIUM.draw(var2, var6, var16, var12, var14, var7);
        float var17 = var12 + var7 + 2.0F;
        Fonts.SFP_MEDIUM.draw(var2, "Pos: ", var8, var17, var13, var7);
        var16 = var8 + Fonts.SFP_MEDIUM.getWidth("Pos: ", var7);
        Fonts.SFP_MEDIUM.draw(var2, var3, var16, var17, var14, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth(var3, var7);
        Fonts.SFP_MEDIUM.draw(var2, " | ", var16, var17, var15, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth(" | ", var7);
        Fonts.SFP_MEDIUM.draw(var2, "Speed: ", var16, var17, var13, var7);
        var16 += Fonts.SFP_MEDIUM.getWidth("Speed: ", var7);
        Fonts.SFP_MEDIUM.draw(var2, var4, var16, var17, var14, var7);
    }

    private float smoothTowards(float var1, float var2, float var3, float var4) {
        if (Float.isFinite(var3) && !(var3 <= 0.0F)) {
            float var5 = 1.0F - (float) Math.exp(-var4 * var3);
            return var1 + (var2 - var1) * var5;
        } else {
            return var2;
        }
    }

    private float computeDtSeconds() {
        long var1 = System.nanoTime();
        if (this.lastTimeNs == 0L) {
            this.lastTimeNs = var1;
            return 0.016666668F;
        } else {
            long var3 = var1 - this.lastTimeNs;
            this.lastTimeNs = var1;
            double var5 = Math.min(Math.max((double) var3 / (double) 1.0E9F, 0.0F), 0.1);
            return (float) var5;
        }
    }
}