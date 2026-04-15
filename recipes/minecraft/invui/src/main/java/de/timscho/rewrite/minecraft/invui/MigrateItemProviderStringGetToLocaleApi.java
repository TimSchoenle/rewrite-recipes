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
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

public class MigrateItemProviderStringGetToLocaleApi extends Recipe {
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";
    private static final String STRING = "java.lang.String";
    private static final String LOCALE = "java.util.Locale";
    private static final MethodMatcher ITEM_PROVIDER_GET_WITH_STRING =
        new MethodMatcher(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER + " get(java.lang.String)");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate ItemProvider#get(String) to Locale API";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates string-based `ItemProvider#get(String)` calls to `Locale` and adds compatibility bridges for legacy provider implementations.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (!this.isItemProviderGetWithStringInvocation(m) || this.isInsideProviderGetBridgeMethod(m)) {
                    return m;
                }
                if (m.getArguments().getFirst() instanceof J.Literal literal && literal.getValue() == null) {
                    return m;
                }

                return JavaTemplate.builder(
                        "java.util.Optional.ofNullable(#{any(java.lang.String)}).map(java.util.Locale::forLanguageTag).orElse(null)"
                    )
                    .build()
                    .apply(getCursor(), m.getCoordinates().replaceArguments(), m.getArguments().getFirst());
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!this.isLegacyStringGetMethod(m)) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass == null || !this.isItemProviderHierarchyClass(enclosingClass)) {
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
                if (!this.isItemProviderHierarchyClass(c)) {
                    return c;
                }
                if (!this.hasMethodWithParamType(c, "get", MigrateItemProviderStringGetToLocaleApi.STRING)) {
                    return c;
                }

                final String initialSource = c.printTrimmed(getCursor());
                final boolean hasLocaleGet = this.hasMethodWithParamType(c, "get", MigrateItemProviderStringGetToLocaleApi.LOCALE) ||
                    initialSource.contains(" get(java.util.Locale ") ||
                    initialSource.contains(" get(Locale ");
                if (!hasLocaleGet) {
                    c = JavaTemplate.builder(
                            "@Override public org.bukkit.inventory.ItemStack get(java.util.Locale locale) { return get(locale == null ? null : locale.toLanguageTag()); }"
                        )
                        .build()
                        .apply(updateCursor(c), c.getBody().getCoordinates().lastStatement());
                }

                final String sourceAfterLocaleBridge = c.printTrimmed(getCursor());
                final boolean hasNoArgGet = this.hasMethod(c, "get", 0) || sourceAfterLocaleBridge.contains(" get()");
                if (!hasNoArgGet) {
                    c = JavaTemplate.builder(
                            "@Override public org.bukkit.inventory.ItemStack get() { return get((java.util.Locale) null); }"
                        )
                        .build()
                        .apply(updateCursor(c), c.getBody().getCoordinates().lastStatement());
                }

                return c;
            }

            private boolean isItemProviderGetWithStringInvocation(final J.MethodInvocation method) {
                if (MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER_GET_WITH_STRING.matches(method)) {
                    return true;
                }

                if (!"get".equals(method.getSimpleName()) || method.getArguments().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return methodType.getParameterTypes().size() == 1 &&
                        TypeUtils.isOfClassType(methodType.getParameterTypes().getFirst(), MigrateItemProviderStringGetToLocaleApi.STRING) &&
                        TypeUtils.isAssignableTo(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER, methodType.getDeclaringType());
                }

                return method.getSelect() != null &&
                    TypeUtils.isAssignableTo(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER, method.getSelect().getType()) &&
                    TypeUtils.isOfClassType(method.getArguments().getFirst().getType(), MigrateItemProviderStringGetToLocaleApi.STRING);
            }

            private boolean isInsideProviderGetBridgeMethod(final J.MethodInvocation method) {
                if (method.getSelect() != null && (!(method.getSelect() instanceof J.Identifier identifier) || !"this".equals(identifier.getSimpleName()))) {
                    return false;
                }

                final J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod == null || !"get".equals(enclosingMethod.getSimpleName())) {
                    return false;
                }

                if (enclosingMethod.getParameters().isEmpty()) {
                    return true;
                }

                return enclosingMethod.getParameters().size() == 1 &&
                    this.parameterHasType(enclosingMethod.getParameters().getFirst(), MigrateItemProviderStringGetToLocaleApi.LOCALE);
            }

            private boolean isLegacyStringGetMethod(final J.MethodDeclaration method) {
                if (!"get".equals(method.getSimpleName()) || method.getParameters().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return methodType.getParameterTypes().size() == 1 &&
                        TypeUtils.isOfClassType(methodType.getParameterTypes().getFirst(), MigrateItemProviderStringGetToLocaleApi.STRING);
                }

                return this.parameterHasType(method.getParameters().getFirst(), MigrateItemProviderStringGetToLocaleApi.STRING);
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

            private boolean isItemProviderHierarchyClass(final J.ClassDeclaration classDecl) {
                if (TypeUtils.isAssignableTo(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER, classDecl.getType()) ||
                    TypeUtils.isOfClassType(classDecl.getType(), MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER)) {
                    return true;
                }

                final TypeTree extendsType = classDecl.getExtends();
                if (extendsType != null &&
                    (TypeUtils.isAssignableTo(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER, extendsType.getType()) ||
                        TypeUtils.isOfClassType(extendsType.getType(), MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER) ||
                        "ItemProvider".equals(this.simpleNameOfTypeTree(extendsType)))) {
                    return true;
                }

                final List<TypeTree> implemented = classDecl.getImplements();
                if (implemented == null) {
                    return false;
                }

                for (final NameTree implement : implemented) {
                    if (TypeUtils.isAssignableTo(MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER, implement.getType()) ||
                        TypeUtils.isOfClassType(implement.getType(), MigrateItemProviderStringGetToLocaleApi.ITEM_PROVIDER) ||
                        "ItemProvider".equals(this.simpleNameOfTypeTree(implement))) {
                        return true;
                    }
                }
                return false;
            }

            private String simpleNameOfTypeTree(final NameTree typeTree) {
                if (typeTree == null) {
                    return null;
                }
                if (typeTree instanceof final J.ParameterizedType parameterizedType) {
                    return this.simpleNameOfTypeTree(parameterizedType.getClazz());
                }
                if (typeTree instanceof final J.Identifier identifier) {
                    return identifier.getSimpleName();
                }
                if (typeTree instanceof final J.FieldAccess fieldAccess) {
                    return fieldAccess.getSimpleName();
                }
                return null;
            }
        };
    }
}
