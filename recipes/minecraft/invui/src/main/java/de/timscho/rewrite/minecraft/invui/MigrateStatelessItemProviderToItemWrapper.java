package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

public class MigrateStatelessItemProviderToItemWrapper extends Recipe {
    private static final String ITEM_PROVIDER = "xyz.xenondevs.invui.item.ItemProvider";
    private static final String ITEM_WRAPPER = "xyz.xenondevs.invui.item.ItemWrapper";

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate stateless ItemProvider anonymous classes to ItemWrapper";
    }

    @Override
    public @NonNull String getDescription() {
        return "Replaces anonymous ItemProvider implementations with ItemWrapper when get(String lang) ignores lang and returns a single ItemStack expression.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<>() {
            @Override
            public @NonNull J visitNewClass(final J.@NonNull NewClass newClass, final @NonNull ExecutionContext ctx) {
                final J nc = super.visitNewClass(newClass, ctx);
                if (!(nc instanceof final J.NewClass n) || n.getBody() == null) {
                    return nc;
                }

                final String newClassSource = n.printTrimmed(this.getCursor());
                final boolean isItemProviderAnonymousClass =
                    TypeUtils.isAssignableTo(MigrateStatelessItemProviderToItemWrapper.ITEM_PROVIDER, n.getType()) ||
                        newClassSource.startsWith("new ItemProvider(") ||
                        newClassSource.startsWith("new xyz.xenondevs.invui.item.ItemProvider(");
                if (!isItemProviderAnonymousClass) {
                    return nc;
                }

                final List<Statement> classStatements = n.getBody().getStatements();
                final J.MethodDeclaration method = this.findSingleGetMethod(classStatements);
                if (method == null) {
                    return nc;
                }

                if (method.getParameters().size() != 1 || method.getBody() == null) {
                    return nc;
                }

                if (!(method.getParameters().getFirst() instanceof final J.VariableDeclarations parameterDecl) ||
                    parameterDecl.getVariables().size() != 1) {
                    return nc;
                }

                if (method.getBody().getStatements().size() != 1 || !(method.getBody().getStatements().getFirst() instanceof final J.Return returnStatement)) {
                    return nc;
                }

                final Expression returnedExpression = returnStatement.getExpression();
                if (returnedExpression == null) {
                    return nc;
                }

                final String parameterName = parameterDecl.getVariables().getFirst().getSimpleName();
                if (this.usesParameter(returnedExpression, parameterName)) {
                    return nc;
                }

                this.maybeAddImport(MigrateStatelessItemProviderToItemWrapper.ITEM_WRAPPER);
                this.doAfterVisit(new AddImport<>(MigrateStatelessItemProviderToItemWrapper.ITEM_WRAPPER, null, false));
                this.maybeRemoveImport(MigrateStatelessItemProviderToItemWrapper.ITEM_PROVIDER);
                return JavaTemplate.builder("new ItemWrapper(#{any()})")
                    .imports(MigrateStatelessItemProviderToItemWrapper.ITEM_WRAPPER)
                    .build()
                    .apply(this.getCursor(), n.getCoordinates().replace(), returnedExpression)
                    .withPrefix(n.getPrefix());
            }

            private boolean usesParameter(final Expression expression, final String parameterName) {
                final boolean[] parameterUsed = {false};
                new JavaVisitor<Integer>() {
                    @Override
                    public @NonNull J visitIdentifier(final J.@NonNull Identifier identifier, final @NonNull Integer integer) {
                        if (parameterName.equals(identifier.getSimpleName())) {
                            parameterUsed[0] = true;
                        }
                        return super.visitIdentifier(identifier, integer);
                    }
                }.visit(expression, 0);

                return parameterUsed[0];
            }

            private J.MethodDeclaration findSingleGetMethod(final List<Statement> classStatements) {
                J.MethodDeclaration found = null;
                for (final Statement statement : classStatements) {
                    if (!(statement instanceof final J.MethodDeclaration methodDeclaration)) {
                        continue;
                    }
                    if (!"get".equals(methodDeclaration.getSimpleName()) || methodDeclaration.getParameters().size() != 1) {
                        continue;
                    }
                    if (found != null) {
                        return null;
                    }
                    found = methodDeclaration;
                }
                return found;
            }
        };
    }
}
