package com.zhigichan_31.thermodynamics.client;

import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CenterModuleRenderer {

    // КЭШ СТРОК: Создаем массив от 0% до 200%, чтобы Java не создавала новые строки в кадре.
    // Если вес уйдет выше 200%, код безопасно создаст временную строку.
    private static final String[] WEIGHT_CACHE = new String[201];

    static {
        for (int i = 0; i <= 200; i++) {
            WEIGHT_CACHE[i] = i + "%";
        }
    }

    public static void render(GuiGraphics graphics, Font font, int centerX, int height, PlayerData data, ResourceLocation texture) {
        int baseModuleX = centerX - 16;
        float baseModuleY = height - 65.0f;

        // Расчет процентов веса
        float maxWeight = ModConfig.MAX_CARRY_WEIGHT.get().floatValue();
        float weightPercentRaw = maxWeight > 0 ? (data.getWeight() / maxWeight) : 0f;
        int weightPercent = (int) (weightPercentRaw * 100f);

        // БЕЗОПАСНОЕ ПОЛУЧЕНИЕ СТРОКИ ИЗ КЭША (0 мс нагрузки)
        String weightDisplayStr;
        if (weightPercent >= 0 && weightPercent <= 200) {
            weightDisplayStr = WEIGHT_CACHE[weightPercent];
        } else {
            weightDisplayStr = weightPercent + "%"; // Для экстремальных значений выше 200%
        }

        // НАЧАЛО ОТРИСОВКИ БЛОКА
        graphics.pose().pushPose();
        graphics.pose().translate(baseModuleX + 0.5f, baseModuleY, 0);

        // СЛОЙ 1: Задняя цветная шкала барабана
        graphics.blit(texture, 0, 0, 32.0F, 64.0F, 32, 32, 256, 256);

        // СЛОЙ 2: Полосочка-стрелка прибора (Оставляем, она теперь "легкая")
        float weightAngle = Mth.clamp(-90.0f + (Math.min(1.0f, weightPercentRaw) * 180.0f), -90.0f, 90.0f);

        graphics.pose().pushPose();
        graphics.pose().translate(16.0f, 19.5f, 0.0f);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(weightAngle));
        graphics.pose().scale(1.0f, 1.5f, 1.0f);
        graphics.pose().translate(-0.5f, -10.0f, 0.0f);
        graphics.blit(texture, 0, 0, 72.0F, 68.0F, 1, 10, 256, 256);
        graphics.pose().popPose();

        // СЛОЙ 3: Металлическая подложка гири
        graphics.blit(texture, 0, 0, 0.0F, 64.0F, 32, 32, 256, 256);

        // СЛОЙ 5: ЦИФРЫ ПРОЦЕНТОВ
        graphics.pose().pushPose();
        graphics.pose().translate(16.0f, 3.5f, 0.0f);
        graphics.pose().scale(0.55f, 0.55f, 1.0f);

        // font.width() тоже может быть тяжелым, но без создания новых строк он работает быстро
        int textOffsetX = -(font.width(weightDisplayStr) / 2);
        graphics.drawString(font, weightDisplayStr, textOffsetX, 0, 0xFFFFFF, true);
        graphics.pose().popPose();

        graphics.pose().popPose(); // КОНЕЦ ОТРИСОВКИ БЛОКА
    }
}
