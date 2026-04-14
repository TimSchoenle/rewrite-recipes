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
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider() {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }
                """
            )
        );
    }

    @Test
    void migratesDeepHierarchyAndSetGuiOverrides() {
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
                    public abstract ItemProvider getItemProvider();
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
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public void bind(CustomGui gui) {
                        super.bind(gui);
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider() {
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
                """
            )
        );
    }

    @Test
    void removesGenericArgumentsFromIndirectControlItemSubclasses() {
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
                    public abstract ItemProvider getItemProvider();
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
                    @Override
                    public ItemProvider getItemProvider(CustomGui gui) {
                        return null;
                    }

                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider() {
                        return getItemProvider((test.CustomGui) getGui());
                    }

                    @Override
                    public test.CustomGui getGui() {
                        return (test.CustomGui) super.getGui();
                    }
                }

                abstract class BaseGuildModuleItem extends BaseControlItem {
                }
                """
            )
        );
    }
}
