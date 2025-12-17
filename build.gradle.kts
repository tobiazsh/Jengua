import org.jreleaser.model.Active
import java.nio.file.Path
import java.util.Properties

plugins {
    id("java")
    id("org.jreleaser") version "1.17.0"
    id("maven-publish")
}

group = project.property("group") as String
version = project.property("version") as String

val projVer = version.toString()
val buildDir: Path = Path.of("./build")

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("com.google.code.gson:gson:2.13.1")
}

java {
    withSourcesJar()
    withJavadocJar()
}

// Load local.properties
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    } else {
        logger.warn("local.properties not found - publishing tasks will fail")
    }
}

val keyFile: File = File("./pgp/key.asc")
val publicKeyFile: File = File("./pgp/public_key.pub")

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("Jengua")
                description.set("A stupidly-simple Java translation library.")
                url.set("https://github.com/tobiazsh/jengua")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://mit-license.org/")
                    }
                }

                developers {
                    developer {
                        id.set("tobiazsh")
                        name.set("Tobias S.")
                        email.set("developer.tobiazsh@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/tobiazsh/jengua.git")
                    developerConnection.set("scm:git:ssh://github.com/tobiazsh/jengua.git")
                    url.set("https://github.com/tobiazsh/jengua")
                }
            }
        }

        repositories {
            maven {
                url = uri(layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }
}

jreleaser {
    project {
        authors.add("Tobiazsh")
        license.set("MIT")
        links {
            homepage.set("https://github.com/tobiazsh/jengua")
        }
        inceptionYear.set("2025")
        version.set(projVer)
        description.set("A stupidly-simple Java translation library.")
    }

    release {
        github {
            repoOwner.set("tobiazsh")
            overwrite.set(true)
            name.set("Jengua")
            tagName.set("jengua_v${project.version}")
            releaseName.set("Jengua v${project.version}")
            draft.set(false)
            prerelease.enabled.set(false)
            token.set(localProperties.getProperty("github.token"))
        }
    }

    signing {
        passphrase.set(localProperties.getProperty("signing.password") ?: System.getenv("SIGNING_PASSWORD"))
        publicKey.set(publicKeyFile.readText())
        secretKey.set(keyFile.readText())

        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    username.set(localProperties.getProperty("sonatypeUsernameToken"))
                    password.set(localProperties.getProperty("sonatypePasswordToken"))
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }

    checksum {
        name.set("${project.name}-${project.version}_checksums.txt")

        individual.set(true)

        algorithm("SHA-256")
        algorithm("SHA-512")
        algorithm("MD5")
        algorithm("SHA-1")

        files.set(true)
    }
}

tasks.register("verifyPublishingConfiguration") {
    doLast {
        // Files to check for existence
        val localProps = File("./local.properties")
        val keyFile = File("./pgp/key.asc")
        val publicKeyFile = File("./pgp/public_key.pub")

        // Check if local.properties exists
        if (!localProps.exists()) {
            throw GradleException("Missing configuration file: " + localProps.path)
        }

        if (!keyFile.exists()) {
            throw GradleException("Missing PGP Key file for publication: " + keyFile.path)
        }

        if (!publicKeyFile.exists()) {
            throw GradleException("Missing PGP Public Key file for publication: " + publicKeyFile.path)
        }

        val requiredProps = listOf(
            "sonatypeUsernameToken", "sonatypePasswordToken",
            "signing.keyId", "signing.password", "github.token"
        )

        val missingProps = requiredProps.filter { prop ->
            localProperties.getProperty(prop).isNullOrBlank() && System.getenv(prop).isNullOrBlank()
        }

        if (missingProps.isNotEmpty()) {
            throw GradleException("Missing required properties: ${missingProps.joinToString()}")
        }

        println("All required publishing properties are configured!")
    }
}