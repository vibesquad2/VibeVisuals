package im.client.client.screen.hud.impl;

import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.screen.hud.AbstractHud;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.animation.util.Easings;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.render.color.ColorFormatting;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.RectUtil;
import im.client.client.utils.render.font.Fonts;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Namespaced;
import net.minecraft.util.StringUtils;
import net.minecraft.util.registry.Registry;
import net.mojang.blaze3d.matrix.MatrixStack;

import java.util.Collection;

public class PotionsRenderer extends AbstractHud implements IRenderer {

    private final DragSetting drag;

    // ---- размеры/отступы ----
    private static final float MARGIN = 5f;
    private static final float TITLE_SIZE = 7.5f;
    private static final float HDR_ICON_SIZE = 6.0f;
    private static final float ROW_FONT = 7.0f;
    private static final float ROW_GAP = 3.0f;
    private static final float RADIUS = 6f;
    private static final float BETWEEN_TEXT_GAP = 8f;
    private static final float EFFECT_ICON_W = 8f;

    // ---- FPS-independent сглаживание размеров панели ----
    private static final float SIZE_SMOOTH_SPEED = 12f;
    private float smoothWidth = -1f;
    private float smoothHeight = -1f;
    private long lastTimeNs = 0L;

    // ---- анимация выезда строк (как в KeybindsRenderer) ----
    private static final float ROW_SLIDE_SPEED = 8f;
    private final float[] rowSlideOffset = new float[64];

    public PotionsRenderer(DragSetting drag) {
        this.drag = drag;
        for (int i = 0; i < rowSlideOffset.length; i++) {
            rowSlideOffset[i] = 0.0f; // старт слева
        }
    }

    @Override
    public void render(Render2DEvent event) {
        Collection<EffectInstance> active = mc.player.getActivePotionEffects();

        for (EffectInstance effect : active) {
            effect.getAnimation().update();
            effect.getAnimation().run((effect.getDuration() / 20F) <= 1F ? 0 : 1, 0.2F, Easings.SINE_OUT, false);
        }

        MatrixStack matrix = event.getMatrix();

        final String title = "Active potions";
        final boolean isEmpty = active.isEmpty();

        float dt = computeDtSeconds();

        final float headerH = MARGIN + ROW_FONT + MARGIN;

        // ---- вычисление ширины ----
        float titleW = Fonts.SFP_MEDIUM.getWidth(title, TITLE_SIZE);
        float titleIconW = Fonts.ICON_DIMAS.getWidth("B", HDR_ICON_SIZE);
        float headerWidth = MARGIN + 1f + titleW + MARGIN + titleIconW + MARGIN;

        float maxRowW = 0f;
        int idx = 0;
        for (EffectInstance potion : active) {
            String leftText = potion.getPotion().getDisplayName().getString() + " " + (potion.getAmplifier() + 1);
            String rightText = getDuration(potion);

            float leftW = Fonts.SFP_MEDIUM.getWidth(leftText, ROW_FONT);
            float rightW = Fonts.SFP_MEDIUM.getWidth(rightText, ROW_FONT);

            float rowW = MARGIN + leftW + BETWEEN_TEXT_GAP + rightW + 8f + MARGIN;
            if (rowW > maxRowW) maxRowW = rowW;

            if (idx < rowSlideOffset.length && potion.getAnimation().get() <= 0f) {
                rowSlideOffset[idx] = 0f;
            }
            idx++;
        }

        float targetWidth = Math.max(90, Math.max(headerWidth, maxRowW));

        if (smoothWidth < 0f) smoothWidth = targetWidth;
        if (smoothHeight < 0f) smoothHeight = headerH;

        smoothWidth = smoothTowards(smoothWidth, targetWidth, dt, SIZE_SMOOTH_SPEED);
        drag.size.x = (float) Mathf.step(smoothWidth, 0.5);

        float x = drag.position.x;
        float y = drag.position.y;

        boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);
        update(closeCondition ? 0 : 1);
        drag.active = !closeCondition;
        if (closeCondition && animValue() == 0.0F) return;

        x = (float) Mathf.step(x, 0.5);
        y = (float) Mathf.step(y, 0.5);
        float width = (float) Mathf.step(drag.size.x, 0.5);

        // обновление анимации строк
        updateRowAnimations(active, dt);

        matrix.push();

        InterFace.getInstance().drawClientRect(matrix, x, y, width, 16, animation.get(), 4);

        Fonts.SFP_MEDIUM.draw(
                matrix,
                title,
                x + MARGIN + 8 + 7F,
                y + (headerH / 2F) - (7.5F / 2F) - 0.6F,
                textWhite(),
                7.7F
        );

        RectUtil.drawRect(matrix, x + MARGIN + 11, y + 3.85F, 0.5F, 8, ColorUtil.getColor(255, 0.1F * animation.get()));

        Fonts.ICON_NURIK.draw(
                matrix,
                "E",
                x + MARGIN,
                y + MARGIN - 0.6F,
                textWhite(),
                8
        );

        float offsetY = 0f;
        float ys = -2.5F;
        int rowIndex = 0;

