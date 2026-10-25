import {readFileSync,readdirSync,lstatSync,realpathSync} from 'node:fs';
import {createHash} from 'node:crypto';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

/** Verify a closed set of regular files. The external ZIP hash is the trust anchor, not this manifest. */
export function verifyPackage(directory) {
  const root=realpathSync(directory);
  const manifest=JSON.parse(readFileSync(path.join(root,'manifest.json'),'utf8'));
  if(manifest.formatVersion!==2 || !Array.isArray(manifest.integrity))throw Error('Unsupported package integrity manifest.');
  const expected=new Map();
  for(const entry of manifest.integrity) {
    if(typeof entry.path!=='string' || !entry.path || entry.path.includes('\\') || /[\x00-\x1f:]/.test(entry.path)
      || entry.path.split('/').some(segment=>!segment || segment==='.' || segment==='..') || entry.path==='manifest.json'
      || !/^[a-f0-9]{64}$/.test(entry.sha256) || expected.has(entry.path))throw Error('Invalid package integrity entry.');
    expected.set(entry.path,entry.sha256);
  }
  for(const required of ['guard.jar','action.yml','scripts/action-runner.mjs','scripts/git-contracts.mjs','scripts/github-presentation.mjs','scripts/verify-package.mjs','README.txt','THIRD-PARTY-REVIEW.md']) {
    if(!expected.has(required))throw Error(`Required package file is not covered: ${required}`);
  }
  const seen=new Set();
  function visit(relative='') {
    for(const name of readdirSync(path.join(root,relative))) {
      const child=relative ? `${relative}/${name}` : name;
      const filename=path.join(root,child),stat=lstatSync(filename);
      if(stat.isSymbolicLink())throw Error(`Package contains a symbolic link: ${child}`);
      if(stat.isDirectory()){visit(child);continue;}
      if(!stat.isFile())throw Error(`Unsupported package file: ${child}`);
      if(child==='manifest.json')continue;
      if(!expected.has(child))throw Error(`Unexpected package file: ${child}`);
      const actual=createHash('sha256').update(readFileSync(filename)).digest('hex');
      if(actual!==expected.get(child))throw Error(`Package integrity mismatch: ${child}`);
      seen.add(child);
    }
  }
  visit();
  for(const filename of expected.keys())if(!seen.has(filename))throw Error(`Missing package file: ${filename}`);
  if(expected.get('guard.jar')!==manifest.jarSha256)throw Error('JAR checksum contradicts integrity manifest.');
  // Existing v2 packages without the optional publisher remain verifiable.
  if(/\n  pr-comment:/.test(readFileSync(path.join(root,'action.yml'),'utf8')) && !expected.has('scripts/pr-comment.mjs'))
    throw Error('PR comment publisher is not covered by package integrity.');
  return manifest;
}
if(process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  try {const manifest=verifyPackage(process.argv[2]);console.log(`Package integrity verified: ${manifest.integrity.length} files.`);}
  catch(error){console.error(error.message);process.exitCode=2;}
}
