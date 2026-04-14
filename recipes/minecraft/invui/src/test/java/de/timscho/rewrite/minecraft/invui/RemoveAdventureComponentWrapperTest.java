package de.timscho.rewrite.minecraft.invui;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveAdventureComponentWrapperTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new RemoveAdventureComponentWrapper());
    }

    @Test
    void removesWrapper() {
        this.rewriteRun(
            java(
                """
                package xyz.xenondevs.inventoryaccess.component;

                class AdventureComponentWrapper {
                    AdventureComponentWrapper(String value) {}
                }

                class Builder {
                    void setTitle(Object title) {}
                }

                class Messages {
                    String title() {
                        return "";
                    }
                }

                class Test {
                    private final Builder builder = new Builder();

                    void example() {
                        builder.setTitle(new AdventureComponentWrapper(new Messages().title()));
                    }
                }
                """,
                """
                package xyz.xenondevs.inventoryaccess.component;

                class AdventureComponentWrapper {
                    AdventureComponentWrapper(String value) {}
                }

                class Builder {
                    void setTitle(Object title) {}
                }

                class Messages {
                    String title() {
                        return "";
                    }
                }

                class Test {
                    private final Builder builder = new Builder();

                    void example() {
                        builder.setTitle(new Messages().title());
                    }
                }
                """
            )
        );
    }
}
