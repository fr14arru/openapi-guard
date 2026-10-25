import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,readFileSync,writeFileSync,rmSync,cpSync} from 'node:fs';
import path from 'node:path';
import {tmpdir} from 'node:os';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
test('pilot kit is complete and validates consumer scenarios through its copied Action',async t=>{
  const root=mkdtempSync(path.join(tmpdir(),'guard pilot '));t.after(()=>rmSync(root,{recursive:true,force:true}));
  const bundle=path.join(root,'bundle'),kit=path.join(root,'kit');
  const ps=(name,args)=>spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts',name),...args],{cwd:project,encoding:'utf8',timeout:60000});
  assert.equal(ps('prepare-evaluation.ps1',['-Destination',bundle]).status,0);
  const prepared=ps('prepare-pilot.ps1',['-Package',bundle,'-Destination',kit]);assert.equal(prepared.status,0,prepared.stderr);
  const workflow=readFileSync(path.join(kit,'.github/workflows/openapi-guard.yml'),'utf8');
  assert.match(workflow,/uses: .\/.guard-action/);assert.match(workflow,/fetch-depth: 0/);assert.match(workflow,/persist-credentials: false/);
  assert.match(workflow,/contents: read/);assert.match(workflow,/if: always\(\)/);assert.ok(!/mvn|mvnd|pull_request_target|continue-on-error/.test(workflow));
  assert.notEqual(ps('prepare-pilot.ps1',['-Package',bundle,'-Destination',kit]).status,0);
  for(const [scenario,code] of [['equivalent',0],['compatible',0],['breaking',1],['review',3],['invalid',2],['removed',1],['renamed',0],['no-contract-change',0]])await t.test(scenario,()=>{
    const consumer=path.join(root,scenario);cpSync(kit,consumer,{recursive:true});
    const git=(...args)=>{const r=spawnSync('git',args,{cwd:consumer,encoding:'utf8'});assert.equal(r.status,0,r.stderr);return r.stdout.trim();};
    git('init','--quiet');git('config','user.name','Pilot fixture');git('config','user.email','fixture@example.invalid');
    git('add','.');git('commit','--quiet','-m','test: baseline');const base=git('rev-parse','HEAD');
    const contract=path.join(consumer,'contracts/api.yaml');
    if(['compatible','breaking','review','invalid'].includes(scenario))writeFileSync(contract,readFileSync(path.join(consumer,`pilot-cases/${scenario}.yaml`)));
    if(scenario==='removed')rmSync(contract);
    if(scenario==='renamed')git('mv','contracts/api.yaml','contracts/renamed.yaml');
    if(scenario==='no-contract-change')writeFileSync(path.join(consumer,'note.txt'),'metadata');
    git('add','.');git('commit','--allow-empty','--quiet','-m','test: candidate');const candidate=git('rev-parse','HEAD');
    let workspace=consumer;
    if(scenario==='breaking') {
      // A GitHub checkout can convert text on Windows. Verify the delivered bytes after a fresh clone.
      workspace=path.join(root,'breaking-checkout');
      const cloned=spawnSync('git',['-c','core.autocrlf=true','clone','--quiet','--no-hardlinks',consumer,workspace],{encoding:'utf8'});
      assert.equal(cloned.status,0,cloned.stderr);
    }
    const run=spawnSync('node',[path.join(workspace,'.guard-action/scripts/action-runner.mjs')],{cwd:workspace,encoding:'utf8',timeout:60000,
      env:{...process.env,GITHUB_WORKSPACE:workspace,GITHUB_EVENT_NAME:'workflow_dispatch',GUARD_BASE:base,GUARD_CANDIDATE:candidate,
        GUARD_ROOT:'contracts',GUARD_JAR:'',GUARD_JAR_SHA256:'',GITHUB_ACTIONS:'true',GITHUB_OUTPUT:'',GITHUB_STEP_SUMMARY:''}});
    assert.equal(run.status,code,run.stderr+run.stdout);
  });
});
