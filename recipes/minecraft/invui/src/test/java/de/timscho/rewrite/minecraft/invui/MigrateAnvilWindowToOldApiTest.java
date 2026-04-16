package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateAnvilWindowToOldApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateAnvilWindowToOldApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesAnvilWindowBuilderMethods() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.window;

                public interface AnvilWindow {
                    static Builder builder() { return null; }
                    static Builder.Split split() { return null; }

                    interface Builder {
                        Builder setUpperGui(Object gui);
                        Builder setGui(Object gui);
                        interface Split extends Builder {}
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.window.AnvilWindow;

                class UsesAnvilWindow {
                    void use() {
                        AnvilWindow.builder().setUpperGui(new Object());
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.window.AnvilWindow;

                class UsesAnvilWindow {
                    void use() {
                        AnvilWindow.split().setGui(new Object());
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotTouchUnrelatedSetUpperGuiMethod() {
        this.rewriteRun(
            java(
                """
                package test;

                class OtherBuilder {
                    OtherBuilder setUpperGui(Object gui) {
                        return this;
                    }
                }

                class UsesOtherBuilder {
                    void use() {
                        new OtherBuilder().setUpperGui(new Object());
                    }
                }
                """
            )
        );
    }
}
