package de.timscho.rewrite.minecraft.invui;

import org.jspecify.annotations.NonNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

public class MigratePagedGuiToNewApi extends Recipe {
    private static final String PAGED_GUI = "xyz.xenondevs.invui.gui.PagedGui";

    private static final MethodMatcher PAGED_GUI_ITEMS_BUILDER_FACTORY =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " items()");
    private static final MethodMatcher PAGED_GUI_GUIS_BUILDER_FACTORY =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " guis()");
    private static final MethodMatcher PAGED_GUI_INVENTORIES_BUILDER_FACTORY =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " inventories()");
    private static final MethodMatcher PAGED_GUI_GET_PAGE_AMOUNT =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " getPageAmount()");
    private static final MethodMatcher PAGED_GUI_GET_CURRENT_PAGE =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " getCurrentPage()");
    private static final MethodMatcher PAGED_GUI_ITEMS_WITH_CONSUMER =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " items(java.util.function.Consumer)");
    private static final MethodMatcher PAGED_GUI_GUIS_WITH_CONSUMER =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " guis(java.util.function.Consumer)");
    private static final MethodMatcher PAGED_GUI_INVENTORIES_WITH_CONSUMER =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " inventories(java.util.function.Consumer)");
    private static final MethodMatcher PAGED_GUI_OF_ITEMS_WITH_INT_SLOTS =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " ofItems(int, int, java.util.List, int[])");
    private static final MethodMatcher PAGED_GUI_OF_GUIS_WITH_INT_SLOTS =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " ofGuis(int, int, java.util.List, int[])");
    private static final MethodMatcher PAGED_GUI_OF_INVENTORIES_WITH_INT_SLOTS =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " ofInventories(int, int, java.util.List, int[])");
    private static final MethodMatcher PAGED_GUI_HAS_NEXT_PAGE =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " hasNextPage()");
    private static final MethodMatcher PAGED_GUI_HAS_PREVIOUS_PAGE =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " hasPreviousPage()");
    private static final MethodMatcher PAGED_GUI_HAS_INFINITE_PAGES =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " hasInfinitePages()");
    private static final MethodMatcher PAGED_GUI_GO_FORWARD =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " goForward()");
    private static final MethodMatcher PAGED_GUI_GO_BACK =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " goBack()");
    private static final MethodMatcher PAGED_GUI_GET_CONTENT_LIST_SLOTS =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + " getContentListSlots()");
    private static final MethodMatcher PAGED_GUI_BUILDER_ADD_CONTENT =
        new MethodMatcher(MigratePagedGuiToNewApi.PAGED_GUI + "$Builder addContent(..)");

    @Override
    public @NonNull String getDisplayName() {
        return "Migrate InvUI PagedGui API to v2";
    }

    @Override
    public @NonNull String getDescription() {
        return "Migrates safe PagedGui API renames from InvUI v1 to v2.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.@NonNull MethodInvocation visitMethodInvocation(final J.@NonNull MethodInvocation method, final @NonNull ExecutionContext ctx) {
                final J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (MigratePagedGuiToNewApi.PAGED_GUI_ITEMS_BUILDER_FACTORY.matches(m)) {
                    return m.withName(m.getName().withSimpleName("itemsBuilder"));
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_GUIS_BUILDER_FACTORY.matches(m)) {
                    return m.withName(m.getName().withSimpleName("guisBuilder"));
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_INVENTORIES_BUILDER_FACTORY.matches(m)) {
                    return m.withName(m.getName().withSimpleName("inventoriesBuilder"));
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_GET_PAGE_AMOUNT.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getPageCount"));
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_GET_CURRENT_PAGE.matches(m)) {
                    return m.withName(m.getName().withSimpleName("getPage"));
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_ITEMS_WITH_CONSUMER.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_GUIS_WITH_CONSUMER.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_INVENTORIES_WITH_CONSUMER.matches(m)) {
                    return SearchResult.found(
                        m,
                        "PagedGui factory + consumer overloads were removed in v2; migrate to <type>Builder() + build() manually."
                    );
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_OF_ITEMS_WITH_INT_SLOTS.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_OF_GUIS_WITH_INT_SLOTS.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_OF_INVENTORIES_WITH_INT_SLOTS.matches(m)) {
                    return SearchResult.found(
                        m,
                        "contentListSlots changed from int... to List<? extends Slot>; migrate slot index usage manually."
                    );
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_HAS_NEXT_PAGE.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_HAS_PREVIOUS_PAGE.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_HAS_INFINITE_PAGES.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_GO_FORWARD.matches(m)
                    || MigratePagedGuiToNewApi.PAGED_GUI_GO_BACK.matches(m)) {
                    return SearchResult.found(
                        m,
                        "PagedGui paging helpers were removed in v2; migrate with page/pageCount properties or explicit setPage logic."
                    );
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_GET_CONTENT_LIST_SLOTS.matches(m)) {
                    return SearchResult.found(
                        m,
                        "PagedGui#getContentListSlots now returns List<Slot> instead of int[]; adjust consumers accordingly."
                    );
                }

                if (MigratePagedGuiToNewApi.PAGED_GUI_BUILDER_ADD_CONTENT.matches(m)) {
                    return SearchResult.found(
                        m,
                        "PagedGui.Builder#addContent was removed in v2; update builder usage to setContent(...)."
                    );
                }

                return m;
            }
        };
    }
}
