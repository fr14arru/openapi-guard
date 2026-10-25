import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,mkdtempSync,rmSync,existsSync,mkdirSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {tmpdir} from 'node:os';
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
test('local evaluation package inventories actual runtime libraries and preserves runnable CLI',t=>{
  const parent=mkdtempSync(path.join(tmpdir(),'guard package '));t.after(()=>rmSync(parent,{recursive:true,force:true}));
  const output=path.join(parent,'bundle');
  const p=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/prepare-evaluation.ps1'),'-Destination',output],{cwd:project,encoding:'utf8',timeout:60000});
  assert.equal(p.status,0,p.stderr+p.stdout);
  const manifest=JSON.parse(readFileSync(path.join(output,'manifest.json'),'utf8'));
  assert.equal(manifest.distributionStatus,'OPEN_SOURCE_LOCAL_PACKAGE');
  assert.equal(readFileSync(path.join(output,'LICENSE.txt'),'utf8'),readFileSync(path.join(project,'LICENSE'),'utf8'));
  assert.equal(manifest.attributions.productLicenseStatus,'MIT');
  assert.equal(existsSync(path.join(output,'PILOT-TERMS-DRAFT.txt')),false);
  assert.equal(manifest.jarSha256,createHash('sha256').update(readFileSync(path.join(output,'guard.jar'))).digest('hex'));
  assert.ok(manifest.dependencies.length>20);assert.ok(manifest.dependencies.every(d=>/^[a-f0-9]{64}$/.test(d.sha256)));
  assert.ok(manifest.dependencies.some(d=>d.artifact.startsWith('swagger-parser-v3-')));
  for(const file of ['action.yml','scripts/action-runner.mjs','scripts/git-contracts.mjs','scripts/github-presentation.mjs','THIRD-PARTY-REVIEW.md','examples/base.yaml'])assert.ok(existsSync(path.join(output,file)),file);
  const help=spawnSync('java',['-jar',path.join(output,'guard.jar'),'--help'],{encoding:'utf8',timeout:30000});
  assert.equal(help.status,0,help.stderr);assert.match(help.stdout,/Usage:/);
  // Run the copied Action, not the source checkout: packaged defaults must find manifest and JAR.
  const consumer=path.join(parent,'consumer');mkdirSync(path.join(consumer,'contracts'),{recursive:true});
  const git=(...args)=>{const r=spawnSync('git',args,{cwd:consumer,encoding:'utf8'});assert.equal(r.status,0,r.stderr);return r.stdout.trim();};
  git('init','--quiet');git('config','user.name','Fixture');git('config','user.email','fixture@example.invalid');
  const save=name=>{writeFileSync(path.join(consumer,'contracts/api.yaml'),readFileSync(path.join(output,`examples/${name}.yaml`)));git('add','.');git('commit','--quiet','-m','test: consumer');return git('rev-parse','HEAD');};
  const base=save('base'),candidate=save('breaking');
  const consumed=spawnSync('node',[path.join(output,'scripts/action-runner.mjs')],{cwd:consumer,encoding:'utf8',timeout:60000,
    env:{...process.env,GITHUB_WORKSPACE:consumer,GITHUB_EVENT_NAME:'workflow_dispatch',GUARD_BASE:base,GUARD_CANDIDATE:candidate,
      GUARD_JAR:'',GUARD_JAR_SHA256:'',GUARD_ROOT:'contracts',GITHUB_OUTPUT:'',GITHUB_STEP_SUMMARY:'',GITHUB_ACTIONS:'true'}});
  assert.equal(consumed.status,1,consumed.stderr+consumed.stdout);assert.match(consumed.stdout,/::error file=contracts\/api.yaml/);
  const again=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/prepare-evaluation.ps1'),'-Destination',output],{cwd:project,encoding:'utf8',timeout:30000});
  assert.notEqual(again.status,0);assert.match(again.stderr,/already exists/);
});
