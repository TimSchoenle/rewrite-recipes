package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class MigrateSimpleItemToItem extends Recipe {
    private static final String SIMPLE_ITEM = "xyz.xenondevs.invui.item.impl.SimpleItem";
    private static final String ITEM = "xyz.xenondevs.invui.item.Item";
    private static final String ITEM_STACK = "org.bukkit.inventory.ItemStack";
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate SimpleItem to Item API";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates `new SimpleItem(itemProviderOrStack, clickHandler)` to the InvUI v2 `Item` API.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public @NonNull J visitNewClass(final J.@NonNull NewClass newClass, final @NonNull ExecutionContext ctx) {
                final J nc = super.visitNewClass(newClass, ctx);
                if (!(nc instanceof final J.NewClass n)
                    || n.getClazz() == null
                    || n.getArguments().isEmpty()
                    || n.getArguments().size() > 2) {
                    return nc;
                }

                if (!TypeUtils.isOfClassType(n.getType(), MigrateSimpleItemToItem.SIMPLE_ITEM)) {
                    return nc;
                }

                final Expression providerOrStack = n.getArguments().get(0);
                final Expression clickHandler = n.getArguments().size() == 2 ? n.getArguments().get(1) : null;
                final boolean isNullClickHandler = clickHandler == null || clickHandler instanceof J.Literal literal && literal.getValue() == null;
                final boolean isItemStack = TypeUtils.isOfClassType(providerOrStack.getType(), MigrateSimpleItemToItem.ITEM_STACK);
                final boolean isItemProvider = TypeUtils.isOfClassType(providerOrStack.getType(), MigrateSimpleItemToItem.ITEM_PROVIDER);

                if (!isItemStack && !isItemProvider) {
                    return nc;
                }

                maybeRemoveImport(MigrateSimpleItemToItem.SIMPLE_ITEM);
                maybeAddImport(MigrateSimpleItemToItem.ITEM);
                doAfterVisit(new AddImport<>(MigrateSimpleItemToItem.ITEM, null, false));
                final String providerType = isItemStack ? MigrateSimpleItemToItem.ITEM_STACK : MigrateSimpleItemToItem.ITEM_PROVIDER;
                if (isNullClickHandler) {
                    return JavaTemplate.builder("Item.simple(#{any(" + providerType + ")})")
                        .imports(MigrateSimpleItemToItem.ITEM)
                        .build()
                        .apply(getCursor(), n.getCoordinates().replace(), providerOrStack)
                        .withPrefix(n.getPrefix());
                }

                return JavaTemplate.builder("Item.builder().setItemProvider(#{any(" + providerType + ")}).addClickHandler(#{any(java.util.function.Consumer)}).build()")
                    .imports(MigrateSimpleItemToItem.ITEM)
                    .build()
                    .apply(getCursor(), n.getCoordinates().replace(), providerOrStack, clickHandler)
                    .withPrefix(n.getPrefix());
            }
        };
    }
}
