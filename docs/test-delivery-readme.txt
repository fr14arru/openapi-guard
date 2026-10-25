OpenAPI Breaking Change Guard - test-mode delivery

This package is prepared for controlled Lemon Squeezy TEST MODE only.
Do not upload to a live product or publicly redistribute the package.
Approved product terms: LICENSE.txt. Pending launch checks: DELIVERY-STATUS.md.
Privacy and the owner's personal legal notice are separate and are not included.
This package does not create a paid subscription or activate sales.

GET STARTED

Extract the complete ZIP to a new folder. Keep all notices and corresponding
third-party source archives with guard.jar. Do not distribute the JAR alone.
Local requirement: Java 25 (Amazon Corretto 25 is the reference distribution).
No Maven, IDE, database, Docker or product account is required to run the CLI.
Windows is the validated environment; other runners remain to be validated.

From the extracted directory:
  java -version
  java -jar guard.jar --help
  java -jar guard.jar --profile=full --format=text examples/base.yaml examples/compatible.yaml
  java -jar guard.jar --profile=full --format=text examples/base.yaml examples/breaking.yaml
  java -jar guard.jar --profile=full --format=text examples/base.yaml examples/review.yaml

Expected exit codes: compatible example 0; breaking example 1; review example 3.
Input errors return 2. A nonzero comparison code is not automatically a crash.

Use existing files for your own contracts (these filenames are illustrative):
  java -jar guard.jar --profile=full --format=text --baseline=previous.yaml --candidate=next.yaml
  java -jar guard.jar --profile=full --format=json --output=reports/report.json --baseline=previous.yaml --candidate=next.yaml
  java -jar guard.jar --profile=full --format=markdown --output=reports/report.md --baseline=previous.yaml --candidate=next.yaml

Always select --profile=full for the implemented coverage. The historical default
profile is narrower. Full does not mean the entire OpenAPI specification.
OpenAPI 3.0.x is the initial commercial scope. Use self-contained UTF-8 YAML/JSON
documents, at most 5 MiB each. External references are not resolved.
Read the verdict, analysis status, diagnostics and excluded facets together.
Candidate positions are navigation context, not an exact diff. Input failures
may concern the baseline and may not have line positions.

GITHUB ACTIONS (CURRENT LOCAL PACKAGE)

For an authorized PRIVATE consumer repository, copy this complete directory to
.guard-action in the repository root, including all third-party material.
Add this line to .gitattributes, keeping any existing entries:
  .guard-action/** -text

Use the workflow from the public documentation:
  https://github.com/fr14arru/OpenAPI-Breaking-Change-Guard-Docs/blob/main/docs/github-actions.md

It checks out full Git history, invokes uses: ./.guard-action, and retains reports.
Set contracts-root to the directory containing complete versioned contracts.
pr-comment: 'true' enables a PR conversation comment and requires
pull-requests: write. The normal workflow token is used; no personal token needed.
fail-on-breaking: 'false' allows proven breaking changes when analysis is complete;
invalid inputs and pending review still fail. Reports preserve the actual verdict.
Defaults are fail-on-breaking: 'true' and pr-comment: 'false'.
Forks skip conversation comments. Do not switch to pull_request_target to gain
write permissions. Public consumer repositories must not commit this engine;
installation into their runner without public redistribution remains pending.

INTEGRITY AND PROVENANCE

The ZIP checksum must come from a trusted channel. manifest.json checks files,
but internal hashes alone do not prove publisher identity. If Node >=20 is
available, run:
  node scripts/verify-package.mjs .

The engine sourceCommit/version and build-proof.json describe the original
verified binary. delivery.packagingCommit identifies the new packaging code.
The engine was not rebuilt or changed for this delivery.

SUPPORT AND LIMITS

Public documentation:
  https://github.com/fr14arru/OpenAPI-Breaking-Change-Guard-Docs
Support: rayopuyi5@gmail.com. Share a minimal sanitized example, version,
profile and exit code. Do not send secrets or private customer contracts.
Program messages are in English; contract names and snippets preserve Unicode.
The tool analyzes contracts locally or in your runner; reports shared through
GitHub may contain contract fragments. No license activation server was added.
