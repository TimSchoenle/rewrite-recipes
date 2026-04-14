import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("rewrite.java-conventions")
    id("rewrite.publish-conventions")
}

// x-release-please-start-version
version = "0.1.0"
// x-release-please-end

dependencies {
    implementation(platform(libs.rewrite.recipe.bom))

    // Core dependencies for writing recipes
    implementation("org.openrewrite:rewrite-java")

    // For testing recipes
    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.openrewrite:rewrite-java-21")
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates("de.timscho.rewrite", "recipes-minecraft-invui", version.toString())

    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc()))
}
