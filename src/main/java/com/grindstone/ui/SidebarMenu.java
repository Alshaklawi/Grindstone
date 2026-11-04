package com.grindstone.ui;

import com.grindstone.content.milestone.MilestonePreset;
import com.grindstone.repository.GrindstoneProfile;
import java.awt.Image;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 11/04/2026
 */
public final class SidebarMenu {
    private SidebarMenu() {
    }

    public static JPopupMenu create(MilestoneCreationActions creation, MilestoneEditActions edits, BulkMilestoneActions bulk, Supplier<GrindstoneProfile> profiles, boolean developerMode, Runnable previewPopup, BiConsumer<Boolean, Boolean> transferBackup, Consumer<Boolean> collapseCategories, Runnable showHistory) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem link = new JMenuItem("Link milestones...");
        link.setToolTipText("Combine existing trackers into a linked milestone.");
        link.addActionListener(event -> edits.editLinkedMilestone(profiles.get(), null));
        menu.add(link);
        menu.addSeparator();
        JMenuItem allCollectionPages = new JMenuItem("Add all collection-log pages...");
        allCollectionPages.setToolTipText("Read every page from the client's five collection-log tabs and add missing page milestones.");
        allCollectionPages.addActionListener(event -> creation.addAllCollectionPages());

        JMenu presets = new JMenu("Add presets");
        presets.setToolTipText("Add a focused milestone pack without replacing existing milestones or edited targets.");
        for (MilestonePreset preset : MilestonePreset.values()) {
            JMenuItem option = new JMenuItem(preset.toString());
            option.addActionListener(event -> creation.addPreset(preset));
            presets.add(option);
        }

        JMenu routines = new JMenu("Routines");
        JMenuItem dailyRoutine = new JMenuItem("Daily skilling & personal");
        dailyRoutine.addActionListener(event -> creation.addRoutine(false));
        JMenuItem weeklyRoutine = new JMenuItem("Weekly activities & personal");
        weeklyRoutine.addActionListener(event -> creation.addRoutine(true));
        routines.add(dailyRoutine);
        routines.add(weeklyRoutine);
        presets.addSeparator();
        presets.add(routines);
        presets.add(allCollectionPages);
        menu.add(presets);
        JMenu milestoneActions = new JMenu("Manage milestones");
        JMenuItem manage = new JMenuItem("Select and edit...");
        manage.setToolTipText("Search and select milestones to pin, delete, categorize, change schedules or set deadlines.");
        manage.addActionListener(event -> bulk.showBulkManager());
        milestoneActions.add(manage);
        JMenuItem clearMilestones = new JMenuItem("Clear all milestones...");
        clearMilestones.setToolTipText("Remove all milestones from this profile, including hidden milestones. Confirmation and undo are available.");
        clearMilestones.addActionListener(event -> bulk.clearAllMilestones());
        milestoneActions.addSeparator();
        milestoneActions.add(clearMilestones);
        menu.add(milestoneActions);
        JMenu backups = new JMenu("Profile backups");
        JMenuItem export = new JMenuItem("Export profile backup...");
        export.setToolTipText("Save a private local JSON backup. No network is used.");
        export.addActionListener(event -> transferBackup.accept(false, false));
        backups.add(export);
        JMenuItem restore = new JMenuItem("Restore / import backup...");
        restore.setToolTipText("Merge missing milestones without replacing existing targets.");
        restore.addActionListener(event -> transferBackup.accept(true, false));
        backups.add(restore);
        JMenu packs = new JMenu("Milestone packs");
        JMenuItem share = new JMenuItem("Export definitions...");
        share.setToolTipText("Share targets and routines without account identity, progress or history.");
        share.addActionListener(event -> transferBackup.accept(false, true));
        packs.add(share);
        JMenuItem importPack = new JMenuItem("Import definitions...");
        importPack.addActionListener(event -> transferBackup.accept(true, true));
        packs.add(importPack);
        menu.add(packs);
        JMenu viewOptions = new JMenu("Category display");
        JMenuItem expandAll = new JMenuItem("Expand all categories");
        expandAll.setToolTipText("Show the milestone cards in every category.");
        expandAll.addActionListener(event -> collapseCategories.accept(false));
        viewOptions.add(expandAll);
        JMenuItem collapseAll = new JMenuItem("Collapse all categories");
        collapseAll.setToolTipText("Show category headers only. Your milestones and progress are kept.");
        collapseAll.addActionListener(event -> collapseCategories.accept(true));
        viewOptions.add(collapseAll);
        menu.add(viewOptions);
        menu.add(backups);
        menu.addSeparator();
        JMenuItem history = new JMenuItem("History...");
        history.setToolTipText("Browse this account's milestone history by date, schedule, milestone and outcome.");
        history.addActionListener(event -> showHistory.run());
        menu.add(history);
        if (developerMode) {
            menu.addSeparator();
            JMenu development = new JMenu("Development");
            JMenuItem preview = new JMenuItem("Preview popup", new ImageIcon(Icon.MAIN.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
            preview.addActionListener(event -> previewPopup.run());
            development.add(preview);
            menu.add(development);
        }
        return menu;
    }
}
