package io.github.tobiazsh.jengua;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**Translator class for managing multiple languages and translating keys in specific contexts.
 * It allows loading languages from files, setting the current language, and translating keys with parameters.
 * If a translation is not found in the current language, it falls back to a specified fallback language.
 */
public class Translator {

    private final Map<String, Language> languages = new HashMap<>();
    private final Language fallbackLanguage;
    private Language currentLanguage;

    public Translator(Language defaultLanguage, Language fallbackLanguage) {
        this.currentLanguage = defaultLanguage;
        this.fallbackLanguage = fallbackLanguage;
        this.languages.put(fallbackLanguage.code(), fallbackLanguage);

        if (fallbackLanguage != defaultLanguage) { // Prevent duplicate keys
            this.languages.put(defaultLanguage.code(), defaultLanguage);
        }
    }

    /**
     * Loads a language from a file and adds it to the translator.
     * @param file the file containing the language data in JSON format
     * @throws IOException if an error occurs while reading the file
     */
    public void loadLanguage(File file) throws IOException {
        Language language = LanguageLoader.loadLanguage(file);
        languages.put(language.code(), language);
    }

    /**
     * Loads a language from a file and adds it to the translator.
     * @param file the file containing the language data in JSON format
     * @throws IOException if an error occurs while reading the file
     */
    public void loadLanguageFromResources(Path file) throws IOException {
        Language language = LanguageLoader.loadLanguageFromResources(file.toString());
        languages.put(language.code(), language);
    }

    /**
     * Adds a language to the translator.
     * @param language the language to add
     * @throws IllegalArgumentException if Language is null
     */
    public void addLanguage(Language language) throws IllegalArgumentException {
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be null");
        }

        if (languages.containsKey(language.code())) {
            return; // Language already loaded, no need to add again
        }

        languages.put(language.code(), language);
    }

    /**
     * Sets the current language for translations.
     * @param languageCode the code of the language to set as current
     */
    public void setLanguage(String languageCode) {
        Language language = languages.get(languageCode);

        if (language == null) {
            throw new IllegalArgumentException("Language not loaded: " + languageCode);
        }

        this.currentLanguage = language;
    }

    /**
     * Translates key in context.
     * Auto-adds missing translations to the fallback language. Their value will be null, indicating that they are untranslated.
     * @param context the context in the language
     * @param key the key to translate
     * @return the translated string, or the key itself if no translation is found
     */
    public String tr(String context, String key) {
        return tr(context, key, Map.of());
    }

    /**
     * Translates key in context with parameters.
     * Auto-adds missing translations to the fallback language. Their value will be null, indicating that they are untranslated.
     * @param context the context in the language
     * @param key the key to translate
     * @param parameters a map of parameters to replace in the translation template
     * @return the translated string, or the key itself if no translation is found
     */
    public String tr(String context, String key, Map<String, Object> parameters) {
        if (currentLanguage != null) {
            String result = currentLanguage.translate(context, key, parameters);
            if (!result.equals(key)) return result;
        }

        if (fallbackLanguage != null) {
            String fallback = fallbackLanguage.translate(context, key, parameters);
            if (!fallback.equals(key)) return fallback;

            addMissingTranslation(fallbackLanguage, context, key);
        }

        // No fallback language available; cannot translate. Return key.
        return key;
    }

    private void addMissingTranslation(Language language, String contextKey, String missingKey) {
    String[] contextParts = contextKey.split("\\."); // Split the contextKey into parts
    Context currentContext = null;

    for (String part : contextParts) {
        if (currentContext == null) {
            // Top-level context
            currentContext = language.getContext(part).orElse(null);
            if (currentContext == null) {
                currentContext = new Context(part, new HashMap<>(), new HashMap<>());
                language.addContext(currentContext);
            }
        } else {
            // Nested sub-context
            Map<String, Context> subContexts = currentContext.subContexts();
            currentContext = subContexts.computeIfAbsent(part, k -> new Context(k, new HashMap<>(), new HashMap<>()));
        }
    }

    // Add the missing translation to the bottom-most context
    Map<String, String> translations = currentContext != null ? currentContext.translations() : new HashMap<>();
    if (!translations.containsKey(missingKey)) {
        translations.put(missingKey, null); // null means untranslated
    }
}
    /**
     * Returns all available languages in the translator.
     * @return a {@link Set} of language codes representing the available languages
     */
    public Set<String> getAvailableLanguages() {
        return languages.keySet();
    }

    /**
     * Returns the fallback language used when no translation is found in the current language.
     * @return the fallback Language
     */
    public Language getFallbackLanguage() {
        return fallbackLanguage;
    }

    /**
     * Returns the current language used for translations.
     * @return the current Language
     */
    public Language getLanguage() {
        return currentLanguage;
    }
}
