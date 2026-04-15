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
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MigrateControlItemToAbstractBoundItem extends Recipe {
    private static final String CONTROL_ITEM = "xyz.xenondevs.invui.item.impl.controlitem.ControlItem";
    private static final String ABSTRACT_BOUND_ITEM = "xyz.xenondevs.invui.item.AbstractBoundItem";
    private static final String GUI = "xyz.xenondevs.invui.gui.Gui";
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";
    private static final String PLAYER = "org.bukkit.entity.Player";

    private static final MethodMatcher CONTROL_ITEM_SET_GUI = new MethodMatcher(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM + " setGui(..)");
    private static final MethodMatcher CONTROL_ITEM_GET_ITEM_PROVIDER = new MethodMatcher(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM + " getItemProvider(..)");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate ControlItem to AbstractBoundItem";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates ControlItem usages and adapts subclasses to the AbstractBoundItem contract.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(final J.CompilationUnit compilationUnit, final ExecutionContext ctx) {
                this.doAfterVisit(new ChangeType(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, true).getVisitor());
                return super.visitCompilationUnit(compilationUnit, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(final J.MethodInvocation method, final ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (this.isSetGuiOnControlItemHierarchy(m)) {
                    m = m.withName(m.getName().withSimpleName("bind"));
                }
                if (this.isGetItemProviderOnControlItemHierarchy(m)) {
                    m = JavaTemplate.builder("((org.bukkit.entity.Player) null)")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replaceArguments());
                }

                return m;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (!"setGui".equals(m.getSimpleName()) || m.getParameters().size() != 1) {
                    return this.removeInvalidGetItemProviderOverride(m);
                }

                final J.ClassDeclaration enclosingClass = this.getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && this.isApiType(enclosingClass.getType())) {
                    return m;
                }

                final JavaType.Method methodType = m.getMethodType();
                if (methodType == null ||
                    (!TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, methodType.getDeclaringType()) &&
                        !TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, methodType.getDeclaringType()))) {
                    return this.removeInvalidGetItemProviderOverride(m);
                }

                return this.removeInvalidGetItemProviderOverride(m.withName(m.getName().withSimpleName("bind")));
            }

            @Override
            public J.ParameterizedType visitParameterizedType(final J.ParameterizedType parameterizedType, final ExecutionContext ctx) {
                final J.ParameterizedType p = super.visitParameterizedType(parameterizedType, ctx);
                final J.ParameterizedType withoutGuiTypeArguments = this.removeGuiTypeArguments(p);
                if (withoutGuiTypeArguments != p) {
                    return withoutGuiTypeArguments;
                }
                if (this.shouldEraseTypeParameters(p)) {
                    return p.withTypeParameters(null);
                }
                return p;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getExtends() instanceof final J.ParameterizedType parameterizedExtends) {
                    J.ParameterizedType rewrittenExtends = this.removeGuiTypeArguments(parameterizedExtends);
                    rewrittenExtends = this.removeGuiTypeArgumentsFromExtendsBySource(c, rewrittenExtends);
                    if (rewrittenExtends != parameterizedExtends) {
                        c = c.withExtends(rewrittenExtends);
                    }
                }

                if (c.getExtends() instanceof final J.ParameterizedType parameterizedExtends &&
                    this.shouldEraseExtendsTypeParameters(c, parameterizedExtends)) {
                    c = c.withExtends(parameterizedExtends.withTypeParameters(null));
                }

                if (this.isApiType(c.getType()) || !this.isControlItemHierarchyClass(c)) {
                    return c;
                }
                c = this.removeGuiTypeParametersFromDeclaration(c);

                final TypeTree extendsType = c.getExtends();
                final JavaType guiType = this.findGuiType(c, extendsType != null ? extendsType.getType() : null);
                if (guiType == null) {
                    return c;
                }

                final String classSource = c.printTrimmed(this.getCursor());
                final boolean hasBridgeGetItemProviderSource = this.hasMethodWithParamType(c, "getItemProvider", MigrateControlItemToAbstractBoundItem.PLAYER);
                final boolean hasBridgeGetGuiSource = classSource.contains("super.getGui()");

                if (!hasBridgeGetItemProviderSource && this.hasSingleArgGetItemProviderWithNonPlayerParam(c)) {
                    final String guiTypePattern = this.typePattern(guiType);
                    c = JavaTemplate.builder(
                            "@Override public " + MigrateControlItemToAbstractBoundItem.ITEM_PROVIDER + " getItemProvider(" + MigrateControlItemToAbstractBoundItem.PLAYER + " viewer) { return getItemProvider((" + guiTypePattern + ") getGui()); }")
                        .imports(MigrateControlItemToAbstractBoundItem.ITEM_PROVIDER, MigrateControlItemToAbstractBoundItem.PLAYER)
                        .build()
                        .apply(this.updateCursor(c), c.getBody().getCoordinates().lastStatement());
                }

                if (!hasBridgeGetGuiSource && !this.hasMethod(c, "getGui", 0) && !TypeUtils.isOfClassType(guiType, MigrateControlItemToAbstractBoundItem.GUI)) {
                    final String guiTypePattern = this.typePattern(guiType);
                    c = JavaTemplate.builder(
                            "@Override public " + guiTypePattern + " getGui() { return (" + guiTypePattern + ") super.getGui(); }")
                        .build()
                        .apply(this.updateCursor(c), c.getBody().getCoordinates().lastStatement());
                }

                this.maybeAddImport(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM);
                this.maybeRemoveImport(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM);
                return c;
            }

            private J.MethodDeclaration removeInvalidGetItemProviderOverride(final J.MethodDeclaration method) {
                if (!"getItemProvider".equals(method.getSimpleName()) || method.getParameters().size() != 1) {
                    return method;
                }

                final J.ClassDeclaration enclosingClass = this.getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass == null || !this.isControlItemHierarchyClass(enclosingClass)) {
                    return method;
                }

                final Statement parameter = method.getParameters().getFirst();
                if (this.parameterHasType(parameter, MigrateControlItemToAbstractBoundItem.PLAYER)) {
                    return method;
                }

                if (method.getLeadingAnnotations().stream().noneMatch(annotation -> "Override".equals(annotation.getSimpleName()))) {
                    return method;
                }

                final List<J.Annotation> filteredAnnotations = method.getLeadingAnnotations().stream()
                    .filter(annotation -> !"Override".equals(annotation.getSimpleName()))
                    .toList();
                if (filteredAnnotations.size() == method.getLeadingAnnotations().size()) {
                    return method;
                }

                return method.withLeadingAnnotations(filteredAnnotations)
                    .withPrefix(method.getPrefix().withWhitespace("\n\n    "));
            }

            private boolean hasMethod(final J.ClassDeclaration classDecl, final String methodName, final int paramCount) {
                final List<Statement> statements = classDecl.getBody().getStatements();
                for (final Statement statement : statements) {
                    if (statement instanceof final J.MethodDeclaration methodDeclaration &&
                        methodDeclaration.getSimpleName().equals(methodName) &&
                        methodDeclaration.getParameters().size() == paramCount) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasMethodWithParamType(final J.ClassDeclaration classDecl, final String methodName, final String fqType) {
                final List<Statement> statements = classDecl.getBody().getStatements();
                for (final Statement statement : statements) {
                    if (!(statement instanceof final J.MethodDeclaration methodDeclaration)
                        || !methodDeclaration.getSimpleName().equals(methodName)
                        || methodDeclaration.getParameters().size() != 1) {
                        continue;
                    }

                    if (this.parameterHasType(methodDeclaration.getParameters().getFirst(), fqType)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasSingleArgGetItemProviderWithNonPlayerParam(final J.ClassDeclaration classDecl) {
                final List<Statement> statements = classDecl.getBody().getStatements();
                for (final Statement statement : statements) {
                    if (!(statement instanceof final J.MethodDeclaration methodDeclaration)
                        || !"getItemProvider".equals(methodDeclaration.getSimpleName())
                        || methodDeclaration.getParameters().size() != 1) {
                        continue;
                    }

                    if (!this.parameterHasType(methodDeclaration.getParameters().getFirst(), MigrateControlItemToAbstractBoundItem.PLAYER)) {
                        return true;
                    }
                }
                return false;
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

            private JavaType findGuiType(final J.ClassDeclaration classDecl, final JavaType extendsType) {
                final JavaType methodParamType = this.findSingleArgGetItemProviderParamType(classDecl);
                if (methodParamType != null) {
                    return methodParamType;
                }

                if (TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, classDecl.getType()) ||
                    TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, classDecl.getType())) {
                    return JavaType.ShallowClass.build(MigrateControlItemToAbstractBoundItem.GUI);
                }

                return this.extractFormerGuiType(extendsType);
            }

            private JavaType findSingleArgGetItemProviderParamType(final J.ClassDeclaration classDecl) {
                final List<Statement> statements = classDecl.getBody().getStatements();
                for (final Statement statement : statements) {
                    if (!(statement instanceof final J.MethodDeclaration methodDeclaration) ||
                        !methodDeclaration.getSimpleName().equals("getItemProvider") ||
                        methodDeclaration.getParameters().size() != 1) {
                        continue;
                    }

                    final Statement parameter = methodDeclaration.getParameters().getFirst();
                    if (parameter instanceof final J.VariableDeclarations variableDeclarations) {
                        return variableDeclarations.getType();
                    }
                }
                return null;
            }

            private JavaType extractFormerGuiType(final JavaType type) {
                if (type == null) {
                    return null;
                }

                if (type instanceof final JavaType.Parameterized parameterized) {
                    if ((!TypeUtils.isOfClassType(parameterized.getType(), MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM) &&
                        !TypeUtils.isOfClassType(parameterized.getType(), MigrateControlItemToAbstractBoundItem.CONTROL_ITEM)) ||
                        parameterized.getTypeParameters().isEmpty()) {
                        return JavaType.ShallowClass.build(MigrateControlItemToAbstractBoundItem.GUI);
                    }
                    return parameterized.getTypeParameters().getFirst();
                }
                if (TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM)) {
                    return JavaType.ShallowClass.build(MigrateControlItemToAbstractBoundItem.GUI);
                }
                return null;
            }

            private String typePattern(final JavaType type) {
                return TypeUtils.asFullyQualified(type) != null
                    ? TypeUtils.asFullyQualified(type).getFullyQualifiedName()
                    : MigrateControlItemToAbstractBoundItem.GUI;
            }

            private boolean isSetGuiOnControlItemHierarchy(final J.MethodInvocation method) {
                if (MigrateControlItemToAbstractBoundItem.CONTROL_ITEM_SET_GUI.matches(method)) {
                    return true;
                }

                if (!"setGui".equals(method.getSimpleName()) || method.getArguments().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, methodType.getDeclaringType()) ||
                        TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, methodType.getDeclaringType());
                }

                return method.getSelect() != null &&
                    (TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, method.getSelect().getType()) ||
                        TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, method.getSelect().getType()));
            }

            private boolean isGetItemProviderOnControlItemHierarchy(final J.MethodInvocation method) {
                if (MigrateControlItemToAbstractBoundItem.CONTROL_ITEM_GET_ITEM_PROVIDER.matches(method)) {
                    return method.getArguments().size() == 1;
                }

                if (!"getItemProvider".equals(method.getSimpleName()) || method.getArguments().size() != 1) {
                    return false;
                }

                final JavaType.Method methodType = method.getMethodType();
                if (methodType != null) {
                    return TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, methodType.getDeclaringType()) ||
                        TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, methodType.getDeclaringType());
                }

                return method.getSelect() != null &&
                    (TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, method.getSelect().getType()) ||
                        TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, method.getSelect().getType()));
            }

            private boolean isControlItemHierarchyClass(final J.ClassDeclaration classDecl) {
                if (TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, classDecl.getType()) ||
                    TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, classDecl.getType())) {
                    return true;
                }

                final TypeTree extendsType = classDecl.getExtends();
                return extendsType != null &&
                    (TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, extendsType.getType()) ||
                        TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, extendsType.getType()) ||
                        TypeUtils.isOfClassType(extendsType.getType(), MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM) ||
                        TypeUtils.isOfClassType(extendsType.getType(), MigrateControlItemToAbstractBoundItem.CONTROL_ITEM));
            }

            private boolean isControlItemHierarchyType(final JavaType type) {
                if (type == null) {
                    return false;
                }

                return TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, type) ||
                    TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, type) ||
                    TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM) ||
                    TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.CONTROL_ITEM);
            }

            private boolean shouldEraseExtendsTypeParameters(final J.ClassDeclaration classDecl, final J.ParameterizedType extendsType) {
                if (this.isApiType(extendsType.getType()) || this.isApiType(extendsType.getClazz().getType())) {
                    return true;
                }
                if ((this.isControlItemHierarchyType(extendsType.getType()) ||
                    this.isControlItemHierarchyType(extendsType.getClazz().getType())) &&
                    !this.hasDeclaredTypeParameters(extendsType.getType()) &&
                    !this.hasDeclaredTypeParameters(extendsType.getClazz().getType())) {
                    return true;
                }
                if (this.hasDeclaredTypeParameters(extendsType.getType()) ||
                    this.hasDeclaredTypeParameters(extendsType.getClazz().getType())) {
                    return false;
                }

                final String extendsSimpleName = this.simpleNameOfTypeTree(extendsType.getClazz());
                if (extendsSimpleName == null) {
                    return false;
                }

                final J.CompilationUnit compilationUnit = this.getCursor().firstEnclosing(J.CompilationUnit.class);
                if (compilationUnit == null) {
                    return false;
                }

                return this.isControlItemHierarchyBySource(compilationUnit, classDecl, extendsSimpleName, new HashSet<>());
            }

            private boolean isControlItemHierarchyBySource(final J.CompilationUnit compilationUnit,
                                                           final J.ClassDeclaration currentClass,
                                                           final String extendsSimpleName,
                                                           final Set<String> visited) {
                if (!visited.add(extendsSimpleName)) {
                    return false;
                }

                if ("ControlItem".equals(extendsSimpleName) || "AbstractBoundItem".equals(extendsSimpleName)) {
                    return true;
                }

                for (final J.ClassDeclaration candidate : compilationUnit.getClasses()) {
                    if (candidate == currentClass) {
                        continue;
                    }
                    if (!extendsSimpleName.equals(candidate.getSimpleName())) {
                        continue;
                    }
                    if (this.hasDeclaredTypeParameters(candidate.getType())) {
                        return false;
                    }
                    if (this.isControlItemHierarchyClass(candidate)) {
                        return true;
                    }
                    if (candidate.getExtends() == null) {
                        continue;
                    }

                    final String parentSimpleName = this.simpleNameOfTypeTree(candidate.getExtends());
                    if (parentSimpleName != null &&
                        this.isControlItemHierarchyBySource(compilationUnit, candidate, parentSimpleName, visited)) {
                        return true;
                    }
                }
                return false;
            }

            private J.ClassDeclaration removeGuiTypeParametersFromDeclaration(final J.ClassDeclaration classDecl) {
                final List<J.TypeParameter> typeParameters = classDecl.getTypeParameters();
                if (typeParameters == null || typeParameters.isEmpty()) {
                    return classDecl;
                }

                final List<J.TypeParameter> filteredTypeParameters = new ArrayList<>(typeParameters.size());
                boolean changed = false;
                for (final J.TypeParameter typeParameter : typeParameters) {
                    if (this.isGuiTypeParameterDeclaration(typeParameter)) {
                        changed = true;
                        continue;
                    }
                    filteredTypeParameters.add(typeParameter);
                }

                if (!changed) {
                    return classDecl;
                }

                if (!filteredTypeParameters.isEmpty()) {
                    final J.TypeParameter firstRemainingTypeParameter = filteredTypeParameters.getFirst();
                    filteredTypeParameters.set(0, firstRemainingTypeParameter.withPrefix(firstRemainingTypeParameter.getPrefix().withWhitespace("")));
                }

                return classDecl.withTypeParameters(filteredTypeParameters.isEmpty() ? null : filteredTypeParameters);
            }

            private J.ParameterizedType removeGuiTypeArgumentsFromExtendsBySource(final J.ClassDeclaration classDecl,
                                                                                   final J.ParameterizedType extendsType) {
                final List<Expression> typeArguments = extendsType.getTypeParameters();
                if (typeArguments == null || typeArguments.isEmpty()) {
                    return extendsType;
                }

                final String extendsSimpleName = this.simpleNameOfTypeTree(extendsType.getClazz());
                if (extendsSimpleName == null) {
                    return extendsType;
                }

                final J.CompilationUnit compilationUnit = this.getCursor().firstEnclosing(J.CompilationUnit.class);
                if (compilationUnit == null) {
                    return extendsType;
                }

                J.ClassDeclaration extendedClass = null;
                for (final J.ClassDeclaration candidate : compilationUnit.getClasses()) {
                    if (candidate == classDecl) {
                        continue;
                    }
                    if (extendsSimpleName.equals(candidate.getSimpleName())) {
                        extendedClass = candidate;
                        break;
                    }
                }
                if (extendedClass == null) {
                    return extendsType;
                }

                final List<Integer> guiTypeParameterIndexes = this.guiTypeParameterIndexesFromDeclaration(extendedClass);
                if (guiTypeParameterIndexes.isEmpty()) {
                    return extendsType;
                }
                final List<J.TypeParameter> declaredTypeParameters = extendedClass.getTypeParameters();
                if (declaredTypeParameters != null && typeArguments.size() < declaredTypeParameters.size()) {
                    return extendsType;
                }

                final Set<Integer> indexesToRemove = new HashSet<>(guiTypeParameterIndexes);
                final List<Expression> filteredTypeArguments = new ArrayList<>(typeArguments.size());
                for (int i = 0; i < typeArguments.size(); i++) {
                    if (!indexesToRemove.contains(i)) {
                        filteredTypeArguments.add(typeArguments.get(i));
                    }
                }

                if (filteredTypeArguments.size() == typeArguments.size()) {
                    return extendsType;
                }

                return filteredTypeArguments.isEmpty()
                    ? extendsType.withTypeParameters(null)
                    : extendsType.withTypeParameters(this.normalizeLeadingTypeArgumentWhitespace(filteredTypeArguments));
            }

            private J.ParameterizedType removeGuiTypeArguments(final J.ParameterizedType parameterizedType) {
                final List<Expression> typeArguments = parameterizedType.getTypeParameters();
                if (typeArguments == null || typeArguments.isEmpty()) {
                    return parameterizedType;
                }

                if (this.isApiType(parameterizedType.getType()) || this.isApiType(parameterizedType.getClazz().getType())) {
                    return parameterizedType.withTypeParameters(null);
                }

                if (!this.isControlItemHierarchyType(parameterizedType.getType()) &&
                    !this.isControlItemHierarchyType(parameterizedType.getClazz().getType())) {
                    return parameterizedType;
                }

                final Set<Integer> guiArgumentIndexes = this.guiTypeArgumentIndexes(parameterizedType);
                if (guiArgumentIndexes.isEmpty()) {
                    return parameterizedType;
                }

                final List<Expression> filteredTypeArguments = new ArrayList<>(typeArguments.size());
                for (int i = 0; i < typeArguments.size(); i++) {
                    if (!guiArgumentIndexes.contains(i)) {
                        filteredTypeArguments.add(typeArguments.get(i));
                    }
                }

                if (filteredTypeArguments.size() == typeArguments.size()) {
                    return parameterizedType;
                }

                return filteredTypeArguments.isEmpty()
                    ? parameterizedType.withTypeParameters(null)
                    : parameterizedType.withTypeParameters(this.normalizeLeadingTypeArgumentWhitespace(filteredTypeArguments));
            }

            private Set<Integer> guiTypeArgumentIndexes(final J.ParameterizedType parameterizedType) {
                final List<Expression> typeArguments = parameterizedType.getTypeParameters();
                if (typeArguments == null || typeArguments.isEmpty()) {
                    return Set.of();
                }

                final List<JavaType> declaredTypeParameters = !this.declaredTypeParameters(parameterizedType.getType()).isEmpty()
                    ? this.declaredTypeParameters(parameterizedType.getType())
                    : this.declaredTypeParameters(parameterizedType.getClazz().getType());
                final Set<Integer> indexes = new HashSet<>();
                if (!declaredTypeParameters.isEmpty() && typeArguments.size() < declaredTypeParameters.size()) {
                    return indexes;
                }

                for (int i = 0; i < typeArguments.size(); i++) {
                    final JavaType declaredTypeParameter = i < declaredTypeParameters.size() ? declaredTypeParameters.get(i) : null;
                    if (this.isGuiRelatedTypeArgument(typeArguments.get(i), declaredTypeParameter)) {
                        indexes.add(i);
                    }
                }
                return indexes;
            }

            private List<Expression> normalizeLeadingTypeArgumentWhitespace(final List<Expression> typeArguments) {
                if (typeArguments.isEmpty()) {
                    return typeArguments;
                }

                final List<Expression> normalizedTypeArguments = new ArrayList<>(typeArguments);
                final Expression firstTypeArgument = normalizedTypeArguments.getFirst();
                normalizedTypeArguments.set(0, (Expression) firstTypeArgument.withPrefix(firstTypeArgument.getPrefix().withWhitespace("")));
                return normalizedTypeArguments;
            }

            private List<Integer> guiTypeParameterIndexesFromDeclaration(final J.ClassDeclaration classDecl) {
                final List<J.TypeParameter> typeParameters = classDecl.getTypeParameters();
                if (typeParameters == null || typeParameters.isEmpty()) {
                    return List.of();
                }

                final List<Integer> indexes = new ArrayList<>();
                for (int i = 0; i < typeParameters.size(); i++) {
                    if (this.isGuiTypeParameterDeclaration(typeParameters.get(i))) {
                        indexes.add(i);
                    }
                }
                return indexes;
            }

            private boolean isGuiTypeParameterDeclaration(final J.TypeParameter typeParameter) {
                final List<TypeTree> bounds = typeParameter.getBounds();
                if (bounds != null) {
                    for (final TypeTree bound : bounds) {
                        if (this.isGuiRelatedBound(bound)) {
                            return true;
                        }
                    }
                }
                return this.isGuiRelatedType(typeParameter.getName().getType());
            }

            private boolean isGuiRelatedBound(final TypeTree bound) {
                if (bound == null) {
                    return false;
                }
                if (bound instanceof final J.TypeBound typeBound) {
                    return this.isGuiRelatedBound(typeBound.getBoundedType());
                }
                return this.isGuiRelatedType(bound.getType()) ||
                    "Gui".equals(this.simpleNameOfTypeTree(bound));
            }

            private boolean isGuiRelatedTypeArgument(final Expression typeArgument, final JavaType declaredTypeParameter) {
                if (this.isGuiRelatedType(declaredTypeParameter)) {
                    return true;
                }
                if (this.isGuiRelatedType(typeArgument.getType())) {
                    return true;
                }
                if (typeArgument instanceof J.Identifier identifier) {
                    return "Gui".equals(identifier.getSimpleName());
                }
                if (typeArgument instanceof J.FieldAccess fieldAccess) {
                    return "Gui".equals(fieldAccess.getSimpleName());
                }
                return false;
            }

            private boolean isGuiRelatedType(final JavaType type) {
                if (type == null) {
                    return false;
                }
                if (TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.GUI) ||
                    TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.GUI, type)) {
                    return true;
                }
                if (type instanceof final JavaType.Parameterized parameterized) {
                    return this.isGuiRelatedType(parameterized.getType());
                }
                if (type instanceof final JavaType.GenericTypeVariable genericTypeVariable) {
                    for (final JavaType bound : genericTypeVariable.getBounds()) {
                        if (this.isGuiRelatedType(bound)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private List<JavaType> declaredTypeParameters(final JavaType type) {
                if (type instanceof final JavaType.Parameterized parameterized && parameterized.getType() != null) {
                    return parameterized.getType().getTypeParameters();
                }
                final JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
                return fullyQualified == null ? List.of() : fullyQualified.getTypeParameters();
            }

            private boolean shouldEraseTypeParameters(final J.ParameterizedType parameterizedType) {
                if (parameterizedType.getTypeParameters() == null || parameterizedType.getTypeParameters().isEmpty()) {
                    return false;
                }
                if (this.isApiType(parameterizedType.getType()) || this.isApiType(parameterizedType.getClazz().getType())) {
                    return true;
                }
                if (!this.isControlItemHierarchyType(parameterizedType.getType()) &&
                    !this.isControlItemHierarchyType(parameterizedType.getClazz().getType())) {
                    return false;
                }
                return !this.hasDeclaredTypeParameters(parameterizedType.getType()) &&
                    !this.hasDeclaredTypeParameters(parameterizedType.getClazz().getType());
            }

            private boolean hasDeclaredTypeParameters(final JavaType type) {
                if (type == null) {
                    return false;
                }
                if (type instanceof final JavaType.Parameterized parameterized && parameterized.getType() != null) {
                    return !parameterized.getType().getTypeParameters().isEmpty();
                }
                final JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
                return fullyQualified != null && !fullyQualified.getTypeParameters().isEmpty();
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

            private boolean isApiType(final JavaType type) {
                return TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.CONTROL_ITEM) || TypeUtils.isOfClassType(type, MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM);
            }
        };
    }
}
