import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const jar=(process.env.GUARD_TEST_JAR || path.join(project,'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'));
const hash=createHash('sha256').update(readFileSync(jar)).digest('hex');
function consumer(t,before,after) {
  const repo=mkdtempSync(path.join(tmpdir(),'guard consumer '));
  t.after(()=>rmSync(repo,{recursive:true,force:true}));
  const git=(...args)=>{const r=spawnSync('git',args,{cwd:repo,encoding:'utf8'});assert.equal(r.status,0,r.stderr);return r.stdout.trim();};
  git('init','--quiet');git('config','user.name','Fixture');git('config','user.email','fixture@example.invalid');
  mkdirSync(path.join(repo,'contracts'));
  const save=content=>{const contract=path.join(repo,'contracts/api.yaml');if(content===null)rmSync(contract);else writeFileSync(contract,content);git('add','.');git('commit','--quiet','-m','test: contract');return git('rev-parse','HEAD');};
  const base=save(before),candidate=save(after);
  const event=path.join(repo,'event.json');writeFileSync(event,JSON.stringify({pull_request:{base:{sha:base},head:{sha:'must-not-use-pr-head'}}}));
  const outputs=path.join(repo,'outputs');writeFileSync(outputs,'');
  const run=(overrides={})=>spawnSync('node',[path.join(project,'scripts/action-runner.mjs')],{cwd:repo,encoding:'utf8',timeout:60000,
    env:{...process.env,GITHUB_ACTIONS:'true',GITHUB_WORKSPACE:repo,GITHUB_EVENT_NAME:'pull_request',GITHUB_EVENT_PATH:event,GITHUB_SHA:candidate,
      GITHUB_OUTPUT:outputs,GITHUB_STEP_SUMMARY:path.join(repo,'summary.md'),GUARD_JAR:jar,GUARD_JAR_SHA256:hash,
      GUARD_BASE:'',GUARD_CANDIDATE:'',GUARD_ROOT:'contracts',...overrides}});
  return {repo,run,base,candidate,outputs};
}
const baseline=readFileSync(path.join(project,'src/test/resources/openapi/parameters-baseline.yaml'),'utf8');
const required=readFileSync(path.join(project,'src/test/resources/openapi/parameters-required.yaml'),'utf8');
for (const [name,content,code] of [['compatible',baseline.replace('title:','title: '),0],['breaking',required,1],['invalid','not an OpenAPI contract',2],['review',baseline.replace('type: integer','type: object'),3]]) {
  test(`external consumer preserves ${name} exit and writes outputs and reports`,t=>{
    const f=consumer(t,baseline,content);const r=f.run();assert.equal(r.status,code,r.stderr+r.stdout);
    const outputs=readFileSync(f.outputs,'utf8');assert.match(outputs,new RegExp(`exit-code=${code}`));
    const reportPath=outputs.match(/^report-path=(.+)$/m)[1];const report=JSON.parse(readFileSync(reportPath,'utf8'));
    assert.equal(report.base,f.base);assert.equal(report.candidate,f.candidate);
    assert.ok(readFileSync(path.join(f.repo,'summary.md'),'utf8').includes('### Findings'));
    if(code===1)assert.match(r.stdout,/::error file=contracts\/api.yaml,line=/);
  });
}
test('checksum mismatch refuses execution and does not expose stale reports',t=>{
  const f=consumer(t,baseline,required);const r=f.run({GUARD_JAR_SHA256:'0'.repeat(64)});
  assert.equal(r.status,2);assert.match(r.stderr,/SHA256 mismatch/);assert.equal(readFileSync(f.outputs,'utf8'),'');
});
test('unsupported events require explicit base and candidate; merge queue uses event baseline',t=>{
  const f=consumer(t,baseline,required);assert.equal(f.run({GITHUB_EVENT_NAME:'push'}).status,2);
  assert.equal(f.run({GITHUB_EVENT_NAME:'workflow_dispatch',GUARD_BASE:f.base,GUARD_CANDIDATE:f.candidate}).status,1);
  const event=path.join(f.repo,'merge.json');writeFileSync(event,JSON.stringify({merge_group:{base_sha:f.base}}));
  assert.equal(f.run({GITHUB_EVENT_NAME:'merge_group',GITHUB_EVENT_PATH:event}).status,1);
});
test('metadata uses pinned setup, environment inputs, outputs and no consumer build',()=>{
  const action=readFileSync(path.join(project,'action.yml'),'utf8');
  assert.match(action,/using: composite/);assert.match(action,/actions\/setup-java@[a-f0-9]{40}/);
  assert.match(action,/distribution: corretto/);assert.match(action,/java-version: '25'/);
  assert.match(action,/steps.compare.outputs.report-path/);assert.match(action,/GUARD_ROOT: \$\{\{ inputs.contracts-root }}/);
  assert.ok(!/mvn|mvnd|continue-on-error|pull_request_target/.test(action));
  assert.match(action,/run: node "\$GITHUB_ACTION_PATH\/scripts\/action-runner.mjs"/);
  assert.match(action,/fail-on-breaking:[\s\S]*default: 'true'/);
  assert.match(action,/GUARD_FAIL_ON_BREAKING: \$\{\{ inputs.fail-on-breaking }}/);
  assert.match(action,/pr-comment:[\s\S]*default: 'false'/);
  assert.match(action,/GUARD_PR_COMMENT: \$\{\{ inputs.pr-comment }}/);
  assert.match(action,/GUARD_GITHUB_TOKEN: \$\{\{ inputs.github-token }}/);
});

