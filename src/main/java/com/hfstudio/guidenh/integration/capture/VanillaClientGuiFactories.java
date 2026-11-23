package com.hfstudio.guidenh.integration.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiControls;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiInventory;

import com.hfstudio.guidenh.client.capture.api.CaptureClientGuiFactory;

import cpw.mods.fml.client.GuiModList;

public class VanillaClientGuiFactories {

    private VanillaClientGuiFactories() {}

    public static void register(ClientGuiBatchProvider provider) {
        provider.register(
            new CaptureClientGuiFactory(
                "main_menu",
                "Main menu",
                GuiMainMenu.class,
                context -> Minecraft.getMinecraft().theWorld == null,
                "The main menu requires leaving the current world",
                parent -> new GuiMainMenu()));
        provider.register(
            new CaptureClientGuiFactory(
                "options",
                "Options",
                GuiOptions.class,
                context -> true,
                "",
                parent -> new GuiOptions(parent, Minecraft.getMinecraft().gameSettings)));
        provider.register(
            new CaptureClientGuiFactory(
                "video",
                "Video settings",
                GuiVideoSettings.class,
                context -> true,
                "",
                parent -> new GuiVideoSettings(parent, Minecraft.getMinecraft().gameSettings)));
        provider.register(
            new CaptureClientGuiFactory(
                "controls",
                "Controls",
                GuiControls.class,
                context -> true,
                "",
                parent -> new GuiControls(parent, Minecraft.getMinecraft().gameSettings)));
        provider.register(
            new CaptureClientGuiFactory(
                "language",
                "Language",
                GuiLanguage.class,
                context -> true,
                "",
                parent -> new GuiLanguage(
                    parent,
                    Minecraft.getMinecraft().gameSettings,
                    Minecraft.getMinecraft()
                        .getLanguageManager())));
        provider.register(
            new CaptureClientGuiFactory(
                "sounds",
                "Sound settings",
                GuiScreenOptionsSounds.class,
                context -> true,
                "",
                parent -> new GuiScreenOptionsSounds(parent, Minecraft.getMinecraft().gameSettings)));
        provider.register(
            new CaptureClientGuiFactory(
                "packs",
                "Resource packs",
                GuiScreenResourcePacks.class,
                context -> true,
                "",
                GuiScreenResourcePacks::new));
        provider.register(
            new CaptureClientGuiFactory("mods", "Mod list", GuiModList.class, context -> true, "", GuiModList::new));
        provider.register(
            new CaptureClientGuiFactory(
                "inventory",
                "Player inventory",
                GuiInventory.class,
                context -> Minecraft.getMinecraft().thePlayer != null,
                "An active player is required",
                parent -> new GuiInventory(Minecraft.getMinecraft().thePlayer)));
        provider.register(
            new CaptureClientGuiFactory(
                "pause",
                "Game menu",
                GuiIngameMenu.class,
                context -> Minecraft.getMinecraft().theWorld != null,
                "A loaded world is required",
                parent -> new GuiIngameMenu()));
    }
}
