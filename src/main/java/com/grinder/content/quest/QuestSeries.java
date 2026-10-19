package com.grinder.content.quest;
import lombok.Getter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Quest;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum QuestSeries {
    ELF("Elf", Quest.PLAGUE_CITY, Quest.BIOHAZARD, Quest.UNDERGROUND_PASS, Quest.REGICIDE, Quest.ROVING_ELVES, Quest.MOURNINGS_END_PART_I, Quest.MOURNINGS_END_PART_II, Quest.SONG_OF_THE_ELVES),
    MYREQUE("Myreque", Quest.IN_SEARCH_OF_THE_MYREQUE, Quest.IN_AID_OF_THE_MYREQUE, Quest.DARKNESS_OF_HALLOWVALE, Quest.A_TASTE_OF_HOPE, Quest.SINS_OF_THE_FATHER),
    FREMENNIK("Fremennik", Quest.THE_FREMENNIK_TRIALS, Quest.THE_FREMENNIK_ISLES, Quest.THE_FREMENNIK_EXILES),
    DORGESHUUN("Dorgeshuun", Quest.THE_LOST_TRIBE, Quest.DEATH_TO_THE_DORGESHUUN, Quest.ANOTHER_SLICE_OF_HAM, Quest.LAND_OF_THE_GOBLINS),
    FAIRY("Fairy", Quest.LOST_CITY, Quest.FAIRYTALE_I__GROWING_PAINS, Quest.FAIRYTALE_II__CURE_A_QUEEN),
    GNOME("Gnome", Quest.TREE_GNOME_VILLAGE, Quest.THE_GRAND_TREE, Quest.MONKEY_MADNESS_I, Quest.MONKEY_MADNESS_II),
    GLOUPHRIE("Glouphrie", Quest.THE_EYES_OF_GLOUPHRIE, Quest.THE_PATH_OF_GLOUPHRIE),
    DWARF("Dwarf", Quest.THE_GIANT_DWARF, Quest.FORGETTABLE_TALE, Quest.BETWEEN_A_ROCK),
    PIRATE("Pirate", Quest.PIRATES_TREASURE, Quest.RUM_DEAL, Quest.CABIN_FEVER, Quest.THE_GREAT_BRAIN_ROBBERY),
    SLUG("Sea Slug", Quest.SEA_SLUG, Quest.THE_SLUG_MENACE),
    TEMPLE_KNIGHT("Temple Knight", Quest.RECRUITMENT_DRIVE, Quest.WANTED, Quest.THE_SLUG_MENACE),
    TROLL("Troll", Quest.DEATH_PLATEAU, Quest.TROLL_STRONGHOLD, Quest.EADGARS_RUSE, Quest.MY_ARMS_BIG_ADVENTURE, Quest.MAKING_FRIENDS_WITH_MY_ARM),
    MAHJARRAT("Mahjarrat", Quest.FIGHT_ARENA, Quest.THE_DIG_SITE, Quest.TEMPLE_OF_IKOV, Quest.DESERT_TREASURE_I, Quest.A_TAIL_OF_TWO_CATS, Quest.ENAKHRAS_LAMENT, Quest.THE_GENERALS_SHADOW, Quest.DEFENDER_OF_VARROCK, Quest.THE_CURSE_OF_ARRAV, Quest.WHILE_GUTHIX_SLEEPS, Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE),
    KOUREND("Great Kourend", Quest.CLIENT_OF_KOUREND, Quest.THE_DEPTHS_OF_DESPAIR, Quest.THE_QUEEN_OF_THIEVES, Quest.THE_ASCENT_OF_ARCEUUS, Quest.THE_FORSAKEN_TOWER, Quest.TALE_OF_THE_RIGHTEOUS, Quest.A_KINGDOM_DIVIDED),
    DESERT("Desert", Quest.PRINCE_ALI_RESCUE, Quest.ICTHLARINS_LITTLE_HELPER, Quest.CONTACT, Quest.BENEATH_CURSED_SANDS),
    DRAGONKIN("Dragonkin", Quest.DRAGON_SLAYER_I, Quest.A_TAIL_OF_TWO_CATS, Quest.DRAGON_SLAYER_II),
    PENGUIN("Penguin", Quest.COLD_WAR),
    MISCELLANIA("Miscellania", Quest.THRONE_OF_MISCELLANIA, Quest.ROYAL_TROUBLE);
    private final String name;
    @Getter
    private final List<Quest> quests;
    QuestSeries(String name, Quest... quests) {
        this.name = name;
        this.quests = Collections.unmodifiableList(Arrays.asList(quests));
    }
    @Override
    public String toString() {
        return name;
    }
}
