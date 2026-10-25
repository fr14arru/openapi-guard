import {readFileSync,mkdtempSync,appendFileSync,mkdirSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {verifyPackage} from './verify-package.mjs';
const actionRoot=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');

/** Thin Action boundary: inputs never become shell code; Git and Java remain in existing adapters. */
async function main() {
  const env=process.env;
  const failurePolicy=env.GUARD_FAIL_ON_BREAKING ?? 'true';
  if(!['true','false'].includes(failurePolicy))throw Error("fail-on-breaking must be 'true' or 'false'.");
  const commentPolicy=env.GUARD_PR_COMMENT ?? 'false';
  if(!['true','false'].includes(commentPolicy))throw Error("pr-comment must be 'true' or 'false'.");
  if(!env.GITHUB_WORKSPACE)throw Error('GITHUB_WORKSPACE is required.');
  const workspace=path.resolve(env.GITHUB_WORKSPACE);
  let base=env.GUARD_BASE,candidate=env.GUARD_CANDIDATE;
  let event;
  if(!base || !candidate) {
    if(!['pull_request','merge_group'].includes(env.GITHUB_EVENT_NAME))throw Error('Specify both base and candidate for events other than pull_request or merge_group.');
    event=JSON.parse(readFileSync(env.GITHUB_EVENT_PATH,'utf8'));
    base ||= env.GITHUB_EVENT_NAME==='pull_request' ? event.pull_request?.base?.sha : event.merge_group?.base_sha;
    candidate ||= env.GITHUB_SHA;
  }
  if(!base || !candidate)throw Error('Baseline and candidate commits are required.');
  const jar=env.GUARD_JAR ? path.resolve(workspace,env.GUARD_JAR) : path.join(actionRoot,'guard.jar');
  let expected=env.GUARD_JAR_SHA256;
  if(!env.GUARD_JAR) {
    const bundled=verifyPackage(actionRoot);
    expected ||= bundled.jarSha256;
  }
  if(!/^[a-f0-9]{64}$/i.test(expected ?? ''))throw Error('Expected jar-sha256 is required (64 hexadecimal characters).');
  const actual=createHash('sha256').update(readFileSync(jar)).digest('hex');
  if(actual!==expected.toLowerCase())throw Error('JAR SHA256 mismatch; refusing execution.');
  // Load companions only after bundled integrity verification, before executing any of their code.
  const {checkExitCode}=await import('./github-presentation.mjs');
  // A fresh report directory prevents a failed run from exposing an earlier successful report.
  const target=path.join(workspace,'target');
  // No cleanup here: these are consumer artifacts, retained for an always() upload step.
  mkdirSync(target,{recursive:true});
  const output=mkdtempSync(path.join(target,'openapi-guard-'));
  // The API credential is not needed by Git/Java or the comparator subprocess.
  const {GUARD_GITHUB_TOKEN,GITHUB_TOKEN,GH_TOKEN,...comparisonEnv}=env;
  const result=spawnSync(process.execPath,[path.join(actionRoot,'scripts/git-contracts.mjs'),
    '--repo',workspace,'--base',base,'--candidate',candidate,'--root',env.GUARD_ROOT || 'contracts','--jar',jar,'--output',output],
    {cwd:workspace,env:comparisonEnv,encoding:'utf8',timeout:15*60*1000,maxBuffer:32*1024*1024});
  if(result.stdout)process.stdout.write(result.stdout);
  if(result.stderr)process.stderr.write(result.stderr);
  if(result.error || ![0,1,2,3].includes(result.status))throw Error(`Comparison process failed: ${result.error?.message ?? result.status}`);
  const reportPath=path.join(output,'report.json'),summaryPath=path.join(output,'summary.md');
  const report=JSON.parse(readFileSync(reportPath,'utf8'));
  if(report.exitCode!==result.status)throw Error('Comparison exit code contradicts report.');
  // Policy changes the check outcome only; never rewrite a BREAKING report as compatible.
  const checkCode=checkExitCode({...report,policy:{failOnBreaking:failurePolicy==='true'}});
  if(env.GITHUB_OUTPUT) {
    // Fixed keys, validated single-line values; no user-controlled workflow command names.
    if(/[\r\n]/.test(reportPath+summaryPath))throw Error('Report paths cannot contain line breaks.');
    appendFileSync(env.GITHUB_OUTPUT,`exit-code=${result.status}\nreport-path=${reportPath}\nsummary-path=${summaryPath}\n`,'utf8');
  }
  let commentStatus='disabled';
  if(commentPolicy==='true') {
    try {
      const {publishPrComment}=await import('./pr-comment.mjs');
      if(!event && env.GITHUB_EVENT_NAME==='pull_request')event=JSON.parse(readFileSync(env.GITHUB_EVENT_PATH,'utf8'));
      commentStatus=await publishPrComment({env,event,report});
      console.log(`PR comment: ${commentStatus}.`);
    } catch(error) {
      if(env.GITHUB_OUTPUT)appendFileSync(env.GITHUB_OUTPUT,'comment-status=failed\ncheck-exit-code=2\n','utf8');
      if(env.GITHUB_STEP_SUMMARY)appendFileSync(env.GITHUB_STEP_SUMMARY,'\n### PR comment\n\nPublication failed. Check token permissions and the workflow log.\n','utf8');
      throw error;
    }
  }
  if(env.GITHUB_OUTPUT)appendFileSync(env.GITHUB_OUTPUT,`comment-status=${commentStatus}\ncheck-exit-code=${checkCode}\n`,'utf8');
  return checkCode;
}
main().then(code=>{process.exitCode=code;}).catch(error=>{console.error(error.message);process.exitCode=2;});
