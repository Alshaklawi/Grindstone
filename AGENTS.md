# Grindstone

- Build for Java 11 and use RuneLite APIs and native UI components.
- Keep this project compatible with RuneLite Plugin Hub's Java 11 Linux packaging workflow and v5 plugin-hub-tooling bundle. Preserve standard plugin source/resource layout, plugin metadata and internal name, supported dependencies and API usage. Keep local launchers and screenshot utilities out of the production artifact. Run relevant build and compliance checks before committing; distinguish local checks from an actual Plugin Hub packager/API verification run.
- Use Lombok for straightforward boilerplate; preserve validation, defensive copies and custom behavior.
- Use explicit imports, same-line opening braces, multiline block contents, one enum entry per line, and one declaration annotation per line. Never wrap solely for line length.
- Keep only the requested author/Discord/creation-date header on each type. Preserve its actual creation date.
- Keep commits atomic; separate formatting from behavior changes. Preserve unrelated edits. Do not push unless asked, or commit the local run-plugin.bat script.
- Keep README player-facing and concise. Do not add redundant developer notes or checklist documents.
- Never automate game input or add prohibited gameplay assistance. No reflection, native access, external processes, dynamic code loading or Java serialization.
- Keep client reads on the client thread and Swing changes on the EDT. Never block the client thread or lifecycle methods; cancel tasks and listeners on shutdown.
- Use events instead of expensive per-tick scans. Use debug logging for diagnostics.
- Use gameval constants, injected Gson and OkHttp, LinkBrowser, and scoped Filepath APIs and chooser. Keep network and disk work off the client thread.
- Preserve config keys and saved-profile compatibility; provide migrations for changes. Third-party services must be opt-in with RuneLite's required warning.
- Retain the permissive license. Do not commit build artifacts; keep icons small and valid.
- Run relevant tests and compliance checks. Never interact with RuneScape through automation; gameplay changes require the player's in-game confirmation.
