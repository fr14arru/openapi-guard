OpenAPI Breaking Change Guard - MIT local package

Project code is free software under LICENSE.txt (MIT).
Third-party libraries retain their own licenses, notices and source obligations.
manifest.json records the JAR SHA256, source checkout and runtime libraries.
This package generator does not publish a release or Marketplace listing.
node scripts/verify-package.mjs . verifies all delivered files, not just the JAR.
The external ZIP SHA256 must be obtained through a trusted channel; internal
checksums detect corruption but do not establish publisher authenticity.

CLI requirements: Java 25. No Maven, IDE, database or Docker is required.
java -jar guard.jar --profile=full --format=text examples/base.yaml examples/breaking.yaml
Exit codes: 0 compatible within profile; 1 breaking; 2 invalid; 3 review required.
Java output is UTF-8. Compatibility is limited to the selected profile.

The package also contains a local composite GitHub Action prototype (action.yml).
Consumer requirements: Git, Node >=20 and Bash on a standard GitHub-hosted runner.
The Action sets up Amazon Corretto 25 and verifies the JAR against manifest.json.
Checkout the consumer with fetch-depth: 0 and persist-credentials: false.
For PRs use the proposed merge checkout and give contents: read permission.
Add pull-requests: write only when enabling the optional conversation comment.
Use the local action directory with uses: ./path/to/evaluation-package.
Outputs: exit-code (original comparison), check-exit-code (workflow policy), report-path, summary-path.
By default codes 1/2/3 fail the step. Set fail-on-breaking: 'false' to allow
proven breaking changes while retaining the BREAKING report and warning annotations.
Invalid inputs still fail with 2. Pending review still fails with 3, including
partial analysis alongside a demonstrated break (original exit-code remains 1).
Use always() on subsequent report upload steps, including failed comparisons.

Optional PR conversation comment: set pr-comment: 'true' and grant
pull-requests: write alongside contents: read in the consumer workflow.
Minimum Action configuration for the visible, non-blocking experience:
  contracts-root: contracts
  pr-comment: 'true'
  fail-on-breaking: 'false'
Only change contracts-root if your complete contracts use another directory.
Set fail-on-breaking to 'true' if your team wants breaks to block the check.
The default workflow token is used; do not provide personal tokens.
One bot comment per workflow/contracts-root is created or updated, including
when breaking changes disappear. It links the analyzed revision and run reports.
Keep per-PR workflow concurrency with cancel-in-progress: true.
Forks and events other than pull_request skip comment publication.
comment-status records publication/skipping; requested publication failures
fail the step with check-exit-code 2 while retaining engine results and reports.

THIRD-PARTY-NOTICES.md links the complete license texts and corresponding source
archives under third-party/licenses/. The catalog pins 35 runtime libraries and
the Spring Boot loader. Third-party rights remain independent of product terms.
Keep the complete package together: the new notices and source archives are
adjacent to guard.jar, not embedded inside it. Do not publish guard.jar alone
as the attributed distribution prepared here.
PRODUCT-LICENSE-STATUS.txt records the MIT grant for project code.
Keep LICENSE.txt and all third-party notices and corresponding source archives.

This is not a published release, a remote Marketplace listing or a paid GitHub App.
Comment creation has been observed in the private Windows pilot. Remote comment
updates after fixing contracts and Linux/macOS execution remain to be validated.
