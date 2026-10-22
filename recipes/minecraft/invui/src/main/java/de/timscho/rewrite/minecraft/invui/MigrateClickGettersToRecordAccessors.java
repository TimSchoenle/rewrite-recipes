package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

/**
 * Renames the four {@code Click} getters to the accessors of the 2.x record, with
 * {@code getClick} and {@code getClickType} landing on the same one.
 */
public class MigrateClickGettersToRecordAccessors extends Recipe {
    private static final String CLICK = "xyz.xenondevs.invui.Click";
    private static final String GET_PLAYER = MigrateClickGettersToRecordAccessors.CLICK + " getPlayer()";
    private static final String GET_CLICK = MigrateClickGettersToRecordAccessors.CLICK + " getClick()";
    private static final String GET_CLICK_TYPE = MigrateClickGettersToRecordAccessors.CLICK + " getClickType()";
    private static final String GET_HOTBAR_BUTTON = MigrateClickGettersToRecordAccessors.CLICK + " getHotbarButton()";

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

    // Mirrors the UsesMethod/DeclaresMethod precondition each scheduled ChangeMethodName applies itself.
    private static TreeVisitor<?, ExecutionContext> usesOrDeclaresClickGetter() {
        return Preconditions.or(
            new UsesMethod<>(MigrateClickGettersToRecordAccessors.GET_PLAYER),
            new DeclaresMethod<>(MigrateClickGettersToRecordAccessors.GET_PLAYER),
            new UsesMethod<>(MigrateClickGettersToRecordAccessors.GET_CLICK),
            new DeclaresMethod<>(MigrateClickGettersToRecordAccessors.GET_CLICK),
            new UsesMethod<>(MigrateClickGettersToRecordAccessors.GET_CLICK_TYPE),
            new DeclaresMethod<>(MigrateClickGettersToRecordAccessors.GET_CLICK_TYPE),
            new UsesMethod<>(MigrateClickGettersToRecordAccessors.GET_HOTBAR_BUTTON),
            new DeclaresMethod<>(MigrateClickGettersToRecordAccessors.GET_HOTBAR_BUTTON)
        );
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(MigrateClickGettersToRecordAccessors.usesOrDeclaresClickGetter(), new JavaIsoVisitor<>() {
            @Override
            public J.@NonNull CompilationUnit visitCompilationUnit(final J.@NonNull CompilationUnit compilationUnit, final @NonNull ExecutionContext ctx) {
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.GET_PLAYER,
                    "player",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.GET_CLICK,
                    "clickType",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.GET_CLICK_TYPE,
                    "clickType",
                    null,
                    null
                ).getVisitor());
                this.doAfterVisit(new ChangeMethodName(
                    MigrateClickGettersToRecordAccessors.GET_HOTBAR_BUTTON,
                    "hotbarButton",
                    null,
                    null
                ).getVisitor());
                return super.visitCompilationUnit(compilationUnit, ctx);
            }
        });
    }
}
