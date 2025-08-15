plugins {
    kotlin("jvm") version "2.2.0"
}

group = "dev.rdh"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("it.unimi.dsi:fastutil:8.5.16")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}