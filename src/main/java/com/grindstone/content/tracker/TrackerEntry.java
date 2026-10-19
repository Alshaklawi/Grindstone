package com.grindstone.content.tracker;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.goal.GoalCategory;
import com.grindstone.ui.Icon;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum TrackerEntry {
    BARROWS("Barrows chests", VarPlayerID.TOTAL_BARROWS_CHESTS, GoalCategory.BOSSING),
    ARMADYL("Kree'Arra kills", VarPlayerID.TOTAL_ARMADYL_KILLS, GoalCategory.BOSSING),
    BANDOS("General Graardor kills", VarPlayerID.TOTAL_BANDOS_KILLS, GoalCategory.BOSSING),
    SARADOMIN("Commander Zilyana kills", VarPlayerID.TOTAL_SARADOMIN_KILLS, GoalCategory.BOSSING),
    ZAMORAK("K'ril Tsutsaroth kills", VarPlayerID.TOTAL_ZAMORAK_KILLS, GoalCategory.BOSSING),
    PRIME("Dagannoth Prime kills", VarPlayerID.TOTAL_PRIME_KILLS, GoalCategory.BOSSING),
    REX("Dagannoth Rex kills", VarPlayerID.TOTAL_REX_KILLS, GoalCategory.BOSSING),
    SUPREME("Dagannoth Supreme kills", VarPlayerID.TOTAL_SUPREME_KILLS, GoalCategory.BOSSING),
    CALLISTO("Callisto kills", VarPlayerID.TOTAL_CALLISTO_KILLS, GoalCategory.BOSSING),
    VENENATIS("Venenatis kills", VarPlayerID.TOTAL_VENENATIS_KILLS, GoalCategory.BOSSING),
    VETION("Vet'ion kills", VarPlayerID.TOTAL_VETION_KILLS, GoalCategory.BOSSING),
    CHAOSELE("Chaos Elemental kills", VarPlayerID.TOTAL_CHAOSELE_KILLS, GoalCategory.BOSSING),
    KBD("King Black Dragon kills", VarPlayerID.TOTAL_KBD_KILLS, GoalCategory.BOSSING),
    MOLE("Giant Mole kills", VarPlayerID.TOTAL_MOLE_KILLS, GoalCategory.BOSSING),
    KALPHITE("Kalphite Queen kills", VarPlayerID.TOTAL_KALPHITE_KILLS, GoalCategory.BOSSING),
    CORPOREAL_BEAST("Corporeal Beast kills", VarPlayerID.TOTAL_CORP_KILLS, GoalCategory.BOSSING),
    SNAKEBOSS("Zulrah kills", VarPlayerID.TOTAL_SNAKEBOSS_KILLS, GoalCategory.BOSSING),
    CHAOSFANATIC("Chaos Fanatic kills", VarPlayerID.TOTAL_CHAOSFANATIC_KILLS, GoalCategory.BOSSING),
    SCORPIA("Scorpia kills", VarPlayerID.TOTAL_SCORPIA_KILLS, GoalCategory.BOSSING),
    CRAZYARCHAEOLOGIST("Crazy Archaeologist kills", VarPlayerID.TOTAL_CRAZYARCHAEOLOGIST_KILLS, GoalCategory.BOSSING),
    JAD("TzTok-Jad kills", VarPlayerID.TOTAL_JAD_KILLS, GoalCategory.BOSSING),
    KRAKEN_BOSS("Kraken kills", VarPlayerID.TOTAL_KRAKEN_BOSS_KILLS, GoalCategory.BOSSING),
    THERMY("Thermonuclear Smoke Devil kills", VarPlayerID.TOTAL_THERMY_KILLS, GoalCategory.BOSSING),
    CERBERUS("Cerberus kills", VarPlayerID.TOTAL_CERBERUS_KILLS, GoalCategory.BOSSING),
    ABYSSALSIRE("Abyssal Sire kills", VarPlayerID.TOTAL_ABYSSALSIRE_KILLS, GoalCategory.BOSSING),
    CATA_BOSS("Skotizo kills", VarPlayerID.TOTAL_CATA_BOSS_KILLS, GoalCategory.BOSSING),
    WINTERTODT("Wintertodt kills", VarPlayerID.TOTAL_WINTERTODT_KILLS, GoalCategory.SKILLING),
    HILLGIANT_BOSS("Obor kills", VarPlayerID.TOTAL_HILLGIANT_BOSS_KILLS, GoalCategory.BOSSING),
    CHAMBERS_OF_XERIC("Chambers of Xeric completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS, GoalCategory.RAIDS),
    ZUK("TzKal-Zuk kills", VarPlayerID.TOTAL_ZUK_KILLS, GoalCategory.BOSSING),
    DERANGEDARCHAEOLOGIST("Deranged Archaeologist kills", VarPlayerID.TOTAL_DERANGEDARCHAEOLOGIST_KILLS, GoalCategory.BOSSING),
    GARGBOSS("Grotesque Guardians kills", VarPlayerID.TOTAL_GARGBOSS_KILLS, GoalCategory.BOSSING),
    VORKATH("Vorkath kills", VarPlayerID.TOTAL_VORKATH_KILLS, GoalCategory.BOSSING),
    BRYOPHYTA("Bryophyta kills", VarPlayerID.TOTAL_BRYOPHYTA_KILLS, GoalCategory.BOSSING),
    XERICCHAMBERS_CHALLENGE("Chambers of Xeric: Challenge Mode completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS_CHALLENGE, GoalCategory.RAIDS),
    THEATREOFBLOOD("Theatre of Blood completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD, GoalCategory.RAIDS),
    HYDRABOSS("Alchemical Hydra kills", VarPlayerID.TOTAL_HYDRABOSS_KILLS, GoalCategory.BOSSING),
    HESPORI("Hespori kills", VarPlayerID.TOTAL_HESPORI_KILLS, GoalCategory.BOSSING),
    MIMIC("Mimic kills", VarPlayerID.TOTAL_MIMIC_KILLS, GoalCategory.BOSSING),
    SARACHNIS("Sarachnis kills", VarPlayerID.TOTAL_SARACHNIS_KILLS, GoalCategory.BOSSING),
    ZALCANO("Zalcano kills", VarPlayerID.TOTAL_ZALCANO_KILLS, GoalCategory.SKILLING),
    GAUNTLET("The Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET, GoalCategory.BOSSING),
    GAUNTLET_HM("The Corrupted Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM, GoalCategory.BOSSING),
    NIGHTMARE("Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_KILLS, GoalCategory.BOSSING),
    NIGHTMARE_CHALLENGE("Phosani's Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_CHALLENGE_KILLS, GoalCategory.BOSSING),
    SOUL_WARS_KILLS("Soul Wars kills", VarPlayerID.SOUL_WARS_TOTAL_KILLS, GoalCategory.MINIGAMES),
    SOUL_WARS_DEATHS("Soul Wars deaths", VarPlayerID.SOUL_WARS_TOTAL_DEATHS, GoalCategory.MINIGAMES),
    SOUL_WARS_GAMES("Soul Wars games", VarPlayerID.SOUL_WARS_TOTAL_GAMES, GoalCategory.MINIGAMES),
    SOUL_WARS_WINS("Soul Wars wins", VarPlayerID.SOUL_WARS_TOTAL_WINS, GoalCategory.MINIGAMES),
    TEMPOROSS("Tempoross kills", VarPlayerID.TOTAL_TEMPOROSS_KILLS, GoalCategory.SKILLING),
    THEATREOFBLOOD_HARD("Theatre of Blood: Hard Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_HARD, GoalCategory.RAIDS),
    THEATREOFBLOOD_STORY("Theatre of Blood: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_STORY, GoalCategory.RAIDS),
    NEX("Nex kills", VarPlayerID.TOTAL_NEX_KILLS, GoalCategory.BOSSING),
    SPEEDRUNS("Quest Speedrunning completions", VarPlayerID.TOTAL_SPEEDRUNS, GoalCategory.QUEST),
    GOTR("Guardians of the Rift rifts closed", VarPlayerID.TOTAL_GOTR_KILLS, GoalCategory.MINIGAMES),
    TOMBSOFAMASCUT_ENTRY("Tombs of Amascut: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_ENTRY, GoalCategory.RAIDS),
    TOMBSOFAMASCUT("Tombs of Amascut completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT, GoalCategory.RAIDS),
    TOMBSOFAMASCUT_EXPERT("Tombs of Amascut: Expert Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_EXPERT, GoalCategory.RAIDS),
    MUSPAH("Phantom Muspah kills", VarPlayerID.TOTAL_MUSPAH_KILLS, GoalCategory.BOSSING),
    ARTIO("Artio kills", VarPlayerID.TOTAL_ARTIO_KILLS, GoalCategory.BOSSING),
    SPINDEL("Spindel kills", VarPlayerID.TOTAL_SPINDEL_KILLS, GoalCategory.BOSSING),
    CALVARION("Calvar'ion kills", VarPlayerID.TOTAL_CALVARION_KILLS, GoalCategory.BOSSING),
    DUKE_SUCELLUS("Duke Sucellus kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_KILLS, GoalCategory.BOSSING),
    LEVIATHAN("The Leviathan kills", VarPlayerID.TOTAL_LEVIATHAN_KILLS, GoalCategory.BOSSING),
    WHISPERER("The Whisperer kills", VarPlayerID.TOTAL_WHISPERER_KILLS, GoalCategory.BOSSING),
    VARDORVIS("Vardorvis kills", VarPlayerID.TOTAL_VARDORVIS_KILLS, GoalCategory.BOSSING),
    VARDORVIS_AWAKENED("Vardorvis: Awakened kills", VarPlayerID.TOTAL_VARDORVIS_AWAKENED_KILLS, GoalCategory.BOSSING),
    WHISPERER_AWAKENED("The Whisperer: Awakened kills", VarPlayerID.TOTAL_WHISPERER_AWAKENED_KILLS, GoalCategory.BOSSING),
    LEVIATHAN_AWAKENED("The Leviathan: Awakened kills", VarPlayerID.TOTAL_LEVIATHAN_AWAKENED_KILLS, GoalCategory.BOSSING),
    DUKE_SUCELLUS_AWAKENED("Duke Sucellus: Awakened kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_AWAKENED_KILLS, GoalCategory.BOSSING),
    RAT_BOSS("Scurrius kills", VarPlayerID.TOTAL_RAT_BOSS_KILLS, GoalCategory.BOSSING),
    COLOSSEUM_WAVES("Fortis Colosseum waves completed", VarPlayerID.TOTAL_COLOSSEUM_WAVES_COMPLETED, GoalCategory.MINIGAMES),
    PERILOUS_MOONS("Lunar Chests opened", VarPlayerID.TOTAL_PMOON_CHESTS, GoalCategory.BOSSING),
    SOL("Sol Heredit kills", VarPlayerID.TOTAL_SOL_KILLS, GoalCategory.BOSSING),
    ARAXXOR("Araxxor kills", VarPlayerID.TOTAL_ARAXXOR_KILLS, GoalCategory.BOSSING),
    AMOXLIATL("Amoxliatl kills", VarPlayerID.TOTAL_AMOXLIATL_KILLS, GoalCategory.BOSSING),
    HUEY("The Hueycoatl kills", VarPlayerID.TOTAL_HUEY_KILLS, GoalCategory.BOSSING),
    MIXOLOGY_ORDERS("Mastering Mixology orders", VarPlayerID.TOTAL_MIXOLOGY_ORDERS, GoalCategory.MINIGAMES),
    ROYAL_TITAN("The Royal Titans kills", VarPlayerID.TOTAL_ROYAL_TITAN_KILLS, GoalCategory.BOSSING),
    YAMA("Yama kills", VarPlayerID.TOTAL_YAMA_KILLS, GoalCategory.BOSSING),
    DOM_LEVELS("Doom of Mokhaiotl delve levels completed", VarPlayerID.TOTAL_DOM_LEVELS, GoalCategory.BOSSING),
    GRYPHON_BOSS("Shellbane Gryphon kills", VarPlayerID.TOTAL_GRYPHON_BOSS_KILLS, GoalCategory.BOSSING),
    COWBOSS("Brutus kills", VarPlayerID.TOTAL_COWBOSS_KILLS, GoalCategory.BOSSING),
    COWBOSS_HARDMODE("Demonic Brutus kills", VarPlayerID.TOTAL_COWBOSS_HARDMODE_KILLS, GoalCategory.BOSSING),
    MAGGOT_KING("Maggot King kills", VarPlayerID.TOTAL_MAGGOT_KING_KILLS, GoalCategory.BOSSING),
    MAD_ANGEL("Mad Angel kills", VarPlayerID.TOTAL_MAD_ANGEL_KILLS, GoalCategory.BOSSING),
    SLAYER_POINTS("Slayer points", VarbitID.SLAYER_POINTS, true, 1000),
    SLAYER_STREAK("Slayer task streak (normal)", VarbitID.SLAYER_TASKS_COMPLETED, true, 100),
    SLAYER_WILDERNESS_STREAK("Slayer task streak (Wilderness)", VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED, true, 100),
    CLUES_TOTAL("Clue scrolls completed", VarPlayerID.COMPLETED_CLUES_TOTAL, GoalCategory.CLUES);
    private static final Map<String, TrackerEntry> BOSS_TRACKERS = new HashMap<>();
    static {
        for (TrackerEntry entry : values()) {
            if ((entry.category == GoalCategory.BOSSING || entry.category == GoalCategory.SKILLING) && entry.displayName.endsWith(" kills")) {
                BOSS_TRACKERS.put(normalized(entry.displayName.substring(0, entry.displayName.length() - 6)), entry);
            }
        }
    }
    @Getter
    private final String displayName;
    @Getter
    private final int id;
    @Getter
    private final GoalCategory category;
    @Getter
    private final boolean varbit;
    @Getter
    private final int defaultTarget;
    private int representativeItem;
    private boolean iconResolved;
    TrackerEntry(String displayName, int id, GoalCategory category) {
        this.displayName = displayName;
        this.id = id;
        this.category = category;
        this.varbit = false;
        this.defaultTarget = 5000;
    }
    TrackerEntry(String displayName, int id, boolean varbit, int defaultTarget) {
        this.displayName = displayName;
        this.id = id;
        this.category = GoalCategory.SLAYER;
        this.varbit = varbit;
        this.defaultTarget = defaultTarget;
        this.representativeItem = ItemID.SLAYER_HELM;
    }
    public String getDescription() {
        if (this == SLAYER_POINTS) {
            return "Current unspent Slayer reward points, not lifetime points earned. Spending points reduces progress. Open Slayer rewards to refresh the client reading.";
        }
        if (this == SLAYER_STREAK || this == SLAYER_WILDERNESS_STREAK) {
            return "Current " + (this == SLAYER_STREAK ? "normal" : "Wilderness") + " Slayer task streak, tracked separately. Resetting this streak reduces progress; this is not a lifetime task total. Check your Slayer information or complete a task to refresh the client reading.";
        }
        return "";
    }
    public Icon getIcon() {
        return category.getIcon();
    }
    public static TrackerEntry forBossName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return BOSS_TRACKERS.get(normalized(name));
    }
    public int getCount(Client client) {
        return readCount(client::getVarpValue, client::getVarbitValue);
    }
    int readCount(IntUnaryOperator varps, IntUnaryOperator varbits) {
        return Math.max(0, (varbit ? varbits : varps).applyAsInt(id));
    }
    public int iconItem(Client client) {
        if (this == CLUES_TOTAL) {
            return ItemID.TRAIL_REWARD_CASKET_MASTER;
        }
        if (representativeItem > 0 || iconResolved || client == null) {
            return representativeItem;
        }
        Map<Integer, String> pages = CollectionLogPages.discover(client);
        if (!pages.isEmpty()) {
            iconResolved = true;
        }
        String name = normalized(displayName);
        int best = 0;
        for (Map.Entry<Integer, String> page : pages.entrySet()) {
            String candidate = normalized(page.getValue());
            if (candidate.length() > best && (name.startsWith(candidate) || candidate.startsWith(name))) {
                int[] items = CollectionLogPages.itemIds(client, page.getKey());
                if (items.length > 0) {
                    representativeItem = items[0];
                    best = candidate.length();
                }
            }
        }
        return representativeItem;
    }
    private static String normalized(String text) {
        return text.toLowerCase(Locale.ROOT).replaceFirst("^the ", "").replaceAll("[^a-z0-9]", "");
    }
}
