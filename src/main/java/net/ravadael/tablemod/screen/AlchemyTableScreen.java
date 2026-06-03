package net.ravadael.tablemod.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.ravadael.tablemod.menu.AlchemyTableMenu;
import net.ravadael.tablemod.network.ModMessages;
import net.ravadael.tablemod.recipe.AlchemyRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlchemyTableScreen extends AbstractContainerScreen<AlchemyTableMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("tablemod", "textures/gui/alchemy_table.png");

    private static final int COLS = 4;
    private static final int ROWS = 3;
    private static final int MAX_VISIBLE = COLS * ROWS;
    private static final int SCROLLBAR_X = 119;
    private static final int SCROLLBAR_Y = 27;
    private static final int GRID_X = 52;
    private static final int GRID_Y = 27;
    private static final int BTN_W = 16;
    private static final int BTN_H = 18;
    private static final int BTN_V_NORMAL = 178;
    private static final int BTN_V_SELECTED = 196;
    private static final int BTN_V_HOVERED = 214;
    private static final int SPACE_X = 16;
    private static final int SPACE_Y = 18;
    private static final int SEARCH_X = 51;
    private static final int SEARCH_Y = 7;
    private static final int SEARCH_W = 81;
    private static final int SEARCH_H = 16;
    private static final int SEARCH_TEXT_PADDING = 2;
    private static final int SEARCH_TEXT_PADDING_Y = 4;

    private int scrollOffset = 0;
    private int selectedIndex = -1;
    private boolean isScrolling = false;
    private ItemStack lastInput = ItemStack.EMPTY;
    private EditBox searchBox;
    private String searchText = "";

    public AlchemyTableScreen(AlchemyTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();

        searchBox = new EditBox(font, leftPos + SEARCH_X + SEARCH_TEXT_PADDING, topPos + SEARCH_Y + SEARCH_TEXT_PADDING_Y,
                SEARCH_W - SEARCH_TEXT_PADDING, SEARCH_H - SEARCH_TEXT_PADDING_Y,
                Component.translatable("gui.tablemod.search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(50);
        searchBox.setHint(Component.translatable("gui.tablemod.search"));
        searchBox.setTextColor(0xF6E7CF);
        searchBox.setTextColorUneditable(0xD1C6BA);
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value;
            scrollOffset = 0;
            selectedIndex = -1;
        });

        addRenderableWidget(searchBox);
    }

    private List<DisplayedResult> collectResults() {
        ItemStack input = menu.getInputItem();
        String query = normalizeSearch(getSearchText());
        List<DisplayedResult> list = new ArrayList<>();
        for (var holder : menu.getCurrentRecipeHolders()) {
            AlchemyRecipe recipe = holder.value();
            for (ItemStack output : recipe.getFilteredResults(input)) {
                if (matchesSearch(output, query)) {
                    list.add(new DisplayedResult(holder.id(), recipe, output));
                }
            }
        }
        return list;
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        ItemStack input = menu.getInputItem();
        if (!ItemStack.isSameItemSameComponents(input, lastInput)) {
            selectedIndex = -1;
            scrollOffset = 0;
            lastInput = input.copy();
        }

        List<DisplayedResult> results = collectResults();
        int totalRows = (int) Math.ceil(results.size() / (double) COLS);
        int maxScroll = Math.max(0, totalRows - ROWS);

        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        if (selectedIndex >= results.size()) {
            selectedIndex = -1;
        }

        syncSelectedIndex(results);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY) {
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderSearchBoxBackground(gfx);

        List<DisplayedResult> results = collectResults();
        int totalRows = (int) Math.ceil(results.size() / (double) COLS);
        int maxScroll = Math.max(0, totalRows - ROWS);
        int barX = leftPos + SCROLLBAR_X;
        int barY = topPos + SCROLLBAR_Y;

        if (maxScroll > 0) {
            int knobY = barY + (int) (41 * scrollOffset / (double) maxScroll);
            gfx.blit(TEXTURE, barX, knobY, 176, 0, 12, 15);
        } else {
            gfx.blit(TEXTURE, barX, barY, 188, 0, 12, 15);
        }

        int start = scrollOffset * COLS;
        for (int i = 0; i < MAX_VISIBLE; i++) {
            int idx = start + i;
            if (idx >= results.size()) {
                break;
            }

            int row = i / COLS;
            int col = i % COLS;
            int x = leftPos + GRID_X + col * SPACE_X;
            int y = topPos + GRID_Y + row * SPACE_Y;
            boolean hovered = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
            boolean selected = idx == selectedIndex;

            if (selected) {
                gfx.blit(TEXTURE, x, y, 0, BTN_V_SELECTED, BTN_W, BTN_H);
            } else if (hovered) {
                gfx.blit(TEXTURE, x, y, 0, BTN_V_HOVERED, BTN_W, BTN_H);
            } else {
                gfx.blit(TEXTURE, x, y, 0, BTN_V_NORMAL, BTN_W, BTN_H);
            }

            DisplayedResult displayedResult = results.get(idx);
            ItemStack stack = displayedResult.stack();
            gfx.renderItem(stack, x, y + 1);

            if (hovered) {
                Item.TooltipContext tooltipContext = minecraft.level != null
                        ? Item.TooltipContext.of(minecraft.level)
                        : Item.TooltipContext.EMPTY;
                List<Component> tooltip = new ArrayList<>(stack.getTooltipLines(tooltipContext, minecraft.player, TooltipFlag.NORMAL));
                AlchemyRecipe recipe = displayedResult.recipe();
                if (recipe != null) {
                    tooltip.add(Component.empty());
                    if (!recipe.isCatalystRequired() || recipe.getCatalyst().isEmpty() || recipe.getCatalyst().getItems().length == 0) {
                        tooltip.add(Component.translatable("gui.tablemod.catalyst_none").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.translatable("gui.tablemod.catalyst").withStyle(ChatFormatting.GRAY));
                        for (ItemStack catalyst : recipe.getCatalyst().getItems()) {
                            tooltip.add(Component.literal(" * ").append(catalyst.getHoverName()).withStyle(ChatFormatting.DARK_GRAY));
                        }
                    }
                }
                gfx.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, playerInventoryTitle, 8, 84, 0x404040, false);

        if (!menu.getInputItem().isEmpty() && collectResults().isEmpty()) {
            gfx.drawString(font, Component.translatable("gui.tablemod.search_empty"), 52, 47, 0x5B5146, false);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (searchBox != null) {
            boolean clickedSearchBounds = isInsideSearchBounds(mx, my);
            boolean clickedSearch = clickedSearchBounds && searchBox.mouseClicked(mx, my, button);
            if (clickedSearchBounds) {
                searchBox.setFocused(true);
                setFocused(searchBox);
                if (!clickedSearch) {
                    searchBox.setCursorPosition(0);
                    searchBox.setHighlightPos(0);
                }
                return true;
            }

            searchBox.setFocused(false);
            setFocused(null);
        }

        if (button == 0) {
            if (clickScrollbar(mx, my)) {
                return true;
            }

            List<DisplayedResult> results = collectResults();
            int start = scrollOffset * COLS;
            for (int i = 0; i < MAX_VISIBLE; i++) {
                int idx = start + i;
                if (idx >= results.size()) {
                    break;
                }

                int row = i / COLS;
                int col = i % COLS;
                int x = leftPos + GRID_X + col * SPACE_X;
                int y = topPos + GRID_Y + row * SPACE_Y;
                if (mx >= x && mx < x + BTN_W && my >= y && my < y + BTN_H) {
                    selectedIndex = idx;
                    DisplayedResult chosen = results.get(idx);
                    ModMessages.sendSelectResult(chosen.recipeId(), chosen.stack().copy());

                    if (minecraft.player != null) {
                        minecraft.player.playSound(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 0.3F, 1.0F);
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }

            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }

    private boolean clickScrollbar(double mx, double my) {
        int barX = leftPos + SCROLLBAR_X;
        int barY = topPos + SCROLLBAR_Y;
        if (mx >= barX && mx < barX + 12 && my >= barY && my < barY + 56) {
            isScrolling = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        isScrolling = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!isScrolling) {
            return false;
        }

        List<DisplayedResult> results = collectResults();
        int totalRows = (int) Math.ceil(results.size() / (double) COLS);
        int maxScroll = Math.max(0, totalRows - ROWS);
        if (maxScroll <= 0) {
            return false;
        }

        double barY = Math.max(0, Math.min(my - (topPos + SCROLLBAR_Y), 41));
        scrollOffset = (int) Math.round((barY / 41D) * maxScroll);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        List<DisplayedResult> results = collectResults();
        int totalRows = (int) Math.ceil(results.size() / (double) COLS);
        int maxScroll = Math.max(0, totalRows - ROWS);

        if (maxScroll > 0) {
            scrollOffset -= (int) scrollY;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }

        return true;
    }

    private String getSearchText() {
        return searchBox != null ? searchBox.getValue() : searchText;
    }

    private static String normalizeSearch(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean matchesSearch(ItemStack stack, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String displayName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (displayName.contains(query)) {
            return true;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && itemId.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean isInsideSearchBounds(double mouseX, double mouseY) {
        int x1 = leftPos + SEARCH_X;
        int y1 = topPos + SEARCH_Y;
        return mouseX >= x1 && mouseX < x1 + SEARCH_W && mouseY >= y1 && mouseY < y1 + SEARCH_H;
    }

    private void syncSelectedIndex(List<DisplayedResult> results) {
        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            selectedIndex = -1;
            return;
        }

        if (selectedIndex >= 0 && selectedIndex < results.size()
                && matchesDisplayedOutput(results.get(selectedIndex).stack(), output)) {
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            if (matchesDisplayedOutput(results.get(i).stack(), output)) {
                selectedIndex = i;
                return;
            }
        }

        selectedIndex = -1;
    }

    private static boolean matchesDisplayedOutput(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameComponents(left, right) || ItemStack.isSameItem(left, right);
    }

    private void renderSearchBoxBackground(GuiGraphics gfx) {
        int x1 = leftPos + SEARCH_X;
        int y1 = topPos + SEARCH_Y;
        int x2 = x1 + SEARCH_W;
        int y2 = y1 + SEARCH_H;

        gfx.fill(x1, y1, x2, y2, 0xFF3D352A);
        gfx.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xCC6E5E49);
    }

    private record DisplayedResult(ResourceLocation recipeId, AlchemyRecipe recipe, ItemStack stack) {
    }
}
