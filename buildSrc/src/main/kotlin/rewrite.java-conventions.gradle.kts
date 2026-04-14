import gradle.kotlin.dsl.accessors._784b33a1853adbd9d7bde23f270505af.mavenPublishing

plugins {
    `java-library`
}

group = "de.timscho.rewrite"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
