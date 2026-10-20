package com.grindstone.content.tracker;
import com.grindstone.content.collectionlog.CollectionLogPages;
import com.grindstone.content.milestone.MilestoneCategory;
import com.grindstone.ui.Icon;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum TrackerEntry {
    BARROWS("Barrows chests", VarPlayerID.TOTAL_BARROWS_CHESTS, MilestoneCategory.BOSSING),
    ARMADYL("Kree'Arra kills", VarPlayerID.TOTAL_ARMADYL_KILLS, MilestoneCategory.BOSSING),
    BANDOS("General Graardor kills", VarPlayerID.TOTAL_BANDOS_KILLS, MilestoneCategory.BOSSING),
    SARADOMIN("Commander Zilyana kills", VarPlayerID.TOTAL_SARADOMIN_KILLS, MilestoneCategory.BOSSING),
    ZAMORAK("K'ril Tsutsaroth kills", VarPlayerID.TOTAL_ZAMORAK_KILLS, MilestoneCategory.BOSSING),
    PRIME("Dagannoth Prime kills", VarPlayerID.TOTAL_PRIME_KILLS, MilestoneCategory.BOSSING),
    REX("Dagannoth Rex kills", VarPlayerID.TOTAL_REX_KILLS, MilestoneCategory.BOSSING),
    SUPREME("Dagannoth Supreme kills", VarPlayerID.TOTAL_SUPREME_KILLS, MilestoneCategory.BOSSING),
    CALLISTO("Callisto kills", VarPlayerID.TOTAL_CALLISTO_KILLS, MilestoneCategory.BOSSING),
    VENENATIS("Venenatis kills", VarPlayerID.TOTAL_VENENATIS_KILLS, MilestoneCategory.BOSSING),
    VETION("Vet'ion kills", VarPlayerID.TOTAL_VETION_KILLS, MilestoneCategory.BOSSING),
    CHAOSELE("Chaos Elemental kills", VarPlayerID.TOTAL_CHAOSELE_KILLS, MilestoneCategory.BOSSING),
    KBD("King Black Dragon kills", VarPlayerID.TOTAL_KBD_KILLS, MilestoneCategory.BOSSING),
    MOLE("Giant Mole kills", VarPlayerID.TOTAL_MOLE_KILLS, MilestoneCategory.BOSSING),
    KALPHITE("Kalphite Queen kills", VarPlayerID.TOTAL_KALPHITE_KILLS, MilestoneCategory.BOSSING),
    CORPOREAL_BEAST("Corporeal Beast kills", VarPlayerID.TOTAL_CORP_KILLS, MilestoneCategory.BOSSING),
    SNAKEBOSS("Zulrah kills", VarPlayerID.TOTAL_SNAKEBOSS_KILLS, MilestoneCategory.BOSSING),
    CHAOSFANATIC("Chaos Fanatic kills", VarPlayerID.TOTAL_CHAOSFANATIC_KILLS, MilestoneCategory.BOSSING),
    SCORPIA("Scorpia kills", VarPlayerID.TOTAL_SCORPIA_KILLS, MilestoneCategory.BOSSING),
    CRAZYARCHAEOLOGIST("Crazy Archaeologist kills", VarPlayerID.TOTAL_CRAZYARCHAEOLOGIST_KILLS, MilestoneCategory.BOSSING),
    JAD("TzTok-Jad kills", VarPlayerID.TOTAL_JAD_KILLS, MilestoneCategory.BOSSING),
    KRAKEN_BOSS("Kraken kills", VarPlayerID.TOTAL_KRAKEN_BOSS_KILLS, MilestoneCategory.BOSSING),
    THERMY("Thermonuclear Smoke Devil kills", VarPlayerID.TOTAL_THERMY_KILLS, MilestoneCategory.BOSSING),
    CERBERUS("Cerberus kills", VarPlayerID.TOTAL_CERBERUS_KILLS, MilestoneCategory.BOSSING),
    ABYSSALSIRE("Abyssal Sire kills", VarPlayerID.TOTAL_ABYSSALSIRE_KILLS, MilestoneCategory.BOSSING),
    CATA_BOSS("Skotizo kills", VarPlayerID.TOTAL_CATA_BOSS_KILLS, MilestoneCategory.BOSSING),
    WINTERTODT("Wintertodt kills", VarPlayerID.TOTAL_WINTERTODT_KILLS, MilestoneCategory.MINIGAMES),
    HILLGIANT_BOSS("Obor kills", VarPlayerID.TOTAL_HILLGIANT_BOSS_KILLS, MilestoneCategory.BOSSING),
    CHAMBERS_OF_XERIC("Chambers of Xeric completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS, MilestoneCategory.RAIDS),
    ZUK("TzKal-Zuk kills", VarPlayerID.TOTAL_ZUK_KILLS, MilestoneCategory.BOSSING),
    DERANGEDARCHAEOLOGIST("Deranged Archaeologist kills", VarPlayerID.TOTAL_DERANGEDARCHAEOLOGIST_KILLS, MilestoneCategory.BOSSING),
    GARGBOSS("Grotesque Guardians kills", VarPlayerID.TOTAL_GARGBOSS_KILLS, MilestoneCategory.BOSSING),
    VORKATH("Vorkath kills", VarPlayerID.TOTAL_VORKATH_KILLS, MilestoneCategory.BOSSING),
    BRYOPHYTA("Bryophyta kills", VarPlayerID.TOTAL_BRYOPHYTA_KILLS, MilestoneCategory.BOSSING),
    XERICCHAMBERS_CHALLENGE("Chambers of Xeric: Challenge Mode completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS_CHALLENGE, MilestoneCategory.RAIDS),
    THEATREOFBLOOD("Theatre of Blood completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD, MilestoneCategory.RAIDS),
    HYDRABOSS("Alchemical Hydra kills", VarPlayerID.TOTAL_HYDRABOSS_KILLS, MilestoneCategory.BOSSING),
    HESPORI("Hespori kills", VarPlayerID.TOTAL_HESPORI_KILLS, MilestoneCategory.BOSSING),
    MIMIC("Mimic kills", VarPlayerID.TOTAL_MIMIC_KILLS, MilestoneCategory.CLUES),
    SARACHNIS("Sarachnis kills", VarPlayerID.TOTAL_SARACHNIS_KILLS, MilestoneCategory.BOSSING),
    ZALCANO("Zalcano kills", VarPlayerID.TOTAL_ZALCANO_KILLS, MilestoneCategory.SKILLING),
    GAUNTLET("The Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET, MilestoneCategory.BOSSING),
    GAUNTLET_HM("The Corrupted Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM, MilestoneCategory.BOSSING),
    NIGHTMARE("Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_KILLS, MilestoneCategory.BOSSING),
    NIGHTMARE_CHALLENGE("Phosani's Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_CHALLENGE_KILLS, MilestoneCategory.BOSSING),
    LMS_SCORE("Last Man Standing score", VarPlayerID.BR_RANK, MilestoneCategory.MINIGAMES),
    SOUL_WARS_KILLS("Soul Wars kills", VarPlayerID.SOUL_WARS_TOTAL_KILLS, MilestoneCategory.MINIGAMES),
    SOUL_WARS_DEATHS("Soul Wars deaths", VarPlayerID.SOUL_WARS_TOTAL_DEATHS, MilestoneCategory.MINIGAMES),
    SOUL_WARS_GAMES("Soul Wars games", VarPlayerID.SOUL_WARS_TOTAL_GAMES, MilestoneCategory.MINIGAMES),
    SOUL_WARS_WINS("Soul Wars wins", VarPlayerID.SOUL_WARS_TOTAL_WINS, MilestoneCategory.MINIGAMES),
    TEMPOROSS("Tempoross kills", VarPlayerID.TOTAL_TEMPOROSS_KILLS, MilestoneCategory.MINIGAMES),
    THEATREOFBLOOD_HARD("Theatre of Blood: Hard Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_HARD, MilestoneCategory.RAIDS),
    THEATREOFBLOOD_STORY("Theatre of Blood: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_STORY, MilestoneCategory.RAIDS),
    NEX("Nex kills", VarPlayerID.TOTAL_NEX_KILLS, MilestoneCategory.BOSSING),
    SPEEDRUNS("Quest Speedrunning completions", VarPlayerID.TOTAL_SPEEDRUNS, MilestoneCategory.QUEST),
    GOTR("Guardians of the Rift rifts closed", VarPlayerID.TOTAL_GOTR_KILLS, MilestoneCategory.MINIGAMES),
    TOMBSOFAMASCUT_ENTRY("Tombs of Amascut: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_ENTRY, MilestoneCategory.RAIDS),
    TOMBSOFAMASCUT("Tombs of Amascut completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT, MilestoneCategory.RAIDS),
    TOMBSOFAMASCUT_EXPERT("Tombs of Amascut: Expert Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_EXPERT, MilestoneCategory.RAIDS),
    MUSPAH("Phantom Muspah kills", VarPlayerID.TOTAL_MUSPAH_KILLS, MilestoneCategory.BOSSING),
    ARTIO("Artio kills", VarPlayerID.TOTAL_ARTIO_KILLS, MilestoneCategory.BOSSING),
    SPINDEL("Spindel kills", VarPlayerID.TOTAL_SPINDEL_KILLS, MilestoneCategory.BOSSING),
    CALVARION("Calvar'ion kills", VarPlayerID.TOTAL_CALVARION_KILLS, MilestoneCategory.BOSSING),
    DUKE_SUCELLUS("Duke Sucellus kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_KILLS, MilestoneCategory.BOSSING),
    LEVIATHAN("The Leviathan kills", VarPlayerID.TOTAL_LEVIATHAN_KILLS, MilestoneCategory.BOSSING),
    WHISPERER("The Whisperer kills", VarPlayerID.TOTAL_WHISPERER_KILLS, MilestoneCategory.BOSSING),
    VARDORVIS("Vardorvis kills", VarPlayerID.TOTAL_VARDORVIS_KILLS, MilestoneCategory.BOSSING),
    VARDORVIS_AWAKENED("Vardorvis: Awakened kills", VarPlayerID.TOTAL_VARDORVIS_AWAKENED_KILLS, MilestoneCategory.BOSSING),
    WHISPERER_AWAKENED("The Whisperer: Awakened kills", VarPlayerID.TOTAL_WHISPERER_AWAKENED_KILLS, MilestoneCategory.BOSSING),
    LEVIATHAN_AWAKENED("The Leviathan: Awakened kills", VarPlayerID.TOTAL_LEVIATHAN_AWAKENED_KILLS, MilestoneCategory.BOSSING),
    DUKE_SUCELLUS_AWAKENED("Duke Sucellus: Awakened kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_AWAKENED_KILLS, MilestoneCategory.BOSSING),
    RAT_BOSS("Scurrius kills", VarPlayerID.TOTAL_RAT_BOSS_KILLS, MilestoneCategory.BOSSING),
    COLOSSEUM_WAVES("Fortis Colosseum waves completed", VarPlayerID.TOTAL_COLOSSEUM_WAVES_COMPLETED, MilestoneCategory.BOSSING),
    PERILOUS_MOONS("Lunar Chests opened", VarPlayerID.TOTAL_PMOON_CHESTS, MilestoneCategory.BOSSING),
    SOL("Sol Heredit kills", VarPlayerID.TOTAL_SOL_KILLS, MilestoneCategory.BOSSING),
    ARAXXOR("Araxxor kills", VarPlayerID.TOTAL_ARAXXOR_KILLS, MilestoneCategory.BOSSING),
    AMOXLIATL("Amoxliatl kills", VarPlayerID.TOTAL_AMOXLIATL_KILLS, MilestoneCategory.BOSSING),
    HUEY("The Hueycoatl kills", VarPlayerID.TOTAL_HUEY_KILLS, MilestoneCategory.BOSSING),
    MIXOLOGY_ORDERS("Mastering Mixology orders", VarPlayerID.TOTAL_MIXOLOGY_ORDERS, MilestoneCategory.MINIGAMES),
    ROYAL_TITAN("The Royal Titans kills", VarPlayerID.TOTAL_ROYAL_TITAN_KILLS, MilestoneCategory.BOSSING),
    YAMA("Yama kills", VarPlayerID.TOTAL_YAMA_KILLS, MilestoneCategory.BOSSING),
    DOM_LEVELS("Doom of Mokhaiotl delve levels completed", VarPlayerID.TOTAL_DOM_LEVELS, MilestoneCategory.BOSSING),
    GRYPHON_BOSS("Shellbane Gryphon kills", VarPlayerID.TOTAL_GRYPHON_BOSS_KILLS, MilestoneCategory.BOSSING),
    COWBOSS("Brutus kills", VarPlayerID.TOTAL_COWBOSS_KILLS, MilestoneCategory.BOSSING),
    COWBOSS_HARDMODE("Demonic Brutus kills", VarPlayerID.TOTAL_COWBOSS_HARDMODE_KILLS, MilestoneCategory.BOSSING),
    MAGGOT_KING("Maggot King kills", VarPlayerID.TOTAL_MAGGOT_KING_KILLS, MilestoneCategory.BOSSING),
    MAD_ANGEL("Mad Angel kills", VarPlayerID.TOTAL_MAD_ANGEL_KILLS, MilestoneCategory.BOSSING),
    SLAYER_POINTS("Slayer points", VarbitID.SLAYER_POINTS, true, 1000),
    SLAYER_STREAK("Slayer task streak (normal)", VarbitID.SLAYER_TASKS_COMPLETED, true, 100),
    SLAYER_WILDERNESS_STREAK("Slayer task streak (Wilderness)", VarbitID.SLAYER_WILDERNESS_TASKS_COMPLETED, true, 100),
    CLUES_TOTAL("Clue scrolls completed", VarPlayerID.COMPLETED_CLUES_TOTAL, MilestoneCategory.CLUES);
    private static final Map<String, TrackerEntry> BOSS_TRACKERS = new HashMap<>();
    static {
        for (TrackerEntry entry : values()) {
            if ((entry.category == MilestoneCategory.BOSSING || entry.category == MilestoneCategory.SKILLING || entry == WINTERTODT || entry == TEMPOROSS || entry == MIMIC) && entry.displayName.endsWith(" kills")) {
                BOSS_TRACKERS.put(normalized(entry.displayName.substring(0, entry.displayName.length() - 6)), entry);
            }
        }
    }
    private final String displayName;
    @Getter
    private final int id;
    @Getter
    private final MilestoneCategory category;
    @Getter
    private final boolean varbit;
    @Getter
    private final int defaultTarget;
    private int representativeItem;
    private boolean iconResolved;
    TrackerEntry(String displayName, int id, MilestoneCategory category) {
        this.displayName = displayName;
        this.id = id;
        this.category = category;
        this.varbit = false;
        this.defaultTarget = 5000;
    }
    TrackerEntry(String displayName, int id, boolean varbit, int defaultTarget) {
        this.displayName = displayName;
        this.id = id;
        this.category = MilestoneCategory.SLAYER;
        this.varbit = varbit;
        this.defaultTarget = defaultTarget;
    }
    public String getDisplayName() {
        return category == MilestoneCategory.BOSSING ? displayName.replaceFirst(" kills$", "") : displayName;
    }
    public boolean isSelectable() {
        return this != SOUL_WARS_DEATHS && this != COLOSSEUM_WAVES;
    }
    public String getDescription() {
        if (this == LMS_SCORE) {
            return "Current competitive Last Man Standing score, not wins or reward points. Your score can decrease. Play a competitive game to refresh the client reading.";
        }
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
    public int getIconItem(Client client) {
        if (category == MilestoneCategory.SLAYER || this == CLUES_TOTAL) {
            return 0;
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
                int[] items = CollectionLogPages.getItemIds(client, page.getKey());
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
