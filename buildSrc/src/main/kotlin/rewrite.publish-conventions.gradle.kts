import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    pom {
        name.set("Rewrite Recipes & Catalog")
        description.set("A collection of Rewrite recipes and catalogs.")
        url.set("https://github.com/TimSchoenle/rewrite-recipes")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("TimSchoenle")
                name.set("Tim Schönle")
                url.set("https://github.com/TimSchoenle")
            }
        }
        scm {
            url.set("https://github.com/TimSchoenle/rewrite-recipes")
        }
    }

    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)

    // We only want to sign publications when running on CI
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }
}
