package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class MigrateAnvilWindowToOldApi extends Recipe {
    private static final String ANVIL_WINDOW = "xyz.xenondevs.invui.window.AnvilWindow";
    private static final String ANVIL_WINDOW_BUILDER = ANVIL_WINDOW + "$Builder";

    private static final MethodMatcher ANVIL_WINDOW_FACTORY_BUILDER_NO_ARGS =
        new MethodMatcher(ANVIL_WINDOW + " builder()");
    private static final MethodMatcher ANVIL_WINDOW_BUILDER_SET_UPPER_GUI =
        new MethodMatcher(ANVIL_WINDOW_BUILDER + " setUpperGui(..)");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate InvUI AnvilWindow API to v1";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates safe AnvilWindow API renames from InvUI v2 to v1.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (ANVIL_WINDOW_FACTORY_BUILDER_NO_ARGS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("split"));
                }

                if (isSetUpperGuiOnAnvilWindowBuilderHierarchy(m)) {
                    return m.withName(m.getName().withSimpleName("setGui"));
                }

                return m;
            }

            private boolean isSetUpperGuiOnAnvilWindowBuilderHierarchy(final J.MethodInvocation method) {
                if (ANVIL_WINDOW_BUILDER_SET_UPPER_GUI.matches(method)) {
                    return true;
                }

                if (!"setUpperGui".equals(method.getSimpleName()) || method.getArguments().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return TypeUtils.isAssignableTo(ANVIL_WINDOW_BUILDER, methodType.getDeclaringType());
                }

                return method.getSelect() != null &&
                    TypeUtils.isAssignableTo(ANVIL_WINDOW_BUILDER, method.getSelect().getType());
            }
        };
    }
}
