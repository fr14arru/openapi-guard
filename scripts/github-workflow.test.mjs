import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,mkdtempSync,writeFileSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const workflow=()=>readFileSync(path.join(project,'.github/workflows/openapi-contract-guard.yml'),'utf8');
test('required job runs for PRs including edits without contracts and merge queue',()=>{
  const w=workflow();assert.match(w,/pull_request:/);assert.match(w,/merge_group:/);
  assert.ok(!/paths(-ignore)?:/.test(w));assert.match(w,/name: OpenAPI contract guard/);
});
test('workflow compares target baseline to proposed merge through environment arguments',()=>{
  const w=workflow();assert.match(w,/pull_request.base.sha/);assert.match(w,/merge_group.base_sha/);
  assert.match(w,/candidate "\$env:GITHUB_SHA"/);assert.ok(!/pull_request_target|continue-on-error/.test(w));
  assert.match(w,/contents: read/);assert.match(w,/persist-credentials: false/);
});
test('official Actions have immutable pins and failed reports are retained',()=>{
  const w=workflow();const actions=[...w.matchAll(/uses: ([^\s]+)/g)].map(m=>m[1]);
  assert.equal(actions.length,3);assert.ok(actions.every(a=>/^actions\/[a-z-]+@[a-f0-9]{40}$/.test(a)));
  assert.match(w,/if: always\(\)/);assert.match(w,/retention-days: 7/);
});
test('runner builds with exact Maven policy and executes Git and packaged CLI tests',()=>{
  const w=workflow();assert.match(w,/distribution: corretto/);assert.match(w,/java-version: '25'/);
  assert.match(w,/setup-maven.ps1/);assert.match(w,/mvn --batch-mode clean verify/);
  assert.match(w,/node --test scripts\/git-contracts.test.mjs scripts\/github-workflow.test.mjs/);
  assert.match(w,/scripts\/pr-comment.test.mjs/);
  const setup=readFileSync(path.join(project,'scripts/setup-maven.ps1'),'utf8');
  assert.match(setup,/3\.10\.0/);assert.match(setup,/SHA512/);assert.match(setup,/archive.apache.org/);
});
test('Maven bootstrap verifies checksum offline and rejects mismatch before extraction',t=>{
  const directory=mkdtempSync(path.join(tmpdir(),'guard checksum '));
  t.after(()=>rmSync(directory,{recursive:true,force:true}));
  const archive=path.join(directory,'archive.zip');const bytes=Buffer.from('corrupted zip fixture');
  writeFileSync(archive,bytes);
  const digest=createHash('sha512').update(bytes).digest('hex');
  const run=checksum=>spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/setup-maven.ps1'),'-ArchivePath',archive,'-ExpectedSha512',checksum,'-ValidateArchiveOnly'],{encoding:'utf8',timeout:30000});
  assert.equal(run(digest).status,0);
  const bad=run('0'.repeat(128));assert.notEqual(bad.status,0);assert.match(bad.stderr,/mismatch/);
  assert.notEqual(run('not-a-checksum').status,0);
});
