package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class RemoveSimpleItemWrapper extends Recipe {
    private static final String SIMPLE_ITEM = "xyz.xenondevs.invui.item.impl.SimpleItem";
    private static final String ITEM_STACK = "org.bukkit.inventory.ItemStack";

    @Override
    public @NonNull String getDisplayName() {
        return "Remove SimpleItem wrapper";
    }

    @Override
    public @NonNull String getDescription() {
        return "Replaces new SimpleItem(itemStack) with itemStack when the only argument is an ItemStack.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public @NonNull J visitNewClass(final J.@NonNull NewClass newClass, final @NonNull ExecutionContext ctx) {
                final J nc = super.visitNewClass(newClass, ctx);

                if (nc instanceof final J.NewClass n &&
                    n.getClazz() != null &&
                    TypeUtils.isOfClassType(n.getType(), RemoveSimpleItemWrapper.SIMPLE_ITEM) &&
                    n.getArguments().size() == 1 &&
                    TypeUtils.isOfClassType(n.getArguments().getFirst().getType(), RemoveSimpleItemWrapper.ITEM_STACK)) {
                        final Expression innerArg = n.getArguments().getFirst();
                        this.maybeRemoveImport(RemoveSimpleItemWrapper.SIMPLE_ITEM);
                        return innerArg.withPrefix(n.getPrefix());
                    }

                return nc;
            }
        };
    }
}
