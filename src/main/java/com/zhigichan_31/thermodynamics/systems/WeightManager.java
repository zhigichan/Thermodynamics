package com.zhigichan_31.thermodynamics.systems;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.api.IWeightProvider;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.network.NetworkHandler;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid = Thermodynamics.MODID)
@SuppressWarnings("resource")
public class WeightManager {
    private static final List<IWeightProvider> addons = new ArrayList<>();
    private static final ResourceLocation SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "weight_penalty");
    public static final ResourceLocation ATTACK_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "exhaustion_attack_slowness");

    public static void registerAddon(IWeightProvider addon) {
        if (addon != null && !addons.contains(addon)) addons.add(addon);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) recalculateWeight(sp);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) recalculateWeight(sp);
    }

    @SubscribeEvent
    public static void onServerPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide || !(p instanceof ServerPlayer sp)) return;

        if (!sp.gameMode.isSurvival()) {
            var sAt = p.getAttribute(Attributes.MOVEMENT_SPEED);
            var aAt = p.getAttribute(Attributes.ATTACK_SPEED);

            // Защита: удаляем модификаторы ТОЛЬКО если они реально присутствуют
            if (sAt != null && sAt.hasModifier(SPEED_MODIFIER_ID)) sAt.removeModifier(SPEED_MODIFIER_ID);
            if (aAt != null && aAt.hasModifier(ATTACK_SPEED_MODIFIER_ID)) aAt.removeModifier(ATTACK_SPEED_MODIFIER_ID);

            PlayerData data = sp.getData(ModDataAttachments.PLAYER_DATA.get());
            if (data.getWeight() > 0) {
                data.setWeight(0.0f);
                NetworkHandler.sendToPlayer(sp);
            }
            return;
        }

        if (sp.tickCount % 5 == 0) {
            recalculateWeight(sp);
        }
    }

    public static void recalculateWeight(ServerPlayer player) {
        float totalWeight = 0.0f;
        for (ItemStack stack : player.getInventory().items) if (!stack.isEmpty()) totalWeight += calculateItemWeight(stack) * stack.getCount();
        for (ItemStack armorStack : player.getInventory().armor) if (!armorStack.isEmpty()) totalWeight += calculateItemWeight(armorStack);
        for (ItemStack offhandStack : player.getInventory().offhand) if (!offhandStack.isEmpty()) totalWeight += calculateItemWeight(offhandStack) * offhandStack.getCount();

        for (IWeightProvider addon : addons) totalWeight += addon.getTotalWeight(player);

        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        float oldWeight = data.getWeight();

        // Обновляем данные и шлём пакет ТОЛЬКО если вес изменился
        if (Math.abs(oldWeight - totalWeight) > 0.001f) {
            data.setWeight(totalWeight);
            NetworkHandler.sendToPlayer(player);
        }

        updateAttackSpeed(player, data.isExhausted());
        updatePlayerSpeed(player, totalWeight);
    }

    public static float calculateItemWeight(ItemStack stack) {
        return stack.isEmpty() ? 0.0f : ConfigParser.getItemWeight(stack, 1.0f);
    }

    public static float getStaminaModifier(Player player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        float currentWeight = data.getWeight(), baseModifier = 1.0f + (currentWeight / ModConfig.MAX_CARRY_WEIGHT.get().floatValue());
        for (IWeightProvider addon : addons) baseModifier *= addon.getStaminaDrainModifier(player);
        return baseModifier;
    }

    private static void updatePlayerSpeed(Player player, float weight) {
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null || !(player instanceof ServerPlayer serverPlayer)) return;

        float overWeightRatio = weight / ModConfig.MAX_CARRY_WEIGHT.get().floatValue();

        if (overWeightRatio > 1.0f) {
            float speedPenalty = Math.max(-((int) overWeightRatio * 0.25f), ModConfig.MAX_SPEED_PENALTY.get().floatValue());

            // Проверяем существующий модификатор, чтобы не пересоздавать его каждый раз
            AttributeModifier existing = speedAttr.getModifier(SPEED_MODIFIER_ID);
            if (existing == null || Math.abs(existing.amount() - speedPenalty) > 0.001f) {
                speedAttr.removeModifier(SPEED_MODIFIER_ID);
                speedAttr.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID, speedPenalty, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(serverPlayer.getId(), Collections.singletonList(speedAttr)));
            }
        } else {
            // Если перевеса нет, удаляем штраф ОДИН раз, а не каждый тик
            if (speedAttr.hasModifier(SPEED_MODIFIER_ID)) {
                speedAttr.removeModifier(SPEED_MODIFIER_ID);
                serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(serverPlayer.getId(), Collections.singletonList(speedAttr)));
            }
        }
    }

    public static void updateAttackSpeed(Player player, boolean isExhausted) {
        AttributeInstance attackSpeedAttr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attackSpeedAttr == null || !(player instanceof ServerPlayer serverPlayer)) return;

        boolean hasModifier = attackSpeedAttr.hasModifier(ATTACK_SPEED_MODIFIER_ID);

        if (isExhausted) {
            if (!hasModifier) {
                attackSpeedAttr.addTransientModifier(new AttributeModifier(ATTACK_SPEED_MODIFIER_ID, -0.50f, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(serverPlayer.getId(), Collections.singletonList(attackSpeedAttr)));
            }
        } else {
            if (hasModifier) {
                attackSpeedAttr.removeModifier(ATTACK_SPEED_MODIFIER_ID);
                serverPlayer.connection.send(new ClientboundUpdateAttributesPacket(serverPlayer.getId(), Collections.singletonList(attackSpeedAttr)));
            }
        }
    }
}
