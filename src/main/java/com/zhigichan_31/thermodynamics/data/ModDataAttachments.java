package com.zhigichan_31.thermodynamics.data;
import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public class ModDataAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Thermodynamics.MODID);

    private static final Codec<PlayerData> PLAYER_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("stamina").forGetter(PlayerData::getStamina),
                    Codec.FLOAT.fieldOf("weight").forGetter(PlayerData::getWeight),
                    Codec.BOOL.optionalFieldOf("is_exhausted", false).forGetter(PlayerData::isExhausted)
            ).apply(instance, PlayerData::new)
    );

    public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA = ATTACHMENT_TYPES.register("player_data", () -> AttachmentType.builder(PlayerData::new).serialize(PLAYER_DATA_CODEC).copyOnDeath().build());
    public static void register(IEventBus eventBus) { ATTACHMENT_TYPES.register(eventBus); }
}
