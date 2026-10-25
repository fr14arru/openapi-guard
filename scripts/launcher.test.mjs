import test from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync,rmSync,readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const run=(...args)=>spawnSync('pwsh',['-NoProfile','-File',path.join(repo,'guard.ps1'),...args,...(process.env.GUARD_TEST_JAR && !args.includes('-Jar') ? ['-Jar',process.env.GUARD_TEST_JAR] : [])],{cwd:repo,encoding:'utf8',timeout:60000});
test('one-command demo compares with full coverage',()=>{
  const result=run('-Demo'); assert.equal(result.status,0,result.stderr);assert.match(result.stdout,/operations-data-response-policy-v1/);
});
for(const [name,code] of [['compatible',0],['breaking',1],['review',3]]) test(`launcher preserves ${name} exit and saves Markdown`,t=>{
  const dir=mkdtempSync(path.join(tmpdir(),'guard launcher '));t.after(()=>rmSync(dir,{recursive:true,force:true}));
  const output=path.join(dir,'report.md');
  const result=run('-Baseline','examples/catalog/base.yaml','-Candidate',`examples/catalog/${name}.yaml`,'-Format','markdown','-Output',output);
  assert.equal(result.status,code,result.stderr);assert.ok(readFileSync(output,'utf8').startsWith('# OpenAPI'));
});
test('invalid input and absent custom JAR fail without downloads',()=>{
  assert.match(run().stderr,/Specify -Baseline and -Candidate, or use -Demo\./);
  assert.equal(run('-Baseline','missing','-Candidate','missing').status,2);
  const absent=run('-Demo','-Jar','missing.jar');assert.equal(absent.status,2);assert.match(absent.stderr,/JAR/);
});
