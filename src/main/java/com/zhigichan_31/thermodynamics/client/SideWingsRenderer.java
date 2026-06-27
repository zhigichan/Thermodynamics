package com.zhigichan_31.thermodynamics.client;

import com.zhigichan_31.thermodynamics.data.PlayerData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

import java.lang.reflect.Method;
import java.util.Map;

@EventBusSubscriber(modid = "thermodynamics", value = net.neoforged.api.distmarker.Dist.CLIENT)
public class SideWingsRenderer {

    public static final ResourceLocation TWT_THIRST_ID = ResourceLocation.fromNamespaceAndPath("thirst", "player_thirst");
    private static final ResourceLocation FD_ID = ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");

    // ОПТИМИЗАЦИЯ РЕФЛЕКСИИ: ссылки на методы кэшируются при первом вызове
    private static Method COLD_SWEAT_GET_TRAITS = null;
    private static boolean COLD_SWEAT_REF_CHECKED = false;

    private static Method THIRST_GET_THIRST = null;
    private static Method THIRST_GET_QUENCHED = null;
    private static Method THIRST_GET_HYDRATION = null;
    private static boolean THIRST_REF_CHECKED = false;

    // ОПТИМИЗАЦИЯ СТРОК: массивы для мгновенного получения текста без .formatted()
    private static final String[] STAMINA_CACHE = new String[101];
    private static final String[] FOOD_CACHE = new String[21];

