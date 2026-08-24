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

// Both halves of the doclint gate. javac runs doclint over every source set it compiles; the
// javadoc tool runs it again over the sources that reach the published javadoc jar. `-Werror` is
// what makes either one a gate, because without it doclint prints and the build stays green.
//
// `/protected` is the access level, so package-private and private members stay a review matter.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xdoclint:all/protected", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        // The javadoc tool rejects the `/protected` qualifier javac accepts. It takes the access
        // level as its own flag instead, and memberLevel is that flag.
        memberLevel = JavadocMemberLevel.PROTECTED
        addStringOption("Xdoclint:all", "-quiet")
        addBooleanOption("Werror", true)
        // The standard doclet does not know `@apiNote`, `@implSpec` or `@implNote` outside the
        // JDK's own build, and an unregistered block tag is a javadoc error rather than a
        // warning, so `-Werror` above is not what makes it fail.
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:",
        )
    }
}
