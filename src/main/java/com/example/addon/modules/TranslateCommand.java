package com.example.addon.modules;

import com.example.addon.AquaTranslate;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Render-only module for the .translate command panel.
 * All command logic lives in {@link com.example.addon.commands.TranslateCmd}.
 */
public class TranslateCommand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Keybind> copyKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("copy-key")
        .description("While hovering over a word in the panel, press this key to copy that word.")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the translated text in the panel.")
        .defaultValue(new SettingColor(0, 200, 255, 255))
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color of the translation panel. Adjust alpha for opacity.")
        .defaultValue(new SettingColor(0, 0, 0, 160))
        .build()
    );

    private final Setting<SettingColor> highlightColor = sgGeneral.add(new ColorSetting.Builder()
        .name("word-highlight-color")
        .description("Highlight shown on the word currently under your cursor.")
        .defaultValue(new SettingColor(0, 200, 255, 60))
        .build()
    );

    private volatile String displayText = null;
    private volatile boolean loading    = false;

    private static final int PANEL_X     = 10;
    private static final int PANEL_TOP_Y = 10;
    private static final int PADDING     = 6;
    private static final int MAX_WIDTH   = 200;

    public TranslateCommand() {
        super(AquaTranslate.CATEGORY, "translate-command",
            "Shows the output of .translate <language> <text> as an on-screen panel. " +
            "Hover a word and press the copy key to copy it.");
    }

    @Override
    public void onDeactivate() {
        displayText = null;
        loading = false;
    }

    public void setDisplay(String text)   { this.displayText = text; }
    public void setLoading(boolean value) { this.loading = value; }
    public void clear()                   { this.displayText = null; this.loading = false; }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (displayText == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        List<String> lines = wordWrap(displayText, tr, MAX_WIDTH);

        int lineHeight  = tr.fontHeight + 2;
        int panelWidth  = lines.stream().mapToInt(tr::getWidth).max().orElse(10) + PADDING * 2;
        int panelHeight = lines.size() * lineHeight + PADDING * 2 - 2;

        event.drawContext.fill(PANEL_X, PANEL_TOP_Y,
            PANEL_X + panelWidth, PANEL_TOP_Y + panelHeight, argb(bgColor.get()));

        double scale  = mc.getWindow().getScaleFactor();
        double mouseX = mc.mouse.getX() / scale;
        double mouseY = mc.mouse.getY() / scale;
        boolean copyDown = copyKey.get().isPressed();

        for (int li = 0; li < lines.size(); li++) {
            String line  = lines.get(li);
            int    lineX = PANEL_X + PADDING;
            int    lineY = PANEL_TOP_Y + PADDING + li * lineHeight;

            String[] tokens = line.split("(?<=\\s)|(?=\\s)");
            int cursorX = lineX;

            for (String token : tokens) {
                int    tokenW = tr.getWidth(token);
                String word   = token.strip();

                if (!word.isEmpty()) {
                    boolean hovered = !loading
                        && mouseX >= cursorX   && mouseX < cursorX + tokenW
                        && mouseY >= lineY - 1 && mouseY < lineY + tr.fontHeight + 1;

                    if (hovered) {
                        event.drawContext.fill(
                            cursorX - 1, lineY - 1,
                            cursorX + tokenW + 1, lineY + tr.fontHeight + 1,
                            argb(highlightColor.get())
                        );
                        if (copyDown) mc.keyboard.setClipboard(word);
                    }
                }

                event.drawContext.drawText(tr, token, cursorX, lineY, textColor.get().getPacked(), true);
                cursorX += tokenW;
            }
        }
    }

    private static int argb(SettingColor c) {
        return (c.a << 24) | (c.r << 16) | (c.g << 8) | c.b;
    }

    private static List<String> wordWrap(String text, TextRenderer tr, int maxWidth) {
        List<String> lines    = new ArrayList<>();
        String[]     words    = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (tr.getWidth(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines.isEmpty() ? List.of(text) : lines;
    }
}
