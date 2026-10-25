import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,existsSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {prepareTestDelivery} from './prepare-test-delivery.mjs';
import {verifyPackage} from './verify-package.mjs';

const repository=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const hash=bytes=>createHash('sha256').update(bytes).digest('hex');
const packagingCommit='b'.repeat(40);

// Synthetic packages test delivery invariants; the actual ZIP is exercised with the real JAR separately.
function fixture(t) {
  const root=mkdtempSync(path.join(tmpdir(),'guard test delivery '));
  t.after(()=>rmSync(root,{recursive:true,force:true}));
  const source=path.join(root,'source'),destination=path.join(root,'delivery');
  mkdirSync(source);
  const files={
    'guard.jar':'synthetic binary', 'action.yml':'name: fixture\n',
    'scripts/action-runner.mjs':'// runner','scripts/git-contracts.mjs':'// git',
    'scripts/github-presentation.mjs':'// report','scripts/pr-comment.mjs':'// comment',
    'scripts/verify-package.mjs':'// verifier',
    'README.txt':'old owner approval pending','THIRD-PARTY-REVIEW.md':'old pending review',
    'PRODUCT-LICENSE-STATUS.txt':'OWNER APPROVAL PENDING','PILOT-TERMS-DRAFT.txt':'old draft',
    'THIRD-PARTY-NOTICES.md':'Original attribution and corresponding source',
    'examples/base.yaml':'baseline','examples/compatible.yaml':'compatible',
    'examples/breaking.yaml':'breaking','examples/review.yaml':'review',
    'third-party/00-00.txt':'Original upstream license',
    'third-party/licenses/files/upstream.txt':'Original upstream notice',
    'third-party/licenses/sources/upstream-sources.jar':'Original upstream source bytes',
  };
  const catalog={files:[{path:'files/upstream.txt',sha256:hash(files['third-party/licenses/files/upstream.txt'])},
    {path:'sources/upstream-sources.jar',sha256:hash(files['third-party/licenses/sources/upstream-sources.jar'])}]};
  files['third-party/licenses/catalog.json']=JSON.stringify(catalog);
  const proof={verified:true,sourceCommit:'a'.repeat(40),jarSha256:hash(files['guard.jar']),version:'0.1.0-rc.3'};
  files['build-proof.json']=JSON.stringify(proof);
  const manifest={formatVersion:2,sourceCommit:proof.sourceCommit,version:proof.version,buildVerified:true,
    workingTreeDirty:false,jarSha256:proof.jarSha256,dependencies:[],loader:{artifact:'fixture',sha256:'c'.repeat(64)},
    attributions:{status:'TECHNICAL_EVIDENCE_COMPLETE',components:1,catalogSha256:hash(files['third-party/licenses/catalog.json'])}};
  function persist(){
    manifest.integrity=Object.entries(files).map(([name,bytes])=>({path:name,sha256:hash(bytes)}));
    for(const [name,bytes] of Object.entries(files)) {const file=path.join(source,name);mkdirSync(path.dirname(file),{recursive:true});writeFileSync(file,bytes);}
    writeFileSync(path.join(source,'manifest.json'),JSON.stringify(manifest));
  }
  persist();
  return {root,source,destination,files,manifest,persist};
}

test('test delivery preserves verified engine provenance and third-party bytes with approved terms',t=>{
  const f=fixture(t),original=readFileSync(path.join(f.source,'manifest.json'));
  const report=prepareTestDelivery(f.source,f.destination,{packagingCommit});
  const result=verifyPackage(f.destination);
  assert.equal(result.jarSha256,f.manifest.jarSha256);
  assert.equal(result.sourceCommit,f.manifest.sourceCommit);
  assert.equal(result.version,'0.1.0-rc.3');
  assert.equal(result.delivery.packagingCommit,packagingCommit);
  assert.equal(result.delivery.mode,'LEMON_SQUEEZY_TEST_ONLY');
  assert.equal(result.delivery.publicationApproved,false);
  assert.equal(result.delivery.privacyPolicyStatus,'PENDING_NOT_INCLUDED');
  assert.equal(result.delivery.license.approvalCommit,'06a9a3bdcae373c99a5f0b793e190cb62cfb8794');
  for(const name of ['guard.jar','build-proof.json','THIRD-PARTY-NOTICES.md','third-party/licenses/sources/upstream-sources.jar'])
    assert.deepEqual(readFileSync(path.join(f.destination,name)),Buffer.from(f.files[name]));
  assert.deepEqual(readFileSync(path.join(f.source,'manifest.json')),original);
  assert.ok(!existsSync(path.join(f.destination,'PILOT-TERMS-DRAFT.txt')));
  const license=readFileSync(path.join(f.destination,'LICENSE.txt'),'utf8');
  for(const clause of ['30 días naturales','4,99 EUR/año/repositorio','todos los colaboradores','EPL-2.0'])assert.ok(license.includes(clause));
  assert.doesNotMatch(license,/BORRADOR|Este borrador|Ficha que debe|Pendientes antes|Propuesta pendiente|titular debe revisar/);
  assert.equal(result.delivery.license.sha256,hash(license));
  assert.equal(report.integrity.length,result.integrity.length);
});

