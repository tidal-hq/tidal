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
    private static final float SIDEBAR_WIDTH_RATIO = 0.15f;
    private static final float CATEGORY_HEIGHT_RATIO = 0.035f;
    private static final float PADDING_RATIO = 0.01f;

    private static final int PANEL_BACKGROUND_COLOR = 0xFF2D2D2D;

    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;

    private int sidebarWidth;
    private int categoryHeight;
    private int padding;

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

        this.sidebarWidth = (int)(panelWidth * SIDEBAR_WIDTH_RATIO);
        this.categoryHeight = (int)(this.height * CATEGORY_HEIGHT_RATIO);
        this.padding = (int)(Math.min(this.width, this.height) * PADDING_RATIO);

        this.sidebarWidth = Math.max(this.sidebarWidth, 80);
        this.categoryHeight = Math.max(this.categoryHeight, 20);
        this.padding = Math.max(this.padding, 4);

        this.sidebar = new Sidebar(this, sidebarWidth, categoryHeight, padding);
        this.sidebar.init(panelLeft, panelTop, panelRight, panelBottom);

        if (Category.values().length > 0) {
            this.sidebar.setSelectedCategory(Category.values()[0]);
        }
    }

    @Override
    public void render(DrawContext dc, int mX, int mY, float delta) {
        dc.fill(panelLeft, panelTop,
                panelRight, panelBottom,
                PANEL_BACKGROUND_COLOR);

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