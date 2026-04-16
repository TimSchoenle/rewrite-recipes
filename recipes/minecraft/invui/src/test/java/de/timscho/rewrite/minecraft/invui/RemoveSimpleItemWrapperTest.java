package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveSimpleItemWrapperTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new RemoveSimpleItemWrapper());
    }

    @Test
    void removesSimpleItemWrapperWithItemStackArgument() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;

                public class SimpleItem {
                    public SimpleItem(ItemStack itemStack) {}
                    public SimpleItem(String value) {}
                    public SimpleItem(ItemStack itemStack, int amount) {}
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesItem {
                    void set(Object value) {}

                    void example(ItemStack informatoryItem) {
                        set(new SimpleItem(informatoryItem));
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;

                class UsesItem {
                    void set(Object value) {}

                    void example(ItemStack informatoryItem) {
                        set(informatoryItem);
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotRemoveWhenArgumentIsNotItemStack() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;

                public class SimpleItem {
                    public SimpleItem(ItemStack itemStack) {}
                    public SimpleItem(String value) {}
                    public SimpleItem(ItemStack itemStack, int amount) {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesItem {
                    void set(Object value) {}

                    void example(String value) {
                        set(new SimpleItem(value));
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotRemoveWhenMultipleArgumentsAreUsed() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;

                public class SimpleItem {
                    public SimpleItem(ItemStack itemStack) {}
                    public SimpleItem(String value) {}
                    public SimpleItem(ItemStack itemStack, int amount) {}
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesItem {
                    void set(Object value) {}

                    void example(ItemStack itemStack, int amount) {
                        set(new SimpleItem(itemStack, amount));
                    }
                }
                """
            )
        );
    }
}
