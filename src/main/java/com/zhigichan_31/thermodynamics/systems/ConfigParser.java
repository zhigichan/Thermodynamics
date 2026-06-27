package com.zhigichan_31.thermodynamics.systems;

import com.mojang.logging.LogUtils; // 🔥 Правильный логгер для 1.21.1
import com.zhigichan_31.thermodynamics.Thermodynamics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 🔥 Убрали параметр bus, так как в 1.21.1 для MOD-автобуса используется IEventBus в главном классе,
// либо мы можем слушать это событие на игровом автобусе (ModConfigEvent публикуется на обоих)
@EventBusSubscriber(modid = Thermodynamics.MODID)
public class ConfigParser {

    private static final Logger LOGGER = LogUtils.getLogger(); // 🔥 Добавили логгер

    private static final Map<Item, Float> ITEM_WEIGHTS_CACHE = new HashMap<>();
    private static final Map<TagKey<Item>, Float> TAG_WEIGHTS_CACHE = new HashMap<>();

    private static final Map<Item, Float> WEAPON_DRAIN_CACHE = new HashMap<>();
    private static final Map<TagKey<Item>, Float> TAG_DRAIN_CACHE = new HashMap<>();

    public static float getItemWeight(ItemStack stack, float defaultWeight) {
        if (stack.isEmpty()) return 0.0f;
        Item item = stack.getItem();

        if (ITEM_WEIGHTS_CACHE.containsKey(item)) {
            return ITEM_WEIGHTS_CACHE.get(item);
        }

        for (Map.Entry<TagKey<Item>, Float> entry : TAG_WEIGHTS_CACHE.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultWeight;
    }

    public static float getWeaponDrain(ItemStack stack, float defaultDrain) {
        if (stack.isEmpty()) return defaultDrain;
        Item item = stack.getItem();

        if (WEAPON_DRAIN_CACHE.containsKey(item)) {
            return WEAPON_DRAIN_CACHE.get(item);
        }

        for (Map.Entry<TagKey<Item>, Float> entry : TAG_DRAIN_CACHE.entrySet()) {
            if (stack.is(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultDrain;
    }

    // 🔥 Это событие в 1.21.1 прекрасно прилетает и на обычный автобус
    @SubscribeEvent
    public static void onConfigLoadOrReload(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Thermodynamics.MODID)) {
            reloadWeightsCache();
            reloadWeaponDrainCache();
            LOGGER.info("[Thermodynamics] Конфигурация успешно перезаписана в кэш памяти без лагов!");
        }
    }

    private static void reloadWeightsCache() {
        ITEM_WEIGHTS_CACHE.clear();
        TAG_WEIGHTS_CACHE.clear();
        List<? extends String> lines = ModConfig.CUSTOM_ITEM_WEIGHTS.get();
        parseLinesToCaches(lines, ITEM_WEIGHTS_CACHE, TAG_WEIGHTS_CACHE);
    }

    private static void reloadWeaponDrainCache() {
        WEAPON_DRAIN_CACHE.clear();
        TAG_DRAIN_CACHE.clear();
        List<? extends String> lines = ModConfig.CUSTOM_WEAPON_STAMINA.get();
        parseLinesToCaches(lines, WEAPON_DRAIN_CACHE, TAG_DRAIN_CACHE);
    }

    private static void parseLinesToCaches(List<? extends String> lines, Map<Item, Float> itemCache, Map<TagKey<Item>, Float> tagCache) {
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("=");
            if (parts.length < 2) continue;

            String key = parts[0].trim(); // 🔥 Исправлен индекс [0] для ключа
            float value;
            try {
                value = Float.parseFloat(parts[1].trim()); // 🔥 Исправлен индекс [1] для значения
            } catch (NumberFormatException ignored) {
                continue;
            }

            try {
                if (key.startsWith("#")) {
                    ResourceLocation tagLocation = ResourceLocation.parse(key.substring(1));
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLocation);
                    tagCache.put(tagKey, value);
                } else {
                    ResourceLocation itemLocation = ResourceLocation.parse(key);
                    Item item = BuiltInRegistries.ITEM.get(itemLocation);
                    if (item != net.minecraft.world.item.Items.AIR || key.equals("minecraft:air")) {
                        itemCache.put(item, value);
                    }
                }
            } catch (Exception ignored) {}
        }
    }
}
