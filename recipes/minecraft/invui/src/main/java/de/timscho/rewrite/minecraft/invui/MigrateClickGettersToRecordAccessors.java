package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Renames the four {@code Click} getters to the accessors of the 2.x record, with
 * {@code getClick} and {@code getClickType} landing on the same one.
 */
public class MigrateClickGettersToRecordAccessors extends Recipe {
    private static final String CLICK = "xyz.xenondevs.invui.Click";

    /** Creates the recipe; OpenRewrite constructs it reflectively from the catalog. */
    public MigrateClickGettersToRecordAccessors() {
    }

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate Click getters to record accessors";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates `Click` getter methods (`getPlayer`, `getClick`, `getClickType`, and `getHotbarButton`) to record-style accessors.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.@NonNull CompilationUnit visitCompilationUnit(final J.@NonNull CompilationUnit compilationUnit, final @NonNull ExecutionContext ctx) {
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.CLICK + " getPlayer()",
                    "player",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.CLICK + " getClick()",
                    "clickType",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.CLICK + " getClickType()",
                    "clickType",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.CLICK + " getHotbarButton()",
                    "hotbarButton",
                    null,
                    null
                ).getVisitor());
                return super.visitCompilationUnit(compilationUnit, ctx);
            }
        };
    }
}
