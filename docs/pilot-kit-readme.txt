# OpenAPI Guard: private pilot repository

This is a PRIVATE test kit, not a public release or a Marketplace installation.
The .guard-action directory contains the prebuilt Java engine and its composite
Action. Java 25 is set up by the Action; the consumer does not build with Maven.
Product pilot terms and third-party publication approval remain pending review.

## First setup

For the visible PR comment experience, configure these two blocks in the
included .github/workflows/openapi-guard.yml (the other steps are already ready):

permissions:
  contents: read
  pull-requests: write

# Under the guard step's with block:
contracts-root: contracts
pr-comment: 'true'
fail-on-breaking: 'false'

If your contracts already live in contracts/, no path change is needed.
This explicitly allows complete breaking comparisons; use 'true' for
fail-on-breaking if your team wants to block them. Invalid inputs and pending
review still fail. No personal token, secret or Maven installation is needed.
The default workflow token is used. Keep the checkout, concurrency and artifact
upload steps from the included workflow. The basic template defaults remain
pr-comment: 'false' and fail-on-breaking: 'true' until you select this recipe.

1. Create an empty PRIVATE GitHub test repository manually.
2. Initialize this directory with Git on main, commit all kit files and push main
   to YOUR private test repository. Keep your existing Git identity and SSH keys.
3. Create a test branch. Copy one pilot-cases file over contracts/api.yaml,
   commit the change, push that branch and open a pull request into main.
4. Inspect Checks, the Action summary and the downloaded JSON/Markdown reports.

Do not push a variant directly to main and expect a PR workflow to run.
The workflow compares the target baseline to GitHub's proposed merge commit.
The bundled tool and workflow must be reviewed if a PR changes them.
Do not use pull_request_target, code write permissions or extra secrets.
pull-requests: write is required only for the optional conversation comment.

## Acceptance matrix

The matrix below uses the default fail-on-breaking: 'true' workflow policy.

- Equivalent or no contract changes: check passes (0).
- compatible.yaml: check passes (0).
- breaking.yaml: check fails (1); candidate annotations contain rule and line.
- review.yaml: check fails (3); uncertainty is visible, not a green result.
- invalid.yaml: check fails (2); input problem is visible.
- Remove contracts/api.yaml: check fails (1), CONTRACT_FILE_REMOVED, no invented line.
- Rename the contract without changing its contents: passes (0).

Use a fresh branch from the baseline for each scenario; do not stack variants.
Retain execution URLs and compare report.base/report.candidate with the analyzed
commits. Confirm annotations refer to candidate definitions or explicit containers.
Only make the check required in THIS consumer after validating the pilot.
Fork and merge queue behavior, and Linux runners, need separate GitHub executions.

## Deliberate breaking changes

To make findings visible in the PR Conversation tab, set pr-comment: 'true'
under the guard step's with block, and add pull-requests: write to workflow
permissions. The template defaults to no comment and read-only permissions.
The workflow token creates/updates one bot comment, also after findings disappear.
Verify its analyzed SHA and run link. Forks and non-PR events skip publication;
permission/network failures fail the requested publication without losing reports.

In .github/workflows/openapi-guard.yml, set fail-on-breaking: 'false' under the
guard step's with block to keep the report while allowing a deliberate break.
The comparison output exit-code stays 1 and the verdict stays BREAKING;
check-exit-code becomes 0 only when analysis is complete. Annotations become
warnings and the summary states the policy. Invalid inputs still fail with 2;
pending review still fails with 3, including review alongside a proven break.
Repeat breaking.yaml with this policy and confirm the check passes while the
report preserves all findings. Repeat invalid.yaml and review.yaml to confirm
they still fail. Other repository protections and checks remain applicable.
Review workflow changes as part of the PR; no label or version is treated as
automatic approval of a breaking change.

## Useful commands after the baseline commit

git switch -c pilot/breaking
Copy-Item -LiteralPath pilot-cases/breaking.yaml -Destination contracts/api.yaml
git add contracts/api.yaml
git commit -m "test(api): exercise a breaking contract change"
git push -u origin pilot/breaking

Open the pull request in GitHub. Public sharing and commercial publication are
not authorized by generating this kit. The JAR is committed only for this private
pilot; a published distribution strategy will replace this manual transfer.
