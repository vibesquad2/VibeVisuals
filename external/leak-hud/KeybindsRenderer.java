package im.client.client.screen.hud.impl;

import com.google.common.collect.Lists;
import im.client.Client;
import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.Module;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.screen.hud.AbstractHud;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.keyboard.Keyboard;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.RectUtil;
import im.client.client.utils.render.font.Fonts;
import im.client.client.utils.ПенисУтилита;
import net.minecraft.client.gui.screen.ChatScreen;
import net.mojang.blaze3d.matrix.MatrixStack;

import java.util.List;

public class KeybindsRenderer extends AbstractHud implements IRenderer {
    private final List<Module> bindings = Lists.newArrayList();
    private final DragSetting drag;

    // размеры шрифтов/отступы
    private static final float TITLE_SIZE = 7.0F;
    private static final float ROW_SIZE = 7F;
    private static final float ICON_TITLE_SIZE = 6.0F;
    private static final float ICON_ROW_SIZE = 6.7F;
    private static final float MARGIN = 5f;
    private static final float NAME_LEFT_GAP = 10f;
    private static final float BETWEEN_TEXT_GAP = 8f;

    // === FPS-independent сглаживание ===
    private static final float SIZE_SMOOTH_SPEED = 12f;
    private static final float ROW_SLIDE_SPEED = 8f; // помедленнее для плавности
    private float smoothWidth = -1f;
    private float smoothHeight = -1f;
    private float smoothMaxKeyWidth = -1f;

    // === АНИМАЦИЯ ROWS ===
    private final float[] rowSlideOffset = new float[64]; // запас по строкам
    private long lastTimeNs = 0L;

    public KeybindsRenderer(DragSetting drag) {
        this.drag = drag;
        for (int i = 0; i < rowSlideOffset.length; i++) {
            rowSlideOffset[i] = 0.0f; // старт вне экрана слева
        }
    }

    @Override
    public void render(Render2DEvent event) {
        float dt = computeDtSeconds();

        Client.inst().moduleManager().values().forEach(m -> m.getAnimation().update());
        sortModules();

        if (InterFace.getInstance().sicretNastriokaEbana.getValue()) {
            renderModernStyle(event, dt);
        } else {
            renderClassicStyle(event, dt);
        }
    }

    private void renderModernStyle(Render2DEvent event, float dt) {
        MatrixStack matrix = event.getMatrix();
        final String title = "Key Binds";
        final boolean isEmpty = bindings.isEmpty();
        final float headerH = MARGIN + ROW_SIZE + MARGIN;

        float targetWidth = computeTargetWidth(title);
        if (smoothWidth < 0f) smoothWidth = targetWidth;
        if (smoothHeight < 0f) smoothHeight = headerH;
        smoothWidth = smoothTowards(smoothWidth, targetWidth + 4, dt, SIZE_SMOOTH_SPEED);
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

        // max width keys (для позиционирования)
        float targetMaxKeyWidth = computeMaxKeyWidth(7.0F);
        if (smoothMaxKeyWidth < 0f) smoothMaxKeyWidth = targetMaxKeyWidth;
        smoothMaxKeyWidth = smoothTowards(smoothMaxKeyWidth, targetMaxKeyWidth, dt, SIZE_SMOOTH_SPEED);

        updateRowAnimations(dt);

        matrix.push();
        ПенисУтилита.КрутойПенаРект(matrix, x, y, width, drag.size.y, animation.get());

        Fonts.SFP_SEMIBOLD.draw(
                matrix,
                title,
                x + MARGIN + 1,
                y + (headerH / 2F) - (7.0F / 2F) - 0.35F,
                textWhite(),
                7.0F
        );
        Fonts.ICON_DESHUX.drawRight(
                matrix,
                "g",
                x + width - MARGIN - 0.5F,
                y + (headerH / 2F) - (7.0F / 2F) + 0.1F,
                textWhite(),
                7.0F
        );

        float offsetY = 0f;
        float ys = -2.5F;
        int rowIndex = 0;

        for (Module binding : bindings) {
            if (rowIndex >= rowSlideOffset.length) break;

            float animPC = binding.getAnimation().get();
            float rawSlide = rowSlideOffset[rowIndex]; // 0..1
            float slidePC = easeOutCubic(rawSlide);    // сглаженный выезд слева [web:7]

            float baseY = y + ys + headerH + MARGIN + offsetY;
            int colorName = ColorUtil.multAlpha(textWhite(), animPC);

            String keyName = Keyboard.keyName(binding.getKey());

            // x: слева за экраном -> в x
            float rowX = x - width + width * slidePC;

            matrix.push();

            ПенисУтилита.КрутойПенаРект(
                    matrix,
                    rowX,
                    baseY + ys,
                    width,
                    12,
                    animation.get() * animPC * slidePC
            );

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    binding.getName(),
                    rowX + 5,
                    baseY + ys - MARGIN + 8,
                    colorName,
                    7.0F
            );

            float keyX = rowX + width - 5;
            Fonts.SFP_SEMIBOLD.drawRight(
                    matrix,
                    keyName,
                    keyX,
                    baseY + ys - MARGIN + 8,
                    colorName,
                    7.0F
            );

            matrix.pop();

            offsetY += (8.5F) * animPC;
            rowIndex++;
        }

