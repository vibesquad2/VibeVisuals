package im.client.client.screen.hud.impl;

import im.client.Client;
import im.client.client.api.client.Constants;
import im.client.client.api.events.orbit.EventHandler;
import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.animation.NumberTransition;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.player.MoveUtil;
import im.client.client.utils.player.PlayerUtil;
import im.client.client.utils.render.color.ColorFormatting;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.RectUtil;
import im.client.client.utils.render.draw.RenderUtil;
import im.client.client.utils.render.font.Fonts;
import im.client.client.utils.render.gif.GifRender;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Namespaced;
import net.mojang.blaze3d.matrix.MatrixStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
public class WatermarkRenderer implements IRenderer {

    float title_width = 50;
    float username_width = 100;
    float server_width = 100;
    float fps_width = 50;
    float ping_width = 50;

    float tempFps = 0;
    float tempPing = 0;

    private final GifRender gifRender = new GifRender(new Namespaced("texture/texture_wt.gif"));

    @EventHandler
    public void render(Render2DEvent e) {
        MatrixStack ms = e.getMatrix();

        float pos = 8;

        int speed = 5;
        int intex = 24;
        pos = 5;
        float pos2 = 5;
        MatrixStack matrix = e.getMatrix();
        String nameWT = Constants.RELEASE;
        StringBuilder fadeUsername = new StringBuilder();

        matrix.push();

        if (InterFace.getInstance().wtnameSF.is("Alpha")) nameWT = "Alpha";

        for (int i = 0; i < nameWT.length(); i++) {
            fadeUsername.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername.append(nameWT.charAt(i));
        }

        String nameWT1 = "P";
        StringBuilder fadeUsername1 = new StringBuilder();

        for (int i = 0; i < nameWT1.length(); i++) {
            fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername1.append(nameWT1.charAt(i));
        }

        InterFace.getInstance().drawClientRect(ms, pos2, pos, 26.5F + Fonts.SFP_MEDIUM.getWidth(nameWT, 7), 14.5F, 1, 3.5F);

        Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.4F, pos + 4, -1, 8);


