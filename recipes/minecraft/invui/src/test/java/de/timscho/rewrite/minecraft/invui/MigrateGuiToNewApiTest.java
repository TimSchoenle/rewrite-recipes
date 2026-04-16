package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateGuiToNewApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateGuiToNewApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesGuiFactoryAndViewerMethods() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.gui;

                import java.util.Collection;
                import java.util.Set;
                import org.bukkit.entity.Player;
                import xyz.xenondevs.invui.window.Window;

                public interface Gui {
                    static Builder.Normal normal() { return null; }
                    static Gui normal(java.util.function.Consumer<Builder.Normal> consumer) { return null; }
                    Collection<Window> getWindows();
                    Set<Window> findAllWindows();
                    Collection<Player> getCurrentViewers();
                    Set<Player> findAllCurrentViewers();

                    interface Builder<G extends Gui, S extends Builder<G, S>> {
                        interface Normal extends Builder<Gui, Normal> {}
                    }
                }
                """
            ),
            java(
                """
                package org.bukkit.entity;

                public class Player {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.window;

                public interface Window {}
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;

                class UsesGui {
                    Object use(Gui gui) {
                        Gui.normal();
                        Gui.normal(builder -> {
                        });
                        gui.findAllWindows();
                        return gui.findAllCurrentViewers();
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;

                class UsesGui {
                    Object use(Gui gui) {
                        Gui.builder();
                        ((java.util.function.Supplier<xyz.xenondevs.invui.gui.Gui>) () -> {
                            java.util.function.Consumer<xyz.xenondevs.invui.gui.Gui.Builder<?, ?>> consumer = builder -> {
                            };
                            xyz.xenondevs.invui.gui.Gui.Builder<?, ?> builder = xyz.xenondevs.invui.gui.Gui.builder();
                            consumer.accept(builder);
                            return builder.build();
                        }).get();
                        gui.getWindows();
                        return gui.getCurrentViewers();
                    }
                }
                """
            )
        );
    }
}
