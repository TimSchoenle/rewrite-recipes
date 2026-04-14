package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MigrateControlItemToAbstractBoundItem extends Recipe {
    private static final String CONTROL_ITEM = "xyz.xenondevs.invui.item.impl.controlitem.ControlItem";
    private static final String ABSTRACT_BOUND_ITEM = "xyz.xenondevs.invui.item.AbstractBoundItem";
    private static final String GUI = "xyz.xenondevs.invui.gui.Gui";
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";

    private static final MethodMatcher CONTROL_ITEM_SET_GUI = new MethodMatcher(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM + " setGui(..)");

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

                return m;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(final J.MethodDeclaration method, final ExecutionContext ctx) {
                final J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                if (!"setGui".equals(m.getSimpleName()) || m.getParameters().size() != 1) {
                    return m;
                }

                final J.ClassDeclaration enclosingClass = this.getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosingClass != null && this.isApiType(enclosingClass.getType())) {
                    return m;
                }

                final JavaType.Method methodType = m.getMethodType();
                if (methodType == null ||
                    (!TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.CONTROL_ITEM, methodType.getDeclaringType()) &&
                        !TypeUtils.isAssignableTo(MigrateControlItemToAbstractBoundItem.ABSTRACT_BOUND_ITEM, methodType.getDeclaringType()))) {
                    return m;
                }

                return m.withName(m.getName().withSimpleName("bind"));
            }

            @Override
            public J.ParameterizedType visitParameterizedType(final J.ParameterizedType parameterizedType, final ExecutionContext ctx) {
                final J.ParameterizedType p = super.visitParameterizedType(parameterizedType, ctx);
                if (this.isControlItemHierarchyType(p.getType())) {
                    return p.withTypeParameters(null);
                }
                return p;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration classDecl, final ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);

                if (c.getExtends() instanceof final J.ParameterizedType parameterizedExtends &&
                    this.shouldEraseExtendsTypeParameters(c, parameterizedExtends)) {
                    c = c.withExtends(parameterizedExtends.withTypeParameters(null));
                }

                if (this.isApiType(c.getType()) || !this.isControlItemHierarchyClass(c)) {
                    return c;
                }

                final TypeTree extendsType = c.getExtends();
                final JavaType guiType = this.findGuiType(c, extendsType != null ? extendsType.getType() : null);
                if (guiType == null) {
                    return c;
                }

                final String classSource = c.printTrimmed(this.getCursor());
                final boolean hasBridgeGetItemProviderSource =
                    classSource.contains("getItemProvider()") && classSource.contains("return getItemProvider((");
                final boolean hasBridgeGetGuiSource = classSource.contains("super.getGui()");

                if (!hasBridgeGetItemProviderSource && !this.hasMethod(c, "getItemProvider", 0) && this.hasMethod(c, "getItemProvider", 1)) {
                    final String guiTypePattern = this.typePattern(guiType);
                    c = JavaTemplate.builder(
                            "@Override public " + MigrateControlItemToAbstractBoundItem.ITEM_PROVIDER + " getItemProvider() { return getItemProvider((" + guiTypePattern + ") getGui()); }")
                        .imports(MigrateControlItemToAbstractBoundItem.ITEM_PROVIDER)
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
                if (this.isControlItemHierarchyType(extendsType.getType()) ||
                    this.isControlItemHierarchyType(extendsType.getClazz().getType())) {
                    return true;
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
