package io.github.tobiazsh.jengua;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a language with a code and a set of contexts for translations.
 * Each context contains translation keys and their corresponding translated strings.
 * The language can be used to translate keys in specific contexts with parameters.
 *
 * @param code     for example "en-US", "de-DE", "de-AT" or "es-ES"
 * @param contexts contextKey -> Context
 */
public record Language(String code, Map<String, Context> contexts) {

    /**
     * Constructs a new Language instance with the specified code and contexts.
     *
     * @param code     the language code (for example "en-US", "de-DE", "de-AT" or "es-ES")
     * @param contexts a map of context keys to Context objects
     */
    public Language(String code, Map<String, Context> contexts) {
        this.code = code;
        this.contexts = new HashMap<>(contexts);
    }

    /**
     * Returns the language code of this language. (for example "en-US", "de-DE", "de-AT" or "es-ES")
     *
     * @return the language code
     */
    @Override
    public String code() {
        return code;
    }

    /**
     * adds a new context to this language. Also see {@link Context}.
     *
     * @param context the context to add
     **/
    public void addContext(Context context) {
        contexts.put(context.contextKey(), context);
    }

    /**
     * Returns the context with the specified key.
     *
     * @param contextKey the key of the context to retrieve
     * @return an Optional containing the Context if found, or null if not found
     */
    public Optional<Context> getContext(String contextKey) {
        return Optional.ofNullable(contexts.get(contextKey));
    }

    /**
     * Returns a set of all contexts in this language.
     *
     * @return a Map containing all Context objects in this language
     */
    @Override
    public Map<String, Context> contexts() {
        return Map.copyOf(contexts);
    }

    /**
     * Translates the given key in the specified context with the provided parameters.
     * If the context or key is not found, it returns the key itself.
     *
     * @param contextKey the key of the context to use for translation
     * @param key        the key to translate
     * @param parameters a map of parameters to replace in the translation template
     * @return the translated string or the key if not found
     */
    public String translate(String contextKey, String key, Map<String, Object> parameters) {
        Context currentContext = getCtx(contextKey);
        return currentContext == null ? key : currentContext.translate(key, parameters);
    }

    public String translate(String contextKey, String key, Object... params) {
        Context currentContext = getCtx(contextKey);
        return currentContext == null ? key : currentContext.translate(key, params);
    }

    /**
     * Retrieves the Context object based on a dot-separated context key.
     * @param ctxKey the dot-separated context key
     * @return the Context object if found, otherwise returns null!
     */
    private Context getCtx(String ctxKey) {
        String[] parts = ctxKey.split("\\.");
        Context currentContext = null;

        for (String part : parts) {
            if (currentContext == null) {
                // TLC (Top-Level Context)
                currentContext = contexts.get(part);
            } else {
                // NST (Nested Sub-Context)
                currentContext = currentContext.subContexts().get(part);
            }

            if (currentContext == null) {
                // Context not found; return null
                return null;
            }
        }

        return currentContext;
    }

    /**
     * Checks if this language contains the specified translation key in any context regardless of its value (translation can also be null!).
     * @param key the translation key to check
     * @return true if the key exists in any context, false otherwise
     */
    public boolean containsTranslationKeyAnywhere(String key) {
        return contexts.values().stream()
                .anyMatch(context -> context.containsTranslationKeyAnywhere(key));
    }
}
