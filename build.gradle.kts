plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.rdh"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

for (file in fileTree("src/test/kotlin/demo")) {
    if (file.isFile && file.extension == "kt") {
        tasks.register<JavaExec>("runDemo_${file.nameWithoutExtension}") {
            group = "demo"
            description = "Run demo: ${file.nameWithoutExtension}"
            classpath = sourceSets["test"].runtimeClasspath
            mainClass.set("demo.${file.nameWithoutExtension}Kt")
        }
    }
}

dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.16")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}