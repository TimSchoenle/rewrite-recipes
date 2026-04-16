package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.stream.Collectors;

public class MigrateScrollGuiToNewApi extends Recipe {
    private static final String SCROLL_GUI = "xyz.xenondevs.invui.gui.ScrollGui";

    private static final String ITEM = "xyz.xenondevs.invui.item.Item";
    private static final String GUI = "xyz.xenondevs.invui.gui.Gui";
    private static final String INVENTORY = "xyz.xenondevs.invui.inventory.Inventory";

    private static final MethodMatcher SCROLL_GUI_ITEMS_WITH_CONSUMER =
        new MethodMatcher(SCROLL_GUI + " items(java.util.function.Consumer)");
    private static final MethodMatcher SCROLL_GUI_GUIS_WITH_CONSUMER =
        new MethodMatcher(SCROLL_GUI + " guis(java.util.function.Consumer)");
    private static final MethodMatcher SCROLL_GUI_INVENTORIES_WITH_CONSUMER =
        new MethodMatcher(SCROLL_GUI + " inventories(java.util.function.Consumer)");

    private static final MethodMatcher SCROLL_GUI_OF_ITEMS_WITH_INT_SLOTS =
        new MethodMatcher(SCROLL_GUI + " ofItems(int, int, java.util.List, int[])");
    private static final MethodMatcher SCROLL_GUI_OF_GUIS_WITH_INT_SLOTS =
        new MethodMatcher(SCROLL_GUI + " ofGuis(int, int, java.util.List, int[])");
    private static final MethodMatcher SCROLL_GUI_OF_INVENTORIES_WITH_INT_SLOTS =
        new MethodMatcher(SCROLL_GUI + " ofInventories(int, int, java.util.List, int[])");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate InvUI ScrollGui API to v2";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates ScrollGui consumer factory overloads and legacy int content-list slot signatures to v2.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (SCROLL_GUI_ITEMS_WITH_CONSUMER.matches(m)) {
                    return this.migrateFactoryConsumerOverload(
                        m,
                        "xyz.xenondevs.invui.gui.ScrollGui<" + ITEM + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.Builder<" + ITEM + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.itemsBuilder()"
                    );
                }

                if (SCROLL_GUI_GUIS_WITH_CONSUMER.matches(m)) {
                    return this.migrateFactoryConsumerOverload(
                        m,
                        "xyz.xenondevs.invui.gui.ScrollGui<" + GUI + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.Builder<" + GUI + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.guisBuilder()"
                    );
                }

                if (SCROLL_GUI_INVENTORIES_WITH_CONSUMER.matches(m)) {
                    return this.migrateFactoryConsumerOverload(
                        m,
                        "xyz.xenondevs.invui.gui.ScrollGui<" + INVENTORY + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.Builder<" + INVENTORY + ">",
                        "xyz.xenondevs.invui.gui.ScrollGui.inventoriesBuilder()"
                    );
                }

                if (SCROLL_GUI_OF_ITEMS_WITH_INT_SLOTS.matches(m)
                    || SCROLL_GUI_OF_GUIS_WITH_INT_SLOTS.matches(m)
                    || SCROLL_GUI_OF_INVENTORIES_WITH_INT_SLOTS.matches(m)) {
                    return this.migrateLegacyOfFactory(m);
                }

                return m;
            }

            private J.MethodInvocation migrateFactoryConsumerOverload(
                final J.MethodInvocation method,
                final String returnType,
                final String builderType,
                final String builderFactory
            ) {
                if (method.getArguments().size() != 1) {
                    return method;
                }

                final Expression consumer = method.getArguments().getFirst();
                return JavaTemplate.builder(
                    "((java.util.function.Supplier<" + returnType + ">) () -> {" +
                        "java.util.function.Consumer<" + builderType + "> consumer = #{any(java.util.function.Consumer)};" +
                        builderType + " builder = " + builderFactory + ";" +
                        "consumer.accept(builder);" +
                        "return builder.build();" +
                        "}).get()"
                )
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(), consumer)
                    .withPrefix(method.getPrefix());
            }

            private J.MethodInvocation migrateLegacyOfFactory(final J.MethodInvocation method) {
                if (method.getArguments().size() < 4) {
                    return method;
                }

                final Expression width = method.getArguments().get(0);
                final Expression height = method.getArguments().get(1);
                final Expression content = method.getArguments().get(2);
                final List<Expression> contentListSlotArguments = method.getArguments().subList(3, method.getArguments().size());

                final String widthSource = width.printTrimmed(getCursor());
                final String contentListSlotCsv = contentListSlotArguments.stream()
                    .map(arg -> arg.printTrimmed(getCursor()))
                    .collect(Collectors.joining(", "));
                final String migratedContentListSlots =
                    "java.util.stream.IntStream.of(" + contentListSlotCsv + ")" +
                        ".mapToObj(slot -> new xyz.xenondevs.invui.gui.Slot(slot % (" + widthSource + "), slot / (" + widthSource + ")))" +
                        ".toList()";

                return JavaTemplate.builder(
                    "#{any(int)}, #{any(int)}, #{any(java.util.List)}, " +
                        migratedContentListSlots +
                        ", xyz.xenondevs.invui.gui.ScrollGui.LineOrientation.HORIZONTAL"
                )
                    .build()
                    .apply(getCursor(), method.getCoordinates().replaceArguments(), width, height, content);
            }
        };
    }
}
