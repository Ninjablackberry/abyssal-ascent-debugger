package com.jkdr.abyssalascentdebugging.util;
import com.jkdr.abyssalascentdebugging.AbyssalAscentDebugging;

import com.jkdr.abyssalascentdebugging.util.AutoResolutionMethod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth; 
import org.lwjgl.glfw.GLFW; 
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.jkdr.abyssalascentdebugging.FileData;
import net.minecraft.ChatFormatting;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
// --- NEW IMPORTS ---
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.network.chat.MutableComponent;
import java.util.HashMap;
// --- END NEW IMPORTS ---

public class FatalErrorScreen extends Screen {

    private List<FileData> message;
    private List<FormattedCharSequence> messageLines = Collections.emptyList();

    
    // --- VARIABLES FOR TEXT SCROLLING ---
    private double scrollOffset = 0.0;
    private int maxScroll = 0;
    private int totalTextHeight = 0;
    private int textAreaTop = 50;
    private int textAreaBottom = 0;
    private Button buttonToRevert = null;
    private Component originalButtonMessage = null;

    private static final String homeScreenText = "Welcome to Abyssal Ascent, an issue with your modpack installation has been detected!\n\nEach of the errors are individually listed and it is highly recommended you look through each files and see if there is an auto resolve option.\n\nIf issues still occur after following the process, you can file a ticket on the discord and copy the logs.\n\nThe errors on the right have different meaning based on colour (Red is a file error which can heavily impact your playthrough, Yellow is a warning to inform you minor changes including balancing and other factors may not match the intended experience)\n\n\nIf you understand these issues were intentional you can press the 'Run Anyways' button or mark indiviual issues as 'Ignore Error' on their respective pages to disable the error from re-appearing. Pressing this button on an unintentional error will most likely result in minor errors (bedrock at roof) or other changes and support with tickets will be highly limited";

    // --- Variables for dynamic text and layout ---
    private Component currentDisplayText;
    private int textAreaWidth = 0; // Will be calculated in init
    private int scrollbarX = 0; // Will be calculated in init

    // --- NEW VARIABLES FOR BUTTON SCROLLING ---
    private List<Button> scrollableButtons = new ArrayList<>();
    private Map<Button, Integer> buttonOriginalRelativeY = new HashMap<>();
    private double buttonScrollOffset = 0.0;
    private int maxButtonScroll = 0;
    private int totalButtonHeight = 0;
    private int buttonAreaTop = 50;
    private int buttonAreaBottom = 0;
    private int buttonAreaX = 0;
    private int buttonScrollbarX = 0;

    private Map<FileData, Button> errorButtonMap = new HashMap<>();
    // --- END NEW VARIABLES ---

    // --- NEW: Variables for dynamic bottom buttons ---
    private List<Button> dynamicBottomButtons = new ArrayList<>();
    private FileData currentSelectedError = null; // null = Home Page
    // --- END NEW ---

    public FatalErrorScreen(List<FileData> message) {
        super(Component.literal("Abyssal Ascent Debugger (Restart game once resolved)"));
        this.message = message;
    }

    private void trackButtonToRevert(Button button, Component originalMessage) {
        // Revert any button that was previously waiting
        revertButtonTextNow();

        // Track the new button and its original message
        this.buttonToRevert = button;
        this.originalButtonMessage = originalMessage;
    }

    private void revertButtonTextNow() {
        if (this.buttonToRevert != null && this.originalButtonMessage != null) {
            // Only revert if the current message is NOT the original message
            if (!this.buttonToRevert.getMessage().equals(this.originalButtonMessage)) {
                 this.buttonToRevert.setMessage(this.originalButtonMessage);
            }
        }

        this.buttonToRevert = null;
        this.originalButtonMessage = null;
    }

    // --- This method now also updates the page state and rebuilds buttons ---
    private void setText(Component newText, FileData selectedError) {
        this.currentDisplayText = newText;
        this.currentSelectedError = selectedError; // Store the current page state

        // Split the new text into lines that fit our calculated text area width
        if (this.font != null) {
            this.messageLines = this.font.split(this.currentDisplayText, this.textAreaWidth);
        } else {
            // Font might be null if called too early, though it shouldn't be
            this.messageLines = Collections.emptyList();
        }
        
        // Recalculate scrolling parameters
        this.totalTextHeight = this.messageLines.size() * this.font.lineHeight;
        int textAreaHeight = this.textAreaBottom - this.textAreaTop;
        this.maxScroll = Math.max(0, this.totalTextHeight - textAreaHeight);
        this.scrollOffset = 0.0; // Reset scroll to top

        // Rebuild the bottom buttons to match the new page state
        this.rebuildBottomButtons();
    }


