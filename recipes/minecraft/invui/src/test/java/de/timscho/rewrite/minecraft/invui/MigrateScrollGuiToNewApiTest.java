package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateScrollGuiToNewApiTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateScrollGuiToNewApi())
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesConsumerFactoryOverloadsToBuilder() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface Item {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.inventory;

                public interface Inventory {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.gui;

                public interface Gui {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.gui;

                import java.util.function.Consumer;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                public interface ScrollGui<C> {
                    static Builder<Item> itemsBuilder() { return null; }
                    static Builder<Gui> guisBuilder() { return null; }
                    static Builder<Inventory> inventoriesBuilder() { return null; }
                    static ScrollGui<Item> items(Consumer<Builder<Item>> consumer) { return null; }
                    static ScrollGui<Gui> guis(Consumer<Builder<Gui>> consumer) { return null; }
                    static ScrollGui<Inventory> inventories(Consumer<Builder<Inventory>> consumer) { return null; }

                    interface Builder<C> {
                        ScrollGui<C> build();
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.gui.ScrollGui;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                class UsesScrollGuiConsumers {
                    Object migrate() {
                        ScrollGui<Item> items = ScrollGui.items(builder -> {
                        });
                        ScrollGui<Gui> guis = ScrollGui.guis(builder -> {
                        });
                        ScrollGui<Inventory> inventories = ScrollGui.inventories(builder -> {
                        });
                        return java.util.List.of(items, guis, inventories);
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.gui.ScrollGui;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                class UsesScrollGuiConsumers {
                    Object migrate() {
                        ScrollGui<Item> items = ((java.util.function.Supplier<xyz.xenondevs.invui.gui.ScrollGui<xyz.xenondevs.invui.item.Item>>) () -> {
                            java.util.function.Consumer<xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.item.Item>> consumer = builder -> {
                            };
                            xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.item.Item> builder = xyz.xenondevs.invui.gui.ScrollGui.itemsBuilder();
                            consumer.accept(builder);
                            return builder.build();
                        }).get();
                        ScrollGui<Gui> guis = ((java.util.function.Supplier<xyz.xenondevs.invui.gui.ScrollGui<xyz.xenondevs.invui.gui.Gui>>) () -> {
                            java.util.function.Consumer<xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.gui.Gui>> consumer = builder -> {
                            };
                            xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.gui.Gui> builder = xyz.xenondevs.invui.gui.ScrollGui.guisBuilder();
                            consumer.accept(builder);
                            return builder.build();
                        }).get();
                        ScrollGui<Inventory> inventories = ((java.util.function.Supplier<xyz.xenondevs.invui.gui.ScrollGui<xyz.xenondevs.invui.inventory.Inventory>>) () -> {
                            java.util.function.Consumer<xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.inventory.Inventory>> consumer = builder -> {
                            };
                            xyz.xenondevs.invui.gui.ScrollGui.Builder<xyz.xenondevs.invui.inventory.Inventory> builder = xyz.xenondevs.invui.gui.ScrollGui.inventoriesBuilder();
                            consumer.accept(builder);
                            return builder.build();
                        }).get();
                        return java.util.List.of(items, guis, inventories);
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesLegacyIntSlotFactories() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.item;

                public interface Item {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.inventory;

                public interface Inventory {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.gui;

                public interface Gui {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.gui;

                import java.util.List;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                public interface ScrollGui<C> {
                    static ScrollGui<Item> ofItems(int width, int height, List<Item> items, int... contentListSlots) { return null; }
                    static ScrollGui<Gui> ofGuis(int width, int height, List<Gui> guis, int... contentListSlots) { return null; }
                    static ScrollGui<Inventory> ofInventories(int width, int height, List<Inventory> inventories, int... contentListSlots) { return null; }
                    static ScrollGui<Item> ofItems(int width, int height, List<? extends Item> items, List<? extends Slot> contentListSlots, LineOrientation orientation) { return null; }
                    static ScrollGui<Gui> ofGuis(int width, int height, List<? extends Gui> guis, List<? extends Slot> contentListSlots, LineOrientation orientation) { return null; }
                    static ScrollGui<Inventory> ofInventories(int width, int height, List<? extends Inventory> inventories, List<? extends Slot> contentListSlots, LineOrientation orientation) { return null; }

                    enum LineOrientation {
                        HORIZONTAL
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.gui;

                public record Slot(int x, int y) {}
                """
            ),
            java(
                """
                package test;

                import java.util.List;
                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.gui.ScrollGui;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                class UsesScrollGuiFactories {
                    Object migrate(List<Item> items, List<Gui> guis, List<Inventory> inventories, int[] slots) {
                        ScrollGui.ofItems(9, 3, items, 0, 1, 2);
                        ScrollGui.ofGuis(9, 3, guis, slots);
                        return ScrollGui.ofInventories(9, 3, inventories, new int[] {4, 5, 6});
                    }
                }
                """,
                """
                package test;

                import java.util.List;
                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.gui.ScrollGui;
                import xyz.xenondevs.invui.inventory.Inventory;
                import xyz.xenondevs.invui.item.Item;

                class UsesScrollGuiFactories {
                    Object migrate(List<Item> items, List<Gui> guis, List<Inventory> inventories, int[] slots) {
                        ScrollGui.ofItems(9, 3, items, java.util.stream.IntStream.of(0, 1, 2).mapToObj(slot -> new xyz.xenondevs.invui.gui.Slot(slot % (9), slot / (9))).toList(), xyz.xenondevs.invui.gui.ScrollGui.LineOrientation.HORIZONTAL);
                        ScrollGui.ofGuis(9, 3, guis, java.util.stream.IntStream.of(slots).mapToObj(slot -> new xyz.xenondevs.invui.gui.Slot(slot % (9), slot / (9))).toList(), xyz.xenondevs.invui.gui.ScrollGui.LineOrientation.HORIZONTAL);
                        return ScrollGui.ofInventories(9, 3, inventories, java.util.stream.IntStream.of(new int[]{4, 5, 6}).mapToObj(slot -> new xyz.xenondevs.invui.gui.Slot(slot % (9), slot / (9))).toList(), xyz.xenondevs.invui.gui.ScrollGui.LineOrientation.HORIZONTAL);
                    }
                }
                """
            )
        );
    }
}
