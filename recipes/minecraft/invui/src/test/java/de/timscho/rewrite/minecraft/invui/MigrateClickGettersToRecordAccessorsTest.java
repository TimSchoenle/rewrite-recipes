package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateClickGettersToRecordAccessorsTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateClickGettersToRecordAccessors())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesClickGetters() {
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
                package xyz.xenondevs.invui;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;

                public class Click {
                    public Player getPlayer() { return null; }
                    public ClickType getClick() { return null; }
                    public int getHotbarButton() { return -1; }
                }
                """,
                """
                package xyz.xenondevs.invui;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;

                public class Click {
                    public Player player() { return null; }
                    public ClickType clickType() { return null; }
                    public int hotbarButton() { return -1; }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.Click;

                class UsesClick {
                    Object use(Click click) {
                        Object player = click.getPlayer();
                        Object clickType = click.getClick();
                        return click.getHotbarButton();
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.Click;

                class UsesClick {
                    Object use(Click click) {
                        Object player = click.player();
                        Object clickType = click.clickType();
                        return click.hotbarButton();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesClickTypeGetterVariant() {
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
                package xyz.xenondevs.invui;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;

                public class Click {
                    public Player getPlayer() { return null; }
                    public ClickType getClickType() { return null; }
                    public int getHotbarButton() { return -1; }
                }
                """,
                """
                package xyz.xenondevs.invui;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;

                public class Click {
                    public Player player() { return null; }
                    public ClickType clickType() { return null; }
                    public int hotbarButton() { return -1; }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.Click;

                class UsesClick {
                    Object use(Click click) {
                        Object player = click.getPlayer();
                        Object clickType = click.getClickType();
                        return click.getHotbarButton();
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.Click;

                class UsesClick {
                    Object use(Click click) {
                        Object player = click.player();
                        Object clickType = click.clickType();
                        return click.hotbarButton();
                    }
                }
                """
            )
        );
    }

    @Test
    void doesNotMigrateUnrelatedGetterNames() {
        this.rewriteRun(
            java(
                """
                package test;

                class OtherClick {
                    Object getPlayer() { return null; }
                    Object getClick() { return null; }
                    int getHotbarButton() { return 0; }
                }
                """
            ),
            java(
                """
                package test;

                class UsesOtherClick {
                    Object use(OtherClick click) {
                        Object player = click.getPlayer();
                        Object clickType = click.getClick();
                        return click.getHotbarButton();
                    }
                }
                """
            )
        );
    }
}
