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

                public class InventoryClickEvent {
                    public int getHotbarButton() {
                        return -1;
                    }
                }
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
                import org.bukkit.event.inventory.InventoryClickEvent;
                import xyz.xenondevs.invui.item.Item;

                class UsesItem {
                    void call(Item item, ClickType clickType, Player player, InventoryClickEvent event) {
                        item.handleClick(clickType, player, new xyz.xenondevs.invui.Click(player, clickType, event.getHotbarButton()));
                    }
                }
                """
            )
        );
    }

    @Test
    void renamesEventAccessorsInsideItemHandleClick() {
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

                public enum ClickType { LEFT }
                """
            ),
            java(
                """
                package org.bukkit.event.inventory;

                import org.bukkit.entity.Player;

                public class InventoryClickEvent {
                    public Player getWhoClicked() { return null; }
                    public ClickType getClick() { return null; }
                    public int getHotbarButton() { return -1; }
                }
                """
            ),
            java(
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
                    InventoryClickEvent lastEvent;

                    @Override
                    public void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
                        Player who = event.getWhoClicked();
                        ClickType type = event.getClick();
                        int button = event.getHotbarButton();
                        Player previous = lastEvent.getWhoClicked();
                    }
                }
                """,
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import org.bukkit.event.inventory.InventoryClickEvent;
                import xyz.xenondevs.invui.Click;
                import xyz.xenondevs.invui.item.Item;

                class ItemImpl implements Item {
                    InventoryClickEvent lastEvent;

                    @Override
                    public void handleClick(ClickType clickType, Player player, Click event) {
                        Player who = event.player();
                        ClickType type = event.clickType();
                        int button = event.hotbarButton();
                        Player previous = lastEvent.getWhoClicked();
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

    @Test
    void doesNotRewriteHandleClickOutsideItemHierarchy() {
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

                public enum ClickType { LEFT }
                """
            ),
            java(
                """
                package org.bukkit.event.inventory;

                import org.bukkit.entity.Player;

                public class InventoryClickEvent {
                    public Player getWhoClicked() { return null; }
                    public ClickType getClick() { return null; }
                    public int getHotbarButton() { return -1; }
                }
                """
            ),
            java(
                """
                package test;

                import org.bukkit.entity.Player;
                import org.bukkit.event.inventory.ClickType;
                import org.bukkit.event.inventory.InventoryClickEvent;

                class MenuListener {
                    void handleClick(ClickType clickType, Player player, InventoryClickEvent event) {
                        Player who = event.getWhoClicked();
                        ClickType type = event.getClick();
                        int button = event.getHotbarButton();
                    }
                }
                """
            )
        );
    }
}
