
package com.grindstone.content.goal;

import com.grindstone.config.GrindstoneConfig;
import com.grindstone.ui.Icon;
import com.grindstone.ui.components.GoalComponent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public interface Goal {
    default boolean supportsTarget() {
        return false;
    }

    default void setTarget(long target) {
        throw new UnsupportedOperationException("This goal tracks completion rather than a target");
    }

    default String getTitle(Client client) {
        return getClass().getSimpleName();
    }

    default String getDescription() {
        return "";
    }

    default String getCardDescription() {
        return getDescription();
    }

    default BufferedImage getIcon(Client client) {
        return Icon.MAIN.getImage();
    }

    default GoalStatus getStatus() {
        return GoalStatus.NOT_STARTED;
    }

    default GoalCategory getCategory() {
        return GoalCategory.fromName(getClass().getSimpleName());
    }

    default boolean hasProgress() {
        return false;
    }

    default long getProgressCurrent() {
        return 0;
    }

    default long getProgressMaximum() {
        return 0;
    }

    default String getProgressText() {
        return hasProgress() ? getProgressCurrent() + " / " + getProgressMaximum() : "";
    }

    default Supplier<GoalComponent> prepare(Client client, GrindstoneConfig config) {
        String title = getTitle(client);
        String description = getCardDescription();
        BufferedImage icon = getIcon(client);
        boolean progress = hasProgress();
        long current = getProgressCurrent();
        long maximum = getProgressMaximum();
        String text = getProgressText();
        String status = getStatus().name();
        boolean showIcon = config == null || config.showGoalIcons();
        boolean showBar = config == null || config.showProgressBar();
        return () -> {
            GoalComponent component = new GoalComponent();
            component.setTitle(title);
            component.setDescription(description);
            component.setIcon(icon);
            if (progress) {
                component.setProgress(current, maximum, text);
            } else {
                component.setProgressStatus(status);
            }
            component.setIconVisible(showIcon);
            component.setProgressBarVisible(showBar);
            return component;
        };
    }

    default void onItemContainerUpdated(Client client, ItemContainer container) {
    }

    default boolean refresh(Client client) {
        return false;
    }

    default boolean isAffectedBy(ItemContainer container) {
        return container != null;
    }
}
