package io.github.tobiazsh.jengua;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

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
     * Checks if the context or any of its sub-contexts contains a translation for the given key, regardless of its value (translation can also be null!).
     *
     * @param key the key to check
     * @return true if the translation is found in this context or any sub-context, otherwise false
     */
    public boolean containsTranslationKeyAnywhere(String key) {
        // Check if the key exists in the translations map
        if (translations.containsKey(key))
            return true;

        // Check in sub-contexts recursively
        for (Context subContext : subContexts.values()) {
            if (subContext.containsTranslationKeyAnywhere(key)) {
                return true;
            }
        }

        // Key not found in this context or any sub-contexts
        return false;
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

    /**
     * Returns the sub-contexts map for this context.
     *
     * @return a map of sub-contexts where the key is the sub-context key and the value is the Context object for that sub-context
     */
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
            // Join the remainder of the key and pass it down to the sub-context
            Context subContext = subContexts.get(parts[0]);

            if (subContext != null) {
                String remainder = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
                return subContext.translate(remainder, parameters); // Translate via sub-context if found
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
     * New varargs overload: translate using positional placeholders ("{}").
     * Behavior/assumptions:
     * - If a translation exists for the key, that translation is used and any "{}" placeholders are replaced in order by the provided args.
     * - If no translation exists and the first vararg is a String, that String is treated as a fallback template and the remaining varargs are used to fill placeholders.
     * - If there are more placeholders than args, remaining placeholders are left as-is. Extra args are ignored.
     * Example: translate("err.key", "Found {} apples and {} pears", 2, 3)
     * @param key the key to translate
     * @param args the positional arguments to replace "{}" placeholders
     * @return the translated string
     */
    public String translate(String key, Object... args) {
        // Split the key to check for sub-contexts
        String[] parts = key.split("\\.");

        if (parts.length > 1) {
            Context subContext = subContexts.get(parts[0]);
            if (subContext != null) {
                String remainder = String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
                return subContext.translate(remainder, args);
            } else {
                return key;
            }
        }

        // If we have a stored translation, use it and perform positional interpolation
        if (translations.containsKey(key)) {
            String template = translations.get(key);
            if (template == null) return key;
            return interpolatePositional(template, args);
        }

        // No stored translation. If first arg is a String, treat it as a fallback template
        if (args != null && args.length > 0 && args[0] instanceof String) {
            String template = (String) args[0];
            Object[] rest = Arrays.copyOfRange(args, 1, args.length);
            return interpolatePositional(template, rest);
        }

        // Nothing to interpolate, return key
        return key;
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

    /**
     * Replaces positional placeholders `{}` in order with the provided args.
     */
    private String interpolatePositional(String template, Object... args) {
        if (template == null || template.isEmpty() || args == null || args.length == 0) return template;

        StringBuilder stringBuilder = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            int index = template.indexOf("{}", i);
            if (index == -1) {
                stringBuilder.append(template, i, template.length());
                break;
            }
            // append up to placeholder
            stringBuilder.append(template, i, index);
            // append arg if available
            if (argIndex < args.length) {
                stringBuilder.append(args[argIndex++]);
            } else {
                // no arg available, keep the placeholder
                stringBuilder.append("{}");
            }
            i = index + 2;
        }

        return stringBuilder.toString();
    }
}
