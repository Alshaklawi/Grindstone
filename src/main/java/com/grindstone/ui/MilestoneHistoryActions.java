package com.grindstone.ui;

import com.grindstone.repository.GrindstoneProfile;
import com.grindstone.repository.HistoryRecord;
import com.grindstone.ui.components.HistoryBrowserPanel;
import com.grindstone.ui.util.DialogPlacement;
import java.awt.Dialog;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
final class MilestoneHistoryActions {
    private MilestoneHistoryActions() {
    }

    static void showAccountHistory(MilestoneActionSupport support, BiConsumer<GrindstoneProfile, String> reveal) {
        GrindstoneProfile active = support.getProfile();
        if (active == null) {
            return;
        }
        support.getClientThread().invokeLater(() -> {
            if (!support.isCurrentProfile(active)) {
                return;
            }
            List<HistoryRecord> records = HistoryRecord.createSnapshot(active, support::getTitle);
            SwingUtilities.invokeLater(() -> {
                if (!support.isCurrentProfile(active)) {
                    return;
                }
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(support.getOwner()), "Account milestone history", Dialog.ModalityType.APPLICATION_MODAL);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setContentPane(new HistoryBrowserPanel(records, id -> {
                    dialog.dispose();
                    reveal.accept(active, id);
                }));
                dialog.pack();
                DialogPlacement.center(dialog, support.getOwner());
                dialog.setVisible(true);
            });
        });
    }
}