    static {
        for (int i = 0; i <= 100; i++) {
            STAMINA_CACHE[i] = i + "%";
        }
        for (int i = 0; i <= 20; i++) {
            FOOD_CACHE[i] = i + " / 20";
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre e) {
        if (e.getName().equals(FD_ID)) e.setCanceled(true);
    }
    // ==========================================
    // ЛЕВАЯ ПАНЕЛЬ GUI (ЗДОРОВЬЕ И СТАМИНА)
    // ==========================================
    public static void renderLeft(GuiGraphics g, Font f, LocalPlayer p, int lX, int bY, int tY, PlayerData d, ResourceLocation tex) {
        float hp = p.getHealth(), max = p.getMaxHealth(), abs = p.getAbsorptionAmount();
        double off = -50.0;

        try {
            var att = net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(ResourceLocation.fromNamespaceAndPath("cold_sweat", "entity_temperature"));
            if (att != null && p.hasData(att)) {
                Object obj = p.getData(att);

                if (!COLD_SWEAT_REF_CHECKED) {
                    try {
                        COLD_SWEAT_GET_TRAITS = obj.getClass().getMethod("getTraits");
                    } catch (Exception ignored) {}
                    COLD_SWEAT_REF_CHECKED = true;
                }

                if (COLD_SWEAT_GET_TRAITS != null) {
                    Map<?, ?> m = (Map<?, ?>) COLD_SWEAT_GET_TRAITS.invoke(obj);
                    if (m != null && !m.isEmpty()) {
                        Object k = null;
                        for (Object key : m.keySet()) {
                            if ("CORE".equals(key.toString())) {
                                k = key;
                                break;
                            }
                        }
                        if (k != null && m.get(k) instanceof Number n) {
                            off = n.doubleValue() - 50.0;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        g.pose().pushPose();
        g.pose().translate(lX, 0, 0);
        g.blit(tex, 0, bY, 0.0F, 0.0F, 81, 9, 256, 256);
        float hU = 1.0F, hV = 19.0F;
        int hH = 7, hX = 1, hY = 1, mW = 79;
        boolean db = p.hasEffect(MobEffects.POISON) || p.hasEffect(MobEffects.WITHER);
        if (p.hasEffect(MobEffects.POISON)) {
            hU = 81.0F; hV = 9.0F; hH = 9; hX = 0; hY = 0; mW = 81;
        } else if (p.hasEffect(MobEffects.WITHER)) {
            hU = 81.0F; hV = 18.0F; hH = 9; hX = 0; hY = 0; mW = 81;
        }
        int hW = (int) (mW * (hp / max));
        if (hW > 0) g.blit(tex, hX, bY + hY, hU, hV, hW, hH, 256, 256);
        if (!db) {
            int mpW = (int) (81 * (hp / max));
            if (off <= -100.0) {
                int cW = (int) (mpW * net.minecraft.util.Mth.clamp((float) ((Math.abs(off) - 100.0) / 50.0), 0f, 1f));
                if (cW > 0) g.blit(tex, 0, bY, 81.0F, 27.0F, cW, 9, 256, 256);
            } else if (off >= 0.0) {
                int hFill = (int) (mpW * net.minecraft.util.Mth.clamp((float) (off / 50.0), 0f, 1f));
                if (hFill > 0) g.blit(tex, 0, bY, 81.0F, 36.0F, hFill, 9, 256, 256);
            }
        }
        if (abs > 0.0f) {
            int aW = (int) (81 * Math.max(0.0f, Math.min(1.0f, abs / max)));
            if (aW > 0) g.blit(tex, 0, bY, 81.0F, 45.0F, aW, 9, 256, 256);
        }
        int arm = p.getArmorValue();
        if (arm > 0) {
            int arW = (int) (81 * Math.max(0.0f, Math.min(1.0f, (float) arm / 20.0f)));
            if (arW > 0) g.blit(tex, 0, bY, 81.0F, 0.0F, arW, 9, 256, 256);
        }
        g.blit(tex, 0, tY, 0.0F, 0.0F, 81, 9, 256, 256);

        float staminaVal = d.getStamina();
        int sW = (int) (79 * Math.max(0.0f, Math.min(1.0f, staminaVal / 100.0f)));
        if (sW > 0) {
            if (d.isExhausted()) g.setColor(0.45F, 0.45F, 0.45F, 1.0F);
            g.blit(tex, 1, tY + 1, 1.0F, 46.0F, sW, 7, 256, 256);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        g.pose().popPose();

        int staminaInt = (int) staminaVal;
        String sStr = (staminaInt >= 0 && staminaInt <= 100) ? STAMINA_CACHE[staminaInt] : (staminaInt + "%");

        StringBuilder hpBuilder = new StringBuilder();
        hpBuilder.append(String.format(java.util.Locale.ROOT, "%.1f", hp));
        if (abs > 0.0f) {
            hpBuilder.append("(+").append((int)abs).append(")");
        }
        hpBuilder.append(" / ").append((int)max);
        String hpStr = hpBuilder.toString();

        g.pose().pushPose();
        g.pose().translate(lX, tY + 2, 0);
        g.pose().scale(0.66f, 0.66f, 1.0f);
        g.drawString(f, sStr, (int) (((81 - (f.width(sStr) * 0.66f)) / 2) / 0.66f), 0, d.isExhausted() ? 0xFF555555 : 0xFFFFFF, true);
        g.pose().popPose();

        g.pose().pushPose();
        g.pose().translate(lX, bY + 2, 0);
        g.pose().scale(0.66f, 0.66f, 1.0f);
        g.drawString(f, hpStr, (int) (((81 - (f.width(hpStr) * 0.66f)) / 2) / 0.66f) + 1, 0, 0xFFFFFF, true);
        g.pose().popPose();

        if (arm > 0) {
            g.pose().pushPose();
            g.pose().translate(lX, bY + 2, 0);
            g.pose().scale(0.66f, 0.66f, 1.0f);
            String aStr = "[🛡" + arm + "]";
            g.drawString(f, aStr, -(f.width(aStr) + 4), 0, 0xDDDDDD, true);
            g.pose().popPose();
        }
    }
    // ==========================================
    // ПРАВАЯ ПАНЕЛЬ GUI (ГОЛОД И ЖАЖДА)
    // ==========================================
    public static void renderRight(GuiGraphics g, Font f, LocalPlayer p, int rX, int bY, int tY, int aY, ResourceLocation tex) {
        g.pose().pushPose();
        g.pose().translate(rX, 0, 0);
        g.blit(tex, 0, bY, 0.0F, 0.0F, 81, 9, 256, 256);
        int fL = p.getFoodData().getFoodLevel();
        float fPct = Math.max(0.0f, Math.min(1.0f, fL / 20.0f));
        int fdW = (int) (79 * fPct);
        if (fdW > 0) g.blit(tex, 1, bY + 1, 1.0F, 28.0F, fdW, 7, 256, 256);

        float fS = p.getFoodData().getSaturationLevel();
        if (fS > 0.0f) {
            int fSW = (int) (81 * Math.max(0.0f, Math.min(1.0f, fS / 20.0f)));
            if (fSW > 0) g.blit(tex, 0, bY, 81.0F, 63.0F, fSW, 9, 256, 256);
        }

        var reg = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT;
        var eff = reg.get(FD_ID);
        if (eff != null && p.hasEffect(reg.wrapAsHolder(eff))) {
            int nW = (int) (81 * fPct);
            if (nW > 0) g.blit(tex, 0, bY, 81.0F, 72.0F, nW, 9, 256, 256);
        }

        g.blit(tex, 0, tY, 0.0F, 0.0F, 81, 9, 256, 256);
        float cT = 20.0f, tS = 0.0f;

        try {
            var att = net.neoforged.neoforge.registries.NeoForgeRegistries.ATTACHMENT_TYPES.get(TWT_THIRST_ID);
            if (att != null && p.hasData(att)) {
                Object cap = p.getData(att);

                if (!THIRST_REF_CHECKED) {
                    try {
                        THIRST_GET_THIRST = cap.getClass().getMethod("getThirst");
                        try {
                            THIRST_GET_QUENCHED = cap.getClass().getMethod("getQuenched");
                        } catch (Exception e) {
                            THIRST_GET_HYDRATION = cap.getClass().getMethod("getHydration");
                        }
                    } catch (Exception ignored) {}
                    THIRST_REF_CHECKED = true;
                }

                if (THIRST_GET_THIRST != null) {
                    cT = ((Number) THIRST_GET_THIRST.invoke(cap)).floatValue();
                    if (THIRST_GET_QUENCHED != null) {
                        tS = ((Number) THIRST_GET_QUENCHED.invoke(cap)).floatValue();
                    } else if (THIRST_GET_HYDRATION != null) {
                        tS = ((Number) THIRST_GET_HYDRATION.invoke(cap)).floatValue();
                    }
                }
            }
        } catch (Exception e) {
            try {
                net.minecraft.nbt.CompoundTag nbt = p.getPersistentData();
                if (nbt.contains("neoforge:attachments") && nbt.getCompound("neoforge:attachments").contains("thirst:player_thirst")) {
                    net.minecraft.nbt.CompoundTag tNbt = nbt.getCompound("neoforge:attachments").getCompound("thirst:player_thirst");
                    cT = tNbt.getInt("thirst");
                    tS = tNbt.getInt("quenched");
                }
            } catch (Exception ignored) {}
        }

        int tW = (int) (79 * Math.max(0.0f, Math.min(1.0f, cT / 20.0f)));
        if (tW > 0) {
            if (cT < 5.0f) g.setColor(Math.max(0.2f, cT / 5.0f), Math.max(0.2f, cT / 5.0f), 1.0f, 1.0f);
            g.blit(tex, 1, tY + 1, 1.0F, 10.0F, tW, 7, 256, 256);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (tS > 0.0f) {
            int tSW = (int) (81 * Math.max(0.0f, Math.min(1.0f, tS / 20.0f)));
            if (tSW > 0) g.blit(tex, 0, tY, 81.0F, 54.0F, tSW, 9, 256, 256);
        }
        int aS = p.getAirSupply(), mI = p.getMaxAirSupply();
        if (aS < mI) {
            g.blit(tex, 0, aY, 0.0F, 0.0F, 81, 9, 256, 256);
            int aW = (int) (79 * Math.max(0.0f, Math.min(1.0f, (float) aS / (float) mI)));
            if (aW > 0) g.blit(tex, 1, aY + 1, 1.0F, 37.0F, aW, 7, 256, 256);
        }
        g.pose().popPose();

        String tStr = String.format(java.util.Locale.ROOT, "%.1f / 20", cT);
        String fStr = (fL >= 0 && fL <= 20) ? FOOD_CACHE[fL] : (fL + " / 20");

        g.pose().pushPose();
        g.pose().translate(rX, tY + 2, 0);
        g.pose().scale(0.66f, 0.66f, 1.0f);
        g.drawString(f, tStr, (int) (((81 - (f.width(tStr) * 0.66f)) / 2) / 0.66f), 0, 0xFFFFFF, true);
        g.pose().popPose();

        if (tS > 0.0f) {
            g.pose().pushPose();
            g.pose().translate(rX, tY + 2, 0);
            g.pose().scale(0.66f, 0.66f, 1.0f);
            String tsS = "[💧+" + String.format(java.util.Locale.ROOT, "%.1f", tS) + "]";
            g.drawString(f, tsS, (int) (83 / 0.66f), 0, 0x55FFFF, true);
            g.pose().popPose();
        }

        g.pose().pushPose();
        g.pose().translate(rX, bY + 2, 0);
        g.pose().scale(0.66f, 0.66f, 1.0f);
        g.drawString(f, fStr, (int) (((81 - (f.width(fStr) * 0.66f)) / 2) / 0.66f), 0, 0xFFFFFF, true);
        g.pose().popPose();

        if (fS > 0.0f) {
            g.pose().pushPose();
            g.pose().translate(rX, bY + 2, 0);
            g.pose().scale(0.66f, 0.66f, 1.0f);
            String fsS = "[🍖+" + (int)fS + "]";
            g.drawString(f, fsS, (int) (83 / 0.66f), 0, 0xFFDD55, true);
            g.pose().popPose();
        }

        if (aS < mI) {
            g.pose().pushPose();
            g.pose().translate(rX, aY + 2, 0);
            g.pose().scale(0.66f, 0.66f, 1.0f);
            String aStr = "🫧 " + (int) (((float) aS / mI) * 100f) + "%";
            g.drawString(f, aStr, (int) (((81 - (f.width(aStr) * 0.66f)) / 2) / 0.66f), 0, 0x88FFFF, true);
            g.pose().popPose();
        }
    }
}
