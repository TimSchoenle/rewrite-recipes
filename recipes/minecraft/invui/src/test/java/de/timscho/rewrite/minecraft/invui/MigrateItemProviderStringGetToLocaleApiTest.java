package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateItemProviderStringGetToLocaleApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateItemProviderStringGetToLocaleApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesStringGetInvocationAndAddsLocaleBridges() {
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
                    default ItemStack get() {
                        return get(null);
                    }
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomProvider implements ItemProvider {
                    @Override
                    public ItemStack get(String lang) {
                        if (lang == null) {
                            return null;
                        }
                        return null;
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomProvider implements ItemProvider {
                    
                    public ItemStack get(String lang) {
                        if (lang == null) {
                            return null;
                        }
                        return null;
                    }

                    @Override
                    public org.bukkit.inventory.ItemStack get(java.util.Locale locale) {
                        return get(locale == null ? null : locale.toLanguageTag());
                    }

                    @Override
                    public org.bukkit.inventory.ItemStack get() {
                        return get((java.util.Locale) null);
                    }
                }
                """.replace(
                    "\nclass CustomProvider implements ItemProvider {\n\n    public ItemStack get(String lang)",
                    "\nclass CustomProvider implements ItemProvider {\n    \n    public ItemStack get(String lang)"
                )
            ),
            java(
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesProvider {
                    ItemStack resolve(ItemProvider provider, String lang) {
                        return provider.get(lang);
                    }
                }
                """,
                """
                package test;

                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesProvider {
                    ItemStack resolve(ItemProvider provider, String lang) {
                        return provider.get(java.util.Optional.ofNullable(lang).map(java.util.Locale::forLanguageTag).orElse(null));
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotRewriteLocaleBasedGetInvocations() {
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
                import java.util.Locale;

                public interface ItemProvider {
                    ItemStack get(Locale locale);
                    ItemStack get();
                }
                """
            ),
            java(
                """
                package test;

                import java.util.Locale;
                import org.bukkit.inventory.ItemStack;
                import xyz.xenondevs.invui.item.ItemProvider;

                class UsesProvider {
                    ItemStack resolve(ItemProvider provider, Locale locale) {
                        return provider.get(locale);
                    }
                }
                """
            )
        );
    }
}
