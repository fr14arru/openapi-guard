import {createHash} from 'node:crypto';
import {checkExitCode,findingSummary,markdown} from './github-presentation.mjs';

/** Stable scope for one workflow/contracts directory; never identify comments by user text alone. */
export function commentMarker(workflow,root='contracts') {
  const key=createHash('sha256').update(JSON.stringify([workflow,root])).digest('hex').slice(0,20);
  return `<!-- openapi-guard:${key} -->`;
}
function runOrder(body) {
  const match=body.match(/<!-- openapi-guard-run:(\d{1,30}):(\d{1,10}) -->/);
  return match?[BigInt(match[1]),BigInt(match[2])]:[0n,0n];
}
function newer(body,env) {
  const [id,attempt]=runOrder(body);
  return id>BigInt(env.GITHUB_RUN_ID) || id===BigInt(env.GITHUB_RUN_ID) && attempt>BigInt(env.GITHUB_RUN_ATTEMPT || '1');
}
function context(env) {
  if(!/^[\w.-]+\/[\w.-]+$/.test(env.GITHUB_REPOSITORY ?? '')
    || !/^\d{1,30}$/.test(env.GITHUB_RUN_ID ?? '') || !/^\d{1,10}$/.test(env.GITHUB_RUN_ATTEMPT || '1'))throw Error('Invalid GitHub comment context.');
  const server=new URL(env.GITHUB_SERVER_URL || 'https://github.com');
  const api=new URL(env.GITHUB_API_URL || 'https://api.github.com');
  if(server.protocol!=='https:' || api.protocol!=='https:' || server.username || server.password || api.username || api.password
    || !(api.origin===server.origin || server.origin==='https://github.com' && api.origin==='https://api.github.com')
    || server.pathname!=='/' || api.search || api.hash)throw Error('GitHub comment API must use the matching HTTPS origin.');
  return {server:server.origin,api:api.href.replace(/\/$/,'')};
}

/** Bounded PR body: fresh status replaces old warnings; the full report stays attached to the run. */
export function commentBody(report,env) {
  const {server}=context(env);
  const run=`${server}/${env.GITHUB_REPOSITORY}/actions/runs/${env.GITHUB_RUN_ID}/attempts/${env.GITHUB_RUN_ATTEMPT || '1'}`;
  const state=report.exitCode===0?'**No breaking changes detected within the selected profile.**'
    :report.exitCode===2?'**Comparison failed. Review input or execution errors.**'
    :checkExitCode(report)===3?'**Review required. Compatibility could not be established completely.**'
    :checkExitCode(report)===0?'**Merge allowed by workflow policy. API compatibility is broken within the selected profile.**'
    :'**Breaking changes detected. The workflow blocks this change.**';
  let details=findingSummary(report).replace(/@/g,'&#64;');
  if(details.length>48000) {
    details=details.slice(0,48000);
    if(/[\uD800-\uDBFF]$/.test(details))details=details.slice(0,-1);
    details+='\n\nReport truncated; see the full report in the workflow run.\n';
    const excluded=[...new Set(report.files.flatMap(file=>file.report?.excludedFacets ?? []))].map(markdown).join(', ');
    details+=`\nExcluded facets: ${excluded.slice(0,2000).replace(/@/g,'&#64;')}${excluded.length>2000?' (full list in report.json)':''}. Compatibility never certifies the whole API.\n`;
  }
  const sha=value=>/^[a-f0-9]{40}$/i.test(value ?? '')?value:'Unavailable';
  return `${commentMarker(env.GITHUB_WORKFLOW,report.root)}\n<!-- openapi-guard-run:${env.GITHUB_RUN_ID}:${env.GITHUB_RUN_ATTEMPT || '1'} -->\n`
    +`## ${report.exitCode===0?'✅':'⚠️'} OpenAPI Guard\n\n${state}\n\n`
    +`Analyzed candidate: ${sha(report.candidate)}\n\nBaseline: ${sha(report.base)}\n\n`
    +`[Full report and workflow run](${run}) — download report.json and summary.md from the run artifacts.\n`
    +details;
}

/** Standard GITHUB_TOKEN publisher. HTTP is injected in tests; contracts never become API routes or code. */
export async function publishPrComment({env,event,report,fetchImpl=globalThis.fetch}) {
  if(env.GITHUB_EVENT_NAME!=='pull_request')return 'skipped-event';
  if(event?.pull_request?.head?.repo?.full_name!==env.GITHUB_REPOSITORY)return 'skipped-fork';
  if(!env.GUARD_GITHUB_TOKEN)throw Error('PR comment token is required; enable pull-requests: write.');
  const {api}=context(env);
  if(!Number.isSafeInteger(event.number) || event.number<1)throw Error('Invalid pull request number.');
  const prefix=`/repos/${env.GITHUB_REPOSITORY}`;
  const request=async(route,method='GET',body)=>{
    let response;
    try {
      response=await fetchImpl(api+route,{method,redirect:'error',signal:AbortSignal.timeout(15000),
        headers:{Authorization:`Bearer ${env.GUARD_GITHUB_TOKEN}`,Accept:'application/vnd.github+json','Content-Type':'application/json'},
        ...(body?{body:JSON.stringify(body)}:{})});
    } catch { throw Error('GitHub PR comment request failed or timed out.'); }
    if(!response.ok)throw Error(`GitHub PR comment API returned HTTP ${response.status}; check pull-requests: write and repository policy.`);
    try { return await response.json(); } catch { throw Error('GitHub PR comment API returned invalid JSON.'); }
  };
  const current=async()=>{
    const pr=await request(`${prefix}/pulls/${event.number}`);
    return pr.state==='open' && pr.head?.sha===event.pull_request.head.sha
      && pr.head?.repo?.full_name===env.GITHUB_REPOSITORY && pr.base?.sha===report.base
      && report.candidate===env.GITHUB_SHA;
  };
  if(!await current())return 'skipped-stale';
  const marker=commentMarker(env.GITHUB_WORKFLOW,report.root);
  let owned;
  // Bound pagination and fail explicitly instead of creating duplicates after a partial search.
  for(let page=1;page<=10;page++) {
    const comments=await request(`${prefix}/issues/${event.number}/comments?per_page=100&page=${page}`);
    if(!Array.isArray(comments))throw Error('GitHub PR comments response must be a list.');
    for(const comment of comments)if(comment.user?.type==='Bot' && comment.user.login==='github-actions[bot]'
      && typeof comment.body==='string' && comment.body.startsWith(marker+'\n')) {
      if(newer(comment.body,env))return 'skipped-stale';
      owned ??= comment;
    }
    if(comments.length<100)break;
    if(page===10)throw Error('PR comment search exceeded 1000 comments; no comment was written.');
  }
  // Recheck after pagination to avoid publishing for an outdated/closed PR. Workflow concurrency is still required.
  if(!await current())return 'skipped-stale';
  const body=commentBody(report,env);
  if(owned) {
    if(!Number.isSafeInteger(owned.id) || owned.id<1)throw Error('Invalid owned PR comment ID.');
    await request(`${prefix}/issues/comments/${owned.id}`,'PATCH',{body});
    return 'updated';
  }
  await request(`${prefix}/issues/${event.number}/comments`,'POST',{body});
  return 'created';
}