    // --- NEW METHOD: Rebuilds the buttons at the bottom of the screen ---
    private void rebuildBottomButtons() {
        // 1. Clear existing dynamic buttons
        for (Button button : this.dynamicBottomButtons) {
            this.removeWidget(button);
        }
        this.dynamicBottomButtons.clear();

        // 2. Define layout for bottom buttons
        int buttonWidth = 95;
        int buttonHeight = 20;
        int buttonGap = 10;
        int buttonXOffset = 20;
        int bottomOffset = this.height - 35; // Y position for the buttons

        // 3. Add new buttons based on the current page
        if (this.currentSelectedError == null) {
            // --- HOME PAGE BUTTONS ---
            // We'll add 3 buttons: Copy AI, Copy Logs, Run Anyways

            int totalWidth = (buttonWidth * 3) + (buttonGap * 2);
            int startX = buttonXOffset;
            buttonWidth = 80;

            // Button 1: Copy AI
            Button copyAIButton = Button.builder(Component.literal("Quit Game"), (button) -> {
                Minecraft.getInstance().stop();
            }).bounds(startX, bottomOffset, buttonWidth, buttonHeight).build();
            this.dynamicBottomButtons.add(copyAIButton);
            this.addRenderableWidget(copyAIButton);

            startX += buttonWidth + buttonGap;

            // Button 2: Copy Logs
            Button copyLogsButton = Button.builder(Component.literal("Copy Logs"), (button) -> {
                this.revertButtonTextNow();
                Component originalMsg = button.getMessage();
                this.trackButtonToRevert(button, originalMsg);
                try {
                    Path logPath = this.minecraft.gameDirectory.toPath().resolve("logs").resolve("latest.log");
                    StringBuilder logBuilder = new StringBuilder();
                    try (FileInputStream fis = new FileInputStream(logPath.toFile());
                         InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                         BufferedReader br = new BufferedReader(isr)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            logBuilder.append(line).append(System.lineSeparator());
                        }
                    }
                    this.minecraft.keyboardHandler.setClipboard(logBuilder.toString());
                    button.setMessage(Component.literal("Copied!"));
                } catch (Exception e) { 
                    e.printStackTrace(); 
                    button.setMessage(Component.literal("Failed to Copy!"));
                }
            }).bounds(startX, bottomOffset, buttonWidth, buttonHeight).build();
            this.dynamicBottomButtons.add(copyLogsButton);
            this.addRenderableWidget(copyLogsButton);

            startX += buttonWidth + buttonGap;


            // Button 3: Run Anyways
            Button runAnywaysButton = Button.builder(Component.literal("Run Anyways"), (button) -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(null); 
                }
            }).bounds(startX, bottomOffset, buttonWidth, buttonHeight).build();
            this.dynamicBottomButtons.add(runAnywaysButton);
            this.addRenderableWidget(runAnywaysButton);

        } else {
            // --- MODIFIED: ERROR PAGE BUTTON ---
            // We'll add just one "Resolve Issue" button, centered.

            int startX = buttonXOffset; // Center the single button

            // Button 1: Resolve Issue
            // We capture 'this.currentSelectedError' in the lambda.
            final FileData errorData = this.currentSelectedError; 
            final CustomErrorResolution error = errorData.getFileError();
            MutableComponent ignoreErrorText = Component.literal("Ignore Error").withStyle(ChatFormatting.RED);
            if (errorData.errorMarkedAsResolved) { ignoreErrorText = Component.literal("Undo Ignore").withStyle(ChatFormatting.GOLD);}
            
            if (!errorData.getActuallyResolved()) {
             buttonWidth = 150;
            Button resolveButton = Button.builder(ignoreErrorText, (button) -> {
                // Does nothing for now, as requested.
                // This is where you would call: errorData.resolve() or similar.
              
                this.revertButtonTextNow(); 
                Component originalMsg = button.getMessage();
                errorData.errorMarkedAsResolved = !errorData.errorMarkedAsResolved;

                if (errorData.errorMarkedAsResolved) {errorData.addIgnoreList();}
                else {errorData.removeIgnoreList();}
                this.trackButtonToRevert(button, originalMsg);
               this.UpdateColour(errorData);
               this.rebuildBottomButtons();
                
                // You can add feedback here if you want
                // button.setMessage(Component.literal("...")); 
                
            }).bounds(startX, bottomOffset, buttonWidth, buttonHeight).build();

            this.dynamicBottomButtons.add(resolveButton);
            this.addRenderableWidget(resolveButton);
            startX += buttonWidth + buttonGap;

            }
           

            
            if (error.hasAutoResolutionMethod() && !errorData.getActuallyResolved()) {
                buttonWidth = 100;
                Button autoFixButton = Button.builder(Component.literal(error.getAutoResolutionName()), (button) -> {
                    // Does nothing for now, as requested.
                    // This is where you would call: errorData.resolve() or similar.
                    this.revertButtonTextNow(); 
                    Component originalMsg = button.getMessage();
                    this.trackButtonToRevert(button, originalMsg);
                    if (error.invokeAutoResolutionMethod(errorData)) {
                        button.setMessage(Component.literal("Success!"));
                        this.rebuildBottomButtons();
                        this.UpdateColour(errorData);
                    } else {button.setMessage(Component.literal("Failed!"));}
                
                    // You can add feedback here if you want
                    // button.setMessage(Component.literal("...")); 

                
                }).bounds(startX, bottomOffset, buttonWidth, buttonHeight).build();

                this.dynamicBottomButtons.add(autoFixButton);
                this.addRenderableWidget(autoFixButton);
            }
     
            // --- END MODIFIED ---
        }
    }

    private void UpdateColour(FileData data) {
        CustomErrorResolution error = data.getFileError();
        if (data.getActuallyResolved() || data.errorMarkedAsResolved) {
            this.refreshErrorButton(data, ChatFormatting.GREEN);
        }
        else if (error.enforceError) {
            this.refreshErrorButton(data, ChatFormatting.RED);
        } else {
            this.refreshErrorButton(data, ChatFormatting.GOLD);
        }
    }
    // --- END NEW METHOD ---

    private void refreshErrorButton(FileData fileData, ChatFormatting newStyle) {
    // 1. Find the button in our map
    Button buttonToRefresh = this.errorButtonMap.get(fileData);

    if (buttonToRefresh != null) {
        // 2. Re-create the button text, but with the new style
        final CustomErrorResolution error = fileData.getFileError();
        MutableComponent newButtonText = Component.literal(" " + fileData.getFileName() + " (" + error.getErrorCode() + ") ");
        newButtonText.withStyle(newStyle); // Apply the new color

        // 3. Set the button's message to the new component
        buttonToRefresh.setMessage(newButtonText);
    }
}

    @Override
    protected void init() {
        super.init();

    this.scrollableButtons.clear();
    this.buttonOriginalRelativeY.clear();
    this.errorButtonMap.clear(); // <-- ADD THIS LINE

    // Define the vertical area for our text
    this.textAreaTop = 50; // 50px from top
    // --- MODIFIED: Make space at the bottom for new buttons ---
    this.textAreaBottom = this.height - 50; 
    // --- END MODIFIED ---

    // --- Define layout areas (MODIFIED) ---
    int leftMargin = 30;
    int rightMargin = 10;
    int scrollbarWidth = 6;
    int scrollbarPadding = 4; // Gap between content and scrollbar
    int buttonAreaPadding = 10; // Gap between text area and button area
    int errorButtonWidth = 140;
    
    this.buttonScrollbarX = this.width - rightMargin - scrollbarWidth;

    this.buttonAreaX = this.buttonScrollbarX - scrollbarPadding - errorButtonWidth;
    this.scrollbarX = this.buttonAreaX - buttonAreaPadding - scrollbarWidth;
    
    int textRightMargin = this.scrollbarX - scrollbarPadding;
    this.textAreaWidth = textRightMargin - leftMargin;

    // --- MODIFIED: Add Scrollable Error Buttons (Right Side) ---
    int errorButtonHeight = 20;
    int errorButtonX = this.buttonAreaX; // Use new calculated X
    int currentRelativeY = 0; // Y position *relative* to the scroll area start
    
    // --- MODIFIED: Button area now respects text area bottom ---
    this.buttonAreaTop = this.textAreaTop; // Align top with text area
    this.buttonAreaBottom = this.textAreaBottom; // Align bottom with text area

        Button homeButton = Button.builder(Component.literal("Home"), (button) -> {
        this.revertButtonTextNow();
        Component originalMsg = button.getMessage();
        this.trackButtonToRevert(button, originalMsg);
        // --- MODIFIED: Pass 'null' to indicate Home page ---
        this.setText(Component.literal(homeScreenText), null); 
    }).bounds(errorButtonX, this.buttonAreaTop + currentRelativeY, errorButtonWidth, errorButtonHeight).build();
    this.scrollableButtons.add(homeButton);
    this.buttonOriginalRelativeY.put(homeButton, currentRelativeY);
    this.addRenderableWidget(homeButton);

    currentRelativeY += errorButtonHeight + 15; // 15px gap
    
    // --- Error Buttons ---
    if (this.message != null) {
        for (int i = 0; i < this.message.size(); i++) {
            final FileData targetFileData = this.message.get(i); 
            final CustomErrorResolution error = targetFileData.getFileError();
            final String buttonName = targetFileData.getFileName() + " ("+ error.getErrorCode() +")";
            MutableComponent buttonText = Component.literal(" "+buttonName+" ");
            if (targetFileData.getActuallyResolved() || targetFileData.errorMarkedAsResolved) {buttonText.withStyle(ChatFormatting.GREEN);}
            else if (error.enforceError) {buttonText.withStyle(ChatFormatting.RED);}
            else {buttonText.withStyle(ChatFormatting.GOLD);}
            
            Button errorButton = Button.builder(buttonText, (button) -> {
                this.revertButtonTextNow();
                Component originalMsg = button.getMessage();
                this.trackButtonToRevert(button, originalMsg);

                String resToUserClient = targetFileData.resolutionToClient()
                .replace("${CHECK_DIRECTORIES}", "(/config, /scripts, /kubejs)")
                .replace("${LOCAL_PATH}", targetFileData.getLocalFilePath())
                .replace("${ROOT_PATH}", targetFileData.getFilePath().toString())
                .replace("${BUTTON_NAME}", buttonName)
                .replace("${BUTTON_AUTO_RESOLVE}", error.getAutoResolutionName())
                .replace("${LOCAL_ENV}", "This panel (Abyssal Ascent Debugger)");

                this.setText(Component.literal(resToUserClient), targetFileData);
            }).bounds(errorButtonX, this.buttonAreaTop + currentRelativeY, errorButtonWidth, errorButtonHeight).build();
            
            this.scrollableButtons.add(errorButton);
            this.buttonOriginalRelativeY.put(errorButton, currentRelativeY);
            this.addRenderableWidget(errorButton);
            
            // --- ADD THIS LINE ---
            // Store a reference to this button, linked to its data
            this.errorButtonMap.put(targetFileData, errorButton);
            // --- END ADDED LINE ---
            
            currentRelativeY += errorButtonHeight + 5; // 5px gap
        }
    }
    
    // --- NEW: Finalize Button Scrolling ---
    this.totalButtonHeight = currentRelativeY;
    int buttonAreaHeight = this.buttonAreaBottom - this.buttonAreaTop;
    this.maxButtonScroll = Math.max(0, this.totalButtonHeight - buttonAreaHeight);
    // --- END BUTTON INIT ---


    // --- MODIFIED: Set Initial Text (and trigger first button build) ---
    if (this.message == null || this.message.isEmpty()) {
         this.setText(Component.literal("An unknown error occurred, please report to Abyssal Ascent or AbyssalAscentDebugging owner"), null);
    } else {
         this.setText(Component.literal(homeScreenText), null);
    }
        // --- END MODIFIED ---
    }

   @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Draw the dark background overlay
        this.renderBackground(guiGraphics);

        int leftMargin = 30;
        // Draw the title
        guiGraphics.drawString(this.font, this.title, leftMargin, 20, 0xAB4946); // Red

        // --- START TEXT SCISSOR/CLIPPING ---
        guiGraphics.enableScissor(0, this.textAreaTop, this.width, this.textAreaBottom);
        
        int y = this.textAreaTop - (int) this.scrollOffset;
        for (FormattedCharSequence line : this.messageLines) {
            guiGraphics.drawString(this.font, line, leftMargin, y, 0xD67465); // White
            y += this.font.lineHeight; // Move to the next line
        }
        
        // --- END TEXT SCISSOR/CLIPPING ---
        guiGraphics.disableScissor();
        
        // --- DRAW TEXT SCROLLBAR ---
        if (this.maxScroll > 0) {
            int scrollbarX = this.scrollbarX; 
            int scrollbarTop = this.textAreaTop;
            int scrollbarBottom = this.textAreaBottom;
            int scrollbarHeight = scrollbarBottom - scrollbarTop;

            guiGraphics.fill(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarBottom, 0x80000000); // Dark semi-transparent

            double thumbHeightRatio = (double)scrollbarHeight / (double)this.totalTextHeight;
            int thumbHeight = (int) (scrollbarHeight * thumbHeightRatio);
            thumbHeight = Mth.clamp(thumbHeight, 8, scrollbarHeight); 
            
            double scrollPercentage = (this.maxScroll > 0) ? Mth.clamp(this.scrollOffset / this.maxScroll, 0.0, 1.0) : 0.0;
            int thumbY = scrollbarTop + (int) (scrollPercentage * (scrollbarHeight - thumbHeight));

            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0x80FFFFFF); // White semi-transparent
        }
        // --- END TEXT SCROLLBAR ---

        
        // --- MODIFIED: BUTTON AREA RENDERING ---
        
        // 1. Update button positions and visibility
        for (Button button : this.scrollableButtons) {
            int originalRelativeY = this.buttonOriginalRelativeY.get(button);
            int newY = this.buttonAreaTop + originalRelativeY - (int)this.buttonScrollOffset;
            button.setY(newY);
            
            // Set visibility based on clipping
            button.visible = (newY + button.getHeight() > this.buttonAreaTop) && (newY < this.buttonAreaBottom);
        }
        
        // 2. Enable scissor for *drawing* the scrollable buttons
        guiGraphics.enableScissor(this.buttonAreaX, this.buttonAreaTop, this.width, this.buttonAreaBottom);

        // 3. Manually render ONLY the scrollable buttons
        // We iterate this list instead of calling super.render()
        for(Button button : this.scrollableButtons) {
            button.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        
        // 4. Disable scissor
        guiGraphics.disableScissor();

        // 5. Manually render ONLY the dynamic bottom buttons (outside of any scissor)
        for(Button button : this.dynamicBottomButtons) {
            button.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        
        // 6. Draw the *button* scrollbar (must be done *after* disabling scissor)
        if (this.maxButtonScroll > 0) {
            int scrollbarTop = this.buttonAreaTop;
            int scrollbarBottom = this.buttonAreaBottom;
            int scrollbarHeight = scrollbarBottom - scrollbarTop;
            int scrollbarX = this.buttonScrollbarX;

            // Draw the scrollbar background track
            guiGraphics.fill(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarBottom, 0x80000000); 

            // Calculate thumb size and position
            double thumbHeightRatio = (double)scrollbarHeight / (double)this.totalButtonHeight;
            int thumbHeight = (int) (scrollbarHeight * thumbHeightRatio);
            thumbHeight = Mth.clamp(thumbHeight, 8, scrollbarHeight);
            
            double scrollPercentage = (this.maxButtonScroll > 0) ? Mth.clamp(this.buttonScrollOffset / this.maxButtonScroll, 0.0, 1.0) : 0.0;
            int thumbY = scrollbarTop + (int) (scrollPercentage * (scrollbarHeight - thumbHeight));

            // Draw the scrollbar thumb
            guiGraphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0x80FFFFFF);
        }
        // --- END MODIFIED BUTTON AREA ---
    }

    /**
     * This method is called when the mouse wheel is scrolled.
     */
    // --- MODIFIED ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        // --- MODIFIED: Check if mouse is within the button scrolling area (vertically) ---
        if (mouseY >= this.buttonAreaTop && mouseY < this.buttonAreaBottom) {
            // Check if mouse is over the button area (from the button X position to the right edge)
            if (mouseX >= this.buttonAreaX) {
                this.buttonScrollOffset -= (scrollDelta * 10);
                this.buttonScrollOffset = Mth.clamp(this.buttonScrollOffset, 0.0, this.maxButtonScroll);
                return true; // We handled this event
            } 
        }
        
        // --- MODIFIED: Check if mouse is within the text scrolling area (vertically) ---
        if (mouseY >= this.textAreaTop && mouseY < this.textAreaBottom) {
            // Check if mouse is over the text area
            if (mouseX < this.buttonAreaX) { 
                this.scrollOffset -= (scrollDelta * 10); 
                this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0, this.maxScroll);
                return true; // We handled this event
            }
        }
        
        return false; // We didn't handle this event
    }
    // --- END MODIFIED ---

    /**
     * This screen should not be escapable by pressing 'Esc'.
     */
    @Override
    public boolean isPauseScreen() {
        return false; // Prevents pausing
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null); 
            }
            return true; // Event handled
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void onClose() {
        // This method is final in the superclass, so we can't override it.
    }
}