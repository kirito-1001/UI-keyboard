# UI Keyboard — notes for Claude

Read this first in every session. It exists so that Claude does not have to re-read and re-analyse the whole project.

**This repository is Khaled Yagami's keyboard, "UI Keyboard" (GitHub: kirito-1001/UI-keyboard).**
It is a fork of FlorisBoard. All the changes on top of FlorisBoard (everything named `Ui*`, the toolbar,
the suggestions engine, the settings changes, the README and store text) were written by Claude in chat sessions
with Khaled. He has only a phone, no computer and no programming experience, so Claude does all the coding.

## Working agreement with Khaled (very important)
- Talk to him in **Arabic** (simple, short). Code, commits and this file are in English.
- **Agree on the complete change list first.** Do not edit anything until he says he is done explaining and
  tells you to start ("ابدأ"). Reading code to understand a request is fine.
- **Do not add or change anything in `README.md` until he says development is finished.**
- Commit and push **directly to `main`** (his decision, 2026-11-07). Keep code changes careful, because they cannot be
  compiled in Claude's session (see below).
- Never invent features: describe only what exists in the code.
- Keep Apache-2.0 attribution to FlorisBoard in the README and license files.

## Building
- Claude's session has no network for Gradle, so **the app cannot be compiled or tested there**. Review the code by
  reading it very carefully (imports, types, braces) before pushing.
- Khaled builds the APK on GitHub: Actions tab -> "Build APK" -> "Run workflow" (manual only, `workflow_dispatch`),
  then downloads the `keyboard-debug-apk` artifact. If a build fails he sends the error text.
- The workflow downloads the FrequencyWords lists and builds the bundled word lists with `tools/build_dict.py` and
  `tools/build_pairs.py` into `app/src/main/assets/ime/dict/` (`ar.txt`, `en.txt`, `*_pairs.txt`, `*_triples.txt`).
  Those files are generated at build time and are not committed.
- Min SDK 26 (Android 8.0). Application id is still `dev.patrickgold.florisboard`; the app name is "UI Keyboard".

## Where the custom code is
| What | Where |
| --- | --- |
| Toolbar (logo, edit slot, language box, emoji slot, hide) and its popup menus | `app/src/main/kotlin/dev/patrickgold/florisboard/ime/uikb/UiToolbar.kt` |
| Panel opened by the logo (grid of all toolbar actions) | `ime/uikb/ActionsPanel.kt` |
| Features for the two customizable toolbar slots | `ime/uikb/UiFeature.kt` |
| Edit-text and numeric panels, key/theme/string helpers | `ime/uikb/*.kt` |
| Toolbar action list and its order (Settings > Smartbar > Toolbar buttons) | `ime/smartbar/quickaction/QuickActionArrangement.kt`, `app/settings/smartbar/ToolbarButtonsScreen.kt` |
| Suggestions: word list, completion | `ime/nlp/UiDictionary.kt` |
| Suggestions: learning from the user's typing, stored on the phone | `ime/nlp/UiLearned.kt` |
| Suggestions: how the slots are filled | `ime/nlp/latin/LatinLanguageProvider.kt` (`suggest`) |
| Suggestion bar layout | `ime/smartbar/CandidatesRow.kt` |
| Settings home screen | `app/settings/HomeScreen.kt` |
| In-app changelog | `app/settings/about/UiChangelog.kt` |
| About screen (developer name + mailto) | `app/settings/about/AboutScreen.kt` |
| Strings | `app/src/main/res/values/strings.xml` and `values-ar/strings.xml` (Ui strings are near the end) |
| Preferences | `app/AppPrefs.kt` |

## How some things work
- Toolbar layout: `[logo] | [edit slot, language box (140dp), emoji slot] | [hide]`. Tapping the logo opens the actions
  grid (`UiPanel.ACTIONS`). The two slots can be changed by long-pressing them; long-pressing the language box opens the
  language / numeric / symbols / themes / settings menu.
- Suggestions work on the phone only. The Arabic word list is matched on a normalized key (no diacritics; alef forms,
  alef maqsura and ta marbuta unified). Learning is off in password fields and in incognito mode; the Typing
  settings have a button that clears what was learned. Suggestions are **off by default** (Settings > Typing).
- Suggestions only, never auto-correct (Khaled's decision).
- The in-app changelog: add a new `UiChangelogEntry` **at the top** of `UiChangelogEntries`. `version` is the 7-character
  commit sha of the code commit, so commit the code first and then add the entry in a second commit.
- Fastlane store text/images: `fastlane/metadata/android/en-US` is updated for UI Keyboard; the es-AR, es-ES and
  pl-PL folders and `featureGraphic.png` are still the FlorisBoard originals.

## Done so far
Language add + switch, toolbar slots and icon size/order settings, per-key customization with reset, key corner
roundness, custom keyboard font, themes with a picture background, recently-copied-text chip, emoji panel,
edit-text panel, opening screen, About screen with developer credit and in-app changelog, suggestions engine
(word list + learning + next word + clear learned data), bilingual README, new icon and screenshots.
Typo correction in the first suggestion slot (slots left to right: closest correction, most used learned word,
word-list word), "Voice input" as a normal reorderable toolbar action shown in the logo grid, and faint (25% opacity)
dividers on the settings home. Khaled tested these on 2026-11-07 and they work; they are merged into `main`.

Prediction improvements (2026-11-14): next-word lists are built from Wikipedia plus OpenSubtitles dialogue text
(`tools/build_pairs.py`, dialogue counted x3); completion while typing uses the previous words (learned and bundled
pairs/triples that start with the typed letters come first); learning keeps a last-used time and ranks recent use
higher (`UiLearned`, file entries are `count@minutes`, old files still load); a word picked from the suggestions
counts twice. Built-in voice typing (`ime/uikb/UiVoice.kt`) and the settings added since the last notes (shortcut
buttons dialog, hint size, popup corners, western digits with the Arabic digit as a secondary popup key) exist too.

## Pending / open
- The old branch `typo-suggestions` is merged and can be deleted by Khaled in GitHub (Claude cannot delete it).
- The "100 times more frequent" rule in `UiDictionary.corrections` may need tuning if corrections feel too eager.
- Cloud AI features for the keyboard were discussed and not chosen (he chose on-device suggestions).

## Git notes
- A stop hook may warn that a branch has "unpushed commits and no remote branch" even after a successful push. Check
  with `git ls-remote --heads origin`; running `git fetch origin +BRANCH:refs/remotes/origin/BRANCH` fixes the warning.
- Claude's session cannot delete remote branches (git and API deletes are refused); Khaled deletes them in GitHub.
- Commit messages end with the `Co-Authored-By` and `Claude-Session` lines the session asks for.

## Keep this file current
At the end of every task, update "Done so far" and "Pending / open" in a few lines (this file only, not the README).
