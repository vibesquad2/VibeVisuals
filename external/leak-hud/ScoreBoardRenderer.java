package im.client.client.screen.hud.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import im.client.client.api.events.orbit.EventHandler;
import im.client.client.api.events.orbit.EventPriority;
import im.client.client.managers.events.render.RenderScoreBoardEvent;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.screen.hud.IScoreBoardRenerer;
import im.client.client.utils.render.draw.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.mojang.blaze3d.matrix.MatrixStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreBoardRenderer implements IScoreBoardRenerer {
    private final DragSetting drag;

    public ScoreBoardRenderer(DragSetting drag) {
        this.drag = drag;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void renderScoreBoard(RenderScoreBoardEvent event) {
        event.cancel();
        mc.gameRenderer.setupOverlayRendering(2);

        MatrixStack matrix = event.getMatrix();
        FontRenderer fontRenderer = event.getFontRenderer();
        InterFace interFace = InterFace.getInstance();

        ScoreObjective objective = event.getObjective();
        Scoreboard scoreboard = objective.getScoreboard();

        Collection<Score> collection = scoreboard.getSortedScores(objective);
        List<Score> list = collection.stream()
                .filter(s -> !s.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());
        collection = list.size() > 15 ? Lists.newArrayList(Iterables.skip(list, list.size() - 15)) : list;

        List<Pair<Score, ITextComponent>> lines = Lists.newArrayListWithCapacity(collection.size());
        ITextComponent title = objective.getDisplayName();

        int titleWidth = fontRenderer.getStringPropertyWidth(title);
        int width = titleWidth;
        int colonWidth = fontRenderer.getStringWidth(": ");

        for (Score score : collection) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            ITextComponent lineText = ScorePlayerTeam.func_237500_a_(team, new StringTextComponent(score.getPlayerName()));
            lines.add(Pair.of(score, lineText));
            width = Math.max(width, fontRenderer.getStringPropertyWidth(lineText) + colonWidth + fontRenderer.getStringWidth(Integer.toString(score.getScorePoints())));
        }

        // фикс по X: отключаем drag по X
        final int margin = 6;
        final int screenW = mc.getMainWindow().getScaledWidth();
        final float fixedX = screenW - width - margin; // или margin для левого угла

        // обновляем hitbox перед принудительной фиксацией
        int height = collection.size() * 9 + 13;
        drag.size.set(width, height);

        // принудительно фиксируем X каждый кадр (любой dx из DragSetting будет игнорирован)
        drag.position.x = fixedX;

        // по Y можно тянуть
        float x = drag.position.x;
        float y = drag.position.y;

        // фон/контейнер
        RenderUtil.clientStyledRectDark(matrix, x, y, width, height, 1, 4);

        int idx = 0;
        for (Pair<Score, ITextComponent> pair : lines) {
            ++idx;
            ITextComponent line = pair.getSecond();
            float lineY = y - idx * 9 + height;
            fontRenderer.drawString(matrix, line, x + 4, lineY, -1);

            if (idx == collection.size()) {
                fontRenderer.drawString(matrix, title, x + width / 2F - titleWidth / 2F, y + 4, -1);
            }
        }

        mc.gameRenderer.setupOverlayRendering();
    }
}
