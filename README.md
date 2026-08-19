# Unused npm Script Companion

IntelliJ-family plugin. Gutter icon on every entry of an open
`package.json`'s `"scripts"` section, showing whether that script is
actually **referenced** anywhere in your project — or an orphan
nobody calls, a real candidate for cleanup. 100% static text/PSI
analysis of files already open in your project: no Node.js/npm
process spawned, no network call.

## Why it exists

An original idea, not a port of an existing competitor — validated
against `CONSTITUTION.md` §1's "Plan B permanente" discipline before
being built: (1) confirmed no plugin in this catalog or in JetBrains
Marketplace does exactly this ("unused dependency" tools like Knip or
Jonnyzzz Dependencies analyze packages under `node_modules`, never the
scripts declared in the project's own `package.json`); (2) confirmed
buildable in the ~10-day budget with techniques this catalog already
has proven — the bundled JSON plugin's real PSI to read `"scripts"`
(same pattern as `json-schema-companion`/`json-to-code-companion`),
and hand-rolled text/regex matching over CI configs and README.md
(same pattern as `circular-dependency-companion`'s Gradle DSL parser).
Same "apuesta consciente sin ancla de mercado" treatment as Refactor
Simulator / Test Scaffold Companion / Dockerfile Layer Size Companion /
Circular Dependency Companion: v0.1 ships free, no time/marketing
investment disproportionate to real demand signal until there's
evidence of adoption.

## Where it looks for a reference (v0.1 scope, stated honestly)

1. **The package.json's own other scripts.** If one script's command
   calls another (`"build": "npm run clean && npm run compile"`),
   `clean` and `compile` are marked used.
2. **GitHub Actions workflows** — every YAML file directly under
   `.github/workflows` next to the package.json.
3. **GitLab CI** — `.gitlab-ci.yml` next to the package.json.
4. **`README.md`** next to the package.json — a textual mention of
   running the script.

**Not covered in v0.1, deferred to a possible future version, not
silently unsupported:** `Jenkinsfile`, `.circleci/config.yml`,
`azure-pipelines.yml`, and monorepos with more than one `package.json`
(a script in one workspace package called from another workspace
package's `package.json` isn't cross-referenced yet — each
`package.json` is analyzed independently).

## Only real invocations count — never a bare word match

A script named `build` is never marked "used" just because the word
"build" appears somewhere unrelated in a README paragraph or another
script's comment. Only these runner shapes count as a real reference:

- `npm run <name>` — the canonical form, works for any script.
- `yarn <name>` — Yarn's shorthand, no `run` keyword needed.
- `pnpm <name>` and `pnpm run <name>` — pnpm accepts both forms.

The match requires a real runner keyword immediately before the exact
script name, with the name not extending into a longer, different
identifier — `npm run build` does not mark `build-prod` as used, and
`npm run build-prod` does not mark `build` as used, and `npm run
rebuild` does not mark `build` as used either. (`npm start`/`npm test`
without `run` are intentionally not matched by this text search — see
next section for why that's a non-issue.)

## npm lifecycle scripts: never flagged as orphaned

These names are never flagged as orphaned, even with **zero** text
references anywhere — npm's own CLI runs them automatically at
specific points in its lifecycle, so their real "caller" is npm
itself, not project source:

- `pretest`, `test`, `posttest`
- `prepublish`, `prepare`, `prepublishOnly`, `prepack`, `postpack`,
  `publish`, `postpublish`
- `preinstall`, `install`, `postinstall`
- `preuninstall`, `uninstall`, `postuninstall`
- `preversion`, `version`, `postversion`
- `start` — the universal `npm start` entry point; a human runs it
  directly from the command line, which static text analysis has no
  way to observe, so flagging it "orphaned" would be actively wrong.
- **Any `pre`/`post` prefix of another script that genuinely exists.**
  If `build` exists, `prebuild`/`postbuild` are treated as used by npm
  convention — but a lone `prefoo` with no real `foo` script anywhere
  is *not* a lifecycle hook of anything, just an oddly-named ordinary
  script, and is scanned for text references like any other.

## Honest handling of missing scripts

A project with no `package.json` open, or a `package.json` with no
`"scripts"` section at all, produces **no gutter icons and no error**
— never a crash, never a misleadingly empty tool window. There's
simply nothing to annotate.

## Why a gutter icon, not an inlay hint

Three sibling plugins built in the same batch (Regex Named Group,
HTTP Status Inline, Dockerfile Layer Size Companion) all render an
inlay hint, because their feature *adds information to a specific
point on the line* (a captured group's name, a status's official
name, a layer's byte size) right where the reader's eye already is.
This feature is different in kind: it's a per-line **verdict** (used
vs. orphaned) about the *whole script entry*, not a value attached to
one token — exactly the shape already proven in this catalog by
`highlight-companion`'s cognitive-complexity gutter icon per method. A
gutter icon also fits the actual task better ("which of these 15
scripts can I safely delete?") than an inlay would: a column of icons
down the gutter is scannable top-to-bottom in a way inline text after
each line is not.

## Why built this way

- **No hand-rolled JSON parser.** `"scripts"` is read via the bundled
  JSON plugin's real PSI (`JsonFile`/`JsonObject`/`JsonProperty`/
  `JsonStringLiteral`) — same "don't reinvent a parser for a format
  the platform already parses correctly" principle already proven in
  `json-schema-companion`/`json-to-code-companion`.
- **CI configs and README read as plain text**, not parsed as real
  YAML/Markdown — v0.1 only needs to find a runner-invocation pattern
  as a substring match, not understand a workflow's job graph or a
  README's document structure. Same hand-rolled-regex-over-plain-text
  principle as `circular-dependency-companion`'s Gradle DSL parser.
- **Heavy work off the EDT.** Reading CI/README files from disk and
  running the usage scan happens inside `collectSlowLineMarkers`,
  which the platform contract guarantees runs on a background thread
  as part of the daemon's slow-line-markers pass — never on the EDT,
  same discipline as every highlighting pass and line-marker provider
  in this catalog (`CONSTITUTION.md` §6).
- **Extraction is dumb, interpretation is smart.** `PackageJsonParser`
  only reads what's literally in `"scripts"`; deciding whether a name
  is a lifecycle hook or genuinely orphaned lives entirely in
  `UsageScanner`/`LifecycleScripts`, both pure functions with no PSI/
  VFS dependency — the same split this catalog already uses
  everywhere (e.g. `GradleBuildFileParser` vs. `CycleDetector`), which
  is what makes the verdict logic exhaustively unit-testable without
  spinning up the platform test framework for every case.

## v0.1 scope

Free, all of it — no paywall, nothing held back for a future tier.
Deferred to a possible future v0.2 (not started, not promised):
full monorepo scanning across several `package.json` files with
cross-package reference resolution, and CI format coverage beyond
GitHub Actions/GitLab CI (`Jenkinsfile`, CircleCI, Azure Pipelines).

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us
at **gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
