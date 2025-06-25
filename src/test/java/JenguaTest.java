import at.tobiazsh.jengua.*;
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
    private static final File FALLBACK_ENGLISH_US_BAK = new File("src/test/resources/fallback-en-US.json.bak");
    private static final File GERMAN_GERMANY = new File("src/test/resources/de-DE.json");
    private static final File GERMAN_AUSTRIA = new File("src/test/resources/de-AT.json");

    private Translator translator;

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

        String germanAustria = """
                {
                    "locale": "de-AT",
                    "Menu": {
                        "File": "Datei",
                        "Edit": "Beorbait'n",
                        "View": "Ousicht",
                        "Help": "Hüfe"
                    }
                }
                """;

        Files.writeString(FALLBACK_ENGLISH_US.toPath(), fallbackEnglishUS);
        Files.writeString(GERMAN_GERMANY.toPath(), germanGermany);
        Files.writeString(GERMAN_AUSTRIA.toPath(), germanAustria);

        // Load fallback language
        Language fallbackLanguage = LanguageLoader.loadLanguage(FALLBACK_ENGLISH_US);
        translator = new Translator(fallbackLanguage, fallbackLanguage);

        translator.loadLanguage(GERMAN_GERMANY);
        translator.loadLanguage(GERMAN_AUSTRIA);
    }

    @AfterEach
    public void cleanup() throws IOException {
        Files.deleteIfExists(FALLBACK_ENGLISH_US.toPath());
        Files.deleteIfExists(GERMAN_GERMANY.toPath());
        Files.deleteIfExists(GERMAN_AUSTRIA.toPath());
        Files.deleteIfExists(FALLBACK_ENGLISH_US_BAK.toPath());
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
    public void testTranslateGermanAustria() {
        translator.setLanguage("de-AT");
        String result = translator.tr("Menu", "Edit");

        Optional<Context> optionalContext = translator.getLanguage().getContext("Menu");

        if (optionalContext.isEmpty()) {
            fail("Context 'Menu' not found in Austrian German language");
        }

        Context menuContext = optionalContext.get();
        String expectedTranslation = menuContext.translations().get("Edit");

        assertEquals(expectedTranslation, result);
    }

    @Test
    public void testSaveLanguageCreatesBackupAndSaves() throws IOException {
        translator.tr("Menu", "Save"); // This will add the missing key with null

        // First save once to create the original file
        LanguageSaver.saveLanguage(translator.getFallbackLanguage(), FALLBACK_ENGLISH_US);
        assertTrue(FALLBACK_ENGLISH_US.exists());

        // Save again to trigger backup creation
        LanguageSaver.saveLanguage(translator.getFallbackLanguage(), FALLBACK_ENGLISH_US);
        assertTrue(FALLBACK_ENGLISH_US_BAK.exists());

        // Check file content includes null for missing keys
        String savedContent = Files.readString(FALLBACK_ENGLISH_US.toPath());
        assertTrue(savedContent.contains("\"Save\": null"));
    }
}
