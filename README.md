# Jengua
**A simple translation library for all kinds of Java Programs**

The name Jengua consists of "Java" and "Lengua", which means "Language" in Spanish.<br>
Java + Lengua = Jengua

## Features

- Simple Setup
- Simple JSON-File for each language
- Easy to use
- Automatic addition of new keys
- Extremely readable
- Multi-Layer nesting of contexts (see json example below)

## Usage

To get started, first create a **Fallback Language File**. It can be any language and doesn't
have to be strictly in english, but it should however be consistent in the language you choose.
This isn't a technical limitation, but it makes the structure more understandable.

### What is a Fallback Language File and what should it contain?

The Fallback Language serves the purpose of being the default language. If everything fails, the
translation will come from this file. It should contain a normal structure like every other language file.

### What does the structure look like?

The structure of a language file is relatively simple:
- Locale attribute: A unique identifier for the language. Good practice is: `language-COUNTRY`, for example: `en-US` or `de-AT`
- A context array: This is solely to improve readability, but is required.
- Inside the context array, you can add as many translations as you want.

### Example

`fallback_en-us.json`
```json
{
    "locale": "en-US",
    
    "MainWindow": {
      "Menu": {
        "Menu": "Menu",
        "File": {
          "File": "File",
          "New": "New",
          "Open": "Open",
          "Save": "Save",
          "Exit": "Exit"
        }
      }
    },
    
    "ChildWindow.GUI": {
        "Title": "Some Child Window",
        "Description": "This is a child window",
        "Click Me": "Click Me"
    }
}
```
<br><br/>
`de-DE.json`
```json
{
    "locale": "de-DE",

    "MainWindow": {
      "Menu": {
        "Menu": "Menü",
        "File": {
          "File": "Datei",
          "New": "Neu",
          "Open": "Öffnen",
          "Save": "Speichern",
          "Exit": "Schließen"
        }
      }
    },
    
    "ChildWindow.GUI": {
        "Title": "Ein Child-Fenster",
        "Description": "Das ist ein Child-Fenster",
        "Click Me": "Klick Mich"
    }
}
```

### And what to do in Java?

Here's a short checklist of what to do and an example of how to use Jengua in your project:

1. Place the language files somewhere accessible
2. Add the Jengua library to your project. This can be done in plain Java or with Maven. Tutorial for the Maven method is further down this README.
3. Inside a class, load the fallback and default `Language` using `LanguageLoader.loadLanguage(File languageFile)`, and then create an instance of the `Translator`-class:
```java
public class Main {
    
    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");
    
    public static void main(String[] args) {
        
        /* The constructor of the Translator requires two parameters each of the type Language:
         * ... = new Translator(Language defaultLanguage, Language fallbackLanguage)
         * The default language is the language it will use by default, as the name implies
         * The fallback language is the language it will use if the selected language fails to translate
         * 
         * Because it requires two languages at start, you have to load the those language files yourself
         */
        
        try {
            // First load the language files. If you use the same language for default and fallback, you only need to load one language file, like I do here.
            // The method signature: LanguageLoader.loadLanguage(File languageFile)
            defaultAndFallbackLanguage = LanguageLoader.loadLanguage(defaultLanguageFile);
            // Load another language file if default and fallback language are separate files
        } catch (IOException e) {
            // Error handling...
        }
        
        // ... = new Translator(Language defaultLanguage, Language fallbackLanguage)
        translator = new Translator(defaultAndFallbackLanguage, defaultAndFallbackLanguage);
    }
}
```
4. Then you can add languages using `translator.addLanguage(Language language)` or via `translator.loadLanguage(File languageFile)`:

```java
import java.io.IOException;

public class Main {

    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");

    public static void main(String[] args) {

        // ...

        // EITHER:
        try {
            Language germanGermany = LanguageLoader.loadLanguage(new File("src/resources/lang/de-DE.json")); // Load Language
            translator.addLanguage(germanGermany); // Add Language to the Translator
        } catch (IOException e) {
            // Error handling...
        }

        // OR:
        try {
            translator.loadLanguage(new File("src/resources/lang/de-AT.json")); // Load German-Austria to the translator
        } catch (IOException e) {
            // Error handling...
        }
        
        // ...
    }
}
```
5. Then you can set the current language using: `translator.setLanguage(String locale)`:
```java
public class Main {

    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");
    
    public static void main(String[] args) {
        
        // ...
        
        translator.setLanguage("de-DE"); // Set the current language to German (Germany)
        
        // ...
    }
}
```
6. Finally, you can translate using `translator.translate(String key)`:
```java
public class Main {

    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");
    
    public static void main(String[] args) {
        
        // ...
        
        String translated = translator.tr("MainWindow.Menu.File", "File");
        
        // ...
    }
}
```
7. When quiting your application, you can save a new language file to the location of the old fallback language if you translated
a key that didn't exist before. It will then be set to JSON null. You can then use any other application to translate, that supports
this format, or do it manually in one run:
```java
public class Main {

    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");
    
    public static void main(String[] args) {
        
        // ...

        // This will create a backup of the old file and add the missing keys to the new file
        LanguageSaver.saveLanguage(defaultAndFallbackLanguage, defaultLanguageFile);
        
        return; // End of application
    }
}
```
8. Now you're done!

### Full example
```java
public class Main {

    public static Translator translator;
    public static Language defaultAndFallbackLanguage;
    private static File defaultLanguageFile = new File("src/resources/lang/fallback_en-us.json");
    
    public static void main(String[] args) {

        /* The constructor of the Translator requires two parameters each of the type Language:
         * ... = new Translator(Language defaultLanguage, Language fallbackLanguage)
         * The default language is the language it will use by default, as the name implies
         * The fallback language is the language it will use if the selected language fails to translate
         *
         * Because it requires two languages at start, you have to load the those language files yourself
         */

        try {
            // First load the language files. If you use the same language for default and fallback, you only need to load one language file, like I do here.
            // The method signature: LanguageLoader.loadLanguage(File languageFile)
            defaultAndFallbackLanguage = LanguageLoader.loadLanguage(defaultLanguageFile);
            // Load another language file if default and fallback language are separate files
        } catch (IOException e) {
            // Error handling...
        }

        // ... = new Translator(Language defaultLanguage, Language fallbackLanguage)
        translator = new Translator(defaultAndFallbackLanguage, defaultAndFallbackLanguage);

        // EITHER:
        try {
            Language germanGermany = LanguageLoader.loadLanguage(new File("src/resources/lang/de-DE.json")); // Load Language
            translator.addLanguage(germanGermany); // Add Language to the Translator
        } catch (IOException e) {
            // Error handling...
        }

        // OR:
        try {
            translator.loadLanguage(new File("src/resources/lang/de-AT.json")); // Load German-Austria to the translator
        } catch (IOException e) {
            // Error handling...
        }

        translator.setLanguage("de-DE"); // Set the current language to German (Germany)
        
        String translated = translator.tr("MainWindow.Menu.File", "File"); // Translate the key "MainWindow.Menu.File" with the fallback "File"
        System.out.println(translated); // This will print "Neu" now

        // This will create a backup of the old file and add the missing keys to the new file
        LanguageSaver.saveLanguage(defaultAndFallbackLanguage, defaultLanguageFile);
    }
}
```

## Building
To build the project, just execute:
- `gradlew.bat build` on windows
- `./gradlew build` on Unix-like systems
