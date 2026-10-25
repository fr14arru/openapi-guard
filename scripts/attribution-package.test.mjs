import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,rmSync,readFileSync,writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
import {verifyPackage} from './verify-package.mjs';
const project=fileURLToPath(new URL('../',import.meta.url));
const jar=process.env.GUARD_TEST_JAR||path.join(project,'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar');
function temporary(t) {const root=mkdtempSync(path.join(tmpdir(),'guard attribution package '));t.after(()=>rmSync(root,{recursive:true,force:true}));return root;}
const prepare=(jar,output)=>spawnSync('pwsh',['-NoProfile','-File',path.join(project,'scripts/prepare-evaluation.ps1'),'-Jar',jar,'-Destination',output],{encoding:'utf8',timeout:60000});
test('real package includes loader, human-readable notices and sources without class files as license text',t=>{
  const output=path.join(temporary(t),'bundle');const p=prepare(jar,output);assert.equal(p.status,0,p.stderr+p.stdout);
  const manifest=verifyPackage(output);
  assert.equal(manifest.attributions.status,'TECHNICAL_EVIDENCE_COMPLETE');
  assert.equal(manifest.attributions.components,manifest.dependencies.length+1);
  assert.equal(manifest.attributions.productLicenseStatus,'MIT');
  assert.match(manifest.loader.artifact,/^spring-boot-loader-/);
  for(const dep of manifest.dependencies)assert.ok(dep.embeddedEvidence.every(e=>!e.originalPath.endsWith('.class')));
  const catalog=JSON.parse(readFileSync(path.join(output,manifest.attributions.catalogFile),'utf8'));
  const notices=readFileSync(path.join(output,'THIRD-PARTY-NOTICES.md'),'utf8');
  for(const c of [...catalog.dependencies,catalog.loader]) {
    assert.ok(notices.includes(c.artifact));
    for(const file of c.files)assert.ok(readFileSync(path.join(output,'third-party/licenses',file)).length>0);
    if(c.selectedLicense==='EPL-2.0')assert.ok(readFileSync(path.join(output,'third-party/licenses',c.source)).length>0);
  }
});
for(const entry of ['BOOT-INF/lib/unreviewed-1.jar','org/springframework/boot/loader/launch/JarLauncher.class']) {
  test(`packager rejects unknown library or altered loader: ${entry}`,t=>{
    const root=temporary(t),modified=path.join(root,'modified.jar');
    // Rebuild the outer ZIP; in-place updates can invalidate Boot's aligned nested JAR headers.
    const script=path.join(root,'alter.ps1');writeFileSync(script,`param($Original,$Jar,$Entry)\n$z=[IO.Compression.ZipFile]::OpenRead($Original);$out=[IO.Compression.ZipFile]::Open($Jar,[IO.Compression.ZipArchiveMode]::Create)\ntry { foreach($e in $z.Entries) {if($e.FullName -eq $Entry){continue};$n=$out.CreateEntry($e.FullName);$i=$e.Open();$o=$n.Open();try{$i.CopyTo($o)}finally{$i.Dispose();$o.Dispose()}}; $n=$out.CreateEntry($Entry);$o=$n.Open();try {if($Entry -like 'BOOT-INF/*'){$e=$z.Entries|Where-Object FullName -like 'BOOT-INF/lib/*.jar'|Select-Object -First 1;$i=$e.Open();try{$i.CopyTo($o)}finally{$i.Dispose()}}else{$bytes=[Text.Encoding]::UTF8.GetBytes('changed bytes');$o.Write($bytes)}}finally{$o.Dispose()} }finally{$z.Dispose();$out.Dispose()}\n`);
    const changed=spawnSync('pwsh',['-NoProfile','-File',script,jar,modified,entry],{encoding:'utf8'});assert.equal(changed.status,0,changed.stderr);
    const p=prepare(modified,path.join(root,'bundle'));assert.notEqual(p.status,0);assert.match(p.stderr,/component|loader/i);
  });
}
