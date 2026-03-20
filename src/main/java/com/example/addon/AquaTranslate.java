package com.example.addon;

import com.example.addon.commands.TranslateCmd;
import com.example.addon.modules.TranslateChat;
import com.example.addon.modules.TranslateCommand;
import com.example.addon.modules.TranslateSigns;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AquaTranslate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Translate");

    @Override
    public void onInitialize() {
        LOG.info("Initializing AquaClient Translator");

        Modules.get().add(new TranslateChat());
        Modules.get().add(new TranslateSigns());
        Modules.get().add(new TranslateCommand());

        // Commands.add() is static — there is no Commands.get()
        Commands.add(new TranslateCmd());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
