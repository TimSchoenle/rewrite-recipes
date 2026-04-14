package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class RemoveAdventureComponentWrapper extends Recipe {
    private static final String ADVENTURE_COMPONENT_WRAPPER = "xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper";

    @Override
    public @NonNull String getDisplayName() {
        return "Remove adventureComponentWrapper";
    }

    @Override
    public @NonNull String getDescription() {
        return "Removes AdventureComponentWrapper and uses the inner argument directly.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public @NonNull J visitNewClass(final J.@NonNull NewClass newClass, final @NonNull ExecutionContext ctx) {
                final J nc = super.visitNewClass(newClass, ctx);

                if (nc instanceof final J.NewClass n &&
                    n.getClazz() != null &&
                    TypeUtils.isOfClassType(n.getType(), RemoveAdventureComponentWrapper.ADVENTURE_COMPONENT_WRAPPER) && n.getArguments().size() == 1) {
                        final Expression innerArg = n.getArguments().getFirst();
                        this.maybeRemoveImport(RemoveAdventureComponentWrapper.ADVENTURE_COMPONENT_WRAPPER);
                        return innerArg.withPrefix(n.getPrefix());
                    }


                return nc;
            }
        };
    }
}
