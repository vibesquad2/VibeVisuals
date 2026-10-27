package im.client.client.screen.hud.impl;

import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.screen.hud.AbstractHud;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.RectUtil;
import im.client.client.utils.render.font.Fonts;
import im.client.client.utils.render.font.StripFont;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Util;
import net.mojang.blaze3d.matrix.MatrixStack;
import net.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CooldownRenderer extends AbstractHud implements IRenderer {

    private final DragSetting drag;

    public CooldownRenderer(DragSetting drag) {
        this.drag = drag;
    }

    // slide-анимация как в Keybinds
    private static final float ROW_SLIDE_SPEED = 8f;
    private final Map<Item, Float> itemSlideMap = new HashMap<>(); // Item -> slide [0..1]

    // появление по времени (легкий фейд/scale)
    private final StripFont stripFont = new StripFont();
    private final Map<Item, Long> appearStart = new HashMap<>();
    private static final long APPEAR_DUR_MS = 220L;

    // размеры/отступы
    private static final float TITLE_SIZE = 7.5F;
    private static final float ROW_SIZE = 7.0F;
    private static final float ICON_TITLE_SIZE = 6.0F;
    private static final float ICON_ROW_SCALE = 0.5F;
    private static final float ICON_ROW_CELL = 10f;
    private static final float MARGIN = 5f;
    private static final float BETWEEN_TEXT_GAP = 8f;

    // сглаживание размеров
    private static final float SIZE_SMOOTH_SPEED = 12f;
    private float smoothWidth = -1f;
    private float smoothHeight = -1f;
    private long lastTimeNs = 0L;

    // дополнительное сглаживание ширины
    private long lastNsForWidth = System.nanoTime();
    private float smoothWidthLegacy = 100f;

    @Override
    public void render(Render2DEvent event) {
        MatrixStack matrix = event.getMatrix();
        CooldownTracker tracker = mc.player.getCooldownTracker();
        Map<Item, CooldownTracker.Cooldown> cooldowns = tracker.cooldowns;

        final String title = "Cooldowns";
        final boolean isEmpty = cooldowns.isEmpty();
        final float headerH = MARGIN + ROW_SIZE + MARGIN;

        boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);
        update(closeCondition ? 0 : 1);
        drag.active = !closeCondition;
        if (closeCondition && animValue() == 0.0F) return;

        float dt = computeDtSeconds();

        float targetWidth = computeTargetWidth(title, cooldowns);
        if (smoothWidth < 0f) smoothWidth = targetWidth;
        if (smoothHeight < 0f) smoothHeight = headerH;

        smoothWidth = smoothTowards(smoothWidth, targetWidth, dt, SIZE_SMOOTH_SPEED);

        float dtLegacy = computeDtLegacy();
        smoothWidthLegacy += (targetWidth - smoothWidthLegacy) *
                (1f - (float) Math.exp(-12f * Math.max(0f, dtLegacy)));
        float widthSmoothed = (smoothWidth + smoothWidthLegacy) * 0.5f;

        float x = drag.position.x;
        float y = drag.position.y;
        x = (float) Mathf.step(x, 0.5);
        y = (float) Mathf.step(y, 0.5);
        float width = (float) Mathf.step(widthSmoothed, 0.5);
        drag.size.x = width;

        long nowMs = Util.milliTime();

        // обновляем appearStart
        appearStart.keySet().retainAll(cooldowns.keySet());
        for (Item it : cooldowns.keySet()) {
            appearStart.putIfAbsent(it, nowMs);
        }

        // slide-анимация как в Keybinds (въезд слева)
        updateRowAnimations(cooldowns.keySet(), dt);

        matrix.push();

        // ===== HEADER =====
        InterFace.getInstance().drawClientRect(matrix, x, y, width, 16, animation.get(), 4);

        Fonts.SFP_MEDIUM.draw(
                matrix, title,
                x + MARGIN + 8 + 8,
                y + (headerH / 2F) - (7.5F / 2F) - 0.6F,
                textWhite(), 7.7F
        );

        RectUtil.drawRect(
                matrix,
                x + MARGIN + 13,
                y + 3.85F,
                0.5F,
                8,
                ColorUtil.getColor(255, 0.1F * animation.get())
        );

        Fonts.ICON_NURIK.draw(
                matrix,
                "T",
                x + MARGIN,
                y + MARGIN - 0.6F,
                textWhite(),
                9
        );

        // ===== ROWS =====
        float offsetY = 0f;
        float ys = -2.5F;

        Iterator<Map.Entry<Item, CooldownTracker.Cooldown>> it = cooldowns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Item, CooldownTracker.Cooldown> entry = it.next();
            Item item = entry.getKey();

            long currentTick = mc.player.getCooldownTracker().getTicks();
            long expireTick = entry.getValue().getExpireTicks();
            long remaining = expireTick - currentTick;

            float secondsLeft = remaining / 20.0f;

            // если кулдаун полностью кончился — просто убираем (как в Keybinds)
            if (remaining <= 0 || secondsLeft <= 0.05f) {
                it.remove();
                appearStart.remove(item);
                itemSlideMap.remove(item);
                continue;
            }

            float slideRaw = itemSlideMap.getOrDefault(item, 0f);
            float slidePC = easeOutCubic(slideRaw);

            // appear (легкий фейд по времени)
            long start = appearStart.getOrDefault(item, nowMs);
            float appear = easeOutCubic((nowMs - start) / (float) APPEAR_DUR_MS);
            if (appear <= 0f) continue;

            float rowBaseY = y + ys + headerH + MARGIN + offsetY;

            // позиция X: слева (-width) -> x (как в Keybinds)
            float rowX = x - width + width * slidePC;

            matrix.push();

            // фон строки
            InterFace.getInstance().drawClientRect(
                    matrix,
                    rowX,
                    rowBaseY + ys,
                    width,
                    12,
                    animation.get() * appear * slidePC,
                    4
            );

            // таймер
            String timeStr = formatTime(Math.max(secondsLeft, 0f));
            RectUtil.drawRect(
                    matrix,
                    rowX + width - MARGIN - 7,
                    rowBaseY + ys + 3,
                    0.5F,
                    6,
                    ColorUtil.getColor(255, 0.1F * animation.get() * appear * slidePC)
            );

            // иконка предмета
            drawItemStack(
                    new ItemStack(item),
                    rowX + width - MARGIN - 5.5F,
                    rowBaseY + ys - MARGIN + 6.7F,
                    false,
                    true,
                    ICON_ROW_SCALE
            );

            // имя
            String itemName = item.getName().getString();
            int leftColor = ColorUtil.multAlpha(textWhite(), appear * slidePC);
            float nameX = rowX + 4.5F;

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    itemName,
                    nameX - 1,
                    rowBaseY + ys - MARGIN + 7.8F,
                    leftColor,
                    6.7F
            );

            // таймер справа
            int rightColor = ColorUtil.multAlpha(textColor(), appear * slidePC);
            float rightX = rowX + width - MARGIN + (3 - (3 * appear * slidePC));

            Fonts.SFP_SEMIBOLD.drawRight(
                    matrix,
                    timeStr,
                    rightX - 9,
                    rowBaseY + ys - MARGIN + 7.8F,
                    rightColor,
                    6.7F
            );

            matrix.pop();

            offsetY += (12.5F) * appear * slidePC;
        }

        float targetHeight = headerH + offsetY + (MARGIN * 2F) - 5.5F;
        smoothHeight = smoothTowards(smoothHeight, targetHeight, dt, SIZE_SMOOTH_SPEED);
        drag.size.y = (float) Mathf.step(smoothHeight, 0.5);

        matrix.pop();
    }

    // slide-анимация как в Keybinds: всегда тянем к 1.0
    private void updateRowAnimations(Iterable<Item> items, float dt) {
        for (Item item : items) {
            float target = 1.0f;
            float slide = itemSlideMap.getOrDefault(item, 0f);
            slide = smoothTowards(slide, target, dt, ROW_SLIDE_SPEED);
            if (slide < 0f) slide = 0f;
            if (slide > 1f) slide = 1f;
            itemSlideMap.put(item, slide);
        }

        // чистим, кого больше нет
        itemSlideMap.entrySet().removeIf(e -> !contains(items, e.getKey()));
        appearStart.entrySet().removeIf(e -> !contains(items, e.getKey()));
    }

    private boolean contains(Iterable<Item> items, Item key) {
        for (Item it : items) if (it == key) return true;
        return false;
    }

    // рендер иконки предмета
    public static void drawItemStack(ItemStack stack, float x, float y, boolean overlay, boolean scale, float scaleValue) {
        RenderSystem.enableDepthTest();
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();

        RenderSystem.translatef(x, y, 0);
        if (scale) GL11.glScaled(scaleValue, scaleValue, scaleValue);

        mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
        if (overlay) mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, stack, 0, 0);

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
        RenderSystem.disableDepthTest();
    }

    // helpers
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

    private float computeDtLegacy() {
        long now = System.nanoTime();
        float dt = (now - lastNsForWidth) / 1_000_000_000f;
        lastNsForWidth = now;
        return dt;
    }

    private float smoothTowards(float current, float target, float dt, float speedPerSec) {
        if (!Float.isFinite(dt) || dt <= 0f) return target;
        float k = 1f - (float) Math.exp(-speedPerSec * dt);
        return current + (target - current) * k;
    }

    // такой же easing, как в Keybinds [web:3]
    private static float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1.0f - (float) Math.pow(1.0f - t, 3.0);
    }

    private String formatTime(float seconds) {
        if (seconds < 0f) seconds = 0f;
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", mins, secs);
    }

    public static String getDuration(EffectInstance potion) {
        if (potion.getIsPotionDurationMax()) return "**:**";
        return StringUtils.ticksToElapsedTime(potion.getDuration());
    }

    private float computeTargetWidth(String title, Map<Item, CooldownTracker.Cooldown> cooldowns) {
        float maxW = 0f;

        float titleW = Fonts.SFP_MEDIUM.getWidth(title, TITLE_SIZE);
        float titleIconW = Fonts.ICON_DIMAS.getWidth("T", ICON_TITLE_SIZE);
        float headerWidth = MARGIN + 1f + titleW + MARGIN + titleIconW + MARGIN;
        maxW = Math.max(maxW, headerWidth);

        float timerSampleW = Fonts.SFP_MEDIUM.getWidth("00:00", ROW_SIZE);

        for (Map.Entry<Item, CooldownTracker.Cooldown> e : cooldowns.entrySet()) {
            String name = e.getKey().getName().getString();
            float nameW = Fonts.SFP_MEDIUM.getWidth(name, ROW_SIZE);
            float rowWidth = MARGIN + ICON_ROW_CELL + nameW + BETWEEN_TEXT_GAP + timerSampleW + MARGIN;
            maxW = Math.max(maxW, rowWidth);
        }

        maxW = Math.max(maxW, 86f);
        return (float) Mathf.step(maxW, 0.5);
    }
}
