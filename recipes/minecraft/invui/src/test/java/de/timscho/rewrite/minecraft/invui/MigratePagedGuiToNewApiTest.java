package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigratePagedGuiToNewApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigratePagedGuiToNewApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesPagedGuiFactoryAndPageMethods() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.gui;

                public interface PagedGui<C> {
                    static Builder<Object> items() { return null; }
                    static Builder<Object> guis() { return null; }
                    static Builder<Object> inventories() { return null; }
                    int getPageAmount();
                    int getCurrentPage();

                    interface Builder<C> {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.PagedGui;

                class UsesPagedGui {
                    int migrate(PagedGui<?> pagedGui) {
                        PagedGui.items();
                        PagedGui.guis();
                        PagedGui.inventories();
                        return pagedGui.getPageAmount() + pagedGui.getCurrentPage();
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.PagedGui;

                class UsesPagedGui {
                    int migrate(PagedGui<?> pagedGui) {
                        PagedGui.itemsBuilder();
                        PagedGui.guisBuilder();
                        PagedGui.inventoriesBuilder();
                        return pagedGui.getPageCount() + pagedGui.getPage();
                    }
                }
                """
            )
        );
    }
}
