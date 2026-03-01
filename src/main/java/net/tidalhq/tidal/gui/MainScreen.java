package net.tidalhq.tidal.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.tidalhq.tidal.Category;
import net.tidalhq.tidal.gui.widgets.Sidebar;

public class MainScreen extends Screen {
    private static final float PANEL_WIDTH_RATIO = 0.86f;
    private static final float PANEL_HEIGHT_RATIO = 0.86f;
    private static final int PANEL_BACKGROUND_COLOR = 0xFF2D2D2D;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int CATEGORY_HEIGHT = 30;
    private static final int PADDING = 10;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;

    private Sidebar sidebar;

    public MainScreen() {
        super(Text.literal("Tidal"));
    }

    @Override
    protected void init() {
        int panelWidth = (int)(this.width * PANEL_WIDTH_RATIO);
        int panelHeight = (int)(this.height * PANEL_HEIGHT_RATIO);

        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelBottom = this.panelTop + panelHeight;

        this.sidebar = new Sidebar(this, SIDEBAR_WIDTH, CATEGORY_HEIGHT, PADDING);
        this.sidebar.init(panelLeft, panelTop, panelRight, panelBottom);

        if (Category.values().length > 0) {
            this.sidebar.setSelectedCategory(Category.values()[0]);
        }
    }

    @Override
    public void render(DrawContext dc, int mX, int mY, float delta) {
        // Draw main panel background
        dc.fill(panelLeft, panelTop,
                panelRight, panelBottom,
                PANEL_BACKGROUND_COLOR);

        // Render sidebar
        if (sidebar != null) {
            sidebar.render(dc, mX, mY, delta);
        }

        super.render(dc, mX, mY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (sidebar != null) {
            sidebar.mouseClicked((int) click.x(), (int) click.y(), click.button());
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}