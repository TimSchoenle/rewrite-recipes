# Recipes

Every recipe this repository publishes, by the name you pass to `activeRecipe`.

Two of them are entry points. The rest are building blocks the InvUI entry point composes, listed
here because OpenRewrite will run any of them on its own if you name it.

## Contents

- [Entry points](#entry-points)
- [Inside the InvUI migration](#inside-the-invui-migration)
- [Java recipes](#java-recipes)
- [What the migration will not do for you](#what-the-migration-will-not-do-for-you)

## Entry points

| Recipe | Purpose |
| --- | --- |
| `de.timscho.rewrite.Style` | Static analysis rules that a formatter cannot apply, because each one rewrites code rather than whitespace. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2` | Migrates a Bukkit plugin from InvUI 1.x to 2.x, and marks what it could not migrate. |

`Style` is a list of 68 recipes: 66 under `org.openrewrite.staticanalysis`, plus
`org.openrewrite.java.RemoveObjectsIsNull` and
`org.openrewrite.recipes.rewrite.OpenRewriteRecipeBestPractices`. It exists to sit next to
Spotless. Spotless owns formatting, and these are the rules that change what the code does, such as
`StringLiteralEquality`, `CovariantEquals` and `NoDoubleBraceInitialization`. The full list is
`catalog/src/main/resources/META-INF/rewrite/style.yml`.

`FinalizeLocalVariables` is commented out in that file. It was removed because it did not work, and
the comment stays so the next person does not add it back.

## Inside the InvUI migration

`de.timscho.rewrite.minecraft.invui.v1-to-v2` runs six recipes in order, then
`org.openrewrite.java.RemoveUnusedImports`. Order matters: the type moves have to land before the
method renames that name the moved types.

| Recipe | What it does |
| --- | --- |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.Dependencies` | Rewrites the `xyz.xenondevs.invui:invui-core` coordinate to `xyz.xenondevs.invui:invui` and moves the version to `2.x`, in Gradle, in Maven, and in the three version-catalog aliases `invui`, `invui-core` and `xenondevs-invui`. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.TypeRelocations` | Twelve `ChangeType` rules for types that moved package. `ComponentWrapper` becomes `net.kyori.adventure.text.Component`, and `MapIcon` and `MapPatch` become nested classes of `CartographyWindow`. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.FactoryAndAccessorRenames` | Thirteen method renames that are safe to apply without reading their arguments, such as `ScrollGui.items()` to `itemsBuilder()` and `AnvilWindow.single()` to `builder()`. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.CustomVisitors` | The twelve Java recipes below, for changes that need to inspect the tree rather than match a signature. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.ManualFollowUps` | Search recipes that mark the call sites and types whose semantics changed. Nothing is rewritten. |
| `de.timscho.rewrite.minecraft.invui.v1-to-v2.RemainingSurfaceFollowUps` | The same, for the InvUI 1.x surface that 2.x dropped outright. |

## Java recipes

`de.timscho.rewrite:recipes-minecraft-invui` carries fourteen recipes. Twelve are in
`CustomVisitors`; the two marked below are not, and only run when you name them.

| Recipe | What it does |
| --- | --- |
| `RemoveAdventureComponentWrapper` | Drops the `AdventureComponentWrapper` call and passes its argument through. |
| `MigrateStatelessItemProviderToItemWrapper` | Replaces an anonymous `ItemProvider` with `ItemWrapper` when its `get(String lang)` ignores `lang` and returns one `ItemStack` expression. |
| `MigrateSimpleItemToItem` | Rewrites `new SimpleItem(provider, clickHandler)` to the 2.x `Item` API. |
| `MigrateItemGetItemProviderToPlayerApi` | Moves `Item#getItemProvider()` implementations onto the player-aware overload and bridges the no-arg one. |
| `MigrateItemProviderStringGetToLocaleApi` | Moves `ItemProvider#get(String)` to the `Locale` overload and bridges legacy providers. |
| `MigrateControlItemToAbstractBoundItem` | Rewrites `ControlItem` subclasses onto the `AbstractBoundItem` contract. |
| `MigrateGuiToNewApi` | The safe `Gui` renames. |
| `MigrateScrollGuiToNewApi` | `ScrollGui` consumer factory overloads and the legacy `int` content-list slot signatures. |
| `MigrateWindowToNewApi` | The safe `Window` renames and type changes, marking the ambiguous ones. |
| `MigratePagedGuiToNewApi` | The safe `PagedGui` renames. |
| `MigrateItemHandleClickToClickApi` | Rewrites `handleClick(ClickType, Player, InventoryClickEvent)` onto `xyz.xenondevs.invui.Click`. |
| `MigrateClickGettersToRecordAccessors` | `Click.getPlayer()`, `getClick()`, `getClickType()` and `getHotbarButton()` become the record accessors. |
| `RemoveSimpleItemWrapper` | Not in `CustomVisitors`. Replaces `new SimpleItem(itemStack)` with `itemStack` when the only argument is an `ItemStack`. |
| `MigrateAnvilWindowToOldApi` | Not in `CustomVisitors`, and it runs backwards: it rewrites `AnvilWindow` from 2.x to 1.x. |

Every one of them is in the package `de.timscho.rewrite.minecraft.invui`, and every one has a test
beside it in `recipes/minecraft/invui/src/test/java`.

## What the migration will not do for you

`ManualFollowUps` and `RemainingSurfaceFollowUps` use `FindMethods` and `FindTypes`, which insert a
`~~>` marker at each match instead of changing it. Run `rewriteDryRun`, read the markers, and fix
those by hand.

They cover three groups.

Call sites whose meaning changed, so a rename would compile and behave differently:
`ScrollGui.canScroll(int)`, `TabGui.normal(Consumer)`, `TabGui.of(int, int, List, int[])`,
`TabGui.Builder.addTab(..)`, the `Consumer` overloads of `AnvilWindow.single`, `AnvilWindow.split`,
`CartographyWindow.single` and `CartographyWindow.split`, `CartographyWindow.updateMap(..)`,
`Click.getEvent()` and `PlayerUpdateReason.getEvent()`.

The `Inventory` and `WindowManager` methods 2.x removed: the pre- and post-update handler
accessors, `setItemSilently`, `replaceItem`, `isSynced`, `getGuiPriority`, `addWindow`,
`removeWindow`, and `WindowManager.getWindow(org.bukkit.inventory.Inventory)`.

Types with no 2.x equivalent. The item builders (`AbstractItemBuilder`, `BannerBuilder`,
`FireworkBuilder`, `PotionBuilder`, `SkullBuilder`), the prebuilt items (`AsyncItem`,
`AutoCycleItem`, `AutoUpdateItem`, `CommandItem`, `CycleItem`, `SuppliedItem`, `PageItem`,
`ScrollItem`, `TabItem`), the nine `animation.impl` classes, the abstract and `Impl` window
classes, `AbstractTabGui`, `GuiParent`, `IngredientList`, `StackSizeProvider`, and the six `util` classes
(`ArrayUtils`, `DataUtils`, `InventoryUtils`, `MathUtils`, `SlotUtils`, `MojangApiUtils`) along
with `Pair` and `Point2D`.
