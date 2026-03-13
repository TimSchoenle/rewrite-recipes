# rewrite-recipes

A collection of OpenRewrite recipes for Java code refactoring and static analysis.

## Available Recipes

### de.timscho.rewrite.Style

General code style recipe that complements Spotless, applying static analysis rules that Spotless does not cover.

## Usage

### Gradle

Add the catalog dependency to your `build.gradle` or `build.gradle.kts`:

```gradle
plugins {
    id("org.openrewrite.rewrite") version "insertLatestVersionHere"
}

rewrite {
    activeRecipe("de.timscho.rewrite.Style")
}

dependencies {
    rewrite("de.timscho.rewrite:catalog:insertLatestVersionHere")
}
```

Run the recipe:

```bash
./gradlew rewriteRun
```

To preview changes without applying them:

```bash
./gradlew rewriteDryRun
```
