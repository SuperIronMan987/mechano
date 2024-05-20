package com.quattage.mechano;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.foundation.block.upgradable.UpgradeCache;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGridProvider;
import com.quattage.mechano.foundation.electricity.grid.GridClientCache;
import com.quattage.mechano.foundation.electricity.grid.GridClientCacheProvider;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.tterrag.registrate.providers.DataGenContext;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Mechano.MOD_ID)
public class Mechano {
    
    public static final String MOD_ID = "mechano";
    public static final String ESC = "\u001b";

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.MOD_ID);
    public static final Capability<GlobalTransferGrid> SERVER_GRID_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<GridClientCache> CLIENT_CACHE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final UpgradeCache UPGRADES = new UpgradeCache();

    public Mechano() {
        Mechano.LOGGER.info("loading mechano");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        REGISTRATE.registerEventListeners(modBus);
        MechanoSettings.init(modBus);
        MechanoBlocks.register(modBus);
        MechanoItems.register(modBus);
        MechanoBlockEntities.register(modBus);
        
        MechanoRecipes.register(modBus);
        MechanoSounds.register(modBus);
        MechanoGroups.register(modBus);
        MechanoMenus.register(modBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MechanoClient.init(modBus, forgeBus));

        modBus.addListener(this::onCommonSetup);
        forgeBus.addGenericListener(Level.class, this::addWorldCapabilities);
    }

    public void onCommonSetup(final FMLCommonSetupEvent event) {
        MechanoPackets.register();
    }

    @SuppressWarnings({"resource"})
    public void addWorldCapabilities(AttachCapabilitiesEvent<Level> event) {
        if(event.getObject().isClientSide) {
            Mechano.LOGGER.info("Attaching ClientCache capability to " + event.getObject().dimension().location());
            event.addCapability(asResource("transfer_grid_client_cache"), new GridClientCacheProvider((ClientLevel)event.getObject()));
        } else {
            Mechano.LOGGER.info("Attaching ServerGrid capability to " + event.getObject().dimension().location());
            event.addCapability(asResource("transfer_grid_server_manager"), new GlobalTransferGridProvider(event.getObject()));
        }
    }

    public static LangBuilder lang() {
        return new LangBuilder(MOD_ID);
    }

    public static void log(String message) {      
        String side = ESC + "[1;34m" + Thread.currentThread().getName() + ESC + "[1;35m";

        String prefix = ESC + "[1;35m[quattage/" + MOD_ID + "] {" + side + "} >> " + ESC + "[1;36m";
        String suffix = ESC + "[1;35m -" + ESC;
        System.out.println(prefix + message + suffix);
    }

    public static void log(Object o) {
        log("'" + o.getClass().getName() + "' -> [" + o + "]");
    }

    public static void logReg(String message) {      
        log("Registering " + MOD_ID + " " + message);
    }

    public static void logSlow(String text) {
        logSlow(text, 500);
    }

    private static long lastLog = 0;

    public static void logSlow(String message, int millis) {
        if((System.currentTimeMillis() - lastLog) > millis) {
            log(message);
            lastLog = System.currentTimeMillis();
        }
    }

    public static ResourceLocation asResource(String filepath) {
        return new ResourceLocation(MOD_ID, filepath.toLowerCase());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append) {
        return defer(ctx, append, ctx.getId().getPath());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append, String realName) {
        String resource = ctx.getId().getNamespace() + ":block/" + append + "/" + realName;
        return new ResourceLocation(resource);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String item) {
        return new ResourceLocation(ctx.getId().getNamespace(), rootType + "/" + ctx.getId().getPath() + "/" + item);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String[] in, String[] sub, String item) {
        
        String path = rootType;

        if(in != null) {
            for(String s : in) path += "/" + s;
        }

        path += "/" + ctx.getName();
        
        if(sub != null) {
            for(String s : sub) path += "/" + s;
        }

        path += "/" + item;

        return new ResourceLocation(ctx.getId().getNamespace(), path);
    }

    public static MutableComponent asKey(String key) {
        return Component.translatable(MOD_ID + "." + key);
    }
}
