package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateItemGetItemProviderToPlayerApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateItemGetItemProviderToPlayerApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesItemNoArgProviderInvocationAndAddsBridgeMethod() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface Item {
                    default ItemProvider getItemProvider() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class LegacyItem implements Item {
                    @Override
                    public ItemProvider getItemProvider() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesItem {
                    ItemProvider provide(Item item) {
                        return item.getItemProvider();
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesItem {
                    ItemProvider provide(Item item) {
                        return item.getItemProvider(((org.bukkit.entity.Player) null));
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotRewriteBridgeNoArgCallInsidePlayerOverload() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.entity.Player;

                public interface Item {
                    ItemProvider getItemProvider(Player viewer);
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.entity.Player;
                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class LegacyItem implements Item {
                    public ItemProvider getItemProvider() {
                        return null;
                    }

                    @Override
                    public ItemProvider getItemProvider(Player viewer) {
                        return getItemProvider();
                    }
                }
                """
            )
        );
    }
}
