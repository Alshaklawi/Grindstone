# Policy compatibility

Grinder is a passive, account-specific progress tracker. Its buttons change local goals, filters, schedules and history; they do not send game actions, inject input, predict boss attacks or recommend combat prayers. Combat achievements track native completion state, not live combat mechanics. Personal recurring schedules are not claims about game reset rules.

Reviewed against [Jagex's third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and [RuneLite's rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features). This is not official approval, a guarantee against future policy changes, or a substitute for Plugin Hub review.

## Storage and threading

- Gson is derived from RuneLite's injected instance. Saved type IDs use an explicit registry; profile data cannot select arbitrary Java classes.
- All plugin filesystem access uses RuneLite's scoped Filepath API and its chooser. Profiles live under `.runelite/plugin-data/grinder/profiles`.
- Directory initialization, loads, writes and backup reads run on a background queue. Client state is captured on the client thread, and asynchronous loads are checked against the current account and login generation.
- Saves use temporary files, atomic replacement where supported, and a recovery copy. Invalid primary files are preserved when a recovered profile is saved. Unrecoverable loads report an error instead of silently replacing data with defaults.
- Shutdown queues its final save without blocking RuneLite. An abrupt JVM exit can prevent pending writes from finishing; explicit saves should finish before closing the client.
- No external service is used for profile synchronization. Exported files contain private account progress; share them carefully.

## Existing profiles

RuneLite can migrate its legacy `.runelite/grinder` directory through the plugin descriptor. Older development builds stored profiles relative to their working directory instead. For those, log into the matching account and choose **Migrate legacy profile…** in the header menu, then select that account's old JSON file. Migration validates the account and asks before replacing the current profile. The source file is left untouched. Keep a separate export before replacing a valuable setup.

## Verification

`./gradlew check` runs unit tests and a source guard against common forbidden APIs. The guard is deliberately limited: it cannot certify policy compliance or in-game accuracy.

Player checks remain required: load/save and relog; account switches while loading; toggling the plugin; legacy migration and source preservation; backup export/import; recovery errors; and passive tracker updates without game actions. Follow [the in-game checklist](IN_GAME_TESTING.md) and [RuneLite's Jagex Account login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
