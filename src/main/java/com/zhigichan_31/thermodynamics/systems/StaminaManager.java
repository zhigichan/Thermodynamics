package com.zhigichan_31.thermodynamics.systems;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid = Thermodynamics.MODID)
@SuppressWarnings("resource")
public class StaminaManager {
    private static final HashMap<UUID, Integer> regenCooldowns = new HashMap<>();
    private static final HashSet<UUID> playersThrowingItem = new HashSet<>();
    private static final HashMap<UUID, BlockPos> activeMiningPositions = new HashMap<>();

    private static final ResourceLocation TWT_THIRST_ID = ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst");
    private static final ResourceLocation EXHAUSTED_MINING_UUID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "exhausted_mining_penalty");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) return;
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        UUID uuid = player.getUUID();
        boolean isChanged = false;

        // Исправленная проверка режима игры для NeoForge 1.21.1
        if (serverPlayer.isCreative() || serverPlayer.isSpectator()) {
            AttributeInstance mAt = serverPlayer.getAttribute(Attributes.BLOCK_BREAK_SPEED);
            if (mAt != null && mAt.hasModifier(EXHAUSTED_MINING_UUID)) mAt.removeModifier(EXHAUSTED_MINING_UUID);
            regenCooldowns.remove(uuid);
            playersThrowingItem.remove(uuid);
            activeMiningPositions.remove(uuid);
            if (data.isExhausted() || data.getStamina() < 100.0f) {
                data.setExhausted(false);
                data.setStamina(100.0f);
                NetworkHandler.sendToPlayer(serverPlayer);
            }
            return;
        }

        float recoveryAt = ModConfig.STAMINA_RECOVERY_THRESHOLD.get().floatValue();

        // Порог усталости для дебаффов меча и кирки (строго на 0%)
        if (data.getStamina() <= 0.0f && !data.isExhausted()) {
            data.setExhausted(true);
            isChanged = true;
            if (player.isSprinting()) player.setSprinting(false);
            WeightManager.updateAttackSpeed(serverPlayer, true); // Метод по-прежнему вызывается из WeightManager
        }
        if (data.isExhausted() && data.getStamina() >= recoveryAt) {
            data.setExhausted(false);
            isChanged = true;
            WeightManager.updateAttackSpeed(serverPlayer, false); // Метод по-прежнему вызывается из WeightManager
        }

        AttributeInstance miningSpeedAttr = serverPlayer.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (miningSpeedAttr != null) {
            if (data.isExhausted()) {
                if (!miningSpeedAttr.hasModifier(EXHAUSTED_MINING_UUID)) {
                    miningSpeedAttr.addPermanentModifier(new AttributeModifier(EXHAUSTED_MINING_UUID, -ModConfig.EXHAUSTED_MINING_PENALTY.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
            } else if (miningSpeedAttr.hasModifier(EXHAUSTED_MINING_UUID)) {
                miningSpeedAttr.removeModifier(EXHAUSTED_MINING_UUID);
            }
        }

        int cooldown = regenCooldowns.getOrDefault(uuid, 0);
        if (cooldown > 0) {
            cooldown--;
            regenCooldowns.put(uuid, cooldown);
        }

        // КОД ВСКАПЫВАНИЯ БЛОКОВ
        if (activeMiningPositions.containsKey(uuid)) {
            data.setStamina(Math.max(0.0f, data.getStamina() - ModConfig.BLOCK_BREAK_HARDNESS_MULTIPLIER.get().floatValue()));
            cooldown = ModConfig.REGEN_COOLDOWN.get();
            regenCooldowns.put(uuid, cooldown);
            isChanged = true;
            activeMiningPositions.remove(uuid);
        }

        // ЛОГИКА БЕГА И РЕГЕНЕРАЦИИ
        if (player.isSprinting() && data.getStamina() > 0.0f) {
            float rawWeightModifier = WeightManager.getStaminaModifier(player);
            float finalWeightModifier = 1.0f + ((rawWeightModifier - 1.0f) * ModConfig.WEIGHT_SPRINT_DRAIN_IMPACT.get().floatValue());
            data.setStamina(Math.max(0.0f, data.getStamina() - (ModConfig.SPRINT_DRAIN.get().floatValue() * finalWeightModifier)));
            cooldown = ModConfig.REGEN_COOLDOWN.get();
            regenCooldowns.put(uuid, cooldown);
            isChanged = true;
        } else if (data.getStamina() < 100.0f && cooldown <= 0) {
            float statusModifier = 1.0f, penalty = ModConfig.HUNGER_THIRST_PENALTY.get().floatValue();
            try {
                var thirstType = net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(TWT_THIRST_ID);
                if (thirstType != null && serverPlayer.hasData(thirstType)) {
                    Object thirstCap = serverPlayer.getData(thirstType);
                    if (((Number) thirstCap.getClass().getMethod("getThirst").invoke(thirstCap)).floatValue() < 6.0f) statusModifier -= penalty;
                }
            } catch (Exception ignored) {}
            if (serverPlayer.getFoodData().getFoodLevel() < 6) statusModifier -= penalty;
            statusModifier = Math.max(0.0f, statusModifier);

            float movementFactor = player.getDeltaMovement().horizontalDistanceSqr() > 0.001 ? ModConfig.MOVING_REGEN_MULTIPLIER.get().floatValue() : 1.0f;
            float rawWeightModifier = WeightManager.getStaminaModifier(player);
            float finalWeightModifier = 1.0f + ((rawWeightModifier - 1.0f) * ModConfig.WEIGHT_REGEN_PENALTY_IMPACT.get().floatValue());
            data.setStamina(Math.min(100.0f, data.getStamina() + (((ModConfig.REGEN_BASE.get().floatValue() * statusModifier) / finalWeightModifier) * movementFactor)));
            isChanged = true;
        }

        if (isChanged) NetworkHandler.sendToPlayer(serverPlayer);
    }

    // Нативный перехват ударов по существам на сервере
    @SubscribeEvent
    public static void onEntityAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            processServerSwing(serverPlayer);
        }
    }

    // Вызывается напрямую из пакета клика по воздуху и из события AttackEntityEvent
    public static void processServerSwing(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (playersThrowingItem.contains(uuid)) {
            playersThrowingItem.remove(uuid);
            return;
        }

        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        float finalDrain = ConfigParser.getWeaponDrain(player.getMainHandItem(), ModConfig.BASE_SWING_DRAIN.get().floatValue());
        float currentScale = player.getAttackStrengthScale(0.0F);
        float scaleModifier = Math.max(ModConfig.MIN_SWING_SCALE_DRAIN.get().floatValue(), currentScale);

        data.setStamina(Math.max(0.0f, data.getStamina() - (finalDrain * scaleModifier)));
        regenCooldowns.put(uuid, ModConfig.REGEN_COOLDOWN.get());
        NetworkHandler.sendToPlayer(player);
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            playersThrowingItem.add(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onMiningTickUpdate(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getPosition().isPresent() && !serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
            activeMiningPositions.put(serverPlayer.getUUID(), event.getPosition().get());
        }
    }
}
