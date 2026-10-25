import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync, mkdirSync, writeFileSync, rmSync, readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {compareRevisions, summarize, parseArguments} from './git-contracts.mjs';
import {fileURLToPath} from 'node:url';
import {annotations, findingSummary} from './github-presentation.mjs';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');

// Real Git repositories establish the oracle; callbacks isolate orchestration from schema rules.
function fixture(t, before, after) {
  const repo = mkdtempSync(path.join(tmpdir(), 'guard git '));
  t.after(() => rmSync(repo, {recursive:true, force:true}));
  function git(...args) {
    const p = spawnSync('git', args, {cwd:repo, encoding:'utf8'});
    assert.equal(p.status, 0, p.stderr);
    return p.stdout.trim();
  }
  git('init', '--quiet');
  git('config', 'user.name', 'Guard fixture');
  git('config', 'user.email', 'fixture@example.invalid');
  const save = files => {
    for (const [name, content] of Object.entries(files)) {
      mkdirSync(path.dirname(path.join(repo,name)), {recursive:true});
      writeFileSync(path.join(repo,name), content);
    }
    git('add', '--all');
    git('commit', '--quiet', '--allow-empty', '-m', 'test: fixture');
    return git('rev-parse', 'HEAD');
  };
  const base = save(before);
  for (const name of Object.keys(before)) rmSync(path.join(repo,name));
  const candidate = save(after);
  return {repo, base, candidate, git};
}
const doc = version => `openapi: 3.0.3\ninfo:\n  title: Fixture\n  version: '${version}'\npaths: {}\n`;
const ok = {exitCode:0, report:{status:'COMPLETED', verdict:'COMPATIBLE'}};
test('metadata-only edits invoke comparator with exact Git blobs, ignoring dirty files', async t => {
  const f = fixture(t, {'contracts/API ñ.yaml':doc(1)}, {'contracts/API ñ.yaml':doc(2)});
  writeFileSync(path.join(f.repo,'contracts/API ñ.yaml'), 'dirty worktree');
  const r = await compareRevisions({...f, compare:(old, next) => {
    assert.equal(readFileSync(old,'utf8'),doc(1)); assert.equal(readFileSync(next,'utf8'),doc(2)); return ok;
  }});
  assert.equal(r.exitCode,0); assert.equal(r.files[0].kind,'MODIFIED');
});
test('no contract changes succeeds without invoking Java', async t => {
  const f = fixture(t, {'README.md':'one'}, {'README.md':'two'});
  const r = await compareRevisions({...f, compare:() => assert.fail('unexpected comparison')});
  assert.equal(r.exitCode,0); assert.deepEqual(r.files,[]);
});
test('deleting a contract blocks with explicit integration rule', async t => {
  const f=fixture(t, {'contracts/api.json':'{}'}, {});
  const r=await compareRevisions({...f});
  assert.equal(r.exitCode,1); assert.equal(r.files[0].ruleId,'CONTRACT_FILE_REMOVED');
  assert.equal(r.files[0].severity,'ERROR');
});
test('new contracts are validated against themselves', async t => {
  const f=fixture(t, {}, {'contracts/new.yml':doc(1)});
  const r=await compareRevisions({...f, compare:(old,next) => {
    assert.equal(readFileSync(old,'utf8'),readFileSync(next,'utf8')); return ok;
  }});
  assert.equal(r.exitCode,0); assert.equal(r.files[0].kind,'ADDED');
});
test('identical renames compare old and new paths', async t => {
  const f=fixture(t, {'contracts/old.yaml':doc(1)}, {'contracts/new.yaml':doc(1)});
  const r=await compareRevisions({...f, compare:() => ok});
  assert.equal(r.files.length,1); assert.equal(r.files[0].kind,'RENAMED'); assert.equal(r.exitCode,0);
});
test('moving outside watched root blocks; moving inside validates', async t => {
  for (const outside of [true,false]) {
    const a=outside?'contracts/a.yaml':'elsewhere/a.yaml', b=outside?'elsewhere/a.yaml':'contracts/a.yaml';
    const f=fixture(t, {[a]:doc(1)}, {[b]:doc(1)});
    const r=await compareRevisions({...f, compare:() => ok});
    assert.equal(r.exitCode,outside?1:0);
  }
});
for (const code of [1,2,3]) test(`comparison exit ${code} blocks and preserves engine report`, async t => {
  const f=fixture(t, {'contracts/a.yaml':doc(1)}, {'contracts/a.yaml':doc(2)});
  const report={status:code===2?'FAILED':code===3?'PARTIAL':'COMPLETED', verdict:code===1?'BREAKING':'INDETERMINATE'};
  const r=await compareRevisions({...f, compare:() => ({exitCode:code,report})});
  assert.equal(r.exitCode,code); assert.deepEqual(r.files[0].report,report);
});
test('failed subprocess does not prevent remaining files from being reported', async t => {
  const f=fixture(t, {'contracts/a.yaml':doc(1),'contracts/z.yaml':doc(1)}, {'contracts/a.yaml':doc(2),'contracts/z.yaml':doc(2)});
  let calls=0;
  const r=await compareRevisions({...f, compare:() => {if (++calls===1) throw Error('timeout'); return ok;}});
  assert.equal(r.exitCode,2); assert.equal(r.files.length,2); assert.equal(calls,2);
});
test('selection supports nested files and excludes unrelated extensions', async t => {
  const f=fixture(t, {}, {'contracts/nested/a.JSON':doc(1),'contracts/notes.md':'text','other/api.yaml':doc(1)});
  const r=await compareRevisions({...f, compare:() => ok});
  assert.equal(r.files.length,1);
});
test('unknown revisions and traversal roots fail closed', async t => {
  const f=fixture(t, {}, {});
  await assert.rejects(compareRevisions({...f, base:'--help'}));
  await assert.rejects(compareRevisions({...f, root:'../'}));
});
test('CLI rejects unsupported, missing and repeated arguments', () => {
  for (const args of [[],['--wat','x'],['--base','x'],['--base','a','--candidate','b','--base','c']]) assert.throws(() => parseArguments(args));
});
test('summary escapes Markdown paths and identifies blockers', () => {
  const summary=summarize({base:'a',candidate:'b',exitCode:3,files:[{path:'contracts/a|<b>.yaml',kind:'MODIFIED',exitCode:3}]});
  assert.match(summary,/3/); assert.ok(!summary.includes('a|<b>')); assert.match(summary,/MODIFIED/);
});
test('aggregate precedence: execution errors, breakages, uncertainty, success', async t => {
  for (const [codes,expected] of [[[1,2,3],2],[[3,1],1],[[0,3],3],[[0,0],0]]) {
    const before={},after={}; codes.forEach((_,i)=>{before[`contracts/${i}.yaml`]=doc(1);after[`contracts/${i}.yaml`]=doc(2);});
    const f=fixture(t,before,after);let i=0;
    assert.equal((await compareRevisions({...f,compare:()=>({...ok,exitCode:codes[i++]})})).exitCode,expected);
  }
});
for (const [name,code] of [['compatible',0],['breaking',1],['review',3],['invalid',2]]) {
  test(`Git to packaged Java to report: ${name}`, async t => {
    const baseline=readFileSync(path.join(project,'examples/catalog/base.yaml'),'utf8');
    const candidate=name==='invalid'?'not an OpenAPI contract':readFileSync(path.join(project,`examples/catalog/${name}.yaml`),'utf8');
    const f=fixture(t, {'contracts/catalog.yaml':baseline}, {'contracts/catalog.yaml':candidate});
    const r=await compareRevisions({...f,jar:(process.env.GUARD_TEST_JAR || path.join(project,'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'))});
    assert.equal(r.exitCode,code); assert.ok(r.files[0].report,JSON.stringify(r));
    assert.equal(r.files[0].report.profileId,'operations-data-response-policy-v1');
    assert.ok(Array.isArray(r.files[0].findings));
    if (name==='breaking') {
      assert.ok(r.files[0].findings.some(finding=>finding.positions.length));
      assert.ok(annotations(r).some(line=>line.includes('file=contracts/catalog.yaml,line=')));
      assert.match(findingSummary(r),/BREAKING/);
    }
  });
}
test('CLI writes structured failure and summary for invalid revisions', t => {
  const f=fixture(t,{},{});
  const p=spawnSync(process.execPath,[path.join(project,'scripts/git-contracts.mjs'),'--repo',f.repo,'--base','missing','--candidate',f.candidate],{encoding:'utf8',env:{...process.env,GITHUB_STEP_SUMMARY:''}});
  assert.equal(p.status,2);
  assert.equal(JSON.parse(readFileSync(path.join(f.repo,'target/git-guard/report.json'),'utf8')).exitCode,2);
  assert.match(readFileSync(path.join(f.repo,'target/git-guard/summary.md'),'utf8'),/Git failed/);
});

