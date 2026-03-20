package com.example.addon.modules;

import com.example.addon.AquaTranslate;
import com.example.addon.TranslateAPI;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

public class TranslateChat extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<TargetLanguage> targetLanguage = sgGeneral.add(new EnumSetting.Builder<TargetLanguage>()
        .name("target-language")
        .description("Language to translate chat messages into.")
        .defaultValue(TargetLanguage.English)
        .build()
    );

    public String getTargetCode() { return targetLanguage.get().code; }

    public TranslateChat() {
        super(AquaTranslate.CATEGORY, "translate-chat",
            "Hover over any chat message to see its translation and detected language.");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        Text original = event.getMessage();

        // Get flat string and strip username prefix (<Name> or [Name]) before translating
        String fullText = original.getString();
        if (fullText.isBlank()) return;

        String targetCode = targetLanguage.get().code;

        // Build a mutable copy with a placeholder "Translating..." hover tooltip.
        // We use event.setMessage() so Meteor inserts our version directly — no cancel+re-add needed.
        MutableText modified = original.copy();
        modified.setStyle(modified.getStyle().withHoverEvent(
            new HoverEvent.ShowText(
                Text.literal("Translating...").formatted(Formatting.GRAY)
            )
        ));
        event.setMessage(modified);

        // Async translation — when it comes back, mutate the style on the same object.
        // The Text object is already in the chat list; mutating its style is safe because
        // Minecraft re-reads the style every render frame from the stored Text reference.
        TranslateAPI.translateAsync(fullText, targetCode, result -> {
            if (result == null || result.translation().isBlank()) {
                // Remove the placeholder tooltip so the message looks normal
                modified.setStyle(modified.getStyle().withHoverEvent(null));
                return;
            }

            String langName = langCodeToName(result.detectedLang());

            MutableText tooltip = Text.literal("Translation: ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(result.translation()).formatted(Formatting.WHITE));

            if (!langName.isBlank()) {
                tooltip.append(Text.literal("\nFrom: ").formatted(Formatting.GRAY))
                       .append(Text.literal(langName).formatted(Formatting.AQUA));
            }

            modified.setStyle(modified.getStyle().withHoverEvent(
                new HoverEvent.ShowText(tooltip)
            ));
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String stripUsername(String text) {
        // "<PlayerName> message" format
        if (text.startsWith("<")) {
            int close = text.indexOf('>');
            if (close > 0 && close + 1 < text.length())
                return text.substring(close + 1).trim();
        }
        // "[PlayerName] message" format (some server plugins)
        if (text.startsWith("[")) {
            int close = text.indexOf(']');
            if (close > 0 && close + 1 < text.length())
                return text.substring(close + 1).trim();
        }
        return text;
    }

    private static String langCodeToName(String code) {
        if (code == null) return "";
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
        // Common Minecraft player languages first
        English("en"), German("de"), French("fr"), Spanish("es"), Portuguese("pt"),
        Russian("ru"), Japanese("ja"), Chinese_Simplified("zh-CN"), Korean("ko"),
        Polish("pl"), Dutch("nl"), Italian("it"), Turkish("tr"),
        // Rest alphabetical
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
