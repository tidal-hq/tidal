package net.tidalhq.tidal.gui.widgets;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.gui.MainScreen;

import java.util.ArrayList;
import java.util.List;

public class Sidebar {
    private static final int SIDEBAR_BACKGROUND_COLOR = 0xFF3A3A3A;
    private static final int BUTTON_SELECTED_COLOR = 0xFF4A4A4A;
    private static final int BUTTON_HOVER_COLOR = 0xFF404040;
    private static final int BUTTON_NORMAL_COLOR = 0xFF353535;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    // Scale text size with screen if needed
    private static final float MIN_TEXT_SCALE = 0.8f;
    private static final float MAX_TEXT_SCALE = 1.2f;

    private final int sidebarWidth;
    private final int categoryHeight;
    private final int padding;

    private int sidebarX;
    private int sidebarY;
    private int sidebarHeight;

    private final MainScreen mainScreen;

    private final List<CategoryButton> categoryButtons;
    private float textScale = 1.0f;

    private Category category;

    public Sidebar(MainScreen screen, int width, int categoryHeight, int padding) {
        this.mainScreen = screen;
        this.sidebarWidth = width;
        this.categoryHeight = categoryHeight;
        this.padding = padding;
        this.categoryButtons = new ArrayList<>();

        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            categoryButtons.add(new CategoryButton(category, i));
        }

        calculateTextScale();
    }

    private void calculateTextScale() {
        int screenWidth = mainScreen.width;
        int screenHeight = mainScreen.height;
        int referenceSize = 1920;

        float scale = (float) screenWidth / referenceSize;
        this.textScale = Math.max(MIN_TEXT_SCALE, Math.min(MAX_TEXT_SCALE, scale));
    }

    public void init(int panelLeft, int panelTop, int panelRight, int panelBottom) {
        this.sidebarX = panelLeft + padding;
        this.sidebarY = panelTop + padding;
        this.sidebarHeight = panelBottom - panelTop - (padding * 2);

        int y = sidebarY;

        int buttonWidth = sidebarWidth;
        int buttonHeight = categoryHeight;

        for (CategoryButton button : categoryButtons) {
            button.setBounds(
                    sidebarX,
                    y,
                    buttonWidth,
                    buttonHeight
            );
            y += buttonHeight + padding;
        }

        for (CategoryButton button : categoryButtons) {
            button.setTextScale(textScale);
        }
    }

    public int getWidth() {
        return sidebarWidth;
    }

    public int getHeight() {
        return (categoryButtons.size() * categoryHeight) +
                ((categoryButtons.size() - 1) * padding) +
                (padding * 2);
    }

    public Category getSelectedCategory() {
        return category;
    }

    public void setSelectedCategory(Category category) {
        this.category = category;
        for (CategoryButton button : categoryButtons) {
            button.setSelected(button.category == category);
        }
    }

    public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
        dc.fill(sidebarX, sidebarY,
                sidebarX + sidebarWidth, sidebarY + sidebarHeight,
                SIDEBAR_BACKGROUND_COLOR);

        for (CategoryButton button : categoryButtons) {
            button.render(dc, mouseX, mouseY, delta);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for (CategoryButton button : categoryButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                button.onClick();
                setSelectedCategory(button.category);
                break;
            }
        }
    }

    private class CategoryButton {
        private final Category category;
        private final int index;
        private boolean selected;
        private int x;
        private int y;
        private int width;
        private int height;
        private float textScale = 1.0f;

        public CategoryButton(Category category, int index) {
            this.category = category;
            this.index = index;
            this.selected = false;
        }

        public void setBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setTextScale(float scale) {
            this.textScale = scale;
        }

        public boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                    mouseY >= y && mouseY <= y + height;
        }

        public void onClick() {
        }

        public void render(DrawContext dc, int mouseX, int mouseY, float delta) {
            boolean hovered = isMouseOver(mouseX, mouseY);

            if (selected) {
                drawSelectedBackground(dc);
            } else if (hovered) {
                drawHoverBackground(dc);
            } else {
                drawNormalBackground(dc);
            }

            drawCategoryName(dc);
        }

        private void drawSelectedBackground(DrawContext dc) {
            dc.fill(x, y, x + width, y + height, BUTTON_SELECTED_COLOR);
        }

        private void drawHoverBackground(DrawContext dc) {
            dc.fill(x, y, x + width, y + height, BUTTON_HOVER_COLOR);
        }

        private void drawNormalBackground(DrawContext dc) {
            dc.fill(x, y, x + width, y + height, BUTTON_NORMAL_COLOR);
        }

        private void drawCategoryName(DrawContext dc) {
            String name = category.name();

            int textWidth = (int)(mainScreen.getTextRenderer().getWidth(name) * textScale);
            int textHeight = (int)(mainScreen.getTextRenderer().fontHeight * textScale);

            int textX = x + padding;
            int textY = y + (height - textHeight) / 2;

            dc.drawText(mainScreen.getTextRenderer(), name, textX, textY, TEXT_COLOR, true);
        }

        public Category getCategory() {
            return category;
        }
    }
}