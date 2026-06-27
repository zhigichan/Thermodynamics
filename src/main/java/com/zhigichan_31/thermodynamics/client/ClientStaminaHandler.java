package com.zhigichan_31.thermodynamics.client;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Thermodynamics.MODID, value = Dist.CLIENT)
public class ClientStaminaHandler {

    // Флаг блокировки спринта до тех пор, пока кнопка зажата
    private static boolean isSprintBlocked = false;

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Player basePlayer = event.getEntity();
        if (basePlayer instanceof LocalPlayer player) {
            PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());

            boolean isSprintKeyPressed = Minecraft.getInstance().options.keySprint.isDown();

            // Если стамина закончилась — включаем блокировку
            if (data.getStamina() <= 0.0f) {
                isSprintBlocked = true;
            }

            // Если спринт заблокирован и игрок ВСЁ ЕЩЕ держит кнопку
            if (isSprintBlocked && isSprintKeyPressed) {
                // Принудительно гасим статус бега у игрока
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                // Аппаратно отжимаем кнопку бега в настройках
                Minecraft.getInstance().options.keySprint.setDown(false);
            }

            // Сбрасываем блокировку, только когда игрок полностью ОТПУСТИЛ кнопку бега
            if (!isSprintKeyPressed) {
                isSprintBlocked = false;
            }
        }
    }
    @SubscribeEvent
    public static void onClientAirClick(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer player) {
            // Проверяем, что игрок НЕ в креативе и НЕ в наблюдателе (значит, он в выживании/приключении)
            if (!player.isCreative() && !player.isSpectator()) {
                // Мгновенно шлём пакет на сервер при клике по воздуху
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.zhigichan_31.thermodynamics.network.ServerboundAirSwingPayload());
            }
        }
    }
}
