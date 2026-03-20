package com.example.addon.modules;

import com.example.addon.AquaTranslate;
import com.example.addon.TranslateAPI;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class TranslateSigns extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<TargetLanguage> targetLanguage = sgGeneral.add(new EnumSetting.Builder<TargetLanguage>()
        .name("target-language")
        .description("Language to translate sign text into.")
        .defaultValue(TargetLanguage.English)
        .build()
    );

    private final Setting<Boolean> showSourceLang = sgGeneral.add(new BoolSetting.Builder()
        .name("show-source-language")
        .description("Show a label indicating which language the sign was detected as.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Color of the translation overlay text.")
        .defaultValue(new SettingColor(0, 200, 255, 255))
        .build()
    );

    private final Setting<SettingColor> sourceLangColor = sgGeneral.add(new ColorSetting.Builder()
        .name("source-lang-color")
        .description("Color of the 'From: X' source language label.")
        .defaultValue(new SettingColor(180, 180, 180, 255))
        .build()
    );

    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color behind the translation text. Adjust alpha for opacity.")
        .defaultValue(new SettingColor(0, 0, 0, 140))
        .build()
    );

    private final Map<BlockPos, TranslateAPI.TranslateResult> translationCache = new HashMap<>();
    private final Map<BlockPos, Boolean> pending = new HashMap<>();
    private BlockPos lastLookedAt = null;
    private TargetLanguage lastUsedLang = null;

    public TranslateSigns() {
        super(AquaTranslate.CATEGORY, "translate-signs", "Shows a translation when you look at a sign.");
    }

    @Override
    public void onDeactivate() {
        translationCache.clear();
        pending.clear();
        lastLookedAt = null;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        TargetLanguage currentLang = targetLanguage.get();
        if (currentLang != lastUsedLang) {
            translationCache.clear();
            pending.clear();
            lastLookedAt = null;
            lastUsedLang = currentLang;
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            lastLookedAt = null;
            return;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        if (!(mc.world.getBlockEntity(pos) instanceof SignBlockEntity sign)) {
            lastLookedAt = null;
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            Text line = sign.getFrontText().getMessage(i, false);
            String lineStr = line.getString().trim();
            if (!lineStr.isEmpty()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(lineStr);
            }
        }

        String signText = sb.toString().trim();
        if (signText.isEmpty()) return;

        if (!pos.equals(lastLookedAt)) {
            lastLookedAt = pos;
            if (!translationCache.containsKey(pos) && !pending.getOrDefault(pos, false)) {
                pending.put(pos, true);
                TranslateAPI.translateAsync(signText, currentLang.code, result -> {
                    pending.remove(pos);
                    if (result != null) translationCache.put(pos, result);
                });
            }
        }

        TranslateAPI.TranslateResult result = translationCache.get(pos);
        if (result == null) return;

        TextRenderer textRenderer = mc.textRenderer;
        int screenWidth  = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        String translationLabel = "Translation: " + result.translation();
        int textWidth = textRenderer.getWidth(translationLabel);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 2 + 20;

        int padding    = 4;
        int lineHeight = textRenderer.fontHeight + 2;

        boolean drawSourceLang = showSourceLang.get() && !result.detectedLang().isBlank();
        int bgHeight = drawSourceLang
            ? (lineHeight + textRenderer.fontHeight + padding * 2)
            : (textRenderer.fontHeight + padding * 2);

        SettingColor bg = bgColor.get();
        int bgArgb = (bg.a << 24) | (bg.r << 16) | (bg.g << 8) | bg.b;
        event.drawContext.fill(x - padding, y - padding,
            x + textWidth + padding, y - padding + bgHeight, bgArgb);

        event.drawContext.drawText(textRenderer, translationLabel, x, y, textColor.get().getPacked(), true);

        if (drawSourceLang) {
            String langName  = langCodeToName(result.detectedLang());
            String fromLabel = "From: " + langName;
            int fromX = (screenWidth - textRenderer.getWidth(fromLabel)) / 2;
            event.drawContext.drawText(textRenderer, fromLabel, fromX, y + lineHeight,
                sourceLangColor.get().getPacked(), true);
        }
    }

    private static String langCodeToName(String code) {
        return switch (code.toLowerCase()) {
            case "af" -> "Afrikaans"; case "sq" -> "Albanian";   case "ar" -> "Arabic";
            case "hy" -> "Armenian";  case "az" -> "Azerbaijani";case "eu" -> "Basque";
            case "be" -> "Belarusian";case "bn" -> "Bengali";    case "bs" -> "Bosnian";
            case "bg" -> "Bulgarian"; case "ca" -> "Catalan";
            case "zh", "zh-cn" -> "Chinese (Simplified)";
            case "zh-tw" -> "Chinese (Traditional)";
            case "hr" -> "Croatian";  case "cs" -> "Czech";      case "da" -> "Danish";
            case "nl" -> "Dutch";     case "en" -> "English";    case "et" -> "Estonian";
            case "tl" -> "Filipino";  case "fi" -> "Finnish";    case "fr" -> "French";
            case "gl" -> "Galician";  case "ka" -> "Georgian";   case "de" -> "German";
            case "el" -> "Greek";     case "gu" -> "Gujarati";   case "ht" -> "Haitian Creole";
            case "he" -> "Hebrew";    case "hi" -> "Hindi";      case "hu" -> "Hungarian";
            case "is" -> "Icelandic"; case "id" -> "Indonesian"; case "ga" -> "Irish";
            case "it" -> "Italian";   case "ja" -> "Japanese";   case "kn" -> "Kannada";
            case "kk" -> "Kazakh";    case "ko" -> "Korean";     case "lv" -> "Latvian";
            case "lt" -> "Lithuanian";case "mk" -> "Macedonian"; case "ms" -> "Malay";
            case "mt" -> "Maltese";   case "mr" -> "Marathi";    case "mn" -> "Mongolian";
            case "ne" -> "Nepali";    case "no" -> "Norwegian";  case "fa" -> "Persian";
            case "pl" -> "Polish";    case "pt" -> "Portuguese"; case "pa" -> "Punjabi";
            case "ro" -> "Romanian";  case "ru" -> "Russian";    case "sr" -> "Serbian";
            case "sk" -> "Slovak";    case "sl" -> "Slovenian";  case "es" -> "Spanish";
            case "sw" -> "Swahili";   case "sv" -> "Swedish";    case "ta" -> "Tamil";
            case "te" -> "Telugu";    case "th" -> "Thai";       case "tr" -> "Turkish";
            case "uk" -> "Ukrainian"; case "ur" -> "Urdu";       case "uz" -> "Uzbek";
            case "vi" -> "Vietnamese";case "cy" -> "Welsh";      case "yi" -> "Yiddish";
            default -> code.toUpperCase();
        };
    }

    public enum TargetLanguage {
        English("en"), German("de"), French("fr"), Spanish("es"), Portuguese("pt"),
        Russian("ru"), Japanese("ja"), Chinese_Simplified("zh-CN"), Korean("ko"),
        Polish("pl"), Dutch("nl"), Italian("it"), Turkish("tr"),
        Afrikaans("af"), Albanian("sq"), Arabic("ar"), Armenian("hy"), Azerbaijani("az"),
        Basque("eu"), Belarusian("be"), Bengali("bn"), Bosnian("bs"), Bulgarian("bg"),
        Catalan("ca"), Chinese_Traditional("zh-TW"), Croatian("hr"), Czech("cs"),
        Danish("da"), Estonian("et"), Filipino("tl"), Finnish("fi"), Galician("gl"),
        Georgian("ka"), Greek("el"), Gujarati("gu"), Haitian_Creole("ht"), Hebrew("he"),
        Hindi("hi"), Hungarian("hu"), Icelandic("is"), Indonesian("id"), Irish("ga"),
        Kannada("kn"), Kazakh("kk"), Latvian("lv"), Lithuanian("lt"), Macedonian("mk"),
        Malay("ms"), Maltese("mt"), Marathi("mr"), Mongolian("mn"), Nepali("ne"),
        Norwegian("no"), Persian("fa"), Punjabi("pa"), Romanian("ro"), Serbian("sr"),
        Slovak("sk"), Slovenian("sl"), Swahili("sw"), Swedish("sv"), Tamil("ta"),
        Telugu("te"), Thai("th"), Ukrainian("uk"), Urdu("ur"), Uzbek("uz"),
        Vietnamese("vi"), Welsh("cy"), Yiddish("yi");

        public final String code;
        TargetLanguage(String code) { this.code = code; }
    }
}
