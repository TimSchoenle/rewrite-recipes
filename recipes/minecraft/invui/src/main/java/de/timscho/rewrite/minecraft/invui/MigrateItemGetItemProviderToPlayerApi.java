package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

public class MigrateItemGetItemProviderToPlayerApi extends Recipe {
    private static final String ITEM = "xyz.xenondevs.invui.item.Item";
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";
    private static final String PLAYER = "org.bukkit.entity.Player";
    private static final MethodMatcher ITEM_GET_ITEM_PROVIDER_NO_ARGS =
        new MethodMatcher(MigrateItemGetItemProviderToPlayerApi.ITEM + " getItemProvider()");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate Item#getItemProvider() to player-aware API";
    }

    @Override
    public @NonNull String getDescription() {
        return "Adds compatibility bridges for legacy no-arg `Item#getItemProvider()` implementations and migrates `Item`-typed invocations to the player-based method.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (!this.isItemGetItemProviderNoArgInvocation(m) || this.isInsidePlayerGetItemProviderMethod(m)) {
                    return m;
                }

                return JavaTemplate.builder("((org.bukkit.entity.Player) null)")
                    .build()
                    .apply(getCursor(), m.getCoordinates().replaceArguments());
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!this.isLegacyNoArgGetItemProviderMethod(m)) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass == null) {
                    return m;
                }

                if (m.getLeadingAnnotations().stream().noneMatch(annotation -> "Override".equals(annotation.getSimpleName()))) {
                    return m;
                }

                final List<J.Annotation> filteredAnnotations = m.getLeadingAnnotations().stream()
                    .filter(annotation -> !"Override".equals(annotation.getSimpleName()))
                    .toList();
                return m.withLeadingAnnotations(filteredAnnotations);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getKind() == J.ClassDeclaration.Kind.Type.Interface || c.getKind() == J.ClassDeclaration.Kind.Type.Annotation) {
                    return c;
                }
                if (!this.hasMethod(c, "getItemProvider", 0) || this.hasMethodWithParamType(c, "getItemProvider", MigrateItemGetItemProviderToPlayerApi.PLAYER)) {
                    return c;
                }

                maybeAddImport(MigrateItemGetItemProviderToPlayerApi.PLAYER);
                maybeAddImport(MigrateItemGetItemProviderToPlayerApi.ITEM_PROVIDER);
                return JavaTemplate.builder(
                        "@Override public " + MigrateItemGetItemProviderToPlayerApi.ITEM_PROVIDER +
                            " getItemProvider(" + MigrateItemGetItemProviderToPlayerApi.PLAYER + " viewer) { return getItemProvider(); }"
                    )
                    .imports(MigrateItemGetItemProviderToPlayerApi.ITEM_PROVIDER, MigrateItemGetItemProviderToPlayerApi.PLAYER)
                    .build()
                    .apply(updateCursor(c), c.getBody().getCoordinates().lastStatement());
            }

            private boolean isItemGetItemProviderNoArgInvocation(final J.MethodInvocation method) {
                if (MigrateItemGetItemProviderToPlayerApi.ITEM_GET_ITEM_PROVIDER_NO_ARGS.matches(method)) {
                    return true;
                }

                if (!"getItemProvider".equals(method.getSimpleName()) || !method.getArguments().isEmpty()) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return TypeUtils.isAssignableTo(MigrateItemGetItemProviderToPlayerApi.ITEM, methodType.getDeclaringType())
                        && methodType.getParameterTypes().isEmpty();
                }

                return method.getSelect() != null && TypeUtils.isAssignableTo(MigrateItemGetItemProviderToPlayerApi.ITEM, method.getSelect().getType());
            }

            private boolean isLegacyNoArgGetItemProviderMethod(final J.MethodDeclaration method) {
                if (!"getItemProvider".equals(method.getSimpleName()) || !method.getParameters().isEmpty()) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return methodType.getParameterTypes().isEmpty();
                }

                return true;
            }

            private boolean isInsidePlayerGetItemProviderMethod(final J.MethodInvocation method) {
                if (method.getSelect() != null && (!(method.getSelect() instanceof J.Identifier identifier) || !"this".equals(identifier.getSimpleName()))) {
                    return false;
                }

                final J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                return enclosingMethod != null &&
                    "getItemProvider".equals(enclosingMethod.getSimpleName()) &&
                    enclosingMethod.getParameters().size() == 1 &&
                    this.parameterHasType(enclosingMethod.getParameters().getFirst(), MigrateItemGetItemProviderToPlayerApi.PLAYER);
            }

            private boolean hasMethod(final J.ClassDeclaration classDecl, final String methodName, final int paramCount) {
                for (final Statement statement : classDecl.getBody().getStatements()) {
                    if (statement instanceof final J.MethodDeclaration methodDeclaration &&
                        methodName.equals(methodDeclaration.getSimpleName()) &&
                        methodDeclaration.getParameters().size() == paramCount) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasMethodWithParamType(final J.ClassDeclaration classDecl, final String methodName, final String fqType) {
                for (final Statement statement : classDecl.getBody().getStatements()) {
                    if (!(statement instanceof final J.MethodDeclaration methodDeclaration) ||
                        !methodName.equals(methodDeclaration.getSimpleName()) ||
                        methodDeclaration.getParameters().size() != 1) {
                        continue;
                    }

                    if (this.parameterHasType(methodDeclaration.getParameters().getFirst(), fqType)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean parameterHasType(final Statement parameter, final String fqType) {
                if (!(parameter instanceof J.VariableDeclarations variableDeclarations)) {
                    return false;
                }

                if (TypeUtils.isOfClassType(variableDeclarations.getType(), fqType)) {
                    return true;
                }

                final TypeTree typeExpression = variableDeclarations.getTypeExpression();
                if (typeExpression != null && TypeUtils.isOfClassType(typeExpression.getType(), fqType)) {
                    return true;
                }

                final String simpleName = fqType.substring(fqType.lastIndexOf('.') + 1);
                if (typeExpression instanceof J.Identifier identifier) {
                    return simpleName.equals(identifier.getSimpleName());
                }
                if (typeExpression instanceof J.FieldAccess fieldAccess) {
                    return simpleName.equals(fieldAccess.getSimpleName());
                }

                return false;
            }

        };
    }
}
