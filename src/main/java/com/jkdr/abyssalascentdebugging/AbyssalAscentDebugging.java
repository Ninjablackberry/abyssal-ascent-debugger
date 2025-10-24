package com.jkdr.abyssalascentdebugging;

import com.jkdr.abyssalascentdebugging.util.AutoResolutionMethod;
import com.jkdr.abyssalascentdebugging.util.CustomErrorResolution;
import com.jkdr.abyssalascentdebugging.util.SolutionMethods;

import java.util.EnumSet;
import java.util.List;

public class FileCheckSystemConfig
{
    private static AutoResolutionMethod immersivePortalsConfigAutoResolve = new AutoResolutionMethod(SolutionMethods::fixImmersivePortalsConfig, "Replace Config");
    private static AutoResolutionMethod deleteFileAutoResolve = new AutoResolutionMethod(SolutionMethods::genericFileDelete, "Delete File");

     /*
    
    HOW TO FORMAT Custom Error Resolution CONSTRUCTOR

    new CustomErrorResolution(
        "0x000", //Any error code that is a string (could literally be anything)
        "What is the reason?", //The reason for the error occuring (this will get passed to the ai)
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_NOT_FOUND), //Found in the CustomErrorResolution file contains the conditions to activate under
        CustomErrorResolution.Environment.COMMON, //Environment executed under (common is both) by selecting server, the game will pass it as a warning on the client just incase there is localhosting
        true //Do we pass it as a fatal error (false will set it as a warning signifying to the user the game will run just fine without the config)
    )
    
    Error Conditions:
        FILE_NOT_FOUND,
		FILE_MODFIED,
		FILE_EXISTS,
		FILE_NOT_READABLE,
		FILE_NOT_WRITEABLE
    */


