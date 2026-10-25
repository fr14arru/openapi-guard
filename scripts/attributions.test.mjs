import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,rmSync,cpSync,existsSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {prepareAttributions} from './prepare-attributions.mjs';

const hash=value=>createHash('sha256').update(value).digest('hex');
function fixture(t) {
  const root=mkdtempSync(path.join(tmpdir(),'guard licenses '));
  t.after(()=>rmSync(root,{recursive:true,force:true}));
  const assets=path.join(root,'licensing'),output=path.join(root,'package');
  mkdirSync(assets);mkdirSync(output);
  const text='An upstream license, preserved verbatim.\n';
  writeFileSync(path.join(assets,'LICENSE.txt'),text);
  const component={artifact:'library-1.jar',sha256:hash('binary'),licenseExpression:'EPL-2.0 OR LGPL-2.1-only',selectedLicense:'EPL-2.0',files:['LICENSE.txt'],source:'source.jar'};
  writeFileSync(path.join(assets,'source.jar'),'source archive');
  const catalog={formatVersion:1,files:[{path:'LICENSE.txt',sha256:hash(text),origin:'upstream'},{path:'source.jar',sha256:hash('source archive'),origin:'upstream sources'}],dependencies:[component],loader:{artifact:'spring-boot-loader-1',sha256:hash('loader'),licenseExpression:'Apache-2.0',selectedLicense:'Apache-2.0',files:['LICENSE.txt']}};
  const manifest={dependencies:[{artifact:component.artifact,sha256:component.sha256}],loader:{artifact:catalog.loader.artifact,sha256:catalog.loader.sha256},distributionStatus:'LOCAL_EVALUATION_NOT_APPROVED_FOR_PUBLICATION'};
  const save=()=>writeFileSync(path.join(assets,'catalog.json'),JSON.stringify(catalog));save();
  return {assets,output,catalog,manifest,save};
}
test('attributions include all pinned components, loader, full texts and corresponding sources',t=>{
  const f=fixture(t);const result=prepareAttributions(f.output,f.manifest,f.assets);
  assert.equal(result.components,2);
  assert.equal(result.productLicenseStatus,'MIT');
  assert.match(readFileSync(path.join(f.output,'THIRD-PARTY-NOTICES.md'),'utf8'),/EPL-2.0/);
  assert.match(readFileSync(path.join(f.output,'THIRD-PARTY-NOTICES.md'),'utf8'),/source.jar/);
  assert.equal(readFileSync(path.join(f.output,'third-party/licenses/LICENSE.txt'),'utf8'),'An upstream license, preserved verbatim.\n');
  assert.equal(readFileSync(path.join(f.output,'third-party/licenses/source.jar'),'utf8'),'source archive');
  assert.equal(f.manifest.distributionStatus,'LOCAL_EVALUATION_NOT_APPROVED_FOR_PUBLICATION');
});
test('unknown, changed, missing or duplicated runtime components cannot reuse a review',t=>{
  for(const mutate of [f=>f.manifest.dependencies.push({artifact:'new.jar',sha256:hash('new')}),f=>f.manifest.dependencies[0].sha256=hash('changed'),f=>f.manifest.dependencies=[],f=>f.manifest.dependencies.push(f.manifest.dependencies[0]),f=>f.manifest.loader.sha256=hash('changed loader')]) {
    const f=fixture(t);mutate(f);assert.throws(()=>prepareAttributions(f.output,f.manifest,f.assets),/component|loader/i);
  }
});
test('missing or altered license/source evidence is rejected before copying',t=>{
  for(const name of ['LICENSE.txt','source.jar']) {const f=fixture(t);writeFileSync(path.join(f.assets,name),'altered');assert.throws(()=>prepareAttributions(f.output,f.manifest,f.assets),/evidence/i);}
  const f=fixture(t);rmSync(path.join(f.assets,'source.jar'));assert.throws(()=>prepareAttributions(f.output,f.manifest,f.assets),/evidence/i);
});
test('catalog cannot escape its directory or reference untracked evidence',t=>{
  for(const mutate of [f=>f.catalog.files[0].path='../outside',f=>f.catalog.dependencies[0].files=['not-catalogued.txt'],f=>delete f.catalog.dependencies[0].source]) {
    const f=fixture(t);mutate(f);f.save();assert.throws(()=>prepareAttributions(f.output,f.manifest,f.assets),/evidence|source/i);
  }
});
test('Git checkouts with automatic line conversion preserve every licensed evidence byte',t=>{
  const root=mkdtempSync(path.join(tmpdir(),'guard licensing checkout '));t.after(()=>rmSync(root,{recursive:true,force:true}));
  const project=fileURLToPath(new URL('../',import.meta.url)),repo=path.join(root,'repo'),clone=path.join(root,'clone');mkdirSync(repo);
  cpSync(path.join(project,'licensing'),path.join(repo,'licensing'),{recursive:true});
  if(existsSync(path.join(project,'.gitattributes')))cpSync(path.join(project,'.gitattributes'),path.join(repo,'.gitattributes'));
  const git=(cwd,...args)=>{const p=spawnSync('git',args,{cwd,encoding:'utf8'});assert.equal(p.status,0,p.stderr);};
  git(repo,'init','--quiet');git(repo,'config','core.autocrlf','true');git(repo,'config','user.name','Fixture');git(repo,'config','user.email','fixture@example.invalid');
  git(repo,'add','.');git(repo,'commit','--quiet','-m','test: pin attribution bytes');
  git(root,'-c','core.autocrlf=true','clone','--quiet',repo,clone);
  const catalog=JSON.parse(readFileSync(path.join(clone,'licensing/catalog.json'),'utf8'));
  for(const entry of catalog.files)assert.equal(hash(readFileSync(path.join(clone,'licensing',entry.path))),entry.sha256,entry.path);
});
