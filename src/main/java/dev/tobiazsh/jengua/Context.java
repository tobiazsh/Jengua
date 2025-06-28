package dev.tobiazsh.jengua;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a context for translations, containing a key and a map of translations.
 * The translations map contains key-value pairs where the key is the translation key and the value is the translated string.
 *
 * @param contextKey the key for this context
 * @param translations a map of translations where the key is the translation key and the value is the translated string
 */
public record Context(String contextKey, Map<String, String> translations) {

    /**
     * Constructs a Context with the specified context key and translations.
     *
     * @param contextKey   the key for this context
     * @param translations a map of translations where the key is the translation key and the value is the translated string
     */
    public Context(String contextKey, Map<String, String> translations) {
        this.contextKey = contextKey;
        this.translations = new HashMap<>(translations);
    }

    /**
     * Returns the context key for this context.
     *
     * @return the context key
     */
    @Override
    public String contextKey() {
        return contextKey;
    }

    /**
     * Checks if the context contains a translation for the given key.
     *
     * @param key the key to check
     * @return true if the translations is found, otherwise false
     */
    public boolean contains(String key) {
        return translations.containsKey(key);
    }

    /**
     * Returns the translations map for this context.
     * The map contains key-value pairs where the key is the translation key and the value is the translated string.
     *
     * @return a map of translations
     */
    @Override
    public Map<String, String> translations() {
        return translations;
    }

    /**
     * Translates the given key using the context's translations.
     * If the key is not found, it returns the key itself.
     *
     * @param key the key to translate
     * @return the translated string or the key if not found
     */
    public String translate(String key) {
        return translate(key, Map.of());
    }

    /**
     * Translates the given key using the context's translations, with parameters for interpolation.
     * If the key is not found, it returns the key itself.
     *
     * @param key        the key to translate
     * @param parameters a map of parameters for interpolation
     * @return the translated string with parameters interpolated, or the key if not found
     */
    public String translate(String key, Map<String, Object> parameters) {
        if (!translations.containsKey(key)) {
            return key; // Return the key itself if not found
        }

        String value = translations.getOrDefault(key, key);
        if (value == null) return key;

        return interpolate(value, parameters);
    }

    /**
     * This method replaces placeholders in the format {key} with the corresponding values from the parameters map.
     */
    private String interpolate(String template, Map<String, Object> parameters) {
        String result = template;

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            result = result.replace(placeholder, String.valueOf(entry.getValue()));
        }

        return result;
    }
}
