package io.github.tobiazsh.jengua;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a context for translations, containing a key and a map of translations.
 * The translations map contains key-value pairs where the key is the translation key and the value is the translated string.
 *
 * @param contextKey the key for this context
 * @param translations a map of translations where the key is the translation key and the value is the translated string
 * @param subContexts a map of sub-contexts, where the key is the sub-context key and the value is the Context object for that sub-context
 */
public record Context(String contextKey, Map<String, String> translations, Map<String, Context> subContexts) {

    /**
     * Constructs a Context with the specified context key and translations.
     *
     * @param contextKey   the key for this context
     * @param translations a map of translations where the key is the translation key and the value is the translated string
     */
    public Context(String contextKey, Map<String, String> translations, Map<String, Context> subContexts) {
        this.contextKey = contextKey;
        this.translations = new HashMap<>(translations);
        this.subContexts = subContexts;
    }

    /**
     * Returns the context key for this context.
     *
     * @return the context key
     */
    public String contextKey() {
        return contextKey;
    }

    /**
     * Checks if the context contains a translation for the given key.
     *
     * @param key the key to check
     * @return true if the translations is found, otherwise false
     */
    public boolean containsTranslation(String key) {
        return translations.containsKey(key);
    }

    /**
     * Checks if the context contains a sub-context with the given key.
     *
     * @param key the key of the sub-context to check
     * @return true if the sub-context is found, otherwise false
     */
    public boolean containsContext(String key) {
        return subContexts.containsKey(key);
    }

    /**
     * Returns the translations map for this context.
     * The map contains key-value pairs where the key is the translation key and the value is the translated string.
     *
     * @return a map of translations
     */
    public Map<String, String> translations() {
        return translations;
    }

    public Map<String, Context> subContexts() {
        return subContexts;
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

        // Split the key to check for sub-contexts
        String[] parts = key.split("\\.");

        // If it's directed at a sub-context, translate via that
        if (parts.length > 1) {
            String nextContextKey = parts[1]; // Example: Menu.File.Quit... Menu is this, which is at index 0 and the next would be File, which is at index 1
            Context subContext = subContexts.get(parts[0]);

            if (subContext != null) {
                return subContext.translate(nextContextKey, parameters); // Translate via sub-context if found
            } else {
                return key; // Otherwise just return the key again
            }
        }

        if (translations.containsKey(key)) {
            return interpolate(translations.get(key), parameters);
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
