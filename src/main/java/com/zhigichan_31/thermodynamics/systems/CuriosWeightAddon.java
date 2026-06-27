package com.zhigichan_31.thermodynamics.systems;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.api.IWeightProvider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

@EventBusSubscriber(modid = Thermodynamics.MODID)
public class CuriosWeightAddon implements IWeightProvider {
    private static boolean registered = false;
    private static boolean curiosLoaded = false;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!registered) {
            curiosLoaded = ModList.get().isLoaded("curios");
            if (curiosLoaded) {
                WeightManager.registerAddon(new CuriosWeightAddon());
            }
            registered = true;
        }
    }

    @Override
    public float getTotalWeight(Player player) {
        if (!curiosLoaded) return 0.0f;

        float totalWeight = 0.0f;

        // Нативно получаем менеджер инвентаря Curios для игрока без рефлексии
        var curiosInventoryOpt = CuriosApi.getCuriosInventory(player);
        if (curiosInventoryOpt.isPresent()) {
            ICuriosItemHandler handler = curiosInventoryOpt.get();

            // Проверяем все пять одиночных слотов, включая charm
            String[] singleSlots = {"back", "belt", "necklace", "charm", "body"};
            for (String slot : singleSlots) {
                totalWeight += checkSlot(handler, slot, 0);
            }

            // Проверяем оба слота под кольца
            totalWeight += checkSlot(handler, "ring", 0);
            totalWeight += checkSlot(handler, "ring", 1);
        }

        return totalWeight;
    }

    private float checkSlot(ICuriosItemHandler handler, String slotType, int index) {
        // Получаем обработчик стеков для конкретного типа слота
        ICurioStacksHandler stacksHandler = handler.getCurios().get(slotType);
        if (stacksHandler != null) {
            // Проверяем, существует ли слот с таким индексом (защита от выхода за границы)
            if (index >= 0 && index < stacksHandler.getStacks().getSlots()) {
                ItemStack stack = stacksHandler.getStacks().getStackInSlot(index);
                if (!stack.isEmpty()) {
                    return (WeightManager.calculateItemWeight(stack) * stack.getCount()) + scanInternalBackpack(stack);
                }
            }
        }
        return 0.0f;
    }

    @Override
    public float getStaminaDrainModifier(Player player) {
        return 1.0f;
    }

    public float scanInternalBackpack(ItemStack stack) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if ((name.contains("quark:backpack") || name.contains("backpack")) && stack.has(DataComponents.CONTAINER)) {
            ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
            if (contents != null) {
                float w = 0.0f;
                for (ItemStack is : contents.nonEmptyItems()) {
                    if (!is.isEmpty()) {
                        w += WeightManager.calculateItemWeight(is) * is.getCount();
                    }
                }
                return w;
            }
        }
        return 0.0f;
    }
}