test('CLI emits annotations and step summary for actual Git snapshots and preserves failing exit', t => {
  const before=readFileSync(path.join(project,'src/test/resources/openapi/parameters-baseline.yaml'),'utf8');
  const after=readFileSync(path.join(project,'src/test/resources/openapi/parameters-required.yaml'),'utf8');
  // Keep enough unchanged content for Git's real similarity heuristic to identify the rename.
  const padding='\n'+Array.from({length:40},(_,i)=>`# unchanged fixture context ${i}`).join('\n');
  const f=fixture(t,{'contracts/old.yaml':before+padding},{'contracts/new.yaml':after+padding});
  const summary=path.join(f.repo,'step-summary.md');
  const p=spawnSync(process.execPath,[path.join(project,'scripts/git-contracts.mjs'),'--repo',f.repo,'--base',f.base,'--candidate',f.candidate,'--jar',(process.env.GUARD_TEST_JAR || path.join(project,'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'))],
    {encoding:'utf8',timeout:60000,env:{...process.env,GITHUB_ACTIONS:'true',GITHUB_STEP_SUMMARY:summary}});
  assert.equal(p.status,1,p.stderr);
  assert.match(p.stdout,/::error file=contracts\/new.yaml,line=6,/);
  assert.match(p.stdout,/::error file=contracts\/new.yaml,line=9,/);
  assert.match(p.stdout,/::error file=contracts\/new.yaml,line=18,/);
  assert.ok(!p.stdout.includes('file=contracts/old.yaml'));
  const report=JSON.parse(readFileSync(path.join(f.repo,'target/git-guard/report.json'),'utf8'));
  assert.equal(report.files[0].kind,'RENAMED');
  assert.equal(report.files[0].baselinePath,'contracts/old.yaml');
  assert.match(readFileSync(summary,'utf8'),/### Findings/);
  assert.match(readFileSync(summary,'utf8'),/PARAMETER&#95;BECAME&#95;REQUIRED/);
});
test('missing JAR yields execution error rather than compatibility', async t => {
  const f=fixture(t,{}, {'contracts/a.yaml':doc(1)});
  const r=await compareRevisions({...f,jar:path.join(f.repo,'missing.jar')});
  assert.equal(r.exitCode,2); assert.ok(r.files[0].error);
});
test('symlink contract is rejected without reading its target', async t => {
  const f=fixture(t,{'contracts/a.yaml':doc(1)},{'contracts/a.yaml':doc(2)});
  const blob=spawnSync('git',['hash-object','-w','--stdin'],{cwd:f.repo,input:'../../private.yaml',encoding:'utf8'});
  assert.equal(blob.status,0);
  f.git('update-index','--cacheinfo',`120000,${blob.stdout.trim()},contracts/a.yaml`);
  f.git('commit','--quiet','-m','test: symlink fixture');
  const r=await compareRevisions({...f,candidate:f.git('rev-parse','HEAD'),compare:()=>assert.fail('unsafe comparison')});
  assert.equal(r.exitCode,2);assert.match(r.files[0].error,/regular tracked file/);
});
test('invalid UTF-8 and oversized snapshots fail before Java', async t => {
  for (const content of [Buffer.from([0xff,0xfe]),Buffer.alloc(5*1024*1024+1,32)]) {
    const f=fixture(t,{}, {'contracts/a.yaml':content});
    const r=await compareRevisions({...f,compare:()=>assert.fail('unsafe comparison')});
    assert.equal(r.exitCode,2);
  }
});
test('custom root and literal bracketed paths are supported', async t => {
  const f=fixture(t,{'api specs/a[1].yaml':doc(1)},{'api specs/a[1].yaml':doc(2)});
  const r=await compareRevisions({...f,root:'api specs',compare:()=>ok});
  assert.equal(r.exitCode,0);assert.equal(r.files.length,1);
});
