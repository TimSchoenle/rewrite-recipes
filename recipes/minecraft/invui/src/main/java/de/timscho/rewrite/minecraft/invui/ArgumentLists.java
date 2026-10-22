package de.timscho.rewrite.minecraft.invui;

import org.openrewrite.java.tree.J;

import java.util.List;

/**
 * Reads argument and parameter lists the way OpenRewrite models them.
 *
 * <p>An empty {@code ()} is not an empty list in the tree: it is a list holding a single
 * {@link J.Empty}, so {@code List#isEmpty()} and {@code size() == 0} are never true for it.
 */
final class ArgumentLists {
    private ArgumentLists() {
    }

    /**
     * Tells whether {@code elements}, taken from {@code getArguments()} or {@code getParameters()},
     * stands for an empty {@code ()}.
     *
     * @param elements the arguments of a call or the parameters of a declaration
     * @return {@code true} when every element is a {@link J.Empty}, including when there are none
     */
    static boolean isEmpty(final List<? extends J> elements) {
        for (final J element : elements) {
            if (!(element instanceof J.Empty)) {
                return false;
            }
        }
        return true;
    }
}
