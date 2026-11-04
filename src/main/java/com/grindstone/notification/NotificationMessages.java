package com.grindstone.notification;

import com.grindstone.content.milestone.Milestone;
import com.grindstone.content.milestone.impl.experience.BaseExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SessionExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.SkillExperienceMilestone;
import com.grindstone.content.milestone.impl.experience.TotalExperienceMilestone;
import java.util.Locale;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.util.Text;

/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/20/2026
 */
public final class NotificationMessages {
    private NotificationMessages() {
    }

    public static String formatTitle(String title) {
        String clean = Text.removeTags(title).replaceAll("[\\r\\n\\t]", " ").trim();
        if (clean.isEmpty()) {
            return "Personal milestone";
        }
        return clean.length() > 100 ? clean.substring(0, 97) + "..." : clean;
    }

    public static String formatProgress(long current, long target) {
        return target > 0 ? String.format(Locale.ROOT, " (%,d / %,d)", Math.max(0, current), target) : "";
    }

    public static String formatProgress(Milestone goal, long current, long target) {
        String numbers = formatProgress(current, target);
        if (!numbers.isEmpty() && (goal instanceof SkillExperienceMilestone || goal instanceof TotalExperienceMilestone || goal instanceof BaseExperienceMilestone || goal instanceof SessionExperienceMilestone)) {
            return numbers.substring(0, numbers.length() - 1) + " XP)";
        }
        return numbers;
    }

    public static String formatChat(String heading, String detail, boolean colours) {
        return new ChatMessageBuilder()
                .append(ChatColorType.NORMAL)
                .append("[Grindstone] ")
                .append(colours ? ChatColorType.HIGHLIGHT : ChatColorType.NORMAL)
                .append(heading)
                .append(ChatColorType.NORMAL)
                .append(": " + detail)
                .build();
    }
}
