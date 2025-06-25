plugins {
    id("java")
    id("maven-publish")
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = project.property("group") as String
            artifactId = "jengua"
            version = project.property("version") as String
        }
    }
    // TODO: Configure repository for publishing
}

tasks.test {
    useJUnitPlatform()
}