        for (EffectInstance potion : active) {
            if (rowIndex >= rowSlideOffset.length) break;

            float animPC = potion.getAnimation().get();
            if (animPC <= 0f) {
                rowIndex++;
                continue;
            }

            float baseY = y + ys + headerH + MARGIN + offsetY;

            long time = System.currentTimeMillis();
            float frequency = 2.0f;
            float alphaWave = (float) ((Math.sin(time / 1000.0 * frequency * Math.PI) + 1) / 2);

            int alpha;
            if (potion.getDuration() < 100) {
                alpha = (int) ((100 + 155 * alphaWave) * animPC);
            } else {
                alpha = (int) (255 * animPC);
            }
            boolean bad = isBad(potion);

            int textClrBase = bad
                    ? ColorUtil.getColor(200, 105, 105, alpha)
                    : ColorUtil.replAlpha(textWhite(), alpha);
            int textClr = ColorUtil.replAlpha(textClrBase, alpha);

            String leftText = potion.getPotion().getDisplayName().getString() + " "
                    + ColorFormatting.getColor(textClr)
                    + String.valueOf((potion.getAmplifier() + 1)).replace("1", "");

            String rightText = getDuration(potion);

            float rawSlide = rowSlideOffset[rowIndex];     // 0..1
            float slidePC = easeOutCubic(rawSlide);        // плавный выезд слева [web:12][web:3]
            float rowX = x - width + width * slidePC;      // слева за панелью -> на x

            InterFace.getInstance().drawClientRect(
                    matrix,
                    rowX,
                    baseY + ys,
                    width,
                    12,
                    animation.get() * animPC * slidePC,
                    4
            );

            RectUtil.drawRect(
                    matrix,
                    rowX + width - MARGIN - 7,
                    baseY + ys + 3,
                    0.5F,
                    6,
                    ColorUtil.getColor(255, 0.1F * animation.get() * animPC)
            );

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    leftText,
                    rowX + 4,
                    baseY + ys - MARGIN + 7.8F,
                    textClr,
                    6.7F
            );

            Fonts.SFP_SEMIBOLD.drawRight(
                    matrix,
                    rightText,
                    rowX + width - MARGIN - 9,
                    baseY + ys - MARGIN + 7.8F,
                    textClr,
                    6.7F
            );

            try {
                String effectKey = Registry.EFFECTS.getKey(potion.getPotion()).getPath();
                String iconPath = "effect/" + effectKey + ".png";
                mc.getTextureManager().bindTexture(new Namespaced(iconPath));
                RectUtil.drawRect(
                        matrix,
                        rowX + width - MARGIN - 5,
                        baseY - MARGIN + 4.5F,
                        EFFECT_ICON_W,
                        EFFECT_ICON_W,
                        ColorUtil.replAlpha(ColorUtil.getColor(255), alpha),
                        false,
                        true
                );
            } catch (Throwable ignored) {
            }

            offsetY += (12.5F) * animPC;
            rowIndex++;
        }

        float targetHeight = headerH + offsetY + (MARGIN * 2F) - 5.5F;
        smoothHeight = smoothTowards(smoothHeight, targetHeight, dt, SIZE_SMOOTH_SPEED);
        drag.size.y = (float) Mathf.step(smoothHeight, 0.5);

        matrix.pop();
    }

    // ===== анимация строк =====

    private void updateRowAnimations(Collection<EffectInstance> active, float dt) {
        int i = 0;
        for (EffectInstance potion : active) {
            if (i >= rowSlideOffset.length) break;

            float target = potion.getAnimation().get(); // 0..1
            rowSlideOffset[i] = smoothTowards(
                    rowSlideOffset[i],
                    target,
                    dt,
                    ROW_SLIDE_SPEED
            );
            if (rowSlideOffset[i] < 0f) rowSlideOffset[i] = 0f;
            if (rowSlideOffset[i] > 1f) rowSlideOffset[i] = 1f;
            i++;
        }
    }

    // easeOutCubic(t) [0..1] -> [0..1] [web:12][web:3]
    private float easeOutCubic(float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    // ===== helpers =====

    private boolean isBad(EffectInstance potion) {
        return potion.getPotion().equals(Effects.SLOWNESS)
                || potion.getPotion().equals(Effects.BLINDNESS)
                || potion.getPotion().equals(Effects.POISON)
                || potion.getPotion().equals(Effects.WITHER)
                || potion.getPotion().equals(Effects.HUNGER)
                || potion.getPotion().equals(Effects.NAUSEA)
                || potion.getPotion().equals(Effects.WEAKNESS);
    }

    public static String getDuration(EffectInstance potion) {
        if (potion.getIsPotionDurationMax()) return "**:**";
        return StringUtils.ticksToElapsedTime(potion.getDuration());
    }

    private float blinkTimer = 0f;

    private float blinkAlphaFor(EffectInstance potion) {
        float secondsLeft = potion.getDuration() / 20F;
        if (secondsLeft > 10F) return 1F;

        float dt = computeDtSeconds();

        float k = 1F - Math.min(secondsLeft, 10F) / 10F;
        float baseHz = 1.2F, maxHz = 3.0F;
        float hz = baseHz + (maxHz - baseHz) * k;

        blinkTimer += hz * dt;
        if (blinkTimer > 1f) blinkTimer -= 1f;

        float wave = 0.5F + 0.5F * (float) Math.sin(blinkTimer * 2F * (float) Math.PI);
        float minA = 0.35F, maxA = 1.0F;
        return minA + (maxA - minA) * wave;
    }

    private float smoothTowards(float current, float target, float dt, float speedPerSec) {
        if (!Float.isFinite(dt) || dt <= 0f) return target;
        float k = 1f - (float) Math.exp(-speedPerSec * dt);
        return current + (target - current) * k;
    }

    private float computeDtSeconds() {
        long now = System.nanoTime();
        if (lastTimeNs == 0L) {
            lastTimeNs = now;
            return 1f / 60f;
        }
        long d = now - lastTimeNs;
        lastTimeNs = now;
        double dt = Math.min(Math.max(d / 1_000_000_000.0, 0.0), 0.1);
        return (float) dt;
    }
}
