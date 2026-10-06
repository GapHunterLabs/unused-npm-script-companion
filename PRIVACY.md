# Privacy Policy — Unused npm Script Companion

**Effective date:** 2026-10-06

Unused npm Script Companion is a Gap Hunter Labs plugin for IntelliJ
Platform IDEs. This policy is short because the plugin's design makes
it short: there is nothing to disclose beyond what's below.

## What this plugin collects

**Nothing.** Unused npm Script Companion does not collect, transmit, or sell any data — no source code, no file contents, no file
paths, no usage analytics, no telemetry, no crash reports, no
personally identifiable information. `package.json`, CI config
(`.github/workflows/*`, `.gitlab-ci.yml`), and `README.md` text read
from your local project exists only in memory for as long as the IDE
is open, and only long enough to compute each script's used/orphaned
verdict.

## What it keeps on your machine

To decide when to show its one-time rating prompt, the plugin keeps two values
in the IDE's own settings on your computer: whether you have answered the
prompt, and a list of up to 500 findings it has already counted. Until the
next release, each entry in that list is the file path and line of a finding,
sometimes with its message. From the next release on, each entry is a one-way
fingerprint that cannot be turned back into a path, and the old list is
deleted. None of this is ever sent anywhere.

## Network access

**None.** Unused npm Script Companion makes zero network calls during
normal operation. Every verdict shown is computed directly from files
already present on your local disk — no npm registry lookup, no
Node.js/npm process spawned, ever.

## Third parties

None. Unused npm Script Companion has no third-party SDKs, no
analytics libraries, no ad networks, no external dependencies that
phone home. `package.json` parsing uses only the bundled JSON plugin's
own PSI.

## Changes to this policy

If this ever changes, this file will be updated and the change will be
noted in the plugin's `CHANGELOG.md`.

## Contact

Questions about this policy: **gaphunterlabs@gmail.com**