	public static CustomErrorResolution genericFileNotFound = new CustomErrorResolution(
        "0x001",
        "File not found (Warning)",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_NOT_FOUND),
        CustomErrorResolution.Environment.COMMON,
        "The file was not found at runtime!",
        false //Pass it off as a warning
    );

    public static CustomErrorResolution forceFileNotFound = new CustomErrorResolution(
        "0x002",
       "File not found (Enforced)",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_NOT_FOUND),
        CustomErrorResolution.Environment.COMMON,
        "The file was not found at runtime!",
        true //Enforced this error
    );

    

    public static CustomErrorResolution imptlConfigNotFound = new CustomErrorResolution(
        "0x003",
        "Immersive portals config file missing",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_NOT_FOUND),
        CustomErrorResolution.Environment.SERVER,
        "Immersive portals config was not found, this handles all logic regarding dimension stacking and if changed may break the game",
        true, //Enforce this error
        immersivePortalsConfigAutoResolve
    );

    public static CustomErrorResolution imptlConfigChanged = new CustomErrorResolution(
        "0x004",
        "Immersive portals config file changed",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_MODFIED),
        CustomErrorResolution.Environment.SERVER,
        "Immersive portals config has been changed / corrupted, this handles all logic regarding dimension stacking and if changed may break the game",
        true, //Enforce this error
        immersivePortalsConfigAutoResolve
    );

    public static CustomErrorResolution fileShouldNotExistForced = new CustomErrorResolution(
        "0x005",
        "File should not exist error (Enforced)",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_EXISTS),
        CustomErrorResolution.Environment.COMMON,
        "The file should not exist",
        true //Enforced this error
    );

    public static CustomErrorResolution genericFilePermissionError = new CustomErrorResolution(
        "0x006",
        "File permission error",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_NOT_READABLE, CustomErrorResolution.ErrorCondition.FILE_NOT_WRITEABLE),
        CustomErrorResolution.Environment.COMMON,
        "The Minecraft instance does not have access to the file!",
        true
    );

    public static CustomErrorResolution forceFileDoesExist = new CustomErrorResolution(
        "0x007",
       "File not found (Enforced)",
        EnumSet.of(CustomErrorResolution.ErrorCondition.FILE_EXISTS),
        CustomErrorResolution.Environment.SERVER,
        "The file was found when it shouldn't (This is usually an old script which breaks newer ones)",
        true, //Enforced this error
        deleteFileAutoResolve
    );
    
    public static void initSolutions() {
        /*
            For solutions you can use special variables relative to the user

            ${CHECK_DIRECTORIES} : Contains a list of directories the user should check when they have invalid configs or files to make sure the user does a check they have the correct files.
            ${LOCAL_PATH} : Contains the local path of the file (the directory starting from the minecraft directory).
            ${ROOT_PATH} : Contains the full path of the file, allowing the user to manually navigate on their computer without finding the local directory.
            ${BUTTON_NAME} : Name of the button the error is located under (telling the user which button to click)
            ${LOCAL_ENV} : Name of the panel the user gets when joining the game
            ${BUTTON_AUTO_RESOLVE} : Name of the auto resolve button

            These are solutions which get displayed to the user when the specified error occurs so the user can fix them (These do not get passed to the AI prompt)
        */

        imptlConfigNotFound.addClientSolution(
            "Press the ${BUTTON_AUTO_RESOLVE} button on this page", 
            imptlConfigChanged //When initialising solutions you can add more CustomErrorResolution as arguments at the end to also apply to them.
        );
        imptlConfigNotFound.addServerSolution(
            "Type 'A' in the console (auto resolve this error)", 
            imptlConfigChanged //When initialising solutions you can add more CustomErrorResolution as arguments at the end to also apply to them.
        );
        imptlConfigNotFound.addClientSolution(
            "If you are not hosting a game from this computer, you can just press 'Ignore Error'. (This is a server side only requirement)", 
            imptlConfigChanged //When initialising solutions you can add more CustomErrorResolution as arguments at the end to also apply to them.
        );
        imptlConfigNotFound.addGenericSolution(
            "Download the Abyssal Ascent pack on Curseforge, unzip the file, navigate to ${LOCAL_PATH} and copy the file. Then navigate to the directory ${ROOT_PATH} and paste the file. If you are doing it this way ensure to check if the configs contained in the directory match the Curseforge download.",
            imptlConfigChanged
        );


        /*
            AI solutions are useful and act as a hybrid replacement to human lead tickets which enables more time and energy to focus on modpack development, prompt is highly customisable and contains multiple variables to tell the AI how the user should navigate and what to replace

            AI will NOT have the source files.

            
        
        imptlConfigNotFound.addAIClientSolution(
            "Tell the user to navigate to ${LOCAL_ENV} and locate the button ${BUTTON_NAME} which will bring the user to the specific page where they can then press the 'Auto Fix' button",
            imptlConfigChanged
        );
        imptlConfigNotFound.addAIClientSolution(
            "Tell the user, if they are not intending to host a session from this computer (LAN, C4ME) then they do not need to worry about this error and they can press the 'Ignore Error' button in the ${LOCAL_ENV} specifically in the ${BUTTON_NAME}.",
            imptlConfigChanged
        );
        imptlConfigNotFound.addAIGenericSolution(
            "Tell the user to check directories including ${CHECK_DIRECTORIES} and if the user does not have the files that match, then they should install them or unintended side effects may occur."
        );
        imptlConfigNotFound.addAIGenericSolution(
            "Tell the user to navigate to the modpack Abyssal Ascent and download the latest mod version files, then tell them to extract it, navigate to ${LOCAL_PATH} within the zip, copy the file, paste the file into ${ROOT_PATH}",
            imptlConfigChanged
        );
        */
        fileShouldNotExistForced.addGenericSolution("This file is an old file from a previous version of Abyssal Ascent, to resolve this click 'Confirm Deletion'");
        //fileShouldNotExistForced.addAIGenericSolution("This file should not exist, tell the user to delete this they can either click the ${BUTTON_NAME} in ${LOCAL_ENV} and press 'Confirm Deletion' or manually delete it themselves");

        forceFileDoesExist.addClientSolution("Press the ${BUTTON_AUTO_RESOLVE} button");
        forceFileDoesExist.addServerSolution("Type 'A' in the console (auto resolve this error)");
        forceFileDoesExist.addGenericSolution("Go to the file at ${ROOT_PATH} and delete the file");
        //forceFileDoesExist.addAIGenericSolution("Tell the user to navigate to ${ROOT_PATH} and delete the file.");
    }

    /*
    
    HOW TO FORMAT FILE DATA CONSTRUCTOR

    new FileData(
        "file/at/path.json", //File location (do not add a slash first because that will look in the devices root path C: or /home depending on OS)
        List.of(imptlConfigChanged), //List of errors which the file will enterpret
        "HASH", //Hash of the file (needed if comparing hash - will always return true if not set)
    )
    
    */

    //All of the files we undergo tests on
	public static final List<FileData> fileDataList = List.of(
        new FileData(
            "config/immersive_portals.json",
            List.of(imptlConfigNotFound, imptlConfigChanged),
            "d091819b6bcb77b95c0498f07e5171f24c1403aa0bc1cdcc2641780f5879fe61" //256 SHA HASH (easier than containing actual files and algorithm is near impossible to fail) 
        ),
        new FileData(
            "scripts/undergarden_worldspawn.zs",
            List.of(forceFileDoesExist)
        ),
        new FileData(
            "kubejs/server_scripts/pickaxe_progression.js",
            List.of(forceFileDoesExist)
        )
    );
}
