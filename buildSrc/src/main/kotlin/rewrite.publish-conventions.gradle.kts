import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc()))

    pom {
        name.set("Jextract Gradle Plugin")
        description.set("A Gradle plugin that automates the download and execution of jextract to generate Java Foreign Function & Memory (FFM) API bindings from C header files with bundled library loading support.")
        url.set("https://github.com/TimSchoenle/gradle-jextract")
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
            url.set("https://github.com/TimSchoenle/gradle-jextract")
        }
    }

    publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.PUBLISHED)

    // We only want to sign publications when running on CI
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }
}
