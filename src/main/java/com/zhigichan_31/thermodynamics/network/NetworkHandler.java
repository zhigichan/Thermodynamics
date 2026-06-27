package com.zhigichan_31.thermodynamics.network;

import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import com.zhigichan_31.thermodynamics.systems.StaminaManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Thermodynamics.MODID)
public class NetworkHandler {

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        // Регистрация вашего первого пакета данных стамины
        event.registrar(Thermodynamics.MODID).versioned("1.0.0").playToClient(
                ClientboundSyncDataPacket.TYPE, ClientboundSyncDataPacket.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
                        data.setStamina(packet.stamina());
                        data.setWeight(packet.weight());
                        data.setExhausted(packet.isExhausted());
                    }
                })
        );

        // ⚔️ 🔥 РЕГИСТРАЦИЯ ПАКЕТА ВЗМАХА В ВОЗДУХЕ (От Клиента к Серверу)
        event.registrar(Thermodynamics.MODID).versioned("1.0.0").playToServer(
                ServerboundAirSwingPayload.TYPE, ServerboundAirSwingPayload.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    // Код выполняется строго на СЕРВЕРЕ
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        // Вызываем обработку взмаха напрямую, минуя тяжелые тики
                        StaminaManager.processServerSwing(serverPlayer);
                    }
                })
        );
    }

    public static void sendToPlayer(ServerPlayer player) {
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        PacketDistributor.sendToPlayer(player, new ClientboundSyncDataPacket(data.getStamina(), data.getWeight(), data.isExhausted()));
    }

}
