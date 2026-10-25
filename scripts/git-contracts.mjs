import {spawnSync} from 'node:child_process';
import {mkdtempSync, mkdirSync, readFileSync, writeFileSync, appendFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {annotations, findingSummary, consoleSummary} from './github-presentation.mjs';

const limit = 5 * 1024 * 1024;
const utf8 = new TextDecoder('utf-8', {fatal:true});
// No shell: paths, Git references and contract contents never become executable command text.
function git(repo, args) {
  const result=spawnSync('git', args, {cwd:repo, timeout:30000, maxBuffer:32*1024*1024});
  if (result.error || result.status!==0) throw Error(`Git failed: ${result.error?.message ?? utf8.decode(result.stderr)}`);
  return result.stdout;
}
function revision(repo, ref) {
  if (!ref || ref.startsWith('-') || /[\x00-\x1f]/.test(ref)) throw Error('Invalid Git revision');
  return utf8.decode(git(repo,['rev-parse','--verify','--end-of-options',`${ref}^{commit}`])).trim();
}
function watchedRoot(root) {
  root=root.replace(/\/$/,'');
  if (!root || root.startsWith('/') || root.includes('\\') || root.split('/').some(s=>!s || s==='.' || s==='..') || /[\x00-\x1f:]/.test(root)) throw Error('Use a repository-relative contracts root without traversal');
  return root;
}
function selected(file, root) {return file.startsWith(`${root}/`) && /\.(yaml|yml|json)$/i.test(file);}
// Git's NUL-delimited format preserves spaces, Unicode, tabs and newlines in tracked paths.
function changes(repo,base,candidate,root) {
  const parts=utf8.decode(git(repo,['diff','--name-status','-z','--find-renames',base,candidate,'--'])).split('\0');
  const result=[];
  for (let i=0;i<parts.length && parts[i];) {
    const status=parts[i++], old=parts[i++], next=status.startsWith('R')?parts[i++]:old;
    if (!next) throw Error('Invalid Git diff output');
    const had=status!=='A' && selected(old,root), has=status!=='D' && selected(next,root);
    if (!had && !has) continue;
    result.push({kind:!has?'REMOVED':!had?'ADDED':status.startsWith('R')?'RENAMED':'MODIFIED',path:next,baselinePath:had?old:null,candidatePath:has?next:null});
  }
  return result.sort((a,b)=>a.path<b.path?-1:a.path>b.path?1:0);
}
function snapshot(repo,commit,file,destination) {
  const record=utf8.decode(git(repo,['ls-tree','-z',commit,'--',file]));
  if (!/^100(644|755) blob /.test(record)) throw Error('Contract must be a regular tracked file (no symlinks/submodules)');
  const data=git(repo,['show',`${commit}:${file}`]);
  if (data.length>limit) throw Error('Contract exceeds 5 MiB');
  utf8.decode(data); // Reject malformed UTF-8 before the CLI can receive a lossy conversion.
  writeFileSync(destination,data);
}
export function runJava(jar,base,candidate) {
  const p=spawnSync('java',['-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-jar',path.resolve(jar),'--profile=full','--format=ci',base,candidate],{timeout:60000,maxBuffer:20*1024*1024});
  if (p.error || ![0,1,2,3].includes(p.status)) throw Error(`Java execution failed: ${p.error?.message ?? p.status}`);
  const envelope=JSON.parse(utf8.decode(p.stdout));
  if (envelope.formatVersion!==1 || !Array.isArray(envelope.findings)) throw Error('Unsupported CI report format');
  const report=envelope.report;
  if (!report.status || !report.verdict) throw Error('Java did not return a structured comparison report');
  if (p.status===0 && (report.status!=='COMPLETED' || report.verdict!=='COMPATIBLE')) throw Error('Successful exit contradicts report');
  return {exitCode:p.status,report,findings:envelope.findings};
}
export async function compareRevisions({repo=process.cwd(),base,candidate,root='contracts',jar,compare}) {
  root=watchedRoot(root); base=revision(repo,base); candidate=revision(repo,candidate);
  const files=changes(repo,base,candidate,root);
  const temporary=mkdtempSync(path.join(tmpdir(),'openapi-guard-'));
  try {
    for (const [i,file] of files.entries()) {
      if (file.kind==='REMOVED') {
        Object.assign(file,{exitCode:1,ruleId:'CONTRACT_FILE_REMOVED',severity:'ERROR',location:file.baselinePath,description:'A tracked contract was removed from the watched scope.'});
        continue;
      }
      try {
        const old=path.join(temporary,`${i}-base`),next=path.join(temporary,`${i}-candidate`);
        snapshot(repo,candidate,file.candidatePath,next);
        if (file.baselinePath) snapshot(repo,base,file.baselinePath,old);
        else writeFileSync(old,readFileSync(next));
        Object.assign(file,await (compare ?? ((a,b)=>runJava(jar,a,b)))(old,next));
        if (![0,1,2,3].includes(file.exitCode)) throw Error('Unknown comparator exit code');
      } catch (error) {Object.assign(file,{exitCode:2,error:error.message});}
    }
    const codes=files.map(f=>f.exitCode);
    return {formatVersion:1,profile:'full',base,candidate,root,exitCode:codes.includes(2)?2:codes.includes(1)?1:codes.includes(3)?3:0,files};
  } finally {
    // Only remove the directory returned by mkdtemp, never a repository/user-supplied path.
    if (path.dirname(temporary)!==path.resolve(tmpdir()) || !path.basename(temporary).startsWith('openapi-guard-')) throw Error('Unsafe temporary path');
    rmSync(temporary,{recursive:true,force:true});
  }
}
function escape(value) {return String(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/[|`\[\]()*_\\]/g,c=>`&#${c.charCodeAt(0)};`).replace(/[\x00-\x1f\x7f-\x9f]/g,' ');}
export function summarize(report) {
  return `## OpenAPI contract guard\n\nExit code: ${report.exitCode}. Profile: full. Compatibility is limited to the published profile.\n\nBase: ${escape(report.base)}\n\nCandidate: ${escape(report.candidate)}\n\n${report.error?escape(report.error)+'\n\n':''}| Contract | Change | Exit code |\n| --- | --- | --- |\n${report.files.map(f=>`| ${escape(f.path)} | ${f.kind} | ${f.exitCode} |`).join('\n')}\n\n${report.files.length?'See report.json for rules, locations and uncertainty.':'No tracked contracts changed.'}\n`;
}
export function parseArguments(args) {
  const allowed=new Set(['repo','base','candidate','root','jar','output']);const parsed={};
  for (let i=0;i<args.length;i+=2) {
    const key=args[i]?.replace(/^--/,'');
    if (!args[i]?.startsWith('--') || !allowed.has(key) || parsed[key]!==undefined || !args[i+1] || args[i+1].startsWith('--')) throw Error('Invalid arguments');
    parsed[key]=args[i+1];
  }
  if (!parsed.base || !parsed.candidate) throw Error('--base and --candidate are required');
  return parsed;
}
async function main() {
  let options,report;
  try {options=parseArguments(process.argv.slice(2));} catch(error) {console.error(error.message);return 2;}
  const repo=path.resolve(options.repo ?? process.cwd());
  const output=path.resolve(repo,options.output ?? 'target/git-guard');
  const relative=path.relative(path.join(repo,'target'),output);
  if (!relative || relative.startsWith('..') || path.isAbsolute(relative)) {console.error('Output must be inside target/ in the repository');return 2;}
  try {report=await compareRevisions({...options,repo,jar:options.jar ?? path.join(repo,'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar')});}
  catch(error) {report={formatVersion:1,profile:'full',base:options.base,candidate:options.candidate,exitCode:2,error:error.message,files:[]};}
  mkdirSync(output,{recursive:true});
  report.policy={failOnBreaking:process.env.GUARD_FAIL_ON_BREAKING!=='false'};
  writeFileSync(path.join(output,'report.json'),JSON.stringify(report,null,2)+'\n','utf8');
  const markdown=summarize(report)+findingSummary(report);
  writeFileSync(path.join(output,'summary.md'),markdown,'utf8');
  if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY,markdown,'utf8');
  console.log(consoleSummary(report));
  // An annotation does not fail a job: the comparator exit code below still controls failure.
  if (process.env.GITHUB_ACTIONS==='true') {
    console.log('::group::Candidate annotations (one per position; findings above are grouped)');
    for (const annotation of annotations(report)) console.log(annotation);
    console.log('::endgroup::');
  }
  return report.exitCode;
}
if (process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  main().then(code=>{process.exitCode=code;}).catch(error=>{console.error(error.message);process.exitCode=2;});
}
