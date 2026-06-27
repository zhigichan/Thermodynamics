package com.zhigichan_31.thermodynamics.systems;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.api.IWeightProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Thermodynamics.MODID)
public class QuarkWeightAddon implements IWeightProvider {

    private static boolean registered = false;

    /**
     * Автоматически регистрируем этот аддон в WeightManager при входе игрока,
     * чтобы связать систему вилки без жесткого хардкода.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!registered) {
            WeightManager.registerAddon(new QuarkWeightAddon());
            registered = true;
        }
    }

    @Override
    public float getTotalWeight(Player player) {
        float backpackWeight = 0.0f;

        // Сканируем все слоты обычного инвентаря игрока в поисках рюкзака Кварка
        for (ItemStack stack : player.getInventory().items) {
            backpackWeight += scanBackpackContents(stack);
        }

        // Сканируем все слоты надетой брони (нагрудник/спина) в поисках рюкзака Кварка
        for (ItemStack armorStack : player.getInventory().armor) {
            backpackWeight += scanBackpackContents(armorStack);
        }

        return backpackWeight;
    }

    @Override
    public float getStaminaDrainModifier(Player player) {
        return 1.0f; // Рюкзак Quark не влияет на множитель траты стамины напрямую
    }

    /**
     * Вскрывает рюкзак и считает вес лежащих внутри предметов.
     * ИСПРАВЛЕНО ДЛЯ 1.21.1: contents.nonEmptyItems() правильно возвращает список предметов сумок в NeoForge!
     */
    private float scanBackpackContents(ItemStack stack) {
        if (stack.isEmpty()) return 0.0f;

        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String name = rl.toString();

        // Проверяем, рюкзак ли это от Quark или любого другого мода
        if (name.contains("quark:backpack") || name.contains("backpack")) {
            float internalWeight = 0.0f;

            if (stack.has(DataComponents.CONTAINER)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    // Используем метод nonEmptyItems() — он стабилен и гарантированно существует в 1.21.1
                    for (ItemStack internalStack : contents.nonEmptyItems()) {
                        if (!internalStack.isEmpty()) {
                            // Передаем вес в метод ядра WeightManager
                            internalWeight += WeightManager.calculateItemWeight(internalStack) * internalStack.getCount();
                        }
                    }
                }
            }
            return internalWeight;
        }
        return 0.0f;
    }
}
