package com.zhigichan_31.thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.systems.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;

@Mod(Thermodynamics.MODID)
public class Thermodynamics {
    public static final String MODID = "thermodynamics";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MODID);

    public Thermodynamics(IEventBus modEventBus) {
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerConfig(Type.SERVER, ModConfig.SPEC);
        ModDataAttachments.register(modEventBus);
    }
}
