package at.tobiazsh.jengua;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading a Language from a JSON file.
 */
public class LanguageLoader {

    private LanguageLoader() {
        // Private constructor to prevent instantiation
    }

    /**
     * Loads a Language from a JSON file.
     * The JSON file must contain a "locale" key and context objects with string translations.
     *
     * @param languageFile the JSON file to load
     * @return a Language object containing the locale and contexts
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the JSON structure is invalid
     */
    public static Language loadLanguage(File languageFile) throws IOException {
        try (Reader reader = new FileReader(languageFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            JsonObject rootObject = root.getAsJsonObject();

            // Verify structure
            verifyStructure(rootObject);

            // Extract locale
            String locale = rootObject.get("locale").getAsString();

            // Remove locale from root to isolate contexts
            rootObject.remove("locale");

            Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            Map<String, Map<String, String>> rawContexts = new Gson().fromJson(rootObject, type);

            if (rawContexts == null) rawContexts = new HashMap<>();

            Map<String, Context> contextMap = new HashMap<>();
            for (var entry : rawContexts.entrySet()) {
                contextMap.put(entry.getKey(), new Context(entry.getKey(), entry.getValue()));
            }

            return new Language(locale, contextMap);
        }
    }

    /**
     * Verifies the structure of the JSON Language Object.
     * @param root the root JSON object to verify
     * @throws IllegalArgumentException if the structure is invalid
     */
    public static void verifyStructure(JsonObject root) throws IllegalArgumentException {
        // Check if root is an object
        if (root == null) throw new IllegalArgumentException("JSON root is null!");

        // Check "locale" exists and is a string
        if (!root.has("locale") || !root.get("locale").isJsonPrimitive() || !root.get("locale").getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'locale' key is missing or not a string!");
        }

        // Check each context
        for (var entry : root.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (key.equals("locale")) continue; // Ignore locale key

            if (!value.isJsonObject()) {
                throw new IllegalArgumentException("Context '" + key + "' is not an object!");
            }

            JsonObject contextObject = value.getAsJsonObject();

            for (var translationEntry : contextObject.entrySet()) {
                JsonElement translationValue = translationEntry.getValue();
                if (translationValue.isJsonNull()) continue; // Indication that translation does not exist but is present for future translation
                if (!translationValue.isJsonPrimitive() || !translationValue.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Translation for key '" + translationEntry.getKey() + "' in context '" + key + "' is not a string!");
                }
            }
        }
    }
}
