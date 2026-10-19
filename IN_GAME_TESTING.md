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

## Base goals and expanded workflows

- Add base level and base XP goals with targets above your lowest skill. Check against every skill: the minimum reading must drive the bar, and the number of skills meeting the target must be correct. Training a non-lowest skill must not falsely raise the minimum. Boosts must not affect base levels. Edit the target and check the qualifying-skill count immediately. Try level 1/99 and XP 1/200m, then reject out-of-range values. Save/relog and verify targets remain.
- Add per-skill and total session-XP goals. Existing XP must count as zero gains; manually earn XP, edit the target, and confirm gains are preserved. Restart one goal and check its new baseline. Relog/reconnect and confirm the session restarts while completion history and target settings remain. Session state must not transfer through backups.
- Add a manual checklist with a long step name, then check/uncheck steps. Test narrow sidebar width, collapsed details, undo, cancellation and editing notes without losing checks. Make it daily or weekly: all steps must be required, resets must clear checks and archive progress. Leave the checkbox UI open across a UTC reset; an old-period click must not complete the new period. Back up / restore on the same account, then test another account: checks must be cleared there.
- Show example goals from the header menu. Inspect every linked mode, including the sample daily checklist and its repeat-completion goal. Hide examples without deleting them. Promote an example, restore defaults twice, and confirm the promoted goal remains visible and is not duplicated.
- Create a linked all-of checklist and an any-of goal from small manual trackers. Exercise partially progressed sources, enough completed sources, unavailable data and duplicate source selection. Any-of must complete after the required count, even if another source is missing. All-of must not falsely complete with a missing source.
- Create a ladder on a skill or manual source with three increasing milestones. Advance through them and edit only its name/notes; its identity and history must remain. Reject duplicate, decreasing or nonpositive milestones. Reordering equivalent checklist source rows must not replace its identity or lose observed achievements.
- Create a collection subset from individual collection-item goals. Open the native collection log and verify newly observed unlocks update it without needing to reopen the plugin. Owned inventory quantities must not substitute for unlock observations.
- Create a spending-safe owned-item milestone with a small target. Load the relevant storage, reach the target, then manually spend/move the items and refresh storage. The milestone observation must remain. A new goal must not invent unobserved historical wealth. A cross-account import must clear the original account's achievement observations.
- Count completed recurring periods from a daily checklist. Repeated views and repeated checking within the same period must not count additional periods. Cross a reset and complete the next period. Save/relog and verify the observed count remains; unobserved offline periods must not be counted as completed.
- Create a prerequisite goal. The first/main source's threshold must survive reopening its editor; pending prerequisites must prevent completion. Fulfilling all prerequisites and the main threshold must complete it. No game action or interface unlock must occur. Deleting a source must show unavailability; undo its deletion or choose a replacement. Nested/cyclic linked references in an imported file must be rejected.
- Review a new goal and an existing subject/schedule match. Cancel at each preview: no data should change. Default and preset previews must show additions separately from preserved matches. Edited targets must survive adding a pack twice.
- Open bulk management, search, select visible rows, change search and verify hidden selections are still counted. Clear selections must clear hidden rows too. Test pin/unpin, display categories and original-category restoration, examples/promotion, deadlines, deletion, and one-step undo. Deletion undo must restore order and history without duplicates.
- Try a bulk schedule change containing an unsupported goal, and another containing duplicate destinations. Neither may change any selected goal. Apply a valid schedule to custom goals and checklists; manual progress resets appropriately, checklist period targets stay equal to step count, and undo restores previous settings/progress. Switch account or make another edit while the manager is open; stale changes must be rejected.
- Set a deadline for today in UTC with a header reminder. Verify its end-of-day boundary, optional reminder and global hide setting. Try a past date, invalid date, removal and cancellation. Passing a deadline must not delete or reset progress. Idle deadline status can take up to 30 seconds to refresh while logged in.
- Toggle related-skill grouping and categorized/flat modes. Check searches, pinning, recurring sections, long names, tooltips, fonts and scroll position. Every target must remain individually editable and deletable.

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

## Dashboard, controls and safe changes

- Add Woodcutting XP lifetime, daily, weekly and monthly goals together, then try adding each again. The four schedules must remain independent; duplicate subject/schedule pairs must be skipped. Save and relog, then export/import on the same account.
- Switch Today / All goals. Today should contain recurring goals, with next-reset ordering and a known-reading progress overview. Test pins, grouped/flat layouts, filters and no-routine/empty-result states.
- Add a daily and weekly routine from the header menu. Uncheck an entry and edit a target before confirming. Add the same routine again: it must preserve existing matches and targets. Cancel a preview: nothing should change.
- Use a custom checkbox, rapidly click +1 on a larger target, then use Set progress. Counts must stay within the target; progress controls and labels must remain visible when details collapse. Undo should restore the latest manual change.
- Preview and cancel a subject, target or schedule edit. Apply one, then undo. Changing a subject must retain the previous subject's observed history in account history.
- Restart a recurring period after making progress: verify the archived record, new baseline and reset custom progress. Undo before another edit/reset; after a later edit or reset it must safely expire.
- Leave Set progress or a custom-goal editor open across a scheduled reset. Saving old-period progress must be rejected with a prompt to reopen the dialog. A recurring custom editor should expose only the effective period target, not two competing target fields.
- Undo a routine, default/preset pack, bulk collection-log addition and deletion. Only the intended additions or last change should be reverted, not unrelated live progress.
- In account history, filter by title/outcome, schedule and overlapping UTC dates; try invalid and reversed date ranges. Check summaries, numeric sorting and read-only rows. Go to an active goal, including a completed one. Archived records must not offer a broken live-goal link.
- Delete a disposable goal with observed history, relog and open History. Its archive should remain. Undoing a deletion should restore the goal without duplicating its archive. Cross-account imports must not transfer archived history.
- Check reliability labels for manual goals, XP, activity counters, bank-not-loaded items, bank snapshots, collection-log observations and late baselines. Waiting readings must not be presented as confirmed zeros or included in the average.
- Keep the sidebar open while making progress and while idle countdowns update. Scroll position should stay stable when membership/order is unchanged; only changed cards should update. Test narrow sidebar width, hover, keyboard focus and long titles/descriptions.
