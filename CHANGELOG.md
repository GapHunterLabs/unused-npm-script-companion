<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Unused npm Script Companion Changelog

## [Unreleased]

## [0.1.0]

### Added

- **Gutter icon on every `"scripts"` entry of an open `package.json`**,
  showing whether it's referenced elsewhere in the project or a
  genuine orphan candidate for cleanup.
- **Reference search covers**: the package.json's own other scripts
  (`npm run`/`yarn`/`pnpm` invocations inside another script's
  command), GitHub Actions workflows (`.github/workflows/*.yml`),
  GitLab CI (`.gitlab-ci.yml`), and `README.md`.
- **Real invocation matching, not substring matching**: only
  `npm run <name>` / `yarn <name>` / `pnpm <name>` / `pnpm run <name>`
  count as a reference, so a script named e.g. `build` is never
  flagged "used" just because the word appears unrelated in prose.
- **npm lifecycle scripts never flagged as orphaned**: `pretest`/
  `test`/`posttest`, `prepublish`/`prepare`/`prepublishOnly`/`prepack`/
  `postpack`/`publish`/`postpublish`, `preinstall`/`install`/
  `postinstall`, `preuninstall`/`uninstall`/`postuninstall`,
  `preversion`/`version`/`postversion`, `start`, and any real script's
  own `pre`/`post` hook.
- **Honest handling of missing scripts**: a project with no
  `package.json`, or one with no `"scripts"` section, produces no
  markers and no crash -- never a misleading empty state.
- 100% static text/PSI analysis -- no Node.js/npm process spawned, no
  network call.

[Unreleased]: https://github.com/GapHunterLabs/unused-npm-script-companion/compare/0.1.0...HEAD
[0.1.0]: https://github.com/GapHunterLabs/unused-npm-script-companion/commits/0.1.0
