package com.jkdr.abyssalascentdebugging;

// All client-only imports are safe here
import com.jkdr.abyssalascentdebugging.util.FatalErrorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

/**
 * This class handles all client-side event registration and logic.
 * It is only ever loaded on the physical client, so it's safe to
 * import client-only classes like Screen, Minecraft, etc.
 */
public class ClientEvents {

    private boolean hasClientCheckRun = false;

    // This static method is called by the main mod constructor ONLY on the client
    public static void register() {
        // We create an instance of this class to register its non-static methods
        ClientEvents instance = new ClientEvents();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(instance::clientSetup);

        MinecraftForge.EVENT_BUS.addListener(instance::onScreenInit);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // Place any client-only setup logic here in the future
    }

    @SubscribeEvent
    public void onScreenInit(final ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen && !this.hasClientCheckRun) {
            // Use the public static LOGGER from the main class
            AbyssalAscentDebugging.LOGGER.info("Main menu screen loaded. Running client file integrity check...");
            this.hasClientCheckRun = true;
            this.runFileCheckAndShowScreen();
        }
    }

    /**
     * Called by the CLIENT.
     * Performs the check and shows an error screen if errors are found.
     */
    private void runFileCheckAndShowScreen() {
        List<FileData> errorList = AbyssalAscentDebugging.performFileCheck();

        if (!errorList.isEmpty()) {
            Minecraft.getInstance().setScreen(new FatalErrorScreen(errorList));
        }
    }
}
