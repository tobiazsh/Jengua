package io.github.tobiazsh.jengua;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading a Language from a JSON file.
 */
public class LanguageLoader {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private LanguageLoader() {
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
            return parseLanguage(reader);
        }
    }

    /**
     * Loads a Language from a JSON file in resources (files, that are packed in the JAR).
     *
     * @param resourcePath the path to the JSON file in resources, e.g. "/languages/en-US.json"
     * @return a Language object containing the locale and contexts
     * @throws IOException if an error occurs while reading the resource
     */
    public static Language loadLanguageFromResources(String resourcePath) throws IOException {
        try (InputStream in = LanguageLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }
            try (Reader reader = new InputStreamReader(in)) {
                return parseLanguage(reader);
            }
        }
    }

    /**
     * Parses a Language from a Reader.
     */
    private static Language parseLanguage(Reader reader) {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Root element is not a JSON object!");
        }

        JsonObject rootObject = root.getAsJsonObject();

        // Verify structure
        verifyStructure(rootObject);

        // Locale
        String locale = rootObject.get("locale").getAsString(); // Extract locale
        rootObject.remove("locale"); // Remove locale from root to isolate contexts


        Type type = new TypeToken<Map<String, JsonObject>>() {}.getType();
        Map<String, JsonObject> rawContexts = new Gson().fromJson(rootObject, type);

        if (rawContexts == null) rawContexts = new HashMap<>();

        Map<String, Context> contextMap = new HashMap<>();
        for (var entry : rawContexts.entrySet()) {
            contextMap.put(entry.getKey(), createContext(entry.getKey(), entry.getValue()));
        }

        return new Language(locale, contextMap);
    }

    /**
     * Creates a Context from a JSON object. Checks for multiple levels of nesting
     * and creates a Context object with translations and sub-contexts.
     * @param contextKey The key for the context, used to identify it.
     * @param jsonObject The JSON object containing translations and possibly sub-contexts.
     * @return A new context object containing translations and sub-contexts.
     */
    private static Context createContext(String contextKey, JsonObject jsonObject) {
        Map<String, String> translations = new HashMap<>();
        Map<String, Context> subContexts = new HashMap<>();

        for (var entry : jsonObject.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.isJsonObject()) {
                // If the value is a nested object, treat it as a sub-context
                subContexts.put(entry.getKey(), createContext(entry.getKey(), value.getAsJsonObject()));
            } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                translations.put(entry.getKey(), value.getAsString());
            }
        }

        return new Context(contextKey, translations, subContexts);
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

            verifyContext(key, value.getAsJsonObject());
        }
    }

    /**
     * Verifies the context object for valid translations.
     * @param contextKey the key of the context being verified
     * @param contextObject the JSON object representing the context
     */
    public static void verifyContext(String contextKey, JsonObject contextObject) {
        for (var contextEntry : contextObject.entrySet()) {
            String key = contextEntry.getKey();
            JsonElement value = contextEntry.getValue();

            if (value.isJsonObject()){
                // If the value is a nested object, recursively verify it as a sub-context
                verifyContext(key, value.getAsJsonObject());
            } else if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Translation for key '" + key + "' in context '" + contextKey + "' is not a string or null!");
            }
        }
    }
}
