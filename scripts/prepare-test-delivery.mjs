import {readFileSync,writeFileSync,mkdirSync,copyFileSync,existsSync,realpathSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {execFileSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {verifyPackage} from './verify-package.mjs';

const repository=fileURLToPath(new URL('../',import.meta.url));
const approvalCommit='06a9a3bdcae373c99a5f0b793e190cb62cfb8794';
const approvalBlob='e1496cb7a22a32ae80ea615f92c2757c56182503';
const sha256=bytes=>createHash('sha256').update(bytes).digest('hex');
const replaced=new Set(['README.txt','PILOT-TERMS-DRAFT.txt','PRODUCT-LICENSE-STATUS.txt','THIRD-PARTY-REVIEW.md']);
const runtime=new Set(['guard.jar','action.yml','build-proof.json','THIRD-PARTY-NOTICES.md',
  ...['action-runner','git-contracts','github-presentation','pr-comment','verify-package'].map(name=>`scripts/${name}.mjs`),
  ...['base','compatible','breaking','review'].map(name=>`examples/${name}.yaml`)]);

/** Extract only customer clauses from the exact approved Git blob, normalized to LF. */
function approvedLicense(filename) {
  const text=readFileSync(filename,'utf8').replace(/\r\n/g,'\n');
  const bytes=Buffer.from(text);
  const blob=createHash('sha1').update(`blob ${bytes.length}\0`).update(bytes).digest('hex');
  if(blob!==approvalBlob)throw Error('The approved license document has changed; review the new text explicitly.');
  const start=text.indexOf('## Condiciones del motor: redacción para revisión');
  const end=text.indexOf('## Envoltura de GitHub Action');
  if(start<0||end<=start)throw Error('Cannot extract the approved license clauses.');
  // Remove internal review instructions, without changing rights or third-party clauses.
  const body=text.slice(start,end)
    .replace('## Condiciones del motor: redacción para revisión','## Condiciones del motor')
    .replace('Este borrador no aplica','Estas condiciones no aplican')
    .replace('## Cláusulas específicas de terceros para revisión','## Componentes de terceros')
    .split('\n\n').filter(paragraph=>!paragraph.startsWith('Revisar antes de activar ventas')
      &&!paragraph.startsWith('**El titular debe revisar la sección 4')).join('\n\n').trim();
  return '# OpenAPI Breaking Change Guard — License terms\n\n'
    +'Approved wording: 2026-10-22. Owner: Francisco Arce Ruiz. Contact: rayopuyi5@gmail.com.\n\n'
    +'This copy accompanies a controlled test-mode delivery. It does not establish a paid subscription or authorize public redistribution. Public sales and privacy publication remain pending.\n\n'
    +body+'\n';
}

/**
 * Repackage a previously verified engine without claiming it was rebuilt here.
 * Validate the closed input, build provenance, pinned attribution files and approved
 * wording before creating output. Copy only known runtime/evidence paths, never
 * private legal documents. The caller supplies the delivery script's commit;
 * sourceCommit/version remain the original engine provenance. A fresh destination
 * is required. A failed copy may leave a partial directory; do not use it.
 */
export function prepareTestDelivery(source,destination,{packagingCommit,approvedDocument=path.join(repository,'docs/product-license-draft.md')}={}) {
  if(!/^[a-f0-9]{40}$/.test(packagingCommit??''))throw Error('An explicit packaging commit is required.');
  if(existsSync(destination))throw Error('Delivery destination already exists.');
  // Resolve the closest existing parent as well, so a directory alias cannot put
  // the output inside the immutable source. Concurrent hostile renames are outside this local workflow.
  const absoluteDestination=path.resolve(destination);
  let parent=path.dirname(absoluteDestination);
  while(!existsSync(parent))parent=path.dirname(parent);
  const planned=path.resolve(realpathSync(parent),path.relative(parent,absoluteDestination));
  const relative=path.relative(realpathSync(source),planned);
  if(!relative||(!relative.startsWith(`..${path.sep}`)&&relative!=='..'&&!path.isAbsolute(relative)))
    throw Error('Delivery output must be outside the source package.');
  const manifest=verifyPackage(source);
  const sourceManifest=readFileSync(path.join(source,'manifest.json'));
  if(!manifest.integrity.some(entry=>entry.path==='build-proof.json'))throw Error('Verified build proof is required.');
  const proof=JSON.parse(readFileSync(path.join(source,'build-proof.json'),'utf8'));
  if(proof.verified!==true||manifest.buildVerified!==true||manifest.workingTreeDirty!==false
    ||!/^[a-f0-9]{40}$/.test(proof.sourceCommit??'')||proof.sourceCommit!==manifest.sourceCommit
    ||proof.version!==manifest.version||proof.jarSha256!==manifest.jarSha256)
    throw Error('Build provenance contradicts the source package.');
  const license=approvedLicense(approvedDocument);
  const catalogPath='third-party/licenses/catalog.json';
  const catalogBytes=readFileSync(path.join(source,catalogPath));
  if(manifest.attributions?.status!=='TECHNICAL_EVIDENCE_COMPLETE'
    ||sha256(catalogBytes)!==manifest.attributions.catalogSha256)throw Error('Attribution catalog is missing or changed.');
  const catalog=JSON.parse(catalogBytes),evidence=new Set([catalogPath]);
  if(!Array.isArray(catalog.files))throw Error('Attribution evidence list is missing.');
  for(const entry of catalog.files) {
    if(typeof entry.path!=='string'||/[\\:\x00-\x1f]/.test(entry.path)
      ||entry.path.split('/').some(part=>!part||part==='.'||part==='..'))throw Error('Attribution evidence path is invalid.');
    const filename=`third-party/licenses/${entry.path}`;
    if(!manifest.integrity.some(item=>item.path===filename)
      ||sha256(readFileSync(path.join(source,filename)))!==entry.sha256)throw Error('Attribution evidence is missing or changed.');
    evidence.add(filename);
  }
  for(const entry of manifest.integrity)
    if(!runtime.has(entry.path)&&!replaced.has(entry.path)&&!evidence.has(entry.path)&&!/^third-party\/\d{2}-\d{2}\.txt$/.test(entry.path))
      throw Error(`Unexpected delivery input: ${entry.path}`);
  for(const name of runtime)if(!manifest.integrity.some(entry=>entry.path===name))throw Error(`Required delivery input is missing: ${name}`);

  const readme=readFileSync(path.join(repository,'docs/test-delivery-readme.txt'),'utf8');
  mkdirSync(destination,{recursive:true});
  for(const entry of manifest.integrity) {
    if(replaced.has(entry.path))continue;
    const target=path.join(destination,entry.path);
    mkdirSync(path.dirname(target),{recursive:true});copyFileSync(path.join(source,entry.path),target);
  }
  const additions={
    'README.txt':readme,'LICENSE.txt':license,
    'PRODUCT-LICENSE-STATUS.txt':'APPROVED_TEXT_TEST_MODE_ONLY\nProduct wording approved on 2026-10-22. No paid subscription, public distribution or live sale is authorized by this package. See LICENSE.txt and DELIVERY-STATUS.md.\n',
    'THIRD-PARTY-REVIEW.md':'# Third-party evidence\n\nThe verified engine retains its original library/loader inventory, notices, full texts and corresponding sources. See THIRD-PARTY-NOTICES.md and third-party/licenses/catalog.json. This technical inventory is not an independent legal audit.\n',
    'DELIVERY-STATUS.md':'# Delivery status\n\nLEMON_SQUEEZY_TEST_ONLY. Do not upload to a live product or publish this package.\n\n'
      +'Approved product terms are included. Privacy and the personal legal notice are intentionally NOT included: they remain separate local documents. Privacy is not final.\n\n'
      +'Before live sales: finish privacy data/retention/provider checks, publish approved legal information at stable links, set the effective date, verify checkout/renewal/cancellation and validate real file delivery through an authorized procedure. Test-mode purchases cannot download files.\n\n'
      +'The current local Action recipe is for private consumer repositories. A public consumer repository must not commit this proprietary engine; its runner installation remains pending.\n\n'
      +'No license server, activation or telemetry has been added. Engine version/sourceCommit refer to the original binary; delivery.packagingCommit identifies the repackaging code. The build proof remains unmodified.\n',
  };
  for(const [name,content] of Object.entries(additions))writeFileSync(path.join(destination,name),content);
  manifest.distributionStatus='LEMON_SQUEEZY_TEST_ONLY';
  manifest.attributions.productLicenseStatus='APPROVED_TEXT_TEST_MODE_ONLY';
  manifest.delivery={formatVersion:1,mode:'LEMON_SQUEEZY_TEST_ONLY',packagingCommit,
    sourcePackageManifestSha256:sha256(sourceManifest),publicationApproved:false,liveSalesApproved:false,
    privacyPolicyStatus:'PENDING_NOT_INCLUDED',personalLegalNoticeIncluded:false,
    license:{file:'LICENSE.txt',sha256:sha256(license),approvalCommit,approvalDocumentBlob:approvalBlob}};
  const names=[...manifest.integrity.filter(entry=>!replaced.has(entry.path)).map(entry=>entry.path),...Object.keys(additions)].sort();
  manifest.integrity=names.map(name=>({path:name,sha256:sha256(readFileSync(path.join(destination,name)))}));
  writeFileSync(path.join(destination,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
  return verifyPackage(destination);
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  try {
    if(process.argv.length!==4)throw Error('Usage: node scripts/prepare-test-delivery.mjs SOURCE_PACKAGE NEW_DESTINATION');
    if(execFileSync('git',['status','--porcelain'],{cwd:repository,encoding:'utf8'}).trim())throw Error('A clean packaging checkout is required.');
    const packagingCommit=execFileSync('git',['rev-parse','HEAD'],{cwd:repository,encoding:'utf8'}).trim();
    const result=prepareTestDelivery(path.resolve(process.argv[2]),path.resolve(process.argv[3]),{packagingCommit});
    console.log(`Test-mode delivery verified: ${result.integrity.length} files. Engine unchanged. No upload or publication performed.`);
  }catch(error){console.error(error.message);process.exitCode=2;}
}
