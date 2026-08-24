/**
 * Recipes that migrate a Bukkit plugin from InvUI 1.x to 2.x by reading the syntax tree.
 *
 * <p>What lands here is what a YAML recipe cannot express. Everything a {@code ChangeType} or a
 * {@code ChangeMethodName} entry can state stays in
 * {@code catalog/src/main/resources/META-INF/rewrite/minecraft-invui-v1-to-v2.yml}, and the entry
 * point runs those phases first, because a rename keyed on a type has to run after the type has
 * moved package.
 *
 * <p>The posture is the same in every class here. Where 2.x renamed something, the recipe renames
 * it. Where 2.x changed what a call means, or dropped it, the recipe rewrites nothing: it either
 * leaves a {@code SearchResult} marker naming the 2.x shape, or it skips the call and the
 * {@code ManualFollowUps} phase in the YAML marks it. A partial migration that compiles is worse
 * than one that does not, because the first kind ships.
 *
 * <p>Type attribution is not reliable here. A {@code MethodMatcher} matches nothing without it, and
 * the sources these recipes run over are routinely parsed without InvUI on the classpath. Most of
 * these classes therefore match on simple names, declared parameter types or the printed source,
 * either behind a matcher or instead of one, and nearly every test sets
 * {@code TypeValidation.none()}. Deleting one of those paths because the matcher above it looks
 * sufficient is how this stops working on real input while the tests stay green.
 *
 * <p>{@code MethodMatcher} parses its pattern in its constructor and a recipe visits every source
 * file in the repository it runs over, so the matchers are static constants.
 *
 * <p>Every recipe here is composed by
 * {@code de.timscho.rewrite.minecraft.invui.v1-to-v2.CustomVisitors} except two, which run only
 * when named on their own: {@link de.timscho.rewrite.minecraft.invui.RemoveSimpleItemWrapper}, and
 * {@link de.timscho.rewrite.minecraft.invui.MigrateAnvilWindowToOldApi}, which goes the other
 * direction and rewrites 2.x back to 1.x.
 */
package de.timscho.rewrite.minecraft.invui;
