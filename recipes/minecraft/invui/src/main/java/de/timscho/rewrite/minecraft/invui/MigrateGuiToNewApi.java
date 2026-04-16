package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class MigrateGuiToNewApi extends Recipe {
    private static final String GUI = "xyz.xenondevs.invui.gui.Gui";

    private static final MethodMatcher GUI_FACTORY_NORMAL_NO_ARGS =
        new MethodMatcher(MigrateGuiToNewApi.GUI + " normal()");
    private static final MethodMatcher GUI_FACTORY_NORMAL_WITH_CONSUMER =
        new MethodMatcher(MigrateGuiToNewApi.GUI + " normal(java.util.function.Consumer)");
    private static final MethodMatcher GUI_FIND_ALL_WINDOWS =
        new MethodMatcher(MigrateGuiToNewApi.GUI + " findAllWindows()");
    private static final MethodMatcher GUI_FIND_ALL_CURRENT_VIEWERS =
        new MethodMatcher(MigrateGuiToNewApi.GUI + " findAllCurrentViewers()");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate InvUI Gui API to v2";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates safe Gui API renames from InvUI v1 to v2.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.@NonNull MethodInvocation visitMethodInvocation(final J.@NonNull MethodInvocation method, final @NonNull ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (MigrateGuiToNewApi.GUI_FACTORY_NORMAL_NO_ARGS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("builder"));
                }

                if (MigrateGuiToNewApi.GUI_FACTORY_NORMAL_WITH_CONSUMER.matches(m)) {
                    return this.migrateFactoryConsumerOverload(m);
                }

                if (MigrateGuiToNewApi.GUI_FIND_ALL_WINDOWS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getWindows"));
                }

                if (MigrateGuiToNewApi.GUI_FIND_ALL_CURRENT_VIEWERS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getCurrentViewers"));
                }

                return m;
            }

            private J.MethodInvocation migrateFactoryConsumerOverload(final J.MethodInvocation method) {
                if (method.getArguments().size() != 1) {
                    return method;
                }

                final Expression consumer = method.getArguments().getFirst();
                return JavaTemplate.builder(
                        """
                            ((java.util.function.Supplier<xyz.xenondevs.invui.gui.Gui>) () -> {
                                java.util.function.Consumer<xyz.xenondevs.invui.gui.Gui.Builder<?, ?>> consumer = #{any(java.util.function.Consumer)};
                                xyz.xenondevs.invui.gui.Gui.Builder<?, ?> builder = xyz.xenondevs.invui.gui.Gui.builder();
                                consumer.accept(builder);
                                return builder.build();
                            }).get()
                            """
                    )
                    .build()
                    .apply(this.getCursor(), method.getCoordinates().replace(), consumer)
                    .withPrefix(method.getPrefix());
            }
        };
    }
}
