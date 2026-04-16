package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateStatelessItemProviderToItemWrapperTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateStatelessItemProviderToItemWrapper())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesAnonymousItemProviderThatDoesNotUseLang() {
        this.rewriteRun(
            java(
                """
                package org.jetbrains.annotations;

                public @interface NonNull {}
                """
            ),
            java(
                """
                package org.jetbrains.annotations;

                public @interface Nullable {}
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
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public interface ItemProvider {
                    ItemStack get(String lang);
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public final class ItemWrapper implements ItemProvider {
                    public ItemWrapper(ItemStack itemStack) {}

                    @Override
                    public ItemStack get(String lang) {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import org.jetbrains.annotations.NonNull;
                import org.jetbrains.annotations.Nullable;
                import xyz.xenondevs.invui.item.ItemProvider;

                class BaseControlItem {
                    ItemStack createItemStack() {
                        return null;
                    }

                    Object provider() {
                        return new ItemProvider() {
                            @Override
                            public @NonNull ItemStack get(@Nullable final String lang) {
                                return BaseControlItem.this.createItemStack();
                            }
                        };
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import org.jetbrains.annotations.NonNull;
                import org.jetbrains.annotations.Nullable;
                import xyz.xenondevs.invui.item.ItemWrapper;

                class BaseControlItem {
                    ItemStack createItemStack() {
                        return null;
                    }

                    Object provider() {
                        return new ItemWrapper(BaseControlItem.this.createItemStack());
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotMigrateWhenLangIsUsed() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public interface ItemProvider {
                    ItemStack get(String lang);
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesLang {
                    ItemStack stack(String lang) {
                        return null;
                    }

                    Object provider() {
                        return new ItemProvider() {
                            @Override
                            public ItemStack get(String lang) {
                                return stack(lang);
                            }
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesSimpleAnonymousItemProvider() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.inventory;

                public class ItemStack {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public interface ItemProvider {
                    ItemStack get(String lang);
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import org.bukkit.inventory.ItemStack;

                public final class ItemWrapper implements ItemProvider {
                    public ItemWrapper(ItemStack itemStack) {}
                    @Override
                    public ItemStack get(String lang) { return null; }
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesProvider {
                    ItemStack buildStack() {
                        return null;
                    }

                    Object provider() {
                        return new ItemProvider() {
                            @Override
                            public ItemStack get(String lang) {
                                return buildStack();
                            }
                        };
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemWrapper;

                class UsesProvider {
                    ItemStack buildStack() {
                        return null;
                    }

                    Object provider() {
                        return new ItemWrapper(buildStack());
                    }
                }
                """
            )
        );
    }
}
