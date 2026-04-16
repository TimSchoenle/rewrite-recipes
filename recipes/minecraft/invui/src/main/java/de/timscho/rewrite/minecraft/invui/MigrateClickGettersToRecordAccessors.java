package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

public class MigrateClickGettersToRecordAccessors extends Recipe {
    private static final String CLICK = "xyz.xenondevs.invui.Click";

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
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit compilationUnit, final ExecutionContext ctx) {
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
