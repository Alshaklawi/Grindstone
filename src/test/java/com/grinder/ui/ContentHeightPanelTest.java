package com.grinder.ui;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JViewport;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public class ContentHeightPanelTest
{
    @Test
    public void listFitsNarrowSidebarWithoutStretchingCollapsedRows() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            GoalListPanel list = new GoalListPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            ContentHeightPanel row = new ContentHeightPanel();
            row.setPreferredSize(new Dimension(400, 36));
            list.add(row);
            JViewport viewport = new JViewport();
            viewport.setView(list);
            viewport.setSize(225, 900);
            viewport.doLayout();
            list.doLayout();
            assertEquals(225, list.getWidth());
            assertEquals(36, row.getHeight());
        });
    }

    @Test
    public void collapsedSectionsDoNotAbsorbEmptyViewportHeight() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            ContentHeightPanel section = new ContentHeightPanel();
            section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
            JPanel header = new JPanel();
            header.setPreferredSize(new Dimension(200, 36));
            section.add(header);
            JPanel content = new JPanel();
            content.setPreferredSize(new Dimension(200, 300));
            section.add(content);
            list.add(section);
            list.setSize(225, 900);
            list.doLayout();
            assertEquals(336, section.getHeight());
            content.setVisible(false);
            section.revalidate();
            list.invalidate();
            list.doLayout();
            assertEquals(36, section.getHeight());
            content.setVisible(true);
            section.revalidate();
            list.invalidate();
            list.doLayout();
            assertEquals(336, section.getHeight());
        });
    }
}
