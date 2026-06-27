package com.zhigichan_31.thermodynamics.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zhigichan_31.thermodynamics.Thermodynamics;
import com.zhigichan_31.thermodynamics.data.ModDataAttachments;
import com.zhigichan_31.thermodynamics.data.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = Thermodynamics.MODID, value = Dist.CLIENT)
public class StatBarsOverlay {
    private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thermodynamics.MODID, "textures/gui/hud_elements.png");

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH) || event.getName().equals(VanillaGuiLayers.FOOD_LEVEL) || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL) || event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPost(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        Minecraft mc = Minecraft.getInstance(); LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) return;
        PlayerData data = player.getData(ModDataAttachments.PLAYER_DATA.get());
        GuiGraphics graphics = event.getGuiGraphics(); Font font = mc.font;
        int width = graphics.guiWidth(), height = graphics.guiHeight(), centerX = width / 2;
        int leftX = centerX - 102, rightX = centerX + 21, bottomY = height - 40, topY = height - 51, airY = height - 62;

        RenderSystem.enableBlend(); RenderSystem.defaultBlendFunc();
        CenterModuleRenderer.render(graphics, font, centerX, height, data, HUD_TEXTURE);
        SideWingsRenderer.renderLeft(graphics, font, player, leftX, bottomY, topY, data, HUD_TEXTURE);
        SideWingsRenderer.renderRight(graphics, font, player, rightX, bottomY, topY, airY, HUD_TEXTURE);
        RenderSystem.disableBlend();
    }
}
