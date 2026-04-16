package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MigrateControlItemToAbstractBoundItemTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MigrateControlItemToAbstractBoundItem())
            .expectedCyclesThatMakeChanges(1)
            .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void migratesTypeUsageAndSetGuiCall() {
        this.rewriteRun(
            spec -> spec.expectedCyclesThatMakeChanges(2),
            java(
                """
                package org.bukkit.entity;

                public class Player {}
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
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                public abstract class AbstractBoundItem {
                    public abstract ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public void bind(xyz.xenondevs.invui.gui.Gui gui) {}
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                    public void setGui(G gui) {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                class UsesControlItem {
                    void bindIt(ControlItem<Gui> item, Gui gui) {
                        item.setGui(gui);
                        item.getItemProvider(gui);
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.AbstractBoundItem;

                class UsesControlItem {
                    void bindIt(AbstractBoundItem item, Gui gui) {
                        item.bind(gui);
                        item.getItemProvider(((org.bukkit.entity.Player) null));
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesSubclassAndAddsCompatibilityBridgeMethods() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.invui.gui;

                public interface Gui {}
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

                import xyz.xenondevs.invui.gui.Gui;

                public abstract class AbstractBoundItem {
                    public abstract xyz.xenondevs.invui.item.ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public Gui getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                    public G getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                class CustomGui implements xyz.xenondevs.invui.gui.Gui {}

                class MyControlItem extends ControlItem<CustomGui> {
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.item.AbstractBoundItem;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomGui implements xyz.xenondevs.invui.gui.Gui {}

                class MyControlItem extends AbstractBoundItem {

                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(org.bukkit.entity.Player viewer) {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }
                """.replace(
                    "\n\n    public ItemProvider getItemProvider(CustomGui gui)",
                    "\n\n    \n    public ItemProvider getItemProvider(CustomGui gui)"
                )
            )
        );
    }

    @Test
    void migratesDeepHierarchyAndSetGuiOverrides() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
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
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import xyz.xenondevs.invui.gui.Gui;

                public abstract class AbstractBoundItem {
                    public abstract ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public Gui getGui() {
                        return null;
                    }
                    public void bind(Gui gui) {}
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                    public G getGui() {
                        return null;
                    }
                    public void setGui(G gui) {}
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                class CustomGui implements Gui {}

                abstract class BaseControlItem extends ControlItem<CustomGui> {
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public void setGui(CustomGui gui) {
                        super.setGui(gui);
                    }
                }

                class ExtendedControlItem extends BaseControlItem {
                    @Override
                    public void setGui(CustomGui gui) {
                        super.setGui(gui);
                    }
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.AbstractBoundItem;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomGui implements Gui {}

                abstract class BaseControlItem extends AbstractBoundItem {

                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public void bind(CustomGui gui) {
                        super.bind(gui);
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(org.bukkit.entity.Player viewer) {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }

                class ExtendedControlItem extends BaseControlItem {
                    @Override
                    public void bind(CustomGui gui) {
                        super.bind(gui);
                    }
                }
                """.replace(
                    "\n\n    public ItemProvider getItemProvider(CustomGui gui)",
                    "\n\n    \n    public ItemProvider getItemProvider(CustomGui gui)"
                )
            )
        );
    }

    @Test
    void removesGenericArgumentsFromIndirectControlItemSubclasses() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
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
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import xyz.xenondevs.invui.gui.Gui;

                public abstract class AbstractBoundItem {
                    public abstract ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public Gui getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                class CustomGui implements Gui {}

                abstract class BaseControlItem extends ControlItem<CustomGui> {
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }
                }

                abstract class BaseGuildModuleItem extends BaseControlItem<CustomGui> {
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.AbstractBoundItem;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomGui implements Gui {}

                abstract class BaseControlItem extends AbstractBoundItem {

                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(org.bukkit.entity.Player viewer) {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }

                abstract class BaseGuildModuleItem extends BaseControlItem {
                }
                """.replace(
                    "\n\n    public ItemProvider getItemProvider(CustomGui gui)",
                    "\n\n    \n    public ItemProvider getItemProvider(CustomGui gui)"
                )
            )
        );
    }

    @Test
    void keepsGenericArgumentsOnIndirectSubclassesWithOwnTypeParameters() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
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
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import xyz.xenondevs.invui.gui.Gui;

                public abstract class AbstractBoundItem {
                    public abstract ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public Gui getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                    public G getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                class CustomGui implements Gui {}

                abstract class BaseControlItem<T> extends ControlItem<CustomGui> {
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }
                }

                class ExtendedControlItem extends BaseControlItem<String> {
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.AbstractBoundItem;
                import xyz.xenondevs.invui.item.ItemProvider;

                class CustomGui implements Gui {}

                abstract class BaseControlItem<T> extends AbstractBoundItem {
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(org.bukkit.entity.Player viewer) {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }

                class ExtendedControlItem extends BaseControlItem<String> {
                }
                """
            )
        );
    }

    @Test
    void removesGuiTypeParametersRecursivelyFromHierarchyClasses() {
        this.rewriteRun(
            java(
                """
                package org.bukkit.entity;

                public class Player {}
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
                package xyz.xenondevs.invui.item;

                public interface ItemProvider {}
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item;

                import xyz.xenondevs.invui.gui.Gui;

                public abstract class AbstractBoundItem {
                    public abstract ItemProvider getItemProvider(org.bukkit.entity.Player viewer);
                    public Gui getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package xyz.xenondevs.invui.item.impl.controlitem;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.ItemProvider;

                public abstract class ControlItem<G extends Gui> {
                    public abstract ItemProvider getItemProvider(G gui);
                    public G getGui() {
                        return null;
                    }
                }
                """
            ),
            java(
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.impl.controlitem.ControlItem;

                interface UiContext {}

                class CustomGui implements Gui {}
                class CustomContext implements UiContext {}

                abstract class BaseControlItem<G extends Gui, C extends UiContext> extends ControlItem<G> {
                }

                abstract class BaseGuildModuleItem<G extends Gui, C extends UiContext> extends BaseControlItem<G, C> {
                }

                class ConcreteItem extends BaseGuildModuleItem<CustomGui, CustomContext> {
                }
                """,
                """
                package test;

                import xyz.xenondevs.invui.gui.Gui;
                import xyz.xenondevs.invui.item.AbstractBoundItem;

                interface UiContext {}

                class CustomGui implements Gui {}
                class CustomContext implements UiContext {}

                abstract class BaseControlItem<C extends UiContext> extends AbstractBoundItem {
                }

                abstract class BaseGuildModuleItem<C extends UiContext> extends BaseControlItem<C> {
                }

                class ConcreteItem extends BaseGuildModuleItem<CustomContext> {
                }
                """
            )
        );
    }
}
