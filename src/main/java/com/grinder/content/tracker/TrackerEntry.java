package com.grinder.content.tracker;
import com.grinder.ui.Icon;
import com.grinder.content.goal.GoalCategory;
import com.grinder.content.collectionlog.CollectionLogPages;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarPlayerID;
/**
 * @author Ayman Yassen
 * @see <a href="https://discord.gg/l.e.g.e.n.d.a.r.y">Discord</a>
 * @since 10/18/2026
 */
public enum TrackerEntry {
    BARROWS("Barrows chests", VarPlayerID.TOTAL_BARROWS_CHESTS, GoalCategory.BOSSING),
    ARMADYL("Kree'arra kills", VarPlayerID.TOTAL_ARMADYL_KILLS, GoalCategory.BOSSING),
    BANDOS("General Graardor kills", VarPlayerID.TOTAL_BANDOS_KILLS, GoalCategory.BOSSING),
    SARADOMIN("Commander Zilyana kills", VarPlayerID.TOTAL_SARADOMIN_KILLS, GoalCategory.BOSSING),
    ZAMORAK("K'ril Tsutsaroth kills", VarPlayerID.TOTAL_ZAMORAK_KILLS, GoalCategory.BOSSING),
    PRIME("Dagannoth Prime kills", VarPlayerID.TOTAL_PRIME_KILLS, GoalCategory.BOSSING),
    REX("Dagannoth Rex kills", VarPlayerID.TOTAL_REX_KILLS, GoalCategory.BOSSING),
    SUPREME("Dagannoth Supreme kills", VarPlayerID.TOTAL_SUPREME_KILLS, GoalCategory.BOSSING),
    CALLISTO("Callisto kills", VarPlayerID.TOTAL_CALLISTO_KILLS, GoalCategory.BOSSING),
    VENENATIS("Venenatis kills", VarPlayerID.TOTAL_VENENATIS_KILLS, GoalCategory.BOSSING),
    VETION("Vetion kills", VarPlayerID.TOTAL_VETION_KILLS, GoalCategory.BOSSING),
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
    CHAMBERS_OF_XERIC("Chambers of Xeric completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS, GoalCategory.BOSSING),
    ZUK("TzKal-Zuk kills", VarPlayerID.TOTAL_ZUK_KILLS, GoalCategory.BOSSING),
    DERANGEDARCHAEOLOGIST("Deranged Archaeologist kills", VarPlayerID.TOTAL_DERANGEDARCHAEOLOGIST_KILLS, GoalCategory.BOSSING),
    GARGBOSS("Grotesque Guardians kills", VarPlayerID.TOTAL_GARGBOSS_KILLS, GoalCategory.BOSSING),
    VORKATH("Vorkath kills", VarPlayerID.TOTAL_VORKATH_KILLS, GoalCategory.BOSSING),
    BRYOPHYTA("Bryophyta kills", VarPlayerID.TOTAL_BRYOPHYTA_KILLS, GoalCategory.BOSSING),
    XERICCHAMBERS_CHALLENGE("Chambers of Xeric: Challenge Mode completions", VarPlayerID.TOTAL_COMPLETED_XERICCHAMBERS_CHALLENGE, GoalCategory.BOSSING),
    THEATREOFBLOOD("Theatre of Blood completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD, GoalCategory.BOSSING),
    HYDRABOSS("Alchemical Hydra kills", VarPlayerID.TOTAL_HYDRABOSS_KILLS, GoalCategory.BOSSING),
    HESPORI("Hespori kills", VarPlayerID.TOTAL_HESPORI_KILLS, GoalCategory.BOSSING),
    MIMIC("Mimic kills", VarPlayerID.TOTAL_MIMIC_KILLS, GoalCategory.BOSSING),
    SARACHNIS("Sarachnis kills", VarPlayerID.TOTAL_SARACHNIS_KILLS, GoalCategory.BOSSING),
    ZALCANO("Zalcano kills", VarPlayerID.TOTAL_ZALCANO_KILLS, GoalCategory.SKILLING),
    GAUNTLET("Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET, GoalCategory.BOSSING),
    GAUNTLET_HM("Corrupted Gauntlet completions", VarPlayerID.TOTAL_COMPLETED_GAUNTLET_HM, GoalCategory.BOSSING),
    NIGHTMARE("Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_KILLS, GoalCategory.BOSSING),
    NIGHTMARE_CHALLENGE("Phosani's Nightmare kills", VarPlayerID.TOTAL_NIGHTMARE_CHALLENGE_KILLS, GoalCategory.BOSSING),
    SOUL_WARS_KILLS("Soul Wars kills", VarPlayerID.SOUL_WARS_TOTAL_KILLS, GoalCategory.OTHER),
    SOUL_WARS_DEATHS("Soul Wars deaths", VarPlayerID.SOUL_WARS_TOTAL_DEATHS, GoalCategory.OTHER),
    SOUL_WARS_GAMES("Soul Wars games", VarPlayerID.SOUL_WARS_TOTAL_GAMES, GoalCategory.OTHER),
    SOUL_WARS_WINS("Soul Wars wins", VarPlayerID.SOUL_WARS_TOTAL_WINS, GoalCategory.OTHER),
    TEMPOROSS("Tempoross kills", VarPlayerID.TOTAL_TEMPOROSS_KILLS, GoalCategory.SKILLING),
    THEATREOFBLOOD_HARD("Theatre of Blood: Hard Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_HARD, GoalCategory.BOSSING),
    THEATREOFBLOOD_STORY("Theatre of Blood: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_THEATREOFBLOOD_STORY, GoalCategory.BOSSING),
    NEX("Nex kills", VarPlayerID.TOTAL_NEX_KILLS, GoalCategory.BOSSING),
    SPEEDRUNS("Speedruns completed", VarPlayerID.TOTAL_SPEEDRUNS, GoalCategory.OTHER),
    GOTR("Guardians of the Rift kills", VarPlayerID.TOTAL_GOTR_KILLS, GoalCategory.SKILLING),
    TOMBSOFAMASCUT_ENTRY("Tombs of Amascut: Entry Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_ENTRY, GoalCategory.BOSSING),
    TOMBSOFAMASCUT("Tombs of Amascut completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT, GoalCategory.BOSSING),
    TOMBSOFAMASCUT_EXPERT("Tombs of Amascut: Expert Mode completions", VarPlayerID.TOTAL_COMPLETED_TOMBSOFAMASCUT_EXPERT, GoalCategory.BOSSING),
    MUSPAH("Phantom Muspah kills", VarPlayerID.TOTAL_MUSPAH_KILLS, GoalCategory.BOSSING),
    ARTIO("Artio kills", VarPlayerID.TOTAL_ARTIO_KILLS, GoalCategory.BOSSING),
    SPINDEL("Spindel kills", VarPlayerID.TOTAL_SPINDEL_KILLS, GoalCategory.BOSSING),
    CALVARION("Calvarion kills", VarPlayerID.TOTAL_CALVARION_KILLS, GoalCategory.BOSSING),
    DUKE_SUCELLUS("Duke Sucellus kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_KILLS, GoalCategory.BOSSING),
    LEVIATHAN("Leviathan kills", VarPlayerID.TOTAL_LEVIATHAN_KILLS, GoalCategory.BOSSING),
    WHISPERER("Whisperer kills", VarPlayerID.TOTAL_WHISPERER_KILLS, GoalCategory.BOSSING),
    VARDORVIS("Vardorvis kills", VarPlayerID.TOTAL_VARDORVIS_KILLS, GoalCategory.BOSSING),
    VARDORVIS_AWAKENED("Vardorvis: Awakened kills", VarPlayerID.TOTAL_VARDORVIS_AWAKENED_KILLS, GoalCategory.BOSSING),
    WHISPERER_AWAKENED("Whisperer: Awakened kills", VarPlayerID.TOTAL_WHISPERER_AWAKENED_KILLS, GoalCategory.BOSSING),
    LEVIATHAN_AWAKENED("Leviathan: Awakened kills", VarPlayerID.TOTAL_LEVIATHAN_AWAKENED_KILLS, GoalCategory.BOSSING),
    DUKE_SUCELLUS_AWAKENED("Duke Sucellus: Awakened kills", VarPlayerID.TOTAL_DUKE_SUCELLUS_AWAKENED_KILLS, GoalCategory.BOSSING),
    RAT_BOSS("Scurrius kills", VarPlayerID.TOTAL_RAT_BOSS_KILLS, GoalCategory.BOSSING),
    COLOSSEUM_WAVES("Colosseum waves completed", VarPlayerID.TOTAL_COLOSSEUM_WAVES_COMPLETED, GoalCategory.BOSSING),
    PERILOUS_MOONS("Perilous Moons chests", VarPlayerID.TOTAL_PMOON_CHESTS, GoalCategory.BOSSING),
    SOL("Sol Heredit kills", VarPlayerID.TOTAL_SOL_KILLS, GoalCategory.BOSSING),
    ARAXXOR("Araxxor kills", VarPlayerID.TOTAL_ARAXXOR_KILLS, GoalCategory.BOSSING),
    AMOXLIATL("Amoxliatl kills", VarPlayerID.TOTAL_AMOXLIATL_KILLS, GoalCategory.BOSSING),
    HUEY("Hueycoatl kills", VarPlayerID.TOTAL_HUEY_KILLS, GoalCategory.BOSSING),
    MIXOLOGY_ORDERS("Mastering Mixology orders", VarPlayerID.TOTAL_MIXOLOGY_ORDERS, GoalCategory.SKILLING),
    ROYAL_TITAN("Royal Titans kills", VarPlayerID.TOTAL_ROYAL_TITAN_KILLS, GoalCategory.BOSSING),
    YAMA("Yama kills", VarPlayerID.TOTAL_YAMA_KILLS, GoalCategory.BOSSING),
    DOM_LEVELS("Delve levels completed", VarPlayerID.TOTAL_DOM_LEVELS, GoalCategory.BOSSING),
    GEMSTONE_CRAB("Gemstone Crab kills", VarPlayerID.TOTAL_GEMSTONE_CRAB_KILLS, GoalCategory.SKILLING),
    GRYPHON_BOSS("Gryphon boss kills", VarPlayerID.TOTAL_GRYPHON_BOSS_KILLS, GoalCategory.BOSSING),
    COWBOSS("Cow boss kills", VarPlayerID.TOTAL_COWBOSS_KILLS, GoalCategory.BOSSING),
    COWBOSS_HARDMODE("Cow boss: Hard Mode kills", VarPlayerID.TOTAL_COWBOSS_HARDMODE_KILLS, GoalCategory.BOSSING),
    MAGGOT_KING("Maggot King kills", VarPlayerID.TOTAL_MAGGOT_KING_KILLS, GoalCategory.BOSSING),
    MAD_ANGEL("Mad Angel kills", VarPlayerID.TOTAL_MAD_ANGEL_KILLS, GoalCategory.BOSSING);
    private final String displayName;
    private final int id;
    private final GoalCategory category;
    private int representativeItem;
    private boolean iconResolved;
    TrackerEntry(String displayName, int id, GoalCategory category) {
        this.displayName = displayName;
        this.id = id;
        this.category = category;
    }
    public int getId() {
        return id;
    }
    public String getDisplayName() {
        return displayName;
    }
    public GoalCategory getCategory() {
        return category;
    }
    public Icon getIcon() {
        return category == GoalCategory.BOSSING ? Icon.BOSSING : category == GoalCategory.SKILLING ? Icon.SKILLING : Icon.MAIN;
    }
    public int getCount(Client client) {
        return Math.max(0, client.getVarpValue(id));
    }
    public int iconItem(Client client) {
        if (representativeItem > 0 || iconResolved || client == null) {
            return representativeItem;
        }
        java.util.Map<Integer, String> pages = CollectionLogPages.discover(client);
        if (!pages.isEmpty()) {
            iconResolved = true;
        }
        String name = normalized(displayName);
        int best = 0;
        for (java.util.Map.Entry<Integer, String> page : pages.entrySet()) {
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
        return text.toLowerCase(java.util.Locale.ROOT).replaceFirst("^the ", "").replaceAll("[^a-z0-9]", "");
    }
}