test('tampered input, missing proof and contradictory build provenance are refused before output',t=>{
  for(const mode of ['tampered','missing','contradictory']) {
    const f=fixture(t);
    if(mode==='tampered')writeFileSync(path.join(f.source,'guard.jar'),'changed');
    if(mode==='missing'){delete f.files['build-proof.json'];f.persist();}
    if(mode==='contradictory'){f.files['build-proof.json']=JSON.stringify({verified:true,sourceCommit:'d'.repeat(40),jarSha256:f.manifest.jarSha256,version:f.manifest.version});f.persist();}
    assert.throws(()=>prepareTestDelivery(f.source,f.destination,{packagingCommit}));
    assert.equal(existsSync(f.destination),false);
  }
});

test('unrecognized files cannot carry private legal documents into a delivery even with valid integrity',t=>{
  for(const name of ['privacidad.md','legal-setup/datos-del-titular.md','third-party/licenses/files/private.txt']) {
    const f=fixture(t);f.files[name]='private sentinel';f.persist();
    assert.throws(()=>prepareTestDelivery(f.source,f.destination,{packagingCommit}),/Unexpected delivery input/);
    assert.equal(existsSync(f.destination),false);
  }
});

test('license changes or altered corresponding source cannot silently become approved',t=>{
  const f=fixture(t),approval=path.join(f.root,'changed-license.md');
  writeFileSync(approval,'unapproved terms');
  assert.throws(()=>prepareTestDelivery(f.source,f.destination,{packagingCommit,approvedDocument:approval}),/approved license/);
  assert.equal(existsSync(f.destination),false);
  f.files['third-party/licenses/sources/upstream-sources.jar']='changed source';f.persist();
  assert.throws(()=>prepareTestDelivery(f.source,f.destination,{packagingCommit}),/Attribution evidence/);
});

test('existing output is preserved and packaging commit must be explicit',t=>{
  const f=fixture(t);mkdirSync(f.destination);writeFileSync(path.join(f.destination,'keep.txt'),'keep');
  assert.throws(()=>prepareTestDelivery(f.source,f.destination,{packagingCommit}),/exists/);
  assert.equal(readFileSync(path.join(f.destination,'keep.txt'),'utf8'),'keep');
  assert.throws(()=>prepareTestDelivery(f.source,path.join(f.root,'other')),/packaging commit/);
});

test('output cannot be placed inside the verified input package',t=>{
  const f=fixture(t),nested=path.join(f.source,'nested','delivery');
  assert.throws(()=>prepareTestDelivery(f.source,nested,{packagingCommit}),/outside the source/);
  assert.equal(existsSync(nested),false);
  verifyPackage(f.source);
});

test('new delivery survives ZIP checksum and extraction; changing its license fails integrity',t=>{
  const f=fixture(t);prepareTestDelivery(f.source,f.destination,{packagingCommit});
  const archive=path.join(f.root,'store test.zip');
  const run=(script,args)=>spawnSync('pwsh',['-NoProfile','-File',path.join(repository,'scripts',script),...args],{encoding:'utf8',timeout:60000});
  const zip=run('archive-evaluation.ps1',['-Package',f.destination,'-Archive',archive]);assert.equal(zip.status,0,zip.stderr);
  const expected=readFileSync(archive+'.sha256','utf8').trim().split(/\s+/)[0];
  const installed=path.join(f.root,'extracted');
  const install=run('install-evaluation.ps1',['-Archive',archive,'-ExpectedSha256',expected,'-Destination',installed]);assert.equal(install.status,0,install.stderr);
  assert.equal(verifyPackage(installed).delivery.mode,'LEMON_SQUEEZY_TEST_ONLY');
  writeFileSync(path.join(installed,'LICENSE.txt'),'changed');
  assert.throws(()=>verifyPackage(installed),/integrity mismatch/);
});
