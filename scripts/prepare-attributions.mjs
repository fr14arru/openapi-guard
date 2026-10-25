import {readFileSync,writeFileSync,copyFileSync,mkdirSync,lstatSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const sha256=bytes=>createHash('sha256').update(bytes).digest('hex');
const defaultAssets=fileURLToPath(new URL('../licensing/',import.meta.url));

/** Resolve a catalogued regular file, refusing traversal and symlinks at every segment. */
function evidencePath(root,relative) {
  if(typeof relative!=='string'||!relative||/[\\:\x00-\x1f]/.test(relative)||relative.split('/').some(p=>!p||p==='.'||p==='..'))
    throw Error('Invalid attribution evidence path.');
  let current=root;
  for(const segment of relative.split('/')) {
    current=path.join(current,segment);
    let stat;try {stat=lstatSync(current);}catch {throw Error(`Missing attribution evidence: ${relative}`);}
    if(stat.isSymbolicLink())throw Error(`Symbolic attribution evidence: ${relative}`);
  }
  if(!lstatSync(current).isFile())throw Error(`Non-file attribution evidence: ${relative}`);
  return current;
}

/**
 * Package reviewed evidence only for the exact runtime bytes in the catalog.
 * The owner's MIT grant covers project code; upstream licenses remain separate.
 * Validate the whole input before copying; the outer packager hashes all outputs.
 */
export function prepareAttributions(directory,manifest,assets=defaultAssets) {
  const catalogPath=evidencePath(assets,'catalog.json');
  const catalogBytes=readFileSync(catalogPath),catalog=JSON.parse(catalogBytes);
  if(catalog.formatVersion!==1||!Array.isArray(catalog.files)||!Array.isArray(catalog.dependencies)||!catalog.loader)
    throw Error('Unsupported attribution catalog.');
  const files=new Map();
  for(const entry of catalog.files) {
    if(files.has(entry.path)||!/^[a-f0-9]{64}$/.test(entry.sha256)||typeof entry.origin!=='string'||!entry.origin)
      throw Error('Invalid attribution evidence entry.');
    const source=evidencePath(assets,entry.path);
    if(sha256(readFileSync(source))!==entry.sha256)throw Error(`Changed attribution evidence: ${entry.path}`);
    files.set(entry.path,source);
  }
  const expected=new Map(catalog.dependencies.map(c=>[c.artifact,c]));
  if(expected.size!==catalog.dependencies.length||!Array.isArray(manifest.dependencies)||expected.size!==manifest.dependencies.length)
    throw Error('Runtime component set differs from the attribution catalog.');
  const seen=new Set();
  for(const actual of manifest.dependencies) {
    if(seen.has(actual.artifact)||!expected.has(actual.artifact)||actual.sha256!==expected.get(actual.artifact).sha256)
      throw Error(`Unknown or changed runtime component: ${actual.artifact}`);
    seen.add(actual.artifact);
  }
  if(!manifest.loader||manifest.loader.artifact!==catalog.loader.artifact||manifest.loader.sha256!==catalog.loader.sha256)
    throw Error('Unknown or changed Spring Boot loader component.');
  const components=[...catalog.dependencies,catalog.loader];
  for(const component of components) {
    if(typeof component.licenseExpression!=='string'||typeof component.selectedLicense!=='string'||!component.selectedLicense
      ||!component.licenseExpression.includes(component.selectedLicense)||!Array.isArray(component.files)||!component.files.length)
      throw Error(`Missing license evidence for component: ${component.artifact}`);
    for(const file of [...component.files,...(component.source?[component.source]:[])])
      if(!files.has(file))throw Error(`Uncatalogued attribution evidence: ${file}`);
    if(component.selectedLicense==='EPL-2.0'&&!component.source)throw Error(`Missing corresponding source: ${component.artifact}`);
  }
  const target=path.join(directory,'third-party/licenses');
  for(const [relative,source] of files) {
    const destination=path.join(target,relative);
    mkdirSync(path.dirname(destination),{recursive:true});copyFileSync(source,destination);
  }
  writeFileSync(path.join(target,'catalog.json'),catalogBytes);
  const rows=components.map(c=>`| ${c.artifact} | ${c.licenseExpression} | ${c.selectedLicense} | ${c.files.map(f=>`[text](third-party/licenses/${f})`).join(', ')} | ${c.source?`[source archive](third-party/licenses/${c.source})`:'Not required by the selected permissive license'} |`);
  const notices=[
    '# Third-party notices and corresponding source','',
    'These third-party components retain their own licenses and rights. Product terms do not restrict those rights.',
    'License choices below describe the distribution basis prepared for review; they do not grant a license to OpenAPI Guard itself.',
    'For EPL-2.0 components, the corresponding unmodified upstream source archives are included alongside this package under EPL-2.0. Extract the linked JAR (ZIP) to obtain the source, including its original notices.',
    'All original embedded notices and subcomponent license texts are retained. See the catalog for hashes and original locations; no upstream NOTICE has been invented.',
    'The Spring Boot loader is inventoried separately from nested runtime JARs. Its fingerprint covers the sorted path and SHA256 of each loader class.',
    '', '| Component | Upstream license expression | Prepared license choice | License and notices | Corresponding source |',
    '| --- | --- | --- | --- | --- |',...rows,'',
    'Jackson Core includes separately licensed FastDoubleParser/Schubfach material (including MIT and Boost notices). Spring Core preserves its ASM BSD and other bundled subcomponent notices. The linked full texts govern these components.',
    'Source archives are copied from the existing Maven cache with exact versions and pinned SHA256; they are not regenerated or modified. No claim of byte-for-byte recompilation is made.',
    'Java, Node, Git and external GitHub Actions are runtime prerequisites, not binaries included in this package. Website/build/test dependencies are outside this runtime inventory.',''
  ];
  writeFileSync(path.join(directory,'THIRD-PARTY-NOTICES.md'),notices.join('\n'));
  const license=readFileSync(new URL('../LICENSE',import.meta.url));
  writeFileSync(path.join(directory,'LICENSE.txt'),license);
  writeFileSync(path.join(directory,'PRODUCT-LICENSE-STATUS.txt'),'MIT\nOpenAPI Guard project code is licensed under LICENSE.txt. Third-party components retain their own licenses; see THIRD-PARTY-NOTICES.md and third-party/licenses/. Package generation does not publish a release.\n');
  return {formatVersion:1,components:components.length,status:'TECHNICAL_EVIDENCE_COMPLETE',productLicenseStatus:'MIT',productLicenseFile:'LICENSE.txt',productLicenseSha256:sha256(license),catalogFile:'third-party/licenses/catalog.json',catalogSha256:sha256(catalogBytes)};
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  try {
    const directory=path.resolve(process.argv[2]),filename=path.join(directory,'manifest.json');
    const manifest=JSON.parse(readFileSync(filename,'utf8'));
    manifest.attributions=prepareAttributions(directory,manifest);
    writeFileSync(filename,JSON.stringify(manifest,null,2)+'\n');
    console.log(`Attribution evidence prepared: ${manifest.attributions.components} components. Project license: MIT.`);
  }catch(error){console.error(error.message);process.exitCode=2;}
}