for (const [name,content,engineCode,checkCode] of [['compatible',baseline.replace('title:','title: '),0,0],['breaking',required,1,0],['invalid','not an OpenAPI contract',2,2],['review',baseline.replace('type: integer','type: object'),3,3],['removed',null,1,0]]) {
  test(`advisory policy preserves ${name} report and only allows proven breaking changes`,t=>{
    const f=consumer(t,baseline,content);const r=f.run({GUARD_FAIL_ON_BREAKING:'false'});
    assert.equal(r.status,checkCode,r.stderr+r.stdout);
    const outputs=readFileSync(f.outputs,'utf8');
    assert.match(outputs,new RegExp(`exit-code=${engineCode}\\n`));
    assert.match(outputs,new RegExp(`check-exit-code=${checkCode}\\n`));
    const report=JSON.parse(readFileSync(outputs.match(/^report-path=(.+)$/m)[1],'utf8'));
    assert.equal(report.exitCode,engineCode);
    if(engineCode===1) {
      assert.match(r.stdout,name==='removed'?/::warning title=CONTRACT_FILE_REMOVED/:/::warning file=contracts\/api.yaml,line=/);
      assert.ok(!r.stdout.includes('::error '));
      assert.match(r.stdout,/::group::Candidate annotations/);
      assert.match(r.stdout,/Finding 1 - Lines/);
      assert.ok(!r.stdout.includes('&#95;'));
      assert.match(readFileSync(outputs.match(/^summary-path=(.+)$/m)[1],'utf8'),/Breaking changes are allowed/);
    }
  });
}

test('invalid failure policy is rejected rather than silently allowing changes',t=>{
  const f=consumer(t,baseline,required);
  const r=f.run({GUARD_FAIL_ON_BREAKING:'yes'});
  assert.equal(r.status,2);assert.match(r.stderr,/fail-on-breaking/);
  assert.equal(readFileSync(f.outputs,'utf8'),'');
});

test('allowing breaking changes never hides review pending alongside a proven break',t=>{
  const f=consumer(t,baseline,required.replace('type: integer','type: object'));
  const r=f.run({GUARD_FAIL_ON_BREAKING:'false'});
  assert.equal(r.status,3,r.stderr+r.stdout);
  const outputs=readFileSync(f.outputs,'utf8');
  assert.match(outputs,/exit-code=1\n/);assert.match(outputs,/check-exit-code=3\n/);
  assert.match(r.stdout,/Review also required/);
});

test('opt-in comment skips forks without network and preserves comparison result',t=>{
  const f=consumer(t,baseline,required);
  const event=path.join(f.repo,'fork.json');
  writeFileSync(event,JSON.stringify({number:1,pull_request:{base:{sha:f.base},head:{sha:f.candidate,repo:{full_name:'fork/api'}}}}));
  const result=f.run({GUARD_PR_COMMENT:'true',GUARD_FAIL_ON_BREAKING:'false',GUARD_GITHUB_TOKEN:'',GITHUB_REPOSITORY:'owner/api',GITHUB_EVENT_PATH:event});
  assert.equal(result.status,0,result.stdout+result.stderr);
  assert.match(readFileSync(f.outputs,'utf8'),/comment-status=skipped-fork/);
});

test('missing comment permission credentials fails visibility explicitly while retaining report paths',t=>{
  const f=consumer(t,baseline,required);
  const event=path.join(f.repo,'same-repo.json');
  writeFileSync(event,JSON.stringify({number:1,pull_request:{base:{sha:f.base},head:{sha:f.candidate,repo:{full_name:'owner/api'}}}}));
  const result=f.run({GUARD_PR_COMMENT:'true',GUARD_FAIL_ON_BREAKING:'false',GUARD_GITHUB_TOKEN:'',GITHUB_REPOSITORY:'owner/api',GITHUB_EVENT_PATH:event});
  assert.equal(result.status,2,result.stdout+result.stderr);
  const outputs=readFileSync(f.outputs,'utf8');
  assert.match(outputs,/report-path=/);assert.match(outputs,/check-exit-code=2/);assert.match(outputs,/comment-status=failed/);
  assert.match(result.stderr,/token/);
});
