package de.timscho.rewrite.minecraft.invui;

import org.openrewrite.java.tree.J;

/**
 * Whether a compilation unit binds a simple name to a type through an import, for code that has to
 * decide from source text because type attribution is missing.
 */
final class ExplicitImports {
    private ExplicitImports() {
    }

    /**
     * Whether {@code compilationUnit} has a non-static single-type import of
     * {@code fullyQualifiedName}.
     *
     * <p>A wildcard import does not count: a type of the same simple name in the importing package
     * takes precedence over it, so it does not establish what the simple name refers to.
     *
     * @param compilationUnit the file whose imports are searched
     * @param fullyQualifiedName the type that has to be imported, such as {@code java.util.List}
     * @return whether that exact import is present
     */
    static boolean importsType(final J.CompilationUnit compilationUnit, final String fullyQualifiedName) {
        for (final J.Import anImport : compilationUnit.getImports()) {
            if (!anImport.isStatic() && fullyQualifiedName.equals(anImport.getTypeName())) {
                return true;
            }
        }
        return false;
    }
}
