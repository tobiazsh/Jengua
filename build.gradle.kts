import java.util.Properties

plugins {
    id("java")
    id("maven-publish")
    signing
}

group = project.property("group") as String
version = project.property("version") as String

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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])

            pom {
                name.set("Jengua")
                description.set("A stupidly-simple Java translations library")
                url.set("https://github.com/tobiazsh/jengua")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://mit-license.org")
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
    }

    repositories {
        maven {
            val releaseRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")

            url = if (version.toString().endsWith("SNAPSHOT")) snapshotRepoUrl else releaseRepoUrl

            credentials {
                username = localProperties.getProperty("ossrhUsername") ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = localProperties.getProperty("ossrhPassword") ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
}

val keyFile: File = File("./pgp/key.asc")

signing {
    val signingKeyId: String? = localProperties.getProperty("signing.keyId") ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword: String? = localProperties.getProperty("signing.password") ?: System.getenv("SIGNING_PASSWORD")
    val signingKey: String = keyFile.readText()

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("verifyPublishingConfiguration") {
    doLast {

        // Files to check for existence
        val localProps = File("./local.properties")
        val keyFile = File("./pgp/key.asc")

        // Check if local.properties exists
        if (!localProps.exists()) {
            throw GradleException("Missing configuration file: " + localProps.path)
        }

        if (!keyFile.exists()) {
            throw GradleException("Missing PGP Key file for publication: " + keyFile.path)
        }

        val requiredProps = listOf(
            "ossrhUsername", "ossrhPassword",
            "signing.keyId", "signing.password"
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