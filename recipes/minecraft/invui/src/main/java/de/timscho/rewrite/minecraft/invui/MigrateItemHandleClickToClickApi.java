package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MigrateItemHandleClickToClickApi extends Recipe {
    private static final String ITEM = "xyz.xenondevs.invui.item.Item";
    private static final String CLICK_TYPE = "org.bukkit.event.inventory.ClickType";
    private static final String PLAYER = "org.bukkit.entity.Player";
    private static final String INVENTORY_CLICK_EVENT = "org.bukkit.event.inventory.InventoryClickEvent";
    private static final String CLICK = "xyz.xenondevs.invui.Click";

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate Item handleClick InventoryClickEvent to Click";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates `handleClick(ClickType, Player, InventoryClickEvent)` to use `xyz.xenondevs.invui.Click`.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit compilationUnit, final ExecutionContext ctx) {
                if (this.usesLegacyHandleClick(compilationUnit)) {
                    this.doAfterVisit(new ChangeType(INVENTORY_CLICK_EVENT, CLICK, true).getVisitor());
                }
                return super.visitCompilationUnit(compilationUnit, ctx);
            }

            private boolean usesLegacyHandleClick(final J.CompilationUnit compilationUnit) {
                final AtomicBoolean usesLegacyHandleClick = new AtomicBoolean(false);
                new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final AtomicBoolean found) {
                        final J.MethodDeclaration m = super.visitMethodDeclaration(method, found);
                        if (isLegacyHandleClickDeclaration(m)) {
                            found.set(true);
                        }
                        return m;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final AtomicBoolean found) {
                        final J.MethodInvocation m = super.visitMethodInvocation(method, found);
                        if (isLegacyHandleClickInvocation(m)) {
                            found.set(true);
                        }
                        return m;
                    }
                }.visit(compilationUnit, usesLegacyHandleClick);

                return usesLegacyHandleClick.get();
            }

            private boolean isLegacyHandleClickDeclaration(final J.MethodDeclaration method) {
                if (!"handleClick".equals(method.getSimpleName()) || method.getParameters().size() != 3) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    if (!TypeUtils.isAssignableTo(ITEM, methodType.getDeclaringType())) {
                        return false;
                    }

                    final List<JavaType> parameterTypes = methodType.getParameterTypes();
                    return parameterTypes.size() == 3
                        && TypeUtils.isOfClassType(parameterTypes.get(0), CLICK_TYPE)
                        && TypeUtils.isOfClassType(parameterTypes.get(1), PLAYER)
                        && TypeUtils.isOfClassType(parameterTypes.get(2), INVENTORY_CLICK_EVENT);
                }

                return hasLegacyHandleClickParameterTypes(method.getParameters());
            }

            private boolean isLegacyHandleClickInvocation(final J.MethodInvocation method) {
                if (!"handleClick".equals(method.getSimpleName()) || method.getArguments().size() != 3) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    if (!TypeUtils.isAssignableTo(ITEM, methodType.getDeclaringType())) {
                        return false;
                    }

                    final List<JavaType> parameterTypes = methodType.getParameterTypes();
                    return parameterTypes.size() == 3
                        && TypeUtils.isOfClassType(parameterTypes.get(0), CLICK_TYPE)
                        && TypeUtils.isOfClassType(parameterTypes.get(1), PLAYER)
                        && TypeUtils.isOfClassType(parameterTypes.get(2), INVENTORY_CLICK_EVENT);
                }

                return TypeUtils.isOfClassType(method.getArguments().get(0).getType(), CLICK_TYPE)
                    && TypeUtils.isOfClassType(method.getArguments().get(1).getType(), PLAYER)
                    && TypeUtils.isOfClassType(method.getArguments().get(2).getType(), INVENTORY_CLICK_EVENT);
            }

            private boolean hasLegacyHandleClickParameterTypes(final List<Statement> parameters) {
                return parameters.size() == 3
                    && parameterHasType(parameters.get(0), CLICK_TYPE)
                    && parameterHasType(parameters.get(1), PLAYER)
                    && parameterHasType(parameters.get(2), INVENTORY_CLICK_EVENT);
            }

            private boolean parameterHasType(final Statement parameter, final String fqType) {
                if (!(parameter instanceof J.VariableDeclarations variableDeclarations)) {
                    return false;
                }

                return TypeUtils.isOfClassType(variableDeclarations.getType(), fqType)
                    || (variableDeclarations.getTypeExpression() != null
                    && TypeUtils.isOfClassType(variableDeclarations.getTypeExpression().getType(), fqType));
            }
        };
    }
}
