package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class MigrateWindowToNewApi extends Recipe {
    private static final String WINDOW = "xyz.xenondevs.invui.window.Window";
    private static final String WINDOW_BUILDER = MigrateWindowToNewApi.WINDOW + "$Builder";
    private static final String COMPONENT_WRAPPER = "xyz.xenondevs.inventoryaccess.component.ComponentWrapper";
    private static final String ADVENTURE_COMPONENT = "net.kyori.adventure.text.Component";
    private static final String INVENTORY_CLICK_EVENT = "org.bukkit.event.inventory.InventoryClickEvent";
    private static final String CLICK_EVENT = "xyz.xenondevs.invui.ClickEvent";
    private static final String BASE_COMPONENT = "net.md_5.bungee.api.chat.BaseComponent";

    private static final MethodMatcher WINDOW_FACTORY_SPLIT_NO_ARGS = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " split()");
    private static final MethodMatcher WINDOW_FACTORY_MERGED_NO_ARGS = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " merged()");
    private static final MethodMatcher WINDOW_FACTORY_SINGLE_NO_ARGS = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " single()");
    private static final MethodMatcher WINDOW_FACTORY_SPLIT_WITH_CONSUMER = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " split(java.util.function.Consumer)");
    private static final MethodMatcher WINDOW_FACTORY_MERGED_WITH_CONSUMER = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " merged(java.util.function.Consumer)");
    private static final MethodMatcher WINDOW_FACTORY_SINGLE_WITH_CONSUMER = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " single(java.util.function.Consumer)");
    private static final MethodMatcher WINDOW_CHANGE_TITLE = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " changeTitle(..)");
    private static final MethodMatcher WINDOW_GET_CURRENT_VIEWER = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " getCurrentViewer()");
    private static final MethodMatcher WINDOW_GET_VIEWER_UUID = new MethodMatcher(MigrateWindowToNewApi.WINDOW + " getViewerUUID()");
    private static final MethodMatcher WINDOW_BUILDER_SET_GUI = new MethodMatcher(MigrateWindowToNewApi.WINDOW_BUILDER + " setGui(..)");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate InvUI Window API to v2";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates safe Window API changes (renames/type changes) and marks ambiguous migrations for manual follow-up.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit compilationUnit, final ExecutionContext ctx) {
                if (this.usesWindowMethods(compilationUnit)) {
                    this.doAfterVisit(new ChangeType(MigrateWindowToNewApi.COMPONENT_WRAPPER, MigrateWindowToNewApi.ADVENTURE_COMPONENT, true).getVisitor());
                    this.doAfterVisit(new ChangeType(MigrateWindowToNewApi.INVENTORY_CLICK_EVENT, MigrateWindowToNewApi.CLICK_EVENT, true).getVisitor());
                }
                return super.visitCompilationUnit(compilationUnit, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (MigrateWindowToNewApi.WINDOW_FACTORY_SPLIT_NO_ARGS.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("builder"));
                } else if (MigrateWindowToNewApi.WINDOW_FACTORY_MERGED_NO_ARGS.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("mergedBuilder"));
                } else if (MigrateWindowToNewApi.WINDOW_FACTORY_SINGLE_NO_ARGS.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("builder"));
                } else if (MigrateWindowToNewApi.WINDOW_FACTORY_SPLIT_WITH_CONSUMER.matches(m) || MigrateWindowToNewApi.WINDOW_FACTORY_MERGED_WITH_CONSUMER.matches(m)
                    || MigrateWindowToNewApi.WINDOW_FACTORY_SINGLE_WITH_CONSUMER.matches(m)) {
                    return m;
                } else if (MigrateWindowToNewApi.WINDOW_CHANGE_TITLE.matches(m)) {
                    m = this.migrateChangeTitle(m);
                } else if (MigrateWindowToNewApi.WINDOW_GET_CURRENT_VIEWER.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("getViewer"));
                } else if (MigrateWindowToNewApi.WINDOW_GET_VIEWER_UUID.matches(m)) {
                    m = this.convertGetViewerUuid(m);
                } else if (this.isSetGuiOnWindowBuilderHierarchy(m)) {
                    m = m.withName(m.getName().withSimpleName("setUpperGui"));
                }

                return m;
            }

            private J.MethodInvocation migrateChangeTitle(final J.MethodInvocation method) {
                if (method.getArguments().size() != 1) {
                    return method;
                }

                final Expression titleArg = method.getArguments().getFirst();
                if (this.isBaseComponentArray(titleArg.getType())) {
                    return method;
                }

                return method.withName(method.getName().withSimpleName("setTitle"));
            }

            private J.MethodInvocation convertGetViewerUuid(final J.MethodInvocation method) {
                if (method.getSelect() != null) {
                    return JavaTemplate.builder("#{any(" + MigrateWindowToNewApi.WINDOW + ")}.getViewer().getUniqueId()")
                        .build()
                        .apply(this.getCursor(), method.getCoordinates().replace(), method.getSelect())
                        .withPrefix(method.getPrefix());
                }

                return JavaTemplate.builder("getViewer().getUniqueId()")
                    .build()
                    .apply(this.getCursor(), method.getCoordinates().replace())
                    .withPrefix(method.getPrefix());
            }

            private boolean isBaseComponentArray(final JavaType type) {
                if (!(type instanceof final JavaType.Array arrayType)) {
                    return false;
                }
                return TypeUtils.isOfClassType(arrayType.getElemType(), MigrateWindowToNewApi.BASE_COMPONENT);
            }

            private boolean isSetGuiOnWindowBuilderHierarchy(final J.MethodInvocation method) {
                if (MigrateWindowToNewApi.WINDOW_BUILDER_SET_GUI.matches(method)) {
                    return true;
                }

                if (!"setGui".equals(method.getSimpleName()) || method.getArguments().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return TypeUtils.isAssignableTo(MigrateWindowToNewApi.WINDOW_BUILDER, methodType.getDeclaringType());
                }

                return method.getSelect() != null &&
                    TypeUtils.isAssignableTo(MigrateWindowToNewApi.WINDOW_BUILDER, method.getSelect().getType());
            }

            private boolean usesWindowMethods(final J.CompilationUnit compilationUnit) {
                final AtomicBoolean usesWindowMethods = new AtomicBoolean(false);
                new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final AtomicBoolean found) {
                        final J.MethodDeclaration m = super.visitMethodDeclaration(method, found);
                        if (m.getMethodType() != null && TypeUtils.isOfClassType(m.getMethodType().getDeclaringType(), MigrateWindowToNewApi.WINDOW)) {
                            found.set(true);
                        }
                        return m;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final AtomicBoolean found) {
                        final J.MethodInvocation m = super.visitMethodInvocation(method, found);
                        if (MigrateWindowToNewApi.WINDOW_FACTORY_SPLIT_NO_ARGS.matches(m)
                            || MigrateWindowToNewApi.WINDOW_FACTORY_MERGED_NO_ARGS.matches(m)
                            || MigrateWindowToNewApi.WINDOW_FACTORY_SINGLE_NO_ARGS.matches(m)
                            || MigrateWindowToNewApi.WINDOW_FACTORY_SPLIT_WITH_CONSUMER.matches(m)
                            || MigrateWindowToNewApi.WINDOW_FACTORY_MERGED_WITH_CONSUMER.matches(m)
                            || MigrateWindowToNewApi.WINDOW_FACTORY_SINGLE_WITH_CONSUMER.matches(m)
                            || MigrateWindowToNewApi.WINDOW_CHANGE_TITLE.matches(m)
                            || MigrateWindowToNewApi.WINDOW_GET_CURRENT_VIEWER.matches(m)
                            || MigrateWindowToNewApi.WINDOW_GET_VIEWER_UUID.matches(m)
                            || isSetGuiOnWindowBuilderHierarchy(m)) {
                            found.set(true);
                        }
                        return m;
                    }
                }.visit(compilationUnit, usesWindowMethods);

                return usesWindowMethods.get();
            }
        };
    }
}
