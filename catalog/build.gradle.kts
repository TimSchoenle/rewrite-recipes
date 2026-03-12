plugins {
    id("rewrite.java-conventions")
    id("rewrite.publish-conventions")
}

version = "0.0.1"

dependencies {
    runtimeOnly(platform(libs.rewrite.recipe.bom))

    runtimeOnly("org.openrewrite.recipe:rewrite-migrate-java")

    // runtimeOnly(libs.rewrite.static.analysis)
    // runtimeOnly(libs.rewrite.java)
}

mavenPublishing {
    coordinates("de.timscho.rewrite", "catalog", "0.0.1")
}
