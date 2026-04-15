package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;

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

                return parameterHasType(method.getParameters().get(0), CLICK_TYPE)
                    && parameterHasType(method.getParameters().get(1), PLAYER)
                    && parameterHasType(method.getParameters().get(2), INVENTORY_CLICK_EVENT);
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

            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!isLegacyHandleClickDeclaration(m) || m.getParameters().size() != 3) {
                    return m;
                }

                if (!(m.getParameters().get(2) instanceof J.VariableDeclarations thirdParam)) {
                    return m;
                }
                maybeAddImport(CLICK);
                maybeRemoveImport(INVENTORY_CLICK_EVENT);
                final Statement migratedThirdParam = (Statement) new ChangeType(INVENTORY_CLICK_EVENT, CLICK, true)
                    .getVisitor()
                    .visit(thirdParam, ctx, new Cursor(getCursor(), thirdParam));
                if (migratedThirdParam == null) {
                    return m;
                }

                final List<Statement> updatedParameters = m.getParameters().stream()
                    .collect(Collectors.toList());
                updatedParameters.set(2, migratedThirdParam);
                return m.withParameters(updatedParameters);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                m = this.migrateLegacyHandleClickEventAccessors(m);
                if (!isLegacyHandleClickInvocation(m) || m.getArguments().size() != 3) {
                    return m;
                }

                final Expression clickTypeArg = m.getArguments().get(0);
                final Expression playerArg = m.getArguments().get(1);
                final Expression eventArg = m.getArguments().get(2);

                maybeAddImport(CLICK);
                return JavaTemplate.builder(
                    "#{any(" + CLICK_TYPE + ")}, #{any(" + PLAYER + ")}, " +
                        "new " + CLICK + "(#{any(" + PLAYER + ")}, #{any(" + CLICK_TYPE + ")}, #{any(" + INVENTORY_CLICK_EVENT + ")}.getHotbarButton())"
                )
                    .imports(CLICK)
                    .build()
                    .apply(
                        getCursor(),
                        m.getCoordinates().replaceArguments(),
                        clickTypeArg,
                        playerArg,
                        playerArg,
                        clickTypeArg,
                        eventArg
                    );
            }

            private J.MethodInvocation migrateLegacyHandleClickEventAccessors(final J.MethodInvocation method) {
                if (method.getSelect() == null || !method.getArguments().isEmpty() || !(method.getSelect() instanceof final J.Identifier selectIdentifier)) {
                    return method;
                }

                final String replacementAccessor = switch (method.getSimpleName()) {
                    case "getWhoClicked" -> "player";
                    case "getClick" -> "clickType";
                    case "getHotbarButton" -> "hotbarButton";
                    default -> null;
                };
                if (replacementAccessor == null) {
                    return method;
                }

                final J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod == null || !this.isHandleClickLikeMethod(enclosingMethod)) {
                    return method;
                }

                final String clickParamName = this.thirdParameterName(enclosingMethod);
                if (clickParamName == null || !clickParamName.equals(selectIdentifier.getSimpleName())) {
                    return method;
                }

                return method.withName(method.getName().withSimpleName(replacementAccessor));
            }

            private boolean isHandleClickLikeMethod(final J.MethodDeclaration method) {
                return "handleClick".equals(method.getSimpleName()) &&
                    method.getParameters().size() == 3 &&
                    parameterHasType(method.getParameters().get(0), CLICK_TYPE) &&
                    parameterHasType(method.getParameters().get(1), PLAYER) &&
                    (parameterHasType(method.getParameters().get(2), INVENTORY_CLICK_EVENT) ||
                        parameterHasType(method.getParameters().get(2), CLICK));
            }

            private String thirdParameterName(final J.MethodDeclaration method) {
                if (method.getParameters().size() != 3 || !(method.getParameters().get(2) instanceof J.VariableDeclarations variableDeclarations)) {
                    return null;
                }
                if (variableDeclarations.getVariables().isEmpty()) {
                    return null;
                }
                return variableDeclarations.getVariables().getFirst().getSimpleName();
            }

            private boolean parameterHasType(final Statement parameter, final String fqType) {
                if (!(parameter instanceof J.VariableDeclarations variableDeclarations)) {
                    return false;
                }

                if (TypeUtils.isOfClassType(variableDeclarations.getType(), fqType)
                    || (variableDeclarations.getTypeExpression() != null
                    && TypeUtils.isOfClassType(variableDeclarations.getTypeExpression().getType(), fqType))) {
                    return true;
                }

                final String simpleName = fqType.substring(fqType.lastIndexOf('.') + 1);
                final TypeTree typeExpression = variableDeclarations.getTypeExpression();
                if (typeExpression instanceof J.Identifier identifier) {
                    return simpleName.equals(identifier.getSimpleName());
                }
                if (typeExpression instanceof J.FieldAccess fieldAccess) {
                    return simpleName.equals(fieldAccess.getSimpleName());
                }

                final String parameterSource = variableDeclarations.printTrimmed(getCursor());
                return parameterSource.contains(fqType) || parameterSource.contains(" " + simpleName + " ");
            }
        };
    }
}
