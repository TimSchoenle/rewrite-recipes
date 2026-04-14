package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateItemHandleClickToClickApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateItemHandleClickToClickApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesItemHandleClickSignatureAndInvocation() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
                """
            ),
            java(
                """
                package org.bukkit.event.inventory;

                public class ClickType {}
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

                public class Click {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import org.bukkit.event.inventory.InventoryClickEvent;

                public interface Item {
                    void handleClick(ClickType clickType, Player player, InventoryClickEvent event);
                }
                """,
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import xyz.xenondevs.invui.Click;

                public interface Item {
                    void handleClick(ClickType clickType, Player player, Click event);
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import org.bukkit.event.inventory.InventoryClickEvent;
                import xyz.xenondevs.invui.item.Item;

                class ItemImpl implements Item {
                    @Override
                    public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
                    }
                }
                """,
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import xyz.xenondevs.invui.Click;
                import xyz.xenondevs.invui.item.Item;

                class ItemImpl implements Item {
                    @Override
                    public void handleClick(ClickType clickType, Player player, Click event) {
                    }
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import org.bukkit.event.inventory.InventoryClickEvent;
                import xyz.xenondevs.invui.item.Item;

                class UsesItem {
                    void call(Item item, ClickType clickType, Player player, InventoryClickEvent event) {
                        item.handleClick(clickType, player, event);
                    }
                }
                """,
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import xyz.xenondevs.invui.Click;
                import xyz.xenondevs.invui.item.Item;

                class UsesItem {
                    void call(Item item, ClickType clickType, Player player, Click event) {
                        item.handleClick(clickType, player, event);
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotMigrateUnrelatedInventoryClickEventUsage() {
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

                public class Click {}
                """
            ),
            java(
                """
                package test;

                import org.bukkit.event.inventory.InventoryClickEvent;

                class UnrelatedUsage {
                    void use(InventoryClickEvent event) {
                    }
                }
                """
            )
        );
    }
}
