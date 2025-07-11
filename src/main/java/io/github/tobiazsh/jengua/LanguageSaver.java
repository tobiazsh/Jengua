package io.github.tobiazsh.jengua;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Utility class for saving a Language to a JSON file.
 * It creates a backup of the original file before saving the new language data.
 */
public class LanguageSaver {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    /**
     * Creates a backup of the given language in the specified path.
     * @param file the language file to back up
     * @throws IOException if renaming the original file fails
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createBackup(File file) throws IOException {
        if (file.exists()) {
            File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
            if (backupFile.exists()) backupFile.delete();
            if (!file.renameTo(backupFile)) {
                throw new IOException("Could not create backup for file: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Creates a backup of the current file (if exists) and saves the given language to a JSON file.
     * The JSON structure will contain a "locale" key and context objects with string translations.
     *
     * @param language the Language object to save
     * @param languageFile the file of the given language
     * @throws IOException if an error occurs while writing to the file
     */
    public static void saveLanguage(Language language, File languageFile) throws IOException {
        createBackup(languageFile); // Create a backup before saving

        JsonObject root = new JsonObject();
        root.addProperty("locale", language.code());

        for (Map.Entry<String, Context> contextEntry : language.contexts().entrySet()) {
            JsonObject contextJson = serializeContext(contextEntry.getValue());
            root.add(contextEntry.getKey(), contextJson);
        }

        try (Writer writer = new FileWriter(languageFile)) {
            gson.toJson(root, writer);
        }
    }

    /**
     * Recursively serializes a Context object, including its sub-contexts.
     * @param context the Context to serialize
     * @return a JsonObject representing the serialized context
     */
    private static JsonObject serializeContext(Context context) {
        JsonObject contextJson = new JsonObject();

        // Add translations
        for (Map.Entry<String, String> translationEntry : context.translations().entrySet()) {
            if (translationEntry.getValue() == null) {
                contextJson.add(translationEntry.getKey(), JsonNull.INSTANCE);
            } else {
                contextJson.addProperty(translationEntry.getKey(), translationEntry.getValue());
            }
        }

        // Add sub-contexts
        for (Map.Entry<String, Context> subContextEntry : context.subContexts().entrySet()) {
            JsonObject subContextJson = serializeContext(subContextEntry.getValue());
            contextJson.add(subContextEntry.getKey(), subContextJson);
        }

        return contextJson;
    }
}
