package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateWindowToNewApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateWindowToNewApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesWindowMethodCalls() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.inventoryaccess.component;

                public class ComponentWrapper {}
                """
            ),
            java(
                """
                package net.kyori.adventure.text;

                public interface Component {}
                """
            ),
            java(
                """
                package org.bukkit.event.inventory;

                public class InventoryClickEvent {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui;

                public class ClickEvent {}
                """
            ),
            java(
                """
                package org.bukkit.entity;

                import java.util.UUID;

                public class Player {
                    public UUID getUniqueId() {
                        return UUID.randomUUID();
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.window;

                import org.bukkit.entity.Player;
                import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;

                public interface Window {
                    static Builder.Normal.Split split() { return null; }
                    static Builder.Normal.Merged merged() { return null; }
                    void changeTitle(ComponentWrapper component);
                    void changeTitle(String title);
                    Player getViewer();
                    java.util.UUID getViewerUUID();

                    interface Builder<W extends Window, S extends Builder<W, S>> {
                        interface Single<W extends Window, S extends Single<W, S>> extends Builder<W, S> {}
                        interface Double<W extends Window, S extends Double<W, S>> extends Builder<W, S> {}
                        interface Merged<W extends Window, S extends Merged<W, S>> extends Builder<W, S> {}
                        interface Split<W extends Window, S extends Split<W, S>> extends Builder<W, S> {}
                        interface Normal<V, S extends Normal<V, S>> extends Builder<Window, S> {
                            interface Split extends Normal<Player, Split>, Double<Window, Split> {}
                            interface Merged extends Normal<Player, Merged>, Single<Window, Merged> {}
                        }
                    }
                }
                """,
                """
                package xyz.xenondevs.invui.window;

                import net.kyori.adventure.text.Component;
                import org.bukkit.entity.Player;

                public interface Window {
                    static Builder.Normal.Split split() { return null; }
                    static Builder.Normal.Merged merged() { return null; }
                    void changeTitle(Component component);
                    void changeTitle(String title);
                    Player getViewer();
                    java.util.UUID getViewerUUID();

                    interface Builder<W extends Window, S extends Builder<W, S>> {
                        interface Single<W extends Window, S extends Single<W, S>> extends Builder<W, S> {}
                        interface Double<W extends Window, S extends Double<W, S>> extends Builder<W, S> {}
                        interface Merged<W extends Window, S extends Merged<W, S>> extends Builder<W, S> {}
                        interface Split<W extends Window, S extends Split<W, S>> extends Builder<W, S> {}
                        interface Normal<V, S extends Normal<V, S>> extends Builder<Window, S> {
                            interface Split extends Normal<Player, Split>, Double<Window, Split> {}
                            interface Merged extends Normal<Player, Merged>, Single<Window, Merged> {}
                        }
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.inventoryaccess.component.ComponentWrapper;
                import xyz.xenondevs.invui.window.Window;

                class UsesWindow {
                    Object use(Window window, ComponentWrapper title) {
                        Window.Builder.Double<?, ?> builderType = null;
                        Window.split();
                        Window.merged();
                        window.changeTitle(title);
                        window.changeTitle("title");
                        return window.getViewerUUID();
                    }
                }
                """,
                """
                package test;

                import net.kyori.adventure.text.Component;
                import xyz.xenondevs.invui.window.Window;

                class UsesWindow {
                    Object use(Window window, Component title) {
                        Window.Builder.Double<?, ?> builderType = null;
                        Window.builder();
                        Window.mergedBuilder();
                        window.setTitle(title);
                        window.setTitle("title");
                        return window.getViewer().getUniqueId();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesSingleFactoryWithoutConsumerToBuilder() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.window;

                public interface Window {
                    static Builder.Normal.Single single() { return null; }
                    static Window single(java.util.function.Consumer<Builder.Normal.Single> consumer) { return null; }
                    static Builder.Normal.Split builder() { return null; }

                    interface Builder<W extends Window, S extends Builder<W, S>> {
                        S setGui(Object gui);
                        S setUpperGui(Object gui);
                        interface Single<W extends Window, S extends Single<W, S>> extends Builder<W, S> {}
                        interface Normal<V, S extends Normal<V, S>> extends Builder<Window, S> {
                            interface Single extends Normal<java.util.UUID, Single>, Builder.Single<Window, Single> {}
                            interface Split extends Normal<org.bukkit.entity.Player, Split> {}
                        }
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.window.Window;

                class UsesWindow {
                    void use() {
                        Window.single();
                        Window.builder().setGui(new Object());
                        Window.single(builder -> {
                        });
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.window.Window;

                class UsesWindow {
                    void use() {
                        Window.builder();
                        Window.builder().setUpperGui(new Object());
                        Window.single(builder -> {
                        });
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotMigrateInventoryClickEventWhenWindowMethodsAreNotUsed() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.event.inventory;

                public class InventoryClickEvent {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui;

                public class ClickEvent {}
                """
            ),
            java(
                """
                package test;

                import org.bukkit.event.inventory.InventoryClickEvent;

                class UnrelatedUsage {
                    void handle(InventoryClickEvent event) {
                    }
                }
                """
            )
        );
    }
}
