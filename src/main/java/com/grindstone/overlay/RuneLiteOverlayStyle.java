package com.grindstone.overlay;

import com.grindstone.ui.Icon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ImageUtil;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/11/2026
 */
final class RuneLiteOverlayStyle {
    private RuneLiteOverlayStyle() {
    }

    static void renderPanel(Graphics2D graphics, int width, int height, String title) {
        BackgroundComponent background = new BackgroundComponent();
        background.setRectangle(new Rectangle(0, 0, width, height));
        background.render(graphics);
        TitleComponent.builder().text(title).preferredLocation(new Point(4, 2)).preferredSize(new Dimension(width - 8, 0)).build().render(graphics);
    }

    static void renderLine(Graphics2D graphics, int x, int y, int width, String left, String right, Color color) {
        int available = width - graphics.getFontMetrics().stringWidth(right) - (right.isEmpty() ? 0 : 6);
        LineComponent.builder().left(OverlayStyle.fit(left, graphics.getFontMetrics(), available)).right(right).leftColor(color).rightColor(color).preferredLocation(new Point(x, y)).preferredSize(new Dimension(width, 0)).build().render(graphics);
    }

    static void renderIcon(Graphics2D graphics, BufferedImage image, int x, int y) {
        BufferedImage source = image == null ? Icon.MAIN.getImage() : image;
        double scale = Math.min(1, 16.0 / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        ImageComponent icon = new ImageComponent(ImageUtil.resizeImage(source, width, height));
        icon.setPreferredLocation(new Point(x + (16 - width) / 2, y + (16 - height) / 2));
        icon.render(graphics);
    }

    static void renderProgress(Graphics2D graphics, int x, int y, int width, double ratio) {
        ProgressBarComponent progress = new ProgressBarComponent();
        progress.setValue(Math.max(0, Math.min(1, ratio)) * 100);
        progress.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
        progress.setCenterLabel("");
        progress.setPreferredLocation(new Point(x, y));
        progress.setPreferredSize(new Dimension(width, 16));
        progress.render(graphics);
    }
}
