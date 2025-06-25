package at.tobiazsh.jengua;

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
        Context context = contexts.get(contextKey);
        if (context == null) return key;
        return context.translate(key, parameters);
    }
}
