import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,writeFileSync,rmSync,readFileSync} from 'node:fs';
import path from 'node:path';
import {tmpdir} from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const run=(...args)=>spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/build-candidate.ps1'),...args],{encoding:'utf8',timeout:30000});
test('candidate builder rejects invalid versions and dirty source before any build',t=>{
  const bad=run('-Version','not-a-release');assert.equal(bad.status,2);assert.match(bad.stderr,/Candidate version/);
  const repo=mkdtempSync(path.join(tmpdir(),'guard dirty build '));t.after(()=>rmSync(repo,{recursive:true,force:true}));
  const git=(...args)=>spawnSync('git',args,{cwd:repo,encoding:'utf8'});
  assert.equal(git('init','--quiet').status,0);git('config','user.name','Fixture');git('config','user.email','fixture@example.invalid');
  writeFileSync(path.join(repo,'tracked'),'one');git('add','.');git('commit','--quiet','-m','test: initial');writeFileSync(path.join(repo,'tracked'),'two');
  const dirty=run('-Repository',repo);assert.equal(dirty.status,2);assert.match(dirty.stderr,/clean source checkout/);
});
test('packager rejects a fabricated build proof rather than approving a candidate',t=>{
  const root=mkdtempSync(path.join(tmpdir(),'guard proof '));t.after(()=>rmSync(root,{recursive:true,force:true}));
  const proof=path.join(root,'proof.json');writeFileSync(proof,JSON.stringify({verified:true,sourceCommit:'a'.repeat(40),version:'0.1.0-rc.1',jarSha256:'0'.repeat(64)}));
  const p=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/prepare-evaluation.ps1'),'-Destination',path.join(root,'package'),'-BuildProof',proof],{cwd:project,encoding:'utf8',timeout:30000});
  assert.notEqual(p.status,0);assert.match(p.stderr,/Build proof does not match/);
});
