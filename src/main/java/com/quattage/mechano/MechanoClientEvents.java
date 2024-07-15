package com.quattage.mechano;

import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderWrenchBehavior;
import com.quattage.mechano.foundation.behavior.ClientBehavior;
import com.quattage.mechano.foundation.behavior.GridEdgeDebugBehavior;
import com.quattage.mechano.foundation.electricity.grid.WireAnchorSelectionManager;
import com.quattage.mechano.foundation.electricity.rendering.AnchorOverlayRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    public static final DiagonalGirderWrenchBehavior GIRDER_BEHAVIOR = new DiagonalGirderWrenchBehavior("WrenchOnDiagonalGirder");
    public static final GridEdgeDebugBehavior WIRE_DEBUG_BEHAVIOR = new GridEdgeDebugBehavior("DebugMenuElectricNode");

    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
		if(!isGameActive())
			return;

        for(ClientBehavior behavior : ClientBehavior.behaviors.values()) {
            behavior.tick();
        }
    }

    @SuppressWarnings({"resource" })
    protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Mechano.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onReisterReloadListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(MechanoClient.WIRE_TEXTURE_PROVIDER);
            event.registerReloadListener(MechanoClient.HITBOX_PROVIDER);
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("anchor_overlay", AnchorOverlayRenderer.FUNC_INSTANCE);
        }
    }
}
