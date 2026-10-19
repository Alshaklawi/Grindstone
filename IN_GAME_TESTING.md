# Grinder in-game acceptance checklist

Automated tests do not establish live client correctness. These checks are pending player confirmation. Never automate RuneScape input.

Launch with Java 11 using `./gradlew run`. For a Jagex Account, follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Baseline tracker verification

- Add defaults twice. Each tracker type should appear without duplicates or edited-target resets.
- Compare skill level, XP, total level, total XP, and combat level against the client.
- Compare a started quest, finished quest, and quest-series count against the quest list.
- Compare a region diary, all-region diary tier, combat reward points, difficulty task count, and boss task count against the corresponding interfaces.
- Open the collection log and compare page and singleton unlocks. First-seen dates must not claim historical drop dates.
- Check that pages from all five collection-log tabs appear in the goal picker. Try adding all pages twice; existing goals and targets must remain unchanged.
- Add an individual combat-achievement task, compare its description and completion against the client, and verify it after relogging.
- Compare available activity counters against their native interfaces. Check representative activity icons and category fallbacks.
- Set a coin target and transfer coins between inventory, bank, and supported storage. The settled total must not inflate; spending must reduce progress.
- Relog and switch accounts. Goals and history must remain isolated, and unknown bank storage must be labelled until loaded.

## New workflow verification

- Open the bank and check the bank snapshot timestamp. Close it and confirm the label says snapshot, not live or fully synchronized.
- Check update and snapshot status in the header, not repeated on goal entries.
- Switch between categorized and flat lists; test search, pins, visibility settings, and empty results in both layouts.
- Expand and collapse item and task details. Numeric progress and the progress bar must remain visible; the saved expansion choice must survive relogging.
- Try targets of 1, 7, 20, 21, and 100. At the default cap there must be no more than 20 chunks; 100 must use chunks worth 5. Try a lower configured cap too.
- Export a new backup and confirm goal types use short IDs rather than Java class paths. An older class-tagged backup must still import.
- Pin and unpin goals. Pins should appear above categories, respect filters, and survive relogging.
- Complete a small custom target, then raise it. Its status must become incomplete while completion history retains the old target and observation date.
- Change a skill, quest, boss, tier, and item through Edit goal. Cancelling must keep the original. Selecting a duplicate must not delete or overwrite either goal.
- Try each preset on an empty test profile or after exporting a backup. Apply it twice: matches must be skipped and edited targets retained.
- Export a backup, delete a disposable goal, then import the backup. Only missing goals should return; existing targets must remain unchanged.
- Import the same backup into a different account. Targets and pins may transfer, but collection observations, completion history, and manual progress must not.
- Cancel file selection, reject an overwrite, import invalid JSON or an unsupported backup version, and switch profiles during an import. No current-profile data should be lost or applied to the wrong account.

Record the client version, tracker type, expected result, actual result, and relevant debug log if anything differs.

## Recurring goals and history

- Edit an XP or activity goal, choose Daily and set a small period target. Existing XP or kills must not count; only gains after the captured baseline should advance the recurring bar. The lifetime target must remain unchanged.
- Add a custom recurring goal, advance it through Edit, and open Goal history. Current progress and first-observed completion must appear in the Periods tab, without creating a lifetime milestone for that recurring period.
- Check Daily, Weekly and Monthly section countdowns against UTC. Weekly resets on Monday and monthly on the first. Collapse sections and individual entries, then turn Group recurring goals off; search, status filters, pins and progress must remain consistent.
- Hide a recurring goal's category, make progress, then show it again. Progress must still have been recorded while Grinder was enabled.
- Leave a small goal across a UTC boundary while logged in. Allow up to 30 seconds for the idle refresh. Custom progress should reset, automatic goals should capture a new baseline, and the previous period should appear in history.
- Relog within the same period: keep the baseline and progress. Relog after a reset: capture a new baseline and do not count offline gains across the boundary. A multi-period absence must show an unobserved gap, not a fabricated run of missed goals.
- Change a schedule or period target, cancel an edit, and set Repeat to None. Changes should archive the previous observed period; cancellation must preserve it; None should restore normal lifetime tracking without deleting history.
- Sort the history tables and scroll horizontally to inspect baseline, last-observed and completion times. Rows must be read-only, newest first initially, with UTC dates and separate lifetime milestones.
- Export/import on the same account: keep repeat settings, current baseline and history. Import on another account: keep the repeat setup but clear the original character's progress and period history.
