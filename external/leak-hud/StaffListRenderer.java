package im.client.client.screen.hud.impl;

import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.managers.other.config.StaffStorage;
import im.client.client.screen.hud.AbstractHud;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.RectUtil;
import im.client.client.utils.render.font.Fonts;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import net.mojang.blaze3d.matrix.MatrixStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class StaffListRenderer extends AbstractHud implements IRenderer {
    private final List<Staff> staffPlayers = new ArrayList<>();
    private final DragSetting drag;

    // Паттерны для определения стаффа
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|help|wne|хелп|адм|поддержка|кура|own|taf|curat|dev|supp|ста|сотруд).*");

    // Константы для рендера
    private static final float TITLE_SIZE = 7.7F;
    private static final float ROW_SIZE = 6.7F;
    private static final float MARGIN = 5f;

    // FPS-independent сглаживание размеров панели
    private static final float SIZE_SMOOTH_SPEED = 12f;
    private float smoothWidth = -1f;
    private float smoothHeight = -1f;
    private float smoothMaxStatusWidth = -1f;
    private long lastTimeNs = 0L;

    public StaffListRenderer(DragSetting drag) {
        this.drag = drag;
    }

    @Override
    public void render(Render2DEvent event) {
        float dt = computeDtSeconds();

        // Обновляем список стаффа
        updateStaffList();

        MatrixStack matrix = event.getMatrix();

        final String title = "StaffList";
        final boolean isEmpty = staffPlayers.isEmpty();

        final float headerH = MARGIN + ROW_SIZE + MARGIN;

        // Вычисляем целевую ширину
        float targetWidth = computeTargetWidth(title);

        // Инициализация smooth значений
        if (smoothWidth < 0f) smoothWidth = targetWidth;
        if (smoothHeight < 0f) smoothHeight = headerH;

        // Плавное изменение ширины
        smoothWidth = smoothTowards(smoothWidth, targetWidth, dt, SIZE_SMOOTH_SPEED);
        drag.size.x = (float) Mathf.step(smoothWidth, 0.5);

        float x = drag.position.x;
        float y = drag.position.y;

        boolean closeCondition = isEmpty && !(mc.currentScreen instanceof ChatScreen);
        update(closeCondition ? 0 : 1);
        drag.active = !closeCondition;
        if (closeCondition && animValue() == 0.0F) return;

        // Snap к пикселям
        x = (float) Mathf.step(x, 0.5);
        y = (float) Mathf.step(y, 0.5);
        float width = (float) Mathf.step(drag.size.x, 0.5);

        matrix.push();

        // Рисуем основную панель
        InterFace.getInstance().drawClientRect(matrix, x, y, width, 16, animation.get(), 4);

        // Заголовок
        Fonts.SFP_MEDIUM.draw(
                matrix,
                title,
                x + MARGIN + 8 + 8,
                y + (headerH / 2F) - (TITLE_SIZE / 2F) - 0.3F,
                textWhite(),
                TITLE_SIZE
        );

        // Разделитель после иконки
        RectUtil.drawRect(matrix, x + MARGIN + 13, y + 4F, 0.5F, 8, ColorUtil.getColor(255, 0.1F * animation.get()));

        // Иконка стаффа
        Fonts.ICON_NURIK.draw(
                matrix,
                "O",
                x + MARGIN - 0.2F,
                y + MARGIN - 0.65F,
                textWhite(),
                9
        );

        // Находим максимальную ширину среди всех статусов
        float targetMaxStatusWidth = 0f;
        for (Staff staff : staffPlayers) {
            String statusText = staff.getStatus().string;
            float statusWidth = Fonts.SFP_SEMIBOLD.getWidth(statusText, ROW_SIZE);
            targetMaxStatusWidth = Math.max(targetMaxStatusWidth, statusWidth);
        }

        // Плавно интерполируем максимальную ширину статусов
        if (smoothMaxStatusWidth < 0f) smoothMaxStatusWidth = targetMaxStatusWidth;
        smoothMaxStatusWidth = smoothTowards(smoothMaxStatusWidth, targetMaxStatusWidth, dt, SIZE_SMOOTH_SPEED);

        float offsetY = 0f;
        float ys = -2.25F;

        // Рисуем список стаффа
        for (Staff staff : staffPlayers) {
            float baseY = y + ys + headerH + MARGIN + offsetY;

            int colorName = ColorUtil.multAlpha(textWhite(), animation.get());
            int colorStatus = staff.getStatus().color != -1
                    ? ColorUtil.multAlpha(staff.getStatus().color, animation.get())
                    : colorName;

            String staffName = staff.getName();
            String statusText = staff.getStatus().string;

            matrix.push();

            // Рисуем фон строки
            InterFace.getInstance().drawClientRect(matrix, x, baseY + ys, width, 12, animation.get(), 4);

            // Позиция разделителя
            float separatorX = x + width - smoothMaxStatusWidth - 8;

            // Рисуем разделитель
            RectUtil.drawRect(matrix, separatorX, baseY + ys + 3, 0.5F, 6, ColorUtil.getColor(255, 0.1F * animation.get()));

            // Имя стаффа слева
            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    staffName,
                    x + 4,
                    baseY + ys - MARGIN + 7.8F,
                    colorName,
                    ROW_SIZE
            );

            // Статус справа (выравнивание по центру относительно максимальной ширины)
            float currentStatusWidth = Fonts.SFP_SEMIBOLD.getWidth(statusText, ROW_SIZE);
            float statusX = x + width - 4 - smoothMaxStatusWidth + (smoothMaxStatusWidth - currentStatusWidth) / 2f;

            Fonts.SFP_SEMIBOLD.draw(
                    matrix,
                    statusText,
                    statusX,
                    baseY + ys - MARGIN + 7.8F,
                    colorStatus,
                    ROW_SIZE
            );

            matrix.pop();

            offsetY += 12.5F;
        }

        // Вычисляем и обновляем высоту
        float targetHeight = headerH + offsetY + (MARGIN * 2F) - 5.5F;
        smoothHeight = smoothTowards(smoothHeight, targetHeight, dt, SIZE_SMOOTH_SPEED);
        drag.size.y = (float) Mathf.step(smoothHeight, 0.5);

        matrix.pop();
    }

    /**
     * Обновляет список стаффа на сервере
     */
    private void updateStaffList() {
        if (mc.world == null || mc.getConnection() == null) {
            staffPlayers.clear();
            return;
        }

        staffPlayers.clear();

        for (ScorePlayerTeam team : mc.world.getScoreboard().getTeams().stream()
                .sorted(Comparator.comparing(Team::getName)).toList()) {

            String name = team.getMembershipCollection().toString().replaceAll("[\\[\\]]", "");

            // Проверка формата имени
            if (!namePattern.matcher(name).matches() || name.equals(mc.player.getName().getString())) {
                continue;
            }

            boolean vanish = true;
            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    vanish = false;
                    break;
                }
            }

            // Проверяем, является ли игрок стаффом
            boolean isStaff = prefixMatches.matcher(team.getPrefix().getString().toLowerCase(Locale.ROOT)).matches() || StaffStorage.isStaff(name);

            if (!vanish && isStaff) {
                // Игрок онлайн и является стаффом
                Staff staff = new Staff(team.getPrefix(), name, false, Status.NONE);
                staff.updateStatus();
                staffPlayers.add(staff);
            } else if (vanish && !team.getPrefix().getString().isEmpty() && isStaff) {
                // Игрок в vanish/spectator
                Staff staff = new Staff(team.getPrefix(), name, true, Status.VANISHED);
                staffPlayers.add(staff);
            }
        }
    }

    /**
     * Считает требуемую ширину панели под заголовок и все строки.
     */
    private float computeTargetWidth(String title) {
        float maxW = 0f;

        // Ширина заголовка
        float titleW = Fonts.SFP_MEDIUM.getWidth(title, TITLE_SIZE);
        float headerWidth = MARGIN + 13 + titleW + MARGIN * 2;
        maxW = Math.max(maxW, headerWidth);

        // Строки: [margin] [name] [gap] [status] [margin]
        for (Staff staff : staffPlayers) {
            String nameText = staff.getName();
            String statusText = staff.getStatus().string;

            float nameW = Fonts.SFP_SEMIBOLD.getWidth(nameText, ROW_SIZE);
            float statusW = Fonts.SFP_SEMIBOLD.getWidth(statusText, ROW_SIZE);

            float rowWidth = MARGIN + nameW + 16 + statusW + MARGIN;
            maxW = Math.max(maxW, rowWidth);
        }

        // Минимальная ширина
        maxW = Math.max(maxW, 80);

        // Snap к 0.5px для четкости
        return (float) Mathf.step(maxW, 0.5);
    }

    // === Вспомогательные методы ===

    /**
     * Экспоненциальное сглаживание к цели с учётом dt (FPS-independent)
     */
    private float smoothTowards(float current, float target, float dt, float speedPerSec) {
        if (!Float.isFinite(dt) || dt <= 0f) return target;
        float k = 1f - (float) Math.exp(-speedPerSec * dt);
        return current + (target - current) * k;
    }

    /**
     * Вычисляет delta time в секундах
     */
    private float computeDtSeconds() {
        long now = System.nanoTime();
        if (lastTimeNs == 0L) {
            lastTimeNs = now;
            return 1f / 60f; // первый кадр — ~16.7мс
        }
        long d = now - lastTimeNs;
        lastTimeNs = now;
        // clamp dt, чтобы при фризах не прыгало
        double dt = Math.min(Math.max(d / 1_000_000_000.0, 0.0), 0.1); // [0 .. 100мс]
        return (float) dt;
    }

    /**
     * Класс для хранения информации о стаффе
     */
    @AllArgsConstructor
    @Data
    public static class Staff {
        ITextComponent prefix;
        String name;
        boolean isSpec;
        Status status;

        public void updateStatus() {
            if (mc.getConnection() == null) {
                status = Status.VANISHED;
                return;
            }

            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    if (info.getGameType() == GameType.SPECTATOR) {
                        status = Status.VANISHED;
                        return;
                    }
                    status = Status.NONE;
                    return;
                }
            }
            status = Status.VANISHED;
        }
    }

    /**
     * Статусы стаффа
     */
    public enum Status {
        NONE("Online", -1),
        VANISHED("Spec", ColorUtil.getColor(254, 68, 68));

        public final String string;
        public final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }
}
