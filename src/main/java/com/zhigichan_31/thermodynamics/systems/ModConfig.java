package com.zhigichan_31.thermodynamics.systems;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public static ModConfigSpec.DoubleValue BLOCK_BREAK_HARDNESS_MULTIPLIER;

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue SPRINT_DRAIN;
    public static final ModConfigSpec.DoubleValue REGEN_BASE;
    public static final ModConfigSpec.DoubleValue BASE_SWING_DRAIN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_WEAPON_STAMINA;

    public static final ModConfigSpec.DoubleValue STAMINA_RECOVERY_THRESHOLD;
    public static final ModConfigSpec.IntValue REGEN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue MOVING_REGEN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HUNGER_THIRST_PENALTY;
    public static final ModConfigSpec.DoubleValue MIN_SWING_SCALE_DRAIN;
    public static final ModConfigSpec.DoubleValue EXHAUSTED_MINING_PENALTY;
    public static final ModConfigSpec.DoubleValue EXHAUSTED_ATTACK_PENALTY;

    public static final ModConfigSpec.DoubleValue MAX_CARRY_WEIGHT;
    public static final ModConfigSpec.DoubleValue MAX_SPEED_PENALTY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_ITEM_WEIGHTS;

    // Настраиваемые параметры для контроля влияния веса на стамину
    public static final ModConfigSpec.DoubleValue WEIGHT_SPRINT_DRAIN_IMPACT;
    public static final ModConfigSpec.DoubleValue WEIGHT_REGEN_PENALTY_IMPACT;

    static {

        BUILDER.comment("Настройки системы выносливости (Стамины)").push("stamina");

        SPRINT_DRAIN = BUILDER.comment("Базовая трата стамины при беге (за тик)").defineInRange("sprintDrain", 0.35d, 0.0d, 100.0d);
        REGEN_BASE = BUILDER.comment("Базовая скорость регенерации стамины").defineInRange("regenBase", 0.8d, 0.0d, 100.0d);
        BASE_SWING_DRAIN = BUILDER.comment("Трата стамины при взмахе пустой рукой или обычным бытовым предметом (палка, факел)").defineInRange("baseSwingDrain", 1.0d, 0.0d, 100.0d);

        BLOCK_BREAK_HARDNESS_MULTIPLIER = BUILDER.comment("Сколько единиц стамины тратится за один тик (1/20 секунды) вскапывания блока.")
                .defineInRange("blockBreakHardnessMultiplier", 0.15d, 0.0d, 100.0d);

        STAMINA_RECOVERY_THRESHOLD = BUILDER.comment("Порог стамины, который необходимо накопить для выхода из состояния изнурения")
                .defineInRange("recoveryThreshold", 25.0d, 0.0d, 100.0d);

        REGEN_COOLDOWN = BUILDER.comment("Задержка перед началом регенерации стамины после выполнения действий (в тиках, 20 тиков = 1 сек)")
                .defineInRange("regenCooldownTicks", 15, 0, 100);

        MOVING_REGEN_MULTIPLIER = BUILDER.comment("Множитель регенерации во время ходьбы/движения игрока (замедление регенерации)")
                .defineInRange("movingRegenMultiplier", 0.4d, 0.0d, 1.0d);

        HUNGER_THIRST_PENALTY = BUILDER.comment("Штраф к скорости регенерации от сильного голода или жажды (0.25 означает -25% к регенерации)")
                .defineInRange("hungerThirstPenalty", 0.25d, 0.0d, 1.0d);

        MIN_SWING_SCALE_DRAIN = BUILDER.comment("Минимальный множитель стоимости взмаха при быстром закликивании")
                .defineInRange("minSwingScaleDrain", 0.5d, 0.0d, 1.0d);

        EXHAUSTED_MINING_PENALTY = BUILDER.comment("Штраф к скорости копания при полной усталости (0.50 означает -50% скорости, 0.80 означает -80% скорости)")
                .defineInRange("exhaustedMiningPenalty", 0.50d, 0.0d, 1.0d);

        EXHAUSTED_ATTACK_PENALTY = BUILDER.comment("Штраф к скорости замаха оружия при полной усталости (0.50 означает -50% скорости атаки, руку поднимает медленнее)")
                .defineInRange("exhaustedAttackPenalty", 0.50d, 0.0d, 1.0d);

        // Дефолтные значения для выносливости оружия
        List<String> defaultWeaponStamina = new ArrayList<>();
        defaultWeaponStamina.add("minecraft:mace=18.0");
        defaultWeaponStamina.add("minecraft:spear=18.0");
        defaultWeaponStamina.add("minecraft:iron_sword=4.0");
        defaultWeaponStamina.add("minecraft:diamond_axe=6.0");
        defaultWeaponStamina.add("minecraft:netherite_sword=4.5");
        defaultWeaponStamina.add("minecraft:diamond_sword=4.0");
        defaultWeaponStamina.add("golden_sword=2.0");
        defaultWeaponStamina.add("minecraft:stone_sword=3.0");
        defaultWeaponStamina.add("minecraft:wooden_sword=1.5");
        defaultWeaponStamina.add("minecraft:netherite_axe=6.5");
        defaultWeaponStamina.add("minecraft:iron_axe=5.0");
        defaultWeaponStamina.add("minecraft:stone_axe=4.5");
        defaultWeaponStamina.add("minecraft:wooden_axe=3.0");
        defaultWeaponStamina.add("#minecraft:swords=3.5");
        defaultWeaponStamina.add("#minecraft:axes=5.0");

        CUSTOM_WEAPON_STAMINA = BUILDER.comment("Трата стамины для оружия/инструментов при ударах (формат: id_мода:id_предмета=значение ИЛИ #тег=значение)")
                .defineListAllowEmpty("customWeaponStamina", defaultWeaponStamina, o -> o instanceof String);

        BUILDER.pop();

        // ==========================================
        // НАСТРОЙКИ ВЕСА И ДЕБАФОВ
        // ==========================================
        BUILDER.comment("Настройки переносимого веса").push("weight");
        MAX_CARRY_WEIGHT = BUILDER.comment("Максимальный комфортный вес игрока в кг (100% нагрузки)").defineInRange("maxCarryWeight", 50.0d, 1.0d, 1000.0d);
        MAX_SPEED_PENALTY = BUILDER.comment("Максимальный дебаф скорости перемещения при сильном перегрузе").defineInRange("maxSpeedPenalty", -0.8d, -1.0d, 0.0d);

        WEIGHT_SPRINT_DRAIN_IMPACT = BUILDER.comment("Множитель влияния веса на трату стамины при беге. При 1.0 — ванильное влияние. При 0.0 — вес вообще НЕ увеличивает трату стамины.")
                .defineInRange("weightSprintDrainImpact", 1.0d, 0.0d, 10.0d);

        WEIGHT_REGEN_PENALTY_IMPACT = BUILDER.comment("Множитель влияния веса на замедление регенерации стамины. При 1.0 — ванильное замедление. При 0.0 — вес НЕ замедляет регенерацию.")
                .defineInRange("weightRegenPenaltyImpact", 1.0d, 0.0d, 10.0d);

        // Дефолтные значения для веса предметов
        List<String> defaultWeights = new ArrayList<>();
        defaultWeights.add("minecraft:diamond_block=12.5");
        defaultWeights.add("minecraft:netherite_block=20.0");
        defaultWeights.add("minecraft:iron_block=4.0");
        defaultWeights.add("minecraft:gold_block=4.0");
        defaultWeights.add("minecraft:copper_block=4.0");
        defaultWeights.add("minecraft:exposed_copper=4.0");
        defaultWeights.add("minecraft:weathered_copper=4.0");
        defaultWeights.add("minecraft:oxidized_copper=4.0");
        defaultWeights.add("minecraft:waxed_iron_block=4.0");
        defaultWeights.add("minecraft:waxed_gold_block=4.0");
        defaultWeights.add("minecraft:waxed_copper_block=4.0");
        defaultWeights.add("#minecraft:stone_blocks=1.0");
        defaultWeights.add("#minecraft:logs=0.5");
        defaultWeights.add("#neoforge:armors/netherite=8.0");
        defaultWeights.add("#neoforge:armors/gold=6.0");
        defaultWeights.add("#neoforge:armors/iron=5.0");
        defaultWeights.add("#neoforge:armors/diamond=4.0");
        defaultWeights.add("minecraft:leather_helmet=1.5");
        defaultWeights.add("minecraft:leather_chestplate=1.5");
        defaultWeights.add("minecraft:leather_leggings=1.5");
        defaultWeights.add("minecraft:leather_boots=1.5");
        defaultWeights.add("minecraft:chainmail_helmet=1.5");
        defaultWeights.add("minecraft:chainmail_chestplate=1.5");
        defaultWeights.add("minecraft:chainmail_leggings=1.5");
        defaultWeights.add("minecraft:chainmail_boots=1.5");
        defaultWeights.add("quark:backpack=2.0");

        CUSTOM_ITEM_WEIGHTS = BUILDER.comment("Кастомный вес предметов. Формат: id_мода:id_предмета=вес ИЛИ #тег:имя=вес")
                .defineListAllowEmpty("customItemWeights", defaultWeights, o -> o instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