        RectUtil.drawRect(matrix, pos2 + 17F, pos + 3.5F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));
        //RenderUtil.Rounded.drawRectVerticalW(pos2 + 17F, pos + 3.4F, 1, 7, ColorUtil.multAlpha(-1, 0.1F));

        Fonts.SFP_MEDIUM.draw(matrix, fadeUsername.toString(), pos2 + 22F, pos + 3.5F, -1, 7);


        String nameWT12 = "W";
        StringBuilder fadeUsername12 = new StringBuilder();

        for (int i = 0; i < nameWT12.length(); i++) {
            fadeUsername12.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername12.append(nameWT12.charAt(i));
        }

        String nameWT123 = "X";
        StringBuilder fadeUsername123 = new StringBuilder();

        for (int i = 0; i < nameWT123.length(); i++) {
            fadeUsername123.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername123.append(nameWT123.charAt(i));
        }

        String nameWT1234 = "V";
        StringBuilder fadeUsername1234 = new StringBuilder();

        for (int i = 0; i < nameWT1234.length(); i++) {
            fadeUsername1234.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername1234.append(nameWT1234.charAt(i));
        }


        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = currentTime.format(formatter);


        pos2 = pos2 + 26.5F + 2 + Fonts.SFP_MEDIUM.getWidth(nameWT, 7);
        tempFps = NumberTransition.result(tempFps, Minecraft.getDebugFPS());
        String fps = ((int) tempFps) + " Fps";

        float addx = 0;
        float addx2 = 0;
        float addx23 = 0;


        if (InterFace.getInstance().wtElement.getValue("Время")) {
            addx += 11 + Fonts.SFP_MEDIUM.getWidth(formattedTime, 7) + 7;
        }
        if (InterFace.getInstance().wtElement.getValue("Фпс")) {
            addx2 += 18.5F + Fonts.SFP_MEDIUM.getWidth(fps, 7);
            addx += Fonts.SFP_MEDIUM.getWidth(fps, 7) + 4 + 10 + 5;
        }
        if (InterFace.getInstance().wtElement.getValue("Юзер найм")) {
            addx += Fonts.SFP_MEDIUM.getWidth(InterFace.getInstance().namewatermerk.getValue(), 7) + 20;
            addx23 += Fonts.SFP_MEDIUM.getWidth(InterFace.getInstance().namewatermerk.getValue(), 7) + 20;
        }

        InterFace.getInstance().drawClientRect(ms, pos2, pos, addx, 14.5F, 1, 3.5F);

        if (InterFace.getInstance().wtElement.getValue("Фпс")) {

            Fonts.ICON_NURIK.draw(matrix, fadeUsername123.toString(), pos2 + 4 + addx23, pos + 4.2F, -1, 7);

            Fonts.SFP_MEDIUM.draw(matrix, fps, pos2 + 15 + addx23, pos + 3.7F, -1, 7);

            if (InterFace.getInstance().wtElement.getValue("Юзер найм")) {

                RectUtil.drawRect(matrix, pos2 + addx23 - 0.5F, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));
            }
        }

        if (InterFace.getInstance().wtElement.getValue("Юзер найм")) {

            Fonts.ICON_NURIK.draw(matrix, fadeUsername12.toString(), pos2 + 4.5F, pos + 4.2F, -1, 7);

            Fonts.SFP_MEDIUM.draw(matrix, InterFace.getInstance().namewatermerk.getValue(), pos2 + 15, pos + 3.7F, -1, 7);
        }

        //38
        if (InterFace.getInstance().wtElement.getValue("Время")) {
            if (InterFace.getInstance().wtElement.getValue("Юзер найм") || InterFace.getInstance().wtElement.getValue("Фпс")) {

                RectUtil.drawRect(matrix, pos2 + addx2 + addx23 - 0.5F, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));
            }
            Fonts.ICON_NURIK.draw(matrix, fadeUsername1234.toString(), pos2 + 4 + addx2 + addx23 - 0.5F, pos + 4.2F, -1, 7);


            Fonts.SFP_MEDIUM.draw(matrix, formattedTime, pos2 + 14 + addx2 + addx23 - 0.5F, pos + 3.7F, -1, 7);
        }


        /// нижния полоса
        pos = 5 + 14.5F + 2;

        pos2 = 5;
        nameWT1 = "U";
        fadeUsername1 = new StringBuilder();

        for (int i = 0; i < nameWT1.length(); i++) {
            fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername1.append(nameWT1.charAt(i));
        }

        if (InterFace.getInstance().wtnameSF.is("Alpha")) {

            InterFace.getInstance().drawClientRect(ms, pos2, pos, 17, 14.5F, 1, 3.5F);

            Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.2F, pos + 4, -1, 7.5F);
            pos2 += 17 + 2;

        }

        nameWT1 = "F";
        fadeUsername1 = new StringBuilder();

        for (int i = 0; i < nameWT1.length(); i++) {
            fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
            fadeUsername1.append(nameWT1.charAt(i));
        }
        String bps = (int) mc.player.getPosX() + ", " + (int) mc.player.getPosY() + ", " + (int) mc.player.getPosZ();

        if (InterFace.getInstance().wtElement.getValue("Кординаты")) {

            InterFace.getInstance().drawClientRect(ms, pos2, pos, 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7), 14.5F, 1, 3.5F);

            Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.2F, pos + 4.05F, -1, 8);

            RectUtil.drawRect(matrix, pos2 + 17, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));

            Fonts.SFP_MEDIUM.draw(matrix, bps, pos2 + 22, pos + 3.7F, -1, 7);


            pos2 += 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7) + 2;
        }
        if (InterFace.getInstance().wtElement.getValue("Пинг")) {

            nameWT1 = "Q";
            fadeUsername1 = new StringBuilder();

            bps = PlayerUtil.getPing(mc.player) + " Ping";

            for (int i = 0; i < nameWT1.length(); i++) {
                fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
                fadeUsername1.append(nameWT1.charAt(i));
            }

            InterFace.getInstance().drawClientRect(ms, pos2, pos, 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7), 14.5F, 1, 3.5F);

            Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.2F, pos + 4.05F, -1, 8);

            RectUtil.drawRect(matrix, pos2 + 17, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));

            Fonts.SFP_MEDIUM.draw(matrix, bps, pos2 + 22, pos + 3.7F, -1, 7);


            pos2 += 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7) + 2;
        }
        if (InterFace.getInstance().wtElement.getValue("Тпс")) {
            nameWT1 = "T";
            fadeUsername1 = new StringBuilder();

            bps = Mathf.round(Client.inst().serverTps().getTPS(), 1) + " Ticks";

            for (int i = 0; i < nameWT1.length(); i++) {
                fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
                fadeUsername1.append(nameWT1.charAt(i));
            }

            InterFace.getInstance().drawClientRect(ms, pos2, pos, 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7), 14.5F, 1, 3.5F);

            //Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.2F, pos + 4, -1, 8);
            RenderUtil.drawImage(new Namespaced("interface/tps.png"), matrix, pos2 + 4.2F, pos + 3.2F, 8, 8, ColorUtil.fade(4, 0, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F)));

            RectUtil.drawRect(matrix, pos2 + 17, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));

            Fonts.SFP_MEDIUM.draw(matrix, bps, pos2 + 22, pos + 3.7F, -1, 7);

            pos2 += 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7) + 2;
        }
        if (InterFace.getInstance().wtElement.getValue("Бпс")) {
            nameWT1 = "T";
            fadeUsername1 = new StringBuilder();

            bps = Mathf.round(MoveUtil.speedSqrt() * (double) 20.0F, 1) + " Bps";

            for (int i = 0; i < nameWT1.length(); i++) {
                fadeUsername1.append(ColorFormatting.getColor(ColorUtil.fade(speed, intex * i, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F))));
                fadeUsername1.append(nameWT1.charAt(i));
            }

            InterFace.getInstance().drawClientRect(ms, pos2, pos, 27 + Fonts.SFP_MEDIUM.getWidth(bps, 7), 14.5F, 1, 3.5F);

            //Fonts.ICON_NURIK.draw(matrix, fadeUsername1.toString(), pos2 + 4.2F, pos + 4, -1, 8);
            RenderUtil.drawImage(new Namespaced("interface/bps.png"), matrix, pos2 + 4.2F, pos + 3.2F, 8, 8, ColorUtil.fade(4, 0, InterFace.getInstance().themeColor(), ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.45F)));

            RectUtil.drawRect(matrix, pos2 + 17, pos + 3.4F, 0.5F, 7, ColorUtil.multAlpha(-1, 0.1F));

            Fonts.SFP_MEDIUM.draw(matrix, bps, pos2 + 22, pos + 3.7F, -1, 7);
        }
        matrix.pop();
    }

    private void renderPart(String icon, String text, float pos, float x, float y, float width, float height, MatrixStack ms) {
        int alpha = 255;
        float posx = 45;

        InterFace.getInstance().drawClientRect(ms, posx + x, pos + y, width, height, 1, 5);

        Fonts.ICON_ESSENS.draw(ms, icon, posx + x + 4, pos + y + 2 + 1.7F, InterFace.getInstance().iconColor(), 9);
        Fonts.SFP_MEDIUM.draw(ms, text, posx + x + 15, pos + y + 2 + 1.5F, ColorUtil.getColor(235), 7);
    }

}
