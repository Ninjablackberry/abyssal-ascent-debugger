package com.jkdr.abyssalascentdebugging;

import com.jkdr.abyssalascentdebugging.FileCheckSystemConfig;
import com.jkdr.abyssalascentdebugging.FileData;
import com.jkdr.abyssalascentdebugging.config.ModConfigFile;
import com.jkdr.abyssalascentdebugging.util.CustomErrorResolution;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

import org.slf4j.Logger;

@Mod(AbyssalAscentDebugging.MODID)
public class AbyssalAscentDebugging
{

    public static final String MODID = "abyssalascentdebugging";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static Boolean isClient;
    
    public static Boolean isSessionClient() {
        if (isClient == null) {
            isClient = FMLLoader.getDist() == Dist.CLIENT;
        }
        return isClient;
    }


    public AbyssalAscentDebugging() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        FileCheckSystemConfig.initSolutions();
        
        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigFile.SPEC, "abyssalascentdebugging-common.toml");

        if (FMLLoader.getDist() == Dist.CLIENT) {
            ClientEvents.register();
        }
    }

    // --- Removed clientSetup method ---

    @SubscribeEvent
    public void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Common setup running.");

        if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
            LOGGER.info("Running file integrity check on server...");
            this.runFileCheckAndCrashServer();
        }
    }

    // --- Removed onScreenInit method ---

    // --- Made performFileCheck public static ---
    public static List<FileData> performFileCheck() {
        List<FileData> errorData = new ArrayList<>();
        
        FileCheckSystemConfig.fileDataList.forEach((fileData) -> {
            CustomErrorResolution error = fileData.getFileError();
            if (error != null) {
                    errorData.add(fileData);
            }
        });

        return errorData;
    }

    /**
     * Called by the SERVER.
     * This interactive console logic is server-side and is fine.
     */
    private void runFileCheckAndCrashServer() {
        // --- Call the static performFileCheck() ---
        List<FileData> errorList = performFileCheck();
        
        if (!errorList.isEmpty()) {
            LOGGER.error("!!! --- FILE INTEGRITY CHECK FAILED --- !!!");
            LOGGER.error("Found {} error(s). Please resolve them interactively.", errorList.size());
            LOGGER.error("The server will NOT start until these are addressed.");

            Scanner scanner = new Scanner(System.in); 

            for (int i = 0; i < errorList.size(); i++) {
                FileData errorData = errorList.get(i);
                CustomErrorResolution error = errorData.getFileError();

                if (errorData.errorMarkedAsResolved || errorData.getActuallyResolved()) {
                    LOGGER.info("Skipping already resolved/ignored error for: {}", errorData.getLocalFilePath());
                    continue; 
                }

                boolean actionTaken = false;
                Pair<String, String> textContext = null;
                while (!actionTaken) {
                    if (textContext == null) {textContext = new Pair<String, String>("Reason:", error.getErrorReason());}

                    List<String> loggerLines = new ArrayList<>();
                    loggerLines.add("");
                    loggerLines.add("------------------------------------");
                    loggerLines.add(String.format("Error %d/%d", i + 1, errorList.size()));
                    loggerLines.add(String.format("(%s) %s", error.getErrorCode(), error.enforceError ? "Fatal" : "Warning"));
                    loggerLines.add("");
                    loggerLines.add(errorData.getFilePath().toString());
                    loggerLines.add("");
                    loggerLines.add(textContext.getFirst());
                    loggerLines.add(textContext.getSecond());
                    loggerLines.add("");
                    loggerLines.add("Commands:");
                    loggerLines.add("[S]olutions (Lists all viable solutions to fix)");
                    loggerLines.add("[C]ancel (Stop server now)");
                    if (error.hasAutoResolutionMethod()) {loggerLines.add("[A]uto resolve (Uses the built in auto resolution method)");}
                    loggerLines.add("[I]gnore (Ignore the error entirely)");
                    loggerLines.add("");
                    loggerLines.add("Please type in the initial of the action below:");

                    LOGGER.error(String.join("\n", loggerLines));

                    textContext = null;

                    String input = "";
                    try {
                         input = scanner.nextLine().trim().toUpperCase();
                    } catch (Exception e) {
                        LOGGER.error("Failed to read console input. Aborting.", e);
                        throw new RuntimeException("Server startup aborted due to console read error.", e);
                    }
                    

                    switch (input.toUpperCase()) {
                        case "A":
                            if (error.hasAutoResolutionMethod()) {
                                LOGGER.info("Attempting auto-resolution: {}", error.getAutoResolutionName());
                                if (error.invokeAutoResolutionMethod(errorData)) {
                                    LOGGER.info("SUCCESS: Auto-resolution complete.");
                                    errorData.removeIgnoreList();
                                } else {
                                    LOGGER.error("FAILURE: Auto-resolution failed. Check logs.");
                                }
                                actionTaken = true;
                            } else {
                                LOGGER.warn("Invalid choice. 'Resolve' is not available for this error.");
                            }
                            break;
                        
                        case "I":
                            LOGGER.info("Marking error as IGNORED in config.");
                            errorData.addIgnoreList();
                            actionTaken = true;
                            break;

                        case "S":
                           textContext = new Pair<String, String>("Solutions:", 
                           error.getServerSolutions()
                            .replace("${CHECK_DIRECTORIES}", "(/config, /scripts, /kubejs)")
                            .replace("${LOCAL_PATH}", errorData.getLocalFilePath())
                            .replace("${ROOT_PATH}", errorData.getFilePath().toString())
                            .replace("${BUTTON_AUTO_RESOLVE}", error.getAutoResolutionName())
                            .replace("${LOCAL_ENV}", "This panel (Abyssal Ascent Debugger)")
                           );
                            break;
                        
                        case "C":
                            throw new RuntimeException("------------------ SERVER WAS FORCEFULLY CLOSED THIS IS NOT AN ERROR THIS IS A NOTICE THE SERVER HAS CLOSED ------------------");
                        
                        default:
                            LOGGER.warn("'{}' is not a valid choice. Please try again.", input);
                            break;
                    }
                } 
            } 

            scanner.close(); 

            LOGGER.info("-------------------------------------------------");
            LOGGER.info("Finished processing all errors.");
            
            // --- Call the static performFileCheck() ---
            List<FileData> remainingErrors = performFileCheck();
            if (!remainingErrors.isEmpty()) {
                 LOGGER.warn("Some errors were skipped or remain unresolved.");
            } else {
                LOGGER.info("All errors appear to be resolved.");
            }
            
            LOGGER.error("Interactive resolution is complete. The server will now stop.");
            LOGGER.error("Please restart the server to apply changes and continue.");
            throw new RuntimeException("------------------ SERVER WAS FORCEFULLY CLOSED THIS IS NOT AN ERROR THIS IS A NOTICE THE SERVER HAS CLOSED & ALL ERRORS FROM THIS SESSION HAVE BEEN RESOLVED!!!!! ------------------");

        } else {
            LOGGER.info("File integrity check PASSED on server. Proceeding with startup.");
        }
    }

    // --- Removed runFileCheckAndShowScreen method ---
}
