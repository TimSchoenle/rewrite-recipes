package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateSimpleItemToItemTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateSimpleItemToItem())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesItemProviderAndClickHandlerToBuilder() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                import java.util.function.Consumer;

                public interface Item {
                    static Builder builder() {
                        return null;
                    }

                    static Item simple(ItemProvider provider) {
                        return null;
                    }

                    interface Builder {
                        Builder setItemProvider(ItemProvider provider);
                        Builder addClickHandler(Consumer<?> clickHandler);
                        Item build();
                    }
                }
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
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;
                import java.util.function.Consumer;
                import xyz.xenondevs.invui.item.ItemProvider;

                public class SimpleItem {
                    public SimpleItem(ItemProvider itemProvider, Consumer<?> clickHandler) {}
                    public SimpleItem(ItemStack itemStack, Consumer<?> clickHandler) {}
                }
                """
            ),
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package test;

                import java.util.function.Consumer;
                
                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesSimpleItem {
                    Object build(ItemProvider provider, Consumer<Object> clickHandler) {
                        return new SimpleItem(provider, clickHandler);
                    }
                }
                """,
                """
                package test;

                import java.util.function.Consumer;
                
                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesSimpleItem {
                    Object build(ItemProvider provider, Consumer<Object> clickHandler) {
                        return Item.builder().setItemProvider(provider).addClickHandler(clickHandler).build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesItemStackAndClickHandlerToBuilder() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;
                import java.util.function.Consumer;

                public interface Item {
                    static Builder builder() {
                        return null;
                    }

                    static Item simple(ItemStack stack) {
                        return null;
                    }

                    interface Builder {
                        Builder setItemProvider(ItemStack stack);
                        Builder addClickHandler(Consumer<?> clickHandler);
                        Item build();
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;
                import java.util.function.Consumer;

                public class SimpleItem {
                    public SimpleItem(ItemStack itemStack, Consumer<?> clickHandler) {}
                }
                """
            ),
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package test;

                import java.util.function.Consumer;
                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesSimpleItem {
                    Object build(ItemStack stack, Consumer<Object> clickHandler) {
                        return new SimpleItem(stack, clickHandler);
                    }
                }
                """,
                """
                package test;

                import java.util.function.Consumer;
                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.Item;

                class UsesSimpleItem {
                    Object build(ItemStack stack, Consumer<Object> clickHandler) {
                        return Item.builder().setItemProvider(stack).addClickHandler(clickHandler).build();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesNullClickHandlerToSimpleFactory() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface Item {
                    static Item simple(ItemProvider provider) {
                        return null;
                    }
                }
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
                package xyz.xenondevs.invui.item.impl;

                import java.util.function.Consumer;
                import xyz.xenondevs.invui.item.ItemProvider;

                public class SimpleItem {
                    public SimpleItem(ItemProvider itemProvider, Consumer<?> clickHandler) {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesSimpleItem {
                    Object build(ItemProvider provider) {
                        return new SimpleItem(provider, null);
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesSimpleItem {
                    Object build(ItemProvider provider) {
                        return Item.simple(provider);
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesSingleItemProviderConstructorToSimpleFactory() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface Item {
                    static Item simple(ItemProvider provider) {
                        return null;
                    }
                }
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
                package xyz.xenondevs.invui.item.impl;

                import xyz.xenondevs.invui.item.ItemProvider;

                public class SimpleItem {
                    public SimpleItem(ItemProvider itemProvider) {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesSimpleItem {
                    Object build(ItemProvider provider) {
                        return new SimpleItem(provider);
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesSimpleItem {
                    Object build(ItemProvider provider) {
                        return Item.simple(provider);
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesSingleItemStackConstructorToSimpleFactory() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public interface Item {
                    static Item simple(ItemStack stack) {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl;

                import org.bukkit.inventory.ItemStack;

                public class SimpleItem {
                    public SimpleItem(ItemStack itemStack) {}
                }
                """
            ),
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.Item;
                import xyz.xenondevs.invui.item.impl.SimpleItem;

                class UsesSimpleItem {
                    Object build(ItemStack stack) {
                        return new SimpleItem(stack);
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.Item;

                class UsesSimpleItem {
                    Object build(ItemStack stack) {
                        return Item.simple(stack);
                    }
                }
                """
            )
        );
    }
}
