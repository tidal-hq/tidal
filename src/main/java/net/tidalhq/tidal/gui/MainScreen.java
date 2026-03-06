package net.tidalhq.tidal.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.tidalhq.tidal.Category;

public class MainScreen extends Screen {
    private static final float PANEL_WIDTH_RATIO = 0.86f;
    private static final float PANEL_HEIGHT_RATIO = 0.86f;
    private static final int PANEL_BG = 0xFF2D2D2D;

    private int panelLeft, panelTop, panelRight, panelBottom;

    public MainScreen() {
        super(Text.literal("Tidal"));
    }

    @Override
    protected void init() {
        int panelWidth = (int)(width * PANEL_WIDTH_RATIO);
        int panelHeight = (int)(height * PANEL_HEIGHT_RATIO);

        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        panelRight = panelLeft + panelWidth;
        panelBottom = panelTop + panelHeight;
    }

    @Override
    public void render(DrawContext dc, int mX, int mY, float delta) {
        dc.fill(panelLeft, panelTop, panelRight, panelBottom, PANEL_BG);

        super.render(dc, mX, mY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return true; }
}