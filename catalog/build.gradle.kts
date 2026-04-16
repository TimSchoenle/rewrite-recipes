import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("rewrite.java-conventions")
    id("rewrite.publish-conventions")
}


// x-release-please-start-version
version = "0.2.0"
// x-release-please-end

dependencies {
    runtimeOnly(platform(libs.rewrite.recipe.bom))

    runtimeOnly("org.openrewrite.recipe:rewrite-migrate-java")
    runtimeOnly(project(":recipes:minecraft:invui"))
}

mavenPublishing {
    coordinates("de.timscho.rewrite", "catalog", version.toString())

    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc()))
}
