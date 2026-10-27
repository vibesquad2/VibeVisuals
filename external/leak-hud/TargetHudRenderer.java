package im.client.client.screen.hud.impl;

import im.client.client.managers.events.player.AttackEvent;
import im.client.client.managers.events.render.Render2DEvent;
import im.client.client.managers.module.impl.combat.AttackAura;
import im.client.client.managers.module.impl.misc.FixHP;
import im.client.client.managers.module.impl.render.InterFace;
import im.client.client.managers.module.settings.impl.DragSetting;
import im.client.client.screen.hud.IAttack;
import im.client.client.screen.hud.IRenderer;
import im.client.client.utils.animation.Animation;
import im.client.client.utils.animation.NumberTransition;
import im.client.client.utils.animation.util.Easings;
import im.client.client.utils.math.Mathf;
import im.client.client.utils.other.ParticleEngine;
import im.client.client.utils.player.PlayerUtil;
import im.client.client.utils.render.color.ColorFormatting;
import im.client.client.utils.render.color.ColorUtil;
import im.client.client.utils.render.draw.GLUtil;
import im.client.client.utils.render.draw.RenderUtil;
import im.client.client.utils.render.draw.Round;
import im.client.client.utils.render.font.Fonts;
import im.client.client.utils.render.font.StripFont;
import im.client.client.utils.render.particle.Particle;
import im.client.client.utils.time.StopWatch;
import im.client.client.utils.ПенисУтилита;
import im.client.common.impl.fastrandom.FastRandom;
import lombok.AccessLevel;
import lombok.Generated;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Score;
import net.minecraft.util.Namespaced;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.mojang.blaze3d.matrix.MatrixStack;
import net.mojang.blaze3d.platform.GlStateManager;
import net.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.StreamSupport;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetHudRenderer implements IRenderer, IAttack {
    private final DragSetting drag;
    private final Animation openAnimation = new Animation();
    private final Animation healthAnimation = new Animation();
    private final Animation absorptionAnimation = new Animation();
    private final StopWatch time = new StopWatch();
    public boolean inWorld;
    public LivingEntity target;
    private final List<Particle> particles = new ArrayList();
    private final ResourceLocation bloom = new Namespaced("particle/glow.png");
    private final Random random = new FastRandom();
    private final float radius = 3.0F;
    private final Round round = Round.of(3.0F);
    private final StripFont stripFont = new StripFont();
    private float barX;
    private float barY;
    private float barW;
    private float barH;
    private final long lastFrameNanos = 0L;
    private final boolean firstFrame = true;
    private float tempHp = 0.0F;
    private final float tempAbsorption = 0.0F;


    private long lastHeadParticleSpawn = 0L;
    private static final long HEAD_PARTICLE_SPAWN_INTERVAL = 200L;

    public TargetHudRenderer(DragSetting drag) {
        this.drag = drag;
    }


    @Override
    public void render(Render2DEvent event) {
        AttackAura killAura = AttackAura.getInstance();
        if (mc.pointedEntity instanceof PlayerEntity player) {
            target = player;
            time.reset();
        }
        if (killAura.isEnabled() && killAura.target != null) {
            target = killAura.target;
            time.reset();
        }
        if (mc.currentScreen instanceof ChatScreen) {
            target = mc.player;
            time.reset();
        }
        if (target == null) {
            inWorld = false;
            return;
        }

        openAnimation.update();
        healthAnimation.update();
        absorptionAnimation.update();

        inWorld = StreamSupport.stream(mc.world.getAllEntities().spliterator(), true).anyMatch(entity -> entity.equals(target));

        boolean out = (!inWorld || time.finished(1000));
        if (InterFace.getInstance().sicretNastriokaEbana.getValue()) {
            openAnimation.run(out ? 0.0 : 1.0, 0.3, out ? Easings.CUBIC_IN : Easings.CUBIC_OUT, true);
        } else {
            openAnimation.run(out ? 0.0 : 1.0, 0.2, true);
        }
        if (openAnimation.getValue() <= 0.0) {
            return;
        }


        drawDefault(event);
    }


    private void drawDefault(Render2DEvent renderEvent) {
        MatrixStack matrixStack = renderEvent.getMatrix();

        if (InterFace.getInstance().sicretNastriokaEbana.getValue()) {
            float x = drag.position.x;
            float y = drag.position.y;

            float width = drag.size.x;
            float height = drag.size.y;

            float hpHeight = 3.0F;
            float margin = 4.0F;
            float avatarSize = 12;
            float hpWidth = width - margin * 2;

            // чтобы влезало
            drag.size.set(90, 30);

            float appendX = x + margin + (avatarSize + margin);
            float appendY = y + margin;

            matrixStack.push();
            float scale = this.openAnimation.get();
            matrixStack.translate((x + width / 2F), (y + height / 2F), 0);
            matrixStack.scale(scale, scale, 0);
            matrixStack.translate(-(x + width / 2F), -(y + height / 2F), 0);


            ПенисУтилита.ЕбатьПенка(matrixStack, x, y, width, drag.size.y, openAnimation.get(), 5);

            //InterFace.getInstance().drawClientRect(matrixStack, x, y, width, height, openAnimation.get(), 1);

            /// ScissorUtil.enable();
            /// ScissorUtil.scissor(x + 6, y + 0.5F, width - 12, height - 3);
            /// RenderUtil.Shadow.drawShadow(matrixStack, x + 30, y - 30, width - 60, 50, 24, -1);
            ///
            /// ScissorUtil.disable();

            int color = ColorUtil.replAlpha(ColorUtil.getColor(15), openAnimation.get() * 0.35F);

            // ====== HEALTH раньше — для привязки партиклов ======
            // ====== HP текст ======
            float health;
            float maxHealth;

            if (target instanceof AbstractClientPlayerEntity player && target != mc.player) {
                // Для игроков используем HealthFixed для более точного отображения
                health = !inWorld ? 0 : (float) Mathf.round(player.getHealthFixed(), 1);
                maxHealth = player.getMaxHealth();
            } else {
                // Для остальных существ используем обычный getHealth
                health = !inWorld ? 0 : (float) Mathf.round(target.getHealth(), 1);
                maxHealth = target.getMaxHealth();
            }

            // Если включен FixHP и это игрок, пытаемся получить HP из скорборда
            if (target instanceof PlayerEntity && FixHP.getInstance().isEnabled() && !PlayerUtil.isFuntime() && target != mc.player) {
                try {
                    Score score = mc.world.getScoreboard().getOrCreateScore(target.getScoreboardName(), mc.world.getScoreboard().getObjectiveInDisplaySlot(2));
                    if (score != null && score.getScorePoints() != 0) {
                        health = score.getScorePoints();
                    }
                } catch (Exception ignored) {
                    // Если что-то пошло не так со scoreboard, игнорируем
                }
            }

            FloatFormatter formatter = new FloatFormatter();
            float finalHp = formatter.format(health);
            float healthBar = (maxHealth <= 0 ? 0F : (finalHp / maxHealth));
            this.healthAnimation.run(Mathf.clamp01(healthBar), 0.8, Easings.SINE_OUT, true);

            // ====== Параметры HP-бара ======
            int hpColor1;
            int hpColor2;

            if (InterFace.getInstance().colorHP.getValue()) {
                // Плавное окрашивание в зависимости от процента здоровья
                float healthPercent = this.healthAnimation.get();
                int baseColor;

                // Определяем цвета для интерполяции
                int green = ColorUtil.getColor(85, 255, 85);     // Зеленый (100%)
                int yellow = ColorUtil.getColor(255, 255, 85);   // Желтый (75%)
                int orange = ColorUtil.getColor(255, 185, 0);    // Оранжевый (50%)
                int red = ColorUtil.getColor(255, 165, 0);       // Красный (25%)
                int darkRed = ColorUtil.getColor(255, 85, 85);     // Темно-красный (0%)

                // Плавная интерполяция между цветами
                if (healthPercent > 0.75F) {
                    // От 100% до 75%: плавный переход от зеленого к желтому
                    float t = (healthPercent - 0.75F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(yellow, green, t);
                } else if (healthPercent > 0.5F) {
                    // От 75% до 50%: плавный переход от желтого к оранжевому
                    float t = (healthPercent - 0.5F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(orange, yellow, t);
                } else if (healthPercent > 0.25F) {
                    // От 50% до 25%: плавный переход от оранжевого к красному
                    float t = (healthPercent - 0.25F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(red, orange, t);
                } else {
                    // От 25% до 0%: плавный переход от красного к темно-красному
                    float t = healthPercent / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(darkRed, red, t);
                }

                hpColor1 = ColorUtil.replAlpha(ColorUtil.multDark(baseColor, 0.25F), (int) (255 * openAnimation.getValue()));
                hpColor2 = ColorUtil.replAlpha(ColorUtil.multDark(baseColor, 0.8F), (int) (255 * openAnimation.getValue()));
            } else {
                // Стандартные цвета темы
                hpColor1 = ColorUtil.replAlpha(ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.25F), (int) (255 * openAnimation.getValue()));
                hpColor2 = ColorUtil.replAlpha(ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.8F), (int) (255 * openAnimation.getValue()));
            }

            barH = 3;
            barW = hpWidth;
            barX = x + margin;
            barY = appendY + 29 - 9 + 0.15F;


            String rs = EntityType.getKey(target.getType()).getPath();
            ResourceLocation skin = target instanceof AbstractClientPlayerEntity e ? (e).getLocationSkin() : new ResourceLocation("textures/entity/" + rs + ".png");
            float hurtPercent = (target.hurtTime - (target.hurtTime != 0 ? mc.timer.renderPartialTicks : 0.0f)) / 10.0f;
            float xyoff = 0;
            float whoff = 0;
            if (InterFace.getInstance().golova.getValue()) {
                xyoff = 1 * hurtPercent;
                whoff = 2 * hurtPercent;
            }

            if (target instanceof AbstractClientPlayerEntity player) {
                xyoff = -11;
                drawHead(matrixStack, player, x + margin + 1, appendY + 1, 15.5F);
            } else {
                xyoff = -28;
            }

            String name = target.getName().getString();
            stripFont.draw(Fonts.SFP_SEMIBOLD, matrixStack, name, appendX + 15 + xyoff, appendY + 0.5F, 60, ColorUtil.multAlpha(InterFace.getInstance().ThemeTextHud(), openAnimation.get()), 8, 2);


            tempHp = NumberTransition.resultspeed(tempHp, finalHp);
            String healthText = String.format("%.0f", this.tempHp) + (PlayerUtil.isFuntime() || target.getAbsorptionAmount() == 0 ? "" : "(" + String.format("%.0f", target.getAbsorptionAmount()) + ")");
            if (finalHp > 100) {
                healthText = "Неизвестно";
            }
            String hpText = (healthText) + ColorFormatting.getColor(ColorUtil.fade()) + "hp";

            float fontSize = 7;
            stripFont.draw(Fonts.SFP_SEMIBOLD, matrixStack, hpText,
                    appendX + xyoff + 15, appendY + avatarSize - fontSize - margin + 9,
                    70, ColorUtil.multAlpha(InterFace.getInstance().ThemeTextHud(), openAnimation.get() * 0.9F), fontSize, 2);

            // ====== РИСУЕМ HP-БАР ======
            RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW, barH, color, Round.of(1.5F));
            RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW * healthAnimation.get(), barH, hpColor1, hpColor2, Round.of(1.5F));


            // ====== ОТДЕЛЬНАЯ ЗОЛОТАЯ ПОЛОСКА ABSORPTION HP ======
            float absorption = target.getAbsorptionAmount();
            if (absorption > 0 && !PlayerUtil.isFuntime()) {
                float absorptionBar = Math.min(1.0F, absorption / maxHealth);
                this.absorptionAnimation.run(Mathf.clamp01(absorptionBar), 0.8, Easings.SINE_OUT, true);

                if (absorptionAnimation.get() > 0.01F) {
                    float absorptionBarY = barY + 7 + 1.5F; // под основной полоской с отступом
                    float absorptionBarH = 3.5F; // высота золотой полоски

                    int goldColor1 = ColorUtil.replAlpha(ColorUtil.getColor(124, 80, 0), (int) (255 * openAnimation.getValue()));
                    int goldColor2 = ColorUtil.replAlpha(ColorUtil.getColor(255, 200, 0), (int) (255 * openAnimation.getValue()));


                    // Золотая полоска
                    RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW * absorptionAnimation.get(), barH, goldColor1, goldColor2, Round.of(1.5F));
                }
            }


            matrixStack.pop();

            // ====== РЕНДЕР ОРУЖИЯ/БРОНИ ======
            if (target instanceof PlayerEntity) {
                drawArmorRow((PlayerEntity) target, x + width / 2 - 3.5F, y, width);
            }

        } else {

            float x = drag.position.x;
            float y = drag.position.y;

            float width = drag.size.x;
            float height = drag.size.y;

            float hpHeight = 3.0F;
            float margin = 4.0F;
            float avatarSize = 12;
            float hpWidth = width - 37;

            // чтобы влезало
            drag.size.set(100, 35);

            float appendX = x + margin + (avatarSize + margin);
            float appendY = y + margin;

            matrixStack.push();

            InterFace.getInstance().drawClientRect(matrixStack, x, y, width, height, openAnimation.get(), 7);

            /// ScissorUtil.enable();
            /// ScissorUtil.scissor(x + 6, y + 0.5F, width - 12, height - 3);
            /// RenderUtil.Shadow.drawShadow(matrixStack, x + 30, y - 30, width - 60, 50, 24, -1);
            ///
            /// ScissorUtil.disable();

            int color = ColorUtil.replAlpha(ColorUtil.getColor(15), openAnimation.get() * 0.35F);

            // ====== HEALTH раньше — для привязки партиклов ======
            // ====== HP текст ======
            float health;
            float maxHealth;

            if (target instanceof AbstractClientPlayerEntity player && target != mc.player) {
                // Для игроков используем HealthFixed для более точного отображения
                health = !inWorld ? 0 : (float) Mathf.round(player.getHealthFixed(), 1);
                maxHealth = player.getMaxHealth();
            } else {
                // Для остальных существ используем обычный getHealth
                health = !inWorld ? 0 : (float) Mathf.round(target.getHealth(), 1);
                maxHealth = target.getMaxHealth();
            }

            // Если включен FixHP и это игрок, пытаемся получить HP из скорборда
            if (target instanceof PlayerEntity && FixHP.getInstance().isEnabled() && !PlayerUtil.isFuntime() && target != mc.player) {
                try {
                    Score score = mc.world.getScoreboard().getOrCreateScore(target.getScoreboardName(), mc.world.getScoreboard().getObjectiveInDisplaySlot(2));
                    if (score != null && score.getScorePoints() != 0) {
                        health = score.getScorePoints();
                    }
                } catch (Exception ignored) {
                    // Если что-то пошло не так со scoreboard, игнорируем
                }
            }

            FloatFormatter formatter = new FloatFormatter();
            float finalHp = formatter.format(health);
            float healthBar = (maxHealth <= 0 ? 0F : (finalHp / maxHealth));
            this.healthAnimation.run(Mathf.clamp01(healthBar), 0.8, Easings.SINE_OUT, true);

            // ====== Параметры HP-бара ======
            int hpColor1;
            int hpColor2;

            if (InterFace.getInstance().colorHP.getValue()) {
                // Плавное окрашивание в зависимости от процента здоровья
                float healthPercent = this.healthAnimation.get();
                int baseColor;

                // Определяем цвета для интерполяции
                int green = ColorUtil.getColor(85, 255, 85);     // Зеленый (100%)
                int yellow = ColorUtil.getColor(255, 255, 85);   // Желтый (75%)
                int orange = ColorUtil.getColor(255, 185, 0);    // Оранжевый (50%)
                int red = ColorUtil.getColor(255, 165, 0);       // Красный (25%)
                int darkRed = ColorUtil.getColor(255, 85, 85);     // Темно-красный (0%)

                // Плавная интерполяция между цветами
                if (healthPercent > 0.75F) {
                    // От 100% до 75%: плавный переход от зеленого к желтому
                    float t = (healthPercent - 0.75F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(yellow, green, t);
                } else if (healthPercent > 0.5F) {
                    // От 75% до 50%: плавный переход от желтого к оранжевому
                    float t = (healthPercent - 0.5F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(orange, yellow, t);
                } else if (healthPercent > 0.25F) {
                    // От 50% до 25%: плавный переход от оранжевого к красному
                    float t = (healthPercent - 0.25F) / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(red, orange, t);
                } else {
                    // От 25% до 0%: плавный переход от красного к темно-красному
                    float t = healthPercent / 0.25F; // 0..1
                    baseColor = ColorUtil.interpolateColor(darkRed, red, t);
                }

                hpColor1 = ColorUtil.replAlpha(ColorUtil.multDark(baseColor, 0.25F), (int) (255 * openAnimation.getValue()));
                hpColor2 = ColorUtil.replAlpha(ColorUtil.multDark(baseColor, 0.8F), (int) (255 * openAnimation.getValue()));
            } else {
                // Стандартные цвета темы
                hpColor1 = ColorUtil.replAlpha(ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.25F), (int) (255 * openAnimation.getValue()));
                hpColor2 = ColorUtil.replAlpha(ColorUtil.multDark(InterFace.getInstance().themeColor(), 0.8F), (int) (255 * openAnimation.getValue()));
            }

            barH = 6;
            barW = hpWidth;
            barX = x + margin + 30;
            barY = appendY + 29 - 3 - 6 + 0.15F;


            String rs = EntityType.getKey(target.getType()).getPath();
            ResourceLocation skin = target instanceof AbstractClientPlayerEntity e ? (e).getLocationSkin() : new ResourceLocation("textures/entity/" + rs + ".png");
            float hurtPercent = (target.hurtTime - (target.hurtTime != 0 ? mc.timer.renderPartialTicks : 0.0f)) / 10.0f;
            float xyoff = 0;
            float whoff = 0;
            if (InterFace.getInstance().golova.getValue()) {
                xyoff = 1 * hurtPercent;
                whoff = 2 * hurtPercent;
            }
            if (target instanceof AbstractClientPlayerEntity) {
                RenderUtil.drawHead(skin, x + 1F + xyoff - 1, appendY - 3 + xyoff - 1, 29 - whoff + 2, 29 - whoff + 2, 5, openAnimation.get(), hurtPercent);
            } else {
                RenderUtil.Rounded.smooth(matrixStack, x + 3.25F + xyoff, appendY - 1 + xyoff, 29 - whoff, 29 - whoff, color, Round.of(6));
                Fonts.SFP_REGULAR.draw(matrixStack, "?", x + 14.5F, appendY + 7, ColorUtil.replAlpha(-1, openAnimation.get() * 1), 12);
            }

            String name = target.getName().getString();
            stripFont.draw(Fonts.SFP_MEDIUM, matrixStack, name, appendX + 15, appendY + 0.5F, 60, ColorUtil.multAlpha(InterFace.getInstance().ThemeTextHud(), openAnimation.get()), 9, 2);


            tempHp = NumberTransition.resultspeed(tempHp, finalHp);
            String healthText = String.format("%.1f", this.tempHp) + (PlayerUtil.isFuntime() || target.getAbsorptionAmount() == 0 ? "" : "(" + String.format("%.1f", target.getAbsorptionAmount()) + ")");
            if (finalHp > 100) {
                healthText = "Неизвестно";
            }
            String hpText = "HP: " + (healthText);

            float fontSize = 7F;
            stripFont.draw(Fonts.SFP_MEDIUM, matrixStack, hpText,
                    appendX + 15, appendY + avatarSize - fontSize - margin + 9.7F,
                    70, ColorUtil.multAlpha(InterFace.getInstance().ThemeTextHud(), openAnimation.get() * 0.9F), fontSize, 2);

            // ====== РИСУЕМ HP-БАР ======
            RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW, 8, color, Round.of(3));
            RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW * healthAnimation.get(), 8, hpColor1, hpColor2, Round.of(3));

            if (InterFace.getInstance().particlest.getValue())
                ParticleEngine.addParticles(new Vector2f(barX + barW * healthAnimation.get() - 8, barY - 4.5F), new Vector2f(Mathf.random(0.2F, 1), Mathf.random(-.2F, .2F)), (int) (5 * hurtPercent));

            // ====== ОТДЕЛЬНАЯ ЗОЛОТАЯ ПОЛОСКА ABSORPTION HP ======
            float absorption = target.getAbsorptionAmount();
            if (absorption > 0 && !PlayerUtil.isFuntime()) {
                float absorptionBar = Math.min(1.0F, absorption / maxHealth);
                this.absorptionAnimation.run(Mathf.clamp01(absorptionBar), 0.8, Easings.SINE_OUT, true);

                if (absorptionAnimation.get() > 0.01F) {
                    float absorptionBarY = barY + 7 + 1.5F; // под основной полоской с отступом
                    float absorptionBarH = 3.5F; // высота золотой полоски

                    int goldColor1 = ColorUtil.replAlpha(ColorUtil.getColor(124, 80, 0), (int) (255 * openAnimation.getValue()));
                    int goldColor2 = ColorUtil.replAlpha(ColorUtil.getColor(255, 200, 0), (int) (255 * openAnimation.getValue()));


                    // Золотая полоска
                    RenderUtil.Rounded.smooth(matrixStack, barX, barY, barW * absorptionAnimation.get(), 8, goldColor1, goldColor2, Round.of(3));
                }
            }


            matrixStack.pop();

            // ====== РЕНДЕР ОРУЖИЯ/БРОНИ ======
            if (target instanceof PlayerEntity) {
                drawArmorRow((PlayerEntity) target, x + width / 2 - 3.5F, y, width);
            }
        }

    }

    private void drawArmorRow(LivingEntity entity, float x, float y, float width, MatrixStack matrixStack) {
        ArrayList<ItemStack> items = new ArrayList<>();
        items.add(entity.getHeldItemOffhand());
        items.add(entity.getHeldItemMainhand());

        for (ItemStack armorItem : entity.getArmorInventoryList()) {
            items.add(armorItem.isEmpty() ? ItemStack.EMPTY : armorItem.copy());
        }

        float itemSpacing = 9.5F;
        float totalWidth = (float) items.size() * itemSpacing;
        float startX = x;
        float startY = y - 10.0F;
        matrixStack.push();
        RenderSystem.pushMatrix();
        RenderSystem.scaled(0.5F, 0.5F, 0.5F);
        float currentOffset = 0.0F;
        float scale = (float) Math.max(0.001, this.openAnimation.get());

        for (ItemStack item : items) {
            float itemX = (startX + currentOffset) * 2.0F;
            float itemY = startY * 2.0F;
            RenderSystem.pushMatrix();
            if (!item.isEmpty()) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(itemX + 8.0F, itemY + 8.0F, 0.0F);
                RenderSystem.scalef(scale, scale, scale);
                RenderSystem.translatef(-(itemX + 8.0F), -(itemY + 8.0F), 0.0F);
                float animationValue = this.openAnimation.get();
                int itemColor = ColorUtil.replAlpha(ColorUtil.WHITE, (int) (255.0F * animationValue));
                RenderUtil.drawItemStack(matrixStack, item, (int) itemX, (int) itemY, itemColor);
                RenderSystem.popMatrix();
            }

            RenderSystem.popMatrix();
            currentOffset += itemSpacing;
        }

        RenderSystem.popMatrix();
        matrixStack.pop();

    }

    public static String trimToFit(String text, int maxLength, float maxWidth, float fontSize) {
        if (text == null) {
            return "";
        } else if (text.isEmpty()) {
            return "";
        } else if (text.length() <= maxLength && Fonts.SF_BOLD.getWidth(text, fontSize) <= maxWidth) {
            return text;
        } else {
            float ellipsisWidth = Fonts.SF_BOLD.getWidth("...", fontSize);
            int currentLength = Math.min(maxLength, text.length());
            if (ellipsisWidth > maxWidth) {
                return "";
            } else {
                while (currentLength > 0) {
                    boolean needsEllipsis = currentLength < text.length();
                    String substring = text.substring(0, currentLength);
                    String result = substring + (needsEllipsis ? "..." : "");
                    float resultWidth = Fonts.SF_BOLD.getWidth(result, fontSize);
                    if (result.length() <= maxLength && resultWidth <= maxWidth) {
                        return result;
                    }

                    --currentLength;
                }

                return "";
            }
        }
    }

    public class FloatFormatter {
        public float format(float value) {
            float precision = (float) Math.pow(6.0F, 1.0F);
            return (float) Math.round(value * precision) / precision;
        }
    }

    private void spawnParticles(float x, float y, float size, float spread, float velocityX, float velocityY, float gravity, float lifetime) {
        float randomX = (this.random.nextFloat() - 0.5F) * spread;
        float randomY = (this.random.nextFloat() - 0.5F) * spread;
        this.particles.add(new Particle(randomX + x, randomY + y, size, randomX, randomY, velocityX, velocityY, gravity, lifetime));
    }

    private void drawHead(MatrixStack matrix, final AbstractClientPlayerEntity player, final float x, final float y, final float size) {

        float hurtPC = (float) Math.sin(target.hurtTime * (18F * Math.PI / 180F));

        int color = ColorUtil.overCol(ColorUtil.getColor(0, 1F), ColorUtil.RED, hurtPC);

        RenderUtil.Shadow.drawShadow(matrix, x, y, size, size, 8, color, color, color, color, Round.of(4));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.0F);
        RenderSystem.enableTexture();
        mc.getTextureManager().bindTexture(player.getLocationSkin());
        int headColor = ColorUtil.overCol(ColorUtil.WHITE, ColorUtil.RED, hurtPC);
        float[] rgba = ColorUtil.getRGBAf(headColor);

        RenderSystem.color4f(rgba[0], rgba[1], rgba[2], rgba[3]);
        AbstractGui.blit(matrix, x, y, size, size, 4F, 4F, 4F, 4F, 32F, 32F);
        GLUtil.scale(matrix, x + size / 2F, y + size / 2F, 1.15F, () ->
                AbstractGui.blit(matrix, x, y, size, size, 20, 4, 4, 4, 32, 32)
        );
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private void spawnHeadParticles() {
        long currentTime = System.currentTimeMillis();

        // Спавним партиклы за головой через определенные интервалы времени (независимо от FPS)
        if (currentTime - lastHeadParticleSpawn >= HEAD_PARTICLE_SPAWN_INTERVAL) {
            lastHeadParticleSpawn = currentTime;

            float headCenterX = this.drag.position.x + 1.0F + 12.0F; // Центр головы по X
            float headCenterY = this.drag.position.y + 4.0F + 12.0F; // Центр головы по Y

            // Спавним 1-2 партикла за головой
            int count = this.random.nextInt(2) + 1;
            for (int i = 0; i < count; i++) {
                this.spawnParticles(headCenterX, headCenterY, 3, 15, 0.0F, 0.0F, 0.0F, 2000.0F);
            }
        }
    }

    public void attack(AttackEvent attackEvent) {
        if (this.target != null && attackEvent.getTarget() == target) {
            float margin = 4.5F;
            float avatarSize = 29;
            for (int i = 0; i < 5; i++) {
                // motion ~ 80..125 px/s — стабильная скорость при любом FPS
                spawnParticles(drag.position.x + margin + avatarSize / 2F,
                        drag.position.y + margin + avatarSize / 2F,
                        4,
                        50,
                        0F, 0F, 0F, 1500);
            }
        }


    }


    public static void drawCircle(float x, float y, float start, float end, float radius, float width, boolean filled, float alpha) {

        float i;
        float endOffset;
        if (start > end) {
            endOffset = end;
            end = start;
            start = endOffset;
        }
        GlStateManager.enableBlend();
        RenderSystem.disableAlphaTest();
        GL11.glDisable(GL_TEXTURE_2D);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.shadeModel(7425);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (i = end; i >= start; i--) {
            ColorUtil.setColor(ColorUtil.replAlpha(ColorUtil.fade((int) (i * 1)), alpha));
            float cos = MathHelper.cos((float) (i * Math.PI / 180)) * radius;
            float sin = MathHelper.sin((float) (i * Math.PI / 180)) * radius;
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        if (filled) {
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            for (i = end; i >= start; i--) {
                ColorUtil.setColor(ColorUtil.replAlpha(ColorUtil.fade((int) (i * 1)), alpha));
                float cos = MathHelper.cos((float) (i * Math.PI / 180)) * radius;
                float sin = MathHelper.sin((float) (i * Math.PI / 180)) * radius;
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
        }

        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        GL11.glEnable(GL_TEXTURE_2D);
        GlStateManager.disableBlend();
    }


    public static void drawCircle2(float x, float y, float start, float end, float radius, float width, boolean filled, int alpha) {

        float i;
        float endOffset;
        if (start > end) {
            endOffset = end;
            end = start;
            start = endOffset;
        }
        GlStateManager.enableBlend();
        RenderSystem.disableAlphaTest();
        GL11.glDisable(GL_TEXTURE_2D);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.shadeModel(7425);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(width);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (i = end; i >= start; i--) {
            ColorUtil.setColor(alpha);
            float cos = MathHelper.cos((float) (i * Math.PI / 180)) * radius;
            float sin = MathHelper.sin((float) (i * Math.PI / 180)) * radius;
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        if (filled) {
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            for (i = end; i >= start; i--) {
                ColorUtil.setColor(alpha);
                float cos = MathHelper.cos((float) (i * Math.PI / 180)) * radius;
                float sin = MathHelper.sin((float) (i * Math.PI / 180)) * radius;
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
        }

        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        GL11.glEnable(GL_TEXTURE_2D);
        GlStateManager.disableBlend();
    }

    private void drawArmorRow(PlayerEntity player, float x, float y, float width) {
        List<ItemStack> items = new ArrayList<>();
        if (!player.getHeldItemOffhand().isEmpty()) items.add(player.getHeldItemOffhand());
        if (!player.getHeldItemMainhand().isEmpty()) items.add(player.getHeldItemMainhand());
        for (ItemStack itemStack : player.getArmorInventoryList()) {
            if (!itemStack.isEmpty()) items.add(itemStack.copy());
        }

        float totalWidth = 0f;
        for (ItemStack st : items) {
            if (st.isEmpty()) continue;
            totalWidth += 9.5f;
        }

        float startX = x;
        float rowY = y - 10f;

        RenderSystem.pushMatrix();
        RenderSystem.scaled(0.5f, 0.5f, 0.5f);
        float offset = 0f;
        float scale = (float) Math.max(0.001, openAnimation.get()); // аним. масштаб
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;

            float drawX = (startX + offset) * 2f;
            float drawY = rowY * 2f;

            RenderSystem.pushMatrix();
            RenderSystem.translatef(drawX + 8f, drawY + 8f, 0f);
            RenderSystem.scalef(scale, scale, scale);
            RenderSystem.translatef(-(drawX + 8f), -(drawY + 8f), 0f);

            mc.ingameGUI.renderHotbarItem((int) drawX, (int) drawY, mc.getRenderPartialTicks(), player, itemStack);
            RenderSystem.popMatrix();

            offset += 9f;
        }
        RenderSystem.popMatrix();
    }

    @Generated
    public DragSetting drag() {
        return this.drag;
    }

    @Generated
    public Animation openAnimation() {
        return this.openAnimation;
    }

    @Generated
    public Animation healthAnimation() {
        return this.healthAnimation;
    }

    @Generated
    public StopWatch time() {
        return this.time;
    }

    @Generated
    public boolean inWorld() {
        return this.inWorld;
    }

    @Generated
    public LivingEntity target() {
        return this.target;
    }

    @Generated
    public List<Particle> particles() {
        return this.particles;
    }

    @Generated
    public ResourceLocation bloom() {
        return this.bloom;
    }

    @Generated
    public Random random() {
        return this.random;
    }

    @Generated
    public float radius() {
        Objects.requireNonNull(this);
        return 3.0F;
    }

    @Generated
    public Round round() {
        return this.round;
    }

    @Generated
    public StripFont stripFont() {
        return this.stripFont;
    }

    @Generated
    public float barX() {
        return this.barX;
    }

    @Generated
    public float barY() {
        return this.barY;
    }

    @Generated
    public float barW() {
        return this.barW;
    }

    @Generated
    public float barH() {
        return this.barH;
    }

    @Generated
    public long lastFrameNanos() {
        return this.lastFrameNanos;
    }

    @Generated
    public boolean firstFrame() {
        return this.firstFrame;
    }

    @Generated
    public float tempHp() {
        return this.tempHp;
    }
}
