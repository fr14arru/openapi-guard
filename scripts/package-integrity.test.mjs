import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,writeFileSync,mkdirSync,readFileSync,rmSync,existsSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {createHash} from 'node:crypto';
const project=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
function packaged(t){const root=mkdtempSync(path.join(tmpdir(),'guard integrity '));t.after(()=>rmSync(root,{recursive:true,force:true}));
  const bundle=path.join(root,'bundle');const prepared=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/prepare-evaluation.ps1'),'-Destination',bundle],{cwd:project,encoding:'utf8',timeout:60000});
  assert.equal(prepared.status,0,prepared.stderr);return {root,bundle};}
const verify=directory=>spawnSync('node',[path.join(project,'scripts/verify-package.mjs'),directory],{encoding:'utf8',timeout:30000});
test('complete package verifies every file and refuses missing, changed and extra files',t=>{
  const {bundle}=packaged(t);assert.equal(verify(bundle).status,0);
  assert.ok(JSON.parse(readFileSync(path.join(bundle,'manifest.json'),'utf8')).integrity.some(entry=>entry.path==='scripts/pr-comment.mjs'));
  const script=path.join(bundle,'scripts/git-contracts.mjs');const original=readFileSync(script);writeFileSync(script,'tampered');
  assert.notEqual(verify(bundle).status,0);writeFileSync(script,original);
  const extra=path.join(bundle,'unexpected.txt');writeFileSync(extra,'extra');assert.notEqual(verify(bundle).status,0);rmSync(extra);
  rmSync(path.join(bundle,'examples/base.yaml'));assert.notEqual(verify(bundle).status,0);
});
test('manifest cannot redirect verification outside package or omit required runtime files',t=>{
  const {bundle}=packaged(t);const manifestPath=path.join(bundle,'manifest.json');const original=readFileSync(manifestPath,'utf8');
  const manifest=JSON.parse(original);manifest.integrity[0].path='../outside';writeFileSync(manifestPath,JSON.stringify(manifest));assert.notEqual(verify(bundle).status,0);
  const omitted=JSON.parse(original);omitted.integrity=omitted.integrity.filter(entry=>entry.path!=='guard.jar');writeFileSync(manifestPath,JSON.stringify(omitted));assert.notEqual(verify(bundle).status,0);
});
test('ZIP transport checks external checksum, extracts fresh destination and verifies package',t=>{
  const {root,bundle}=packaged(t);const archive=path.join(root,'delivery.zip');
  const packed=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/archive-evaluation.ps1'),'-Package',bundle,'-Archive',archive],{encoding:'utf8',timeout:60000});assert.equal(packed.status,0,packed.stderr);
  const checksum=readFileSync(archive+'.sha256','utf8').trim().split(/\s+/)[0];const destination=path.join(root,'installed');
  const install=hash=>spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/install-evaluation.ps1'),'-Archive',archive,'-ExpectedSha256',hash,'-Destination',destination],{encoding:'utf8',timeout:60000});
  assert.notEqual(install('0'.repeat(64)).status,0);assert.equal(install(checksum).status,0);assert.equal(verify(destination).status,0);
  assert.notEqual(install(checksum).status,0);
});
test('copied bundle Action refuses modified companion script before producing consumer artifacts',t=>{
  const {root,bundle}=packaged(t);const workspace=path.join(root,'consumer');mkdirSync(workspace);
  writeFileSync(path.join(bundle,'scripts/github-presentation.mjs'),'throw Error("tampered");');
  const run=spawnSync('node',[path.join(bundle,'scripts/action-runner.mjs')],{encoding:'utf8',env:{...process.env,GITHUB_WORKSPACE:workspace,
    GUARD_BASE:'base',GUARD_CANDIDATE:'candidate',GUARD_JAR:'',GUARD_JAR_SHA256:''},timeout:30000});
  assert.equal(run.status,2);assert.match(run.stderr,/Package integrity mismatch:/);
});
test('unsafe ZIP names and duplicates are rejected before creating a destination',t=>{
  const root=mkdtempSync(path.join(tmpdir(),'guard unsafe zip '));t.after(()=>rmSync(root,{recursive:true,force:true}));
  const fixture=path.join(root,'fixture.ps1');writeFileSync(fixture,`param([string]$Archive,[string]$Entry,[switch]$Duplicate)
$zip=[IO.Compression.ZipFile]::Open($Archive,[IO.Compression.ZipArchiveMode]::Create)
try { $zip.CreateEntry($Entry) | Out-Null; if($Duplicate){$zip.CreateEntry($Entry) | Out-Null} } finally {$zip.Dispose()}
`);
  for(const [index,name] of ['../escaped.txt','/absolute.txt','C:/outside.txt','a\\..\\outside.txt','duplicate.txt'].entries()) {
    const archive=path.join(root,`unsafe-${index}.zip`),destination=path.join(root,`dest-${index}`);
    const created=spawnSync('pwsh',['-NoProfile','-File',fixture,'-Archive',archive,'-Entry',name,...(index===4?['-Duplicate']:[])],{encoding:'utf8'});assert.equal(created.status,0,created.stderr);
    const checksum=createHash('sha256').update(readFileSync(archive)).digest('hex');
    const installed=spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/install-evaluation.ps1'),'-Archive',archive,'-ExpectedSha256',checksum,'-Destination',destination],{encoding:'utf8'});
    assert.notEqual(installed.status,0);assert.equal(existsSync(destination),false);
  }
});
