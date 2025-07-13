import io.github.tobiazsh.jengua.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class JenguaTest {

    private static final File FALLBACK_ENGLISH_US = new File("src/test/resources/fallback-en-US.json");
    private static final File GERMAN_GERMANY = new File("src/test/resources/de-DE.json");

    private static final File FALLBACK_ENGLISH_NESTED = new File("src/test/resources/fallback-en-US-nested.json");
    private static final File GERMAN_GERMANY_NESTED = new File("src/test/resources/de-DE-nested.json");

    private static final File CUSTOM_LANGUAGE_FILE = new File("src/test/resources/de-DE_CUSTOM.json");

    private Translator translator;
    private Translator nestedTranslator;

    @BeforeEach
    public void setup() throws Exception {
        // Prepare three simple language JSON for testing
        String fallbackEnglishUS = """
                {
                    "locale": "en-US",
                    "Menu": {
                        "File": "File",
                        "Edit": "Edit",
                        "View": "View",
                        "Help": "Help"
                    }
                }
               """;

        String germanGermany = """
                {
                    "locale": "de-DE",
                    "Menu": {
                        "File": "Datei",
                        "Edit": "Bearbeiten",
                        "View": "Ansicht",
                        "Help": "Hilfe"
                    }
                }
                """;


        String nestedGerman = """
                {
                    "locale": "de-DE",
                    "Menu": {
                        "File": "Datei",
                        "Edit": "Bearbeiten",
                        "View": "Ansicht",
                        "Help": "Hilfe"
                    },
                    "TheBetterMenu": {
                        "File": {
                            "File": "Datei",
                            "Save": "Speichern",
                            "Open": "Öffnen"
                        },
                        "Edit": {
                            "Edit": "Bearbeiten",
                            "Undo": "Rückgängig machen",
                            "Redo": "Wiederholen",
                            "More": {
                                "More..": "Mehr..",
                                "Dog": "Hund",
                                "Cat": "Katze"
                            }
                        }
                    }
                }
                """;
        String nested = """
                {
                    "locale": "en-US",
                    "Menu": {
                        "File": "File",
                        "Edit": "Edit",
                        "View": "View",
                        "Help": "Help"
                    },
                    "TheBetterMenu": {
                        "File": {
                            "File": "File",
                            "Save": "Save",
                            "Open": "Open"
                        },
                        "Edit": {
                            "Edit": "Edit",
                            "Undo": "Undo",
                            "Redo": "Redo",
                            "More": {
                                "More..": "More..",
                                "Dog": "Dog",
                                "Cat": "Cat"
                            }
                        }
                    }
                }
                """;

        Files.writeString(FALLBACK_ENGLISH_US.toPath(), fallbackEnglishUS);
        Files.writeString(GERMAN_GERMANY.toPath(), germanGermany);

        Files.writeString(FALLBACK_ENGLISH_NESTED.toPath(), nested);
        Files.writeString(GERMAN_GERMANY_NESTED.toPath(), nestedGerman);

        // Load fallback language
        Language fallbackLanguage = LanguageLoader.loadLanguage(FALLBACK_ENGLISH_US);
        translator = new Translator(fallbackLanguage, fallbackLanguage);

        // Load nested fallback language
        Language nestedFallbackLanguage = LanguageLoader.loadLanguage(FALLBACK_ENGLISH_NESTED);
        nestedTranslator = new Translator(nestedFallbackLanguage, nestedFallbackLanguage);

        translator.loadLanguage(GERMAN_GERMANY);
        nestedTranslator.loadLanguage(GERMAN_GERMANY_NESTED);
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(FALLBACK_ENGLISH_US.toPath());
        Files.deleteIfExists(GERMAN_GERMANY.toPath());
        Files.deleteIfExists(GERMAN_GERMANY_NESTED.toPath());
        Files.deleteIfExists(FALLBACK_ENGLISH_NESTED.toPath());
        //Files.deleteIfExists(CUSTOM_LANGUAGE_FILE.toPath());
    }

    @Test
    public void testTranslateExistingKeys() {
        String result = translator.tr("Menu", "File");
        assertEquals("File", result);
    }

    @Test
    public void testTranslateMissingKeyAddsNull() {
        String missingKey = "Save";

        String result = translator.tr("Menu", missingKey);
        assertEquals(missingKey, result);

        // The missing key should be added with null in fallback language
        Language fallback = translator.getFallbackLanguage();
        Context menuContext = fallback.getContext("Menu").orElseThrow();

        assertTrue(menuContext.translations().containsKey(missingKey));
        assertNull(menuContext.translations().get(missingKey));
    }

    @Test
    public void testTranslateGermanGermany() {
        translator.setLanguage("de-DE");
        String result = translator.tr("Menu", "File");

        Optional<Context> optionalContext = translator.getLanguage().getContext("Menu");

        if (optionalContext.isEmpty()) {
            fail("Context 'Menu' not found in German language");
        }

        Context menuContext = optionalContext.get();
        String expectedTranslation = menuContext.translations().get("File");

        assertEquals(expectedTranslation, result);
    }

    @Test
    public void testTranslateGermanGermanyNested() {
        nestedTranslator.setLanguage("de-DE");
        String result = nestedTranslator.tr("TheBetterMenu.Edit.More", "Dog");

        Optional<Context> optionalContext = nestedTranslator.getLanguage().getContext("TheBetterMenu");

        if (optionalContext.isEmpty()) {
            fail("Context 'Menu2' not found in German nested language");
        }

        Context menu2Context = optionalContext.get();
        String expectedTranslation = menu2Context.subContexts().get("Edit").subContexts().get("More").translations().get("Dog");

        assertEquals(expectedTranslation, result);
    }

    @Test
    public void testSaveLanguageCreatesBackupAndSaves() throws IOException {
        translator.tr("Menu", "Save"); // This will add the missing key with null
        assertNotNull(translator.tr("Menu", "Save")); // Check if uses "null" as translation

        // First save once to create the original file
        LanguageSaver.saveLanguage(translator.getFallbackLanguage(), FALLBACK_ENGLISH_US);
        assertTrue(FALLBACK_ENGLISH_US.exists());

        // Check file content includes null for missing keys
        String savedContent = Files.readString(FALLBACK_ENGLISH_US.toPath());
        assertTrue(savedContent.contains("\"Save\": null"));
    }

    @Test
    public void saveToFile() throws IOException {
        translator.tr("Menu", "Save"); // This will add the missing key with null

        LanguageSaver.saveLanguageFileTo(translator.getFallbackLanguage(), CUSTOM_LANGUAGE_FILE);
        assertTrue(CUSTOM_LANGUAGE_FILE.exists());

        String savedContent = Files.readString(CUSTOM_LANGUAGE_FILE.toPath());
        assertTrue(savedContent.contains("\"Save\": null"));
    }
}