        float targetHeight = headerH + offsetY + (MARGIN * 2F) - 3;
        drag.size.y = targetHeight;
        matrix.pop();
    }

    private void renderClassicStyle(Render2DEvent event, float dt) {
        MatrixStack matrix = event.getMatrix();
        final String title = "Hotkeys";
        final boolean isEmpty = bindings.isEmpty();
        final float headerH = MARGIN + ROW_SIZE + MARGIN;

        float targetWidth = computeTargetWidth(title);
        if (smoothWidth < 0f) smoothWidth = targetWidth;
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

        float targetMaxKeyWidth = computeMaxKeyWidth(6.7F);
        if (smoothMaxKeyWidth < 0f) smoothMaxKeyWidth = targetMaxKeyWidth;
        smoothMaxKeyWidth = smoothTowards(smoothMaxKeyWidth, targetMaxKeyWidth, dt, SIZE_SMOOTH_SPEED);

        updateRowAnimations(dt);

        matrix.push();
        InterFace.getInstance().drawClientRect(matrix, x, y, width, 16, animation.get(), 4);

        Fonts.SFP_MEDIUM.draw(
                matrix,
                title,
                x + MARGIN + 8 + 8,
                y + (headerH / 2F) - (7.0F / 2F) - 0.6F,
                textWhite(),
                7.7F
        );
        RectUtil.drawRect(
                matrix,
                x + MARGIN + 13,
                y + 4,
                0.5F,
                8,
                ColorUtil.getColor(255, 0.1F * animation.get())
        );
        Fonts.ICON_NURIK.draw(
                matrix,
                "C",
                x + MARGIN,
                y + MARGIN - 0.6F,
                textWhite(),
                9
        );

        float offsetY = 0f;
        float ys = -2.5F;
        int rowIndex = 0;

        for (Module binding : bindings) {
            if (rowIndex >= rowSlideOffset.length) break;

            float animPC = binding.getAnimation().get();
            float rawSlide = rowSlideOffset[rowIndex]; // 0..1
            float slidePC = easeOutCubic(rawSlide);    // плавный выезд [web:7]

            float baseY = y + ys + headerH + MARGIN + offsetY;
            int colorName = ColorUtil.multAlpha(textWhite(), animPC);
            String keyName = Keyboard.keyName(binding.getKey());

            float rowX = x - width + width * slidePC;

            matrix.push();

            InterFace.getInstance().drawClientRect(
                    matrix,
                    rowX,
                    baseY + ys,
                    width,
                    12,
                    animation.get() * animPC * slidePC,
                    4
            );

            float separatorX = rowX + width - smoothMaxKeyWidth - 10;
            RectUtil.drawRect(
                    matrix,
                    separatorX,
                    baseY + ys + 3,
                    0.5F,
                    6,
                    ColorUtil.getColor(255, 0.1F * animation.get() * animPC)
            );

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    binding.getName(),
                    rowX + 4,
                    baseY + ys - MARGIN + 7.8F,
                    colorName,
                    6.7F
            );

            float currentKeyWidth = Fonts.SFP_SEMIBOLD.getWidth(keyName, 6.7F);
            float keyX = rowX + width - 4 - smoothMaxKeyWidth +
                    (smoothMaxKeyWidth - currentKeyWidth) / 2f;

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    keyName,
                    keyX,
                    baseY + ys - MARGIN + 7.8F,
                    colorName,
                    6.7F
            );

            matrix.pop();

            offsetY += (12.5F) * animPC;
            rowIndex++;
        }

        float targetHeight = headerH + offsetY + (MARGIN * 2F) - 5.5F;
        smoothHeight = smoothTowards(smoothHeight, targetHeight, dt, SIZE_SMOOTH_SPEED);
        drag.size.y = (float) Mathf.step(smoothHeight, 0.5);
        matrix.pop();
    }

    /**
     * easing для плавного выезда слева [web:7]
     */
    private float easeOutCubic(float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    private void updateRowAnimations(float dt) {
        int rowIndex = 0;
        for (Module binding : bindings) {
            if (rowIndex >= rowSlideOffset.length) break;

            float target = binding.getAnimation().get(); // 0..1
            rowSlideOffset[rowIndex] = smoothTowards(
                    rowSlideOffset[rowIndex],
                    target,
                    dt,
                    ROW_SLIDE_SPEED
            );

            if (rowSlideOffset[rowIndex] < 0f) rowSlideOffset[rowIndex] = 0f;
            if (rowSlideOffset[rowIndex] > 1f) rowSlideOffset[rowIndex] = 1f;

            rowIndex++;
        }
    }

    private float computeMaxKeyWidth(float fontSize) {
        float maxKeyWidth = 0f;
        for (Module binding : bindings) {
            String keyName = Keyboard.keyName(binding.getKey());
            float keyWidth = Fonts.SFP_SEMIBOLD.getWidth(keyName, fontSize);
            maxKeyWidth = Math.max(maxKeyWidth, keyWidth);
        }
        return maxKeyWidth;
    }

    private float computeTargetWidth(String title) {
        float maxW = 0f;

        float titleW = InterFace.getInstance().sicretNastriokaEbana.getValue()
                ? Fonts.SFP_SEMIBOLD.getWidth(title, TITLE_SIZE)
                : Fonts.SFP_MEDIUM.getWidth(title, TITLE_SIZE);
        float titleIconW = InterFace.getInstance().sicretNastriokaEbana.getValue()
                ? Fonts.ICON_DESHUX.getWidth("g", ICON_TITLE_SIZE)
                : Fonts.ICON_NURIK.getWidth("C", ICON_TITLE_SIZE);
        float headerWidth = MARGIN + 1f + titleW + MARGIN + titleIconW + MARGIN;
        maxW = Math.max(maxW, headerWidth);

        float maxKeyWidth = computeMaxKeyWidth(ROW_SIZE);
        float rowWidth = MARGIN + NAME_LEFT_GAP + 50f + BETWEEN_TEXT_GAP + maxKeyWidth + MARGIN;
        maxW = Math.max(maxW, rowWidth);
        maxW = Math.max(maxW, 75f);

        return (float) Mathf.step(maxW, 0.5);
    }

    private void sortModules() {
        bindings.clear();
        bindings.addAll(Client.inst().moduleManager().values().stream()
                .filter(this::shouldDisplay)
                .toList());
    }

    private boolean shouldDisplay(Module module) {
        return module.isAllowDisable()
                && (module.isEnabled() || module.getAnimation().getValue() != 0.0)
                && module.getKey() != Keyboard.KEY_NONE.getKey();
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
