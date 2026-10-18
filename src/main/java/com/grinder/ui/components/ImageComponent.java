
package com.grinder.ui.components;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ImageComponent extends JComponent {
    @Getter
    private BufferedImage image;

    private int imageWidth;
    private int imageHeight;

    public ImageComponent(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        setPreferredSize(new Dimension(imageWidth, imageHeight));
        setMinimumSize(new Dimension(imageWidth, imageHeight));
    }

    public ImageComponent(BufferedImage image, int imageWidth, int imageHeight) {
        this(imageWidth, imageHeight);
        setImage(image);
    }

    public void setImage(BufferedImage image) {
        if (this.image == image) {
            return;
        }

        this.image = image;
        repaint();
    }

    public void setImageSize(int imageWidth, int imageHeight) {
        if (this.imageWidth == imageWidth && this.imageHeight == imageHeight) {
            return;
        }

        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        setPreferredSize(new Dimension(imageWidth, imageHeight));
        setMinimumSize(new Dimension(imageWidth, imageHeight));
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (image == null) {
            return;
        }

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int x = (getWidth() - imageWidth) / 2;
            int y = (getHeight() - imageHeight) / 2;
            g.drawImage(image, x, y, imageWidth, imageHeight, this);
        }
        finally {
            g.dispose();
        }
    }
}
