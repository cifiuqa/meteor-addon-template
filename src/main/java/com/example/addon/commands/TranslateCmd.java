package com.example.addon.commands;

import com.example.addon.TranslateAPI;
import com.example.addon.modules.TranslateCommand;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

public class TranslateCmd extends Command {

    public TranslateCmd() {
        super("translate", "Translates text to a target language and shows it in the on-screen panel.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {

        // .translate clear
        builder.then(literal("clear")
            .executes(ctx -> {
                getModule().clear();
                info("Translation panel cleared.");
                return SINGLE_SUCCESS;
            })
        );

        // .translate <language> <text...>
        builder.then(argument("language", StringArgumentType.word())
            .then(argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String lang = StringArgumentType.getString(ctx, "language");
                    String text = StringArgumentType.getString(ctx, "text");

                    String targetCode = TranslateAPI.resolveLanguageCode(lang);
                    if (targetCode == null) {
                        error("Unknown language: (highlight)%s(default).", lang);
                        return SINGLE_SUCCESS;
                    }

                    getModule().setDisplay("Translating...");
                    getModule().setLoading(true);

                    TranslateAPI.translateAsync(text, targetCode, result -> {
                        getModule().setLoading(false);
                        if (result != null && !result.translation().isBlank()) {
                            getModule().setDisplay(result.translation());
                        } else {
                            getModule().setDisplay("(Already in " + lang + " or translation failed)");
                        }
                    });

                    return SINGLE_SUCCESS;
                })
            )
        );
    }

    private TranslateCommand getModule() {
        return Modules.get().get(TranslateCommand.class);
    }
}
