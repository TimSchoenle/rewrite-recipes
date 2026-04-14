package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class MigrateGuiToNewApi extends Recipe {
    private static final String GUI = "xyz.xenondevs.invui.gui.Gui";

    private static final MethodMatcher GUI_FACTORY_NORMAL_NO_ARGS =
        new MethodMatcher(MigrateGuiToNewApi.GUI + " normal()");
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
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (MigrateGuiToNewApi.GUI_FACTORY_NORMAL_NO_ARGS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("builder"));
                }

                if (MigrateGuiToNewApi.GUI_FIND_ALL_WINDOWS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getWindows"));
                }

                if (MigrateGuiToNewApi.GUI_FIND_ALL_CURRENT_VIEWERS.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getCurrentViewers"));
                }

                return m;
            }
        };
    }
}
