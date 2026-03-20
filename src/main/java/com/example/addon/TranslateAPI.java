package com.example.addon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TranslateAPI {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Gson GSON = new Gson();
    private static final Map<String, String> CACHE = new HashMap<>();

    private static final String BASE_URL =
        "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&dt=t&q=";

    public record TranslateResult(String translation, String detectedLang) {}

    public static void translateAsync(String text, String targetLang, Consumer<TranslateResult> callback) {
        if (text == null || text.isBlank()) {
            callback.accept(null);
            return;
        }

        String cacheKey = targetLang + ":" + text;
        if (CACHE.containsKey(cacheKey)) {
            String cached = CACHE.get(cacheKey);
            int sep = cached.indexOf('|');
            callback.accept(sep >= 0
                ? new TranslateResult(cached.substring(sep + 1), cached.substring(0, sep))
                : new TranslateResult(cached, ""));
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = BASE_URL + encoded
                    + "&tl=" + URLEncoder.encode(targetLang, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

                HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonArray root = GSON.fromJson(response.body(), JsonArray.class);

                    String detectedLang = "";
                    if (root.size() > 2 && !root.get(2).isJsonNull()) {
                        detectedLang = root.get(2).getAsString();
                        if (detectedLang.equalsIgnoreCase(targetLang)) return null;
                    }

                    JsonArray sentences = root.get(0).getAsJsonArray();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sentences.size(); i++) {
                        JsonArray s = sentences.get(i).getAsJsonArray();
                        if (!s.get(0).isJsonNull()) sb.append(s.get(0).getAsString());
                    }

                    String result = sb.toString().trim();
                    if (result.isEmpty()) return null;
                    return new TranslateResult(result, detectedLang);
                }
            } catch (Exception e) {
                AquaTranslate.LOG.error("Translation failed: {}", e.getMessage());
            }
            return null;
        }).thenAccept(result -> {
            if (result != null)
                CACHE.put(cacheKey, result.detectedLang() + "|" + result.translation());
            callback.accept(result);
        });
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static String resolveLanguageCode(String input) {
        if (input == null) return null;
        return switch (input.toLowerCase()) {
            case "english",    "en"                           -> "en";
            case "german",     "de", "deutsch"                -> "de";
            case "french",     "fr", "français", "francais"   -> "fr";
            case "spanish",    "es", "español",  "espanol"    -> "es";
            case "portuguese", "pt", "português","portugues"  -> "pt";
            case "russian",    "ru"                           -> "ru";
            case "japanese",   "ja", "jp"                     -> "ja";
            case "chinese",    "zh", "zh-cn", "mandarin"      -> "zh-CN";
            case "korean",     "ko", "kr"                     -> "ko";
            case "polish",     "pl"                           -> "pl";
            case "dutch",      "nl"                           -> "nl";
            case "italian",    "it"                           -> "it";
            case "turkish",    "tr"                           -> "tr";
            case "afrikaans",  "af"  -> "af";
            case "albanian",   "sq"  -> "sq";
            case "arabic",     "ar"  -> "ar";
            case "armenian",   "hy"  -> "hy";
            case "azerbaijani","az"  -> "az";
            case "basque",     "eu"  -> "eu";
            case "belarusian", "be"  -> "be";
            case "bengali",    "bn"  -> "bn";
            case "bosnian",    "bs"  -> "bs";
            case "bulgarian",  "bg"  -> "bg";
            case "catalan",    "ca"  -> "ca";
            case "croatian",   "hr"  -> "hr";
            case "czech",      "cs"  -> "cs";
            case "danish",     "da"  -> "da";
            case "estonian",   "et"  -> "et";
            case "filipino",   "tl"  -> "tl";
            case "finnish",    "fi"  -> "fi";
            case "galician",   "gl"  -> "gl";
            case "georgian",   "ka"  -> "ka";
            case "greek",      "el"  -> "el";
            case "gujarati",   "gu"  -> "gu";
            case "haitian creole", "ht" -> "ht";
            case "hebrew",     "he"  -> "he";
            case "hindi",      "hi"  -> "hi";
            case "hungarian",  "hu"  -> "hu";
            case "icelandic",  "is"  -> "is";
            case "indonesian", "id"  -> "id";
            case "irish",      "ga"  -> "ga";
            case "kannada",    "kn"  -> "kn";
            case "kazakh",     "kk"  -> "kk";
            case "latvian",    "lv"  -> "lv";
            case "lithuanian", "lt"  -> "lt";
            case "macedonian", "mk"  -> "mk";
            case "malay",      "ms"  -> "ms";
            case "maltese",    "mt"  -> "mt";
            case "marathi",    "mr"  -> "mr";
            case "mongolian",  "mn"  -> "mn";
            case "nepali",     "ne"  -> "ne";
            case "norwegian",  "no"  -> "no";
            case "persian",    "fa"  -> "fa";
            case "punjabi",    "pa"  -> "pa";
            case "romanian",   "ro"  -> "ro";
            case "serbian",    "sr"  -> "sr";
            case "slovak",     "sk"  -> "sk";
            case "slovenian",  "sl"  -> "sl";
            case "swahili",    "sw"  -> "sw";
            case "swedish",    "sv"  -> "sv";
            case "tamil",      "ta"  -> "ta";
            case "telugu",     "te"  -> "te";
            case "thai",       "th"  -> "th";
            case "ukrainian",  "uk"  -> "uk";
            case "urdu",       "ur"  -> "ur";
            case "uzbek",      "uz"  -> "uz";
            case "vietnamese", "vi"  -> "vi";
            case "welsh",      "cy"  -> "cy";
            case "yiddish",    "yi"  -> "yi";
            default -> null;
        };
    }
}
