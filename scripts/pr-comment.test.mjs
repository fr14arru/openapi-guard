import test from 'node:test';
import assert from 'node:assert/strict';
import {publishPrComment, commentBody, commentMarker} from './pr-comment.mjs';

const env={GITHUB_EVENT_NAME:'pull_request',GITHUB_REPOSITORY:'owner/api',GITHUB_WORKFLOW:'OpenAPI pilot',GITHUB_RUN_ID:'100',GITHUB_RUN_ATTEMPT:'1',GITHUB_SHA:'b'.repeat(40),GITHUB_SERVER_URL:'https://github.com',GITHUB_API_URL:'https://api.github.com',GUARD_GITHUB_TOKEN:'fixture-token'};
const event={number:1,pull_request:{head:{sha:'c'.repeat(40),repo:{full_name:'owner/api'}},base:{sha:'a'.repeat(40)}}};
const breaking={base:'a'.repeat(40),candidate:'b'.repeat(40),root:'contracts',exitCode:1,policy:{failOnBreaking:false},files:[{path:'contracts/api.yaml',candidatePath:'contracts/api.yaml',exitCode:1,report:{changes:[{id:'one'}],status:'COMPLETED',verdict:'BREAKING',excludedFacets:['security']},findings:[{id:'one',ruleId:'REQUEST_SCHEMA_VALUES_RESTRICTED',severity:'ERROR',source:'candidate',description:'maximum: 100 -> 10',location:{method:'POST',path:'/orders',direction:'REQUEST'},positions:[{line:58,column:9}]}]}]};
const ok=data=>({ok:true,status:200,json:async()=>structuredClone(data)});
function api() {
  const comments=[],calls=[];
  const fetchImpl=async(url,options)=>{
    calls.push({url,options});
    assert.equal(new URL(url).origin,'https://api.github.com');
    assert.equal(options.headers.Authorization,'Bearer fixture-token');
    if(url.endsWith('/pulls/1'))return ok({state:'open',head:event.pull_request.head,base:event.pull_request.base});
    if(options.method==='POST') {const item={id:12,user:{type:'Bot',login:'github-actions[bot]'},body:JSON.parse(options.body).body};comments.push(item);return ok(item);}
    if(options.method==='PATCH') {comments.find(c=>url.endsWith('/'+c.id)).body=JSON.parse(options.body).body;return ok({});}
    return ok(comments);
  };
  return {comments,calls,fetchImpl};
}

test('creates one visible comment, then updates it when breaking changes disappear',async()=>{
  const fixture=api();
  assert.equal(await publishPrComment({env,event,report:breaking,fetchImpl:fixture.fetchImpl}),'created');
  assert.match(fixture.comments[0].body,/1 breaking change detected/);
  assert.match(fixture.comments[0].body,/Merge allowed by workflow policy/);
  assert.match(fixture.comments[0].body,/POST \/orders/);
  assert.match(fixture.comments[0].body,/actions\/runs\/100/);
  assert.match(fixture.comments[0].body,/b{40}/);
  const compatible={...breaking,exitCode:0,files:[]};
  assert.equal(await publishPrComment({env:{...env,GITHUB_RUN_ID:'101'},event,report:compatible,fetchImpl:fixture.fetchImpl}),'updated');
  assert.equal(fixture.comments.length,1);
  assert.match(fixture.comments[0].body,/No breaking changes detected within the selected profile/);
  assert.ok(!fixture.comments[0].body.includes('maximum: 100'));
});

test('forks and events without a PR skip publication without token or network access',async()=>{
  const fetchImpl=()=>assert.fail('No HTTP expected');
  assert.equal(await publishPrComment({env:{...env,GUARD_GITHUB_TOKEN:''},event:{...event,pull_request:{...event.pull_request,head:{...event.pull_request.head,repo:{full_name:'fork/api'}}}},report:breaking,fetchImpl}),'skipped-fork');
  assert.equal(await publishPrComment({env:{...env,GITHUB_EVENT_NAME:'merge_group'},report:breaking,fetchImpl}),'skipped-event');
});

test('stale PR heads and old workflow runs cannot replace the current comment',async()=>{
  const fixture=api();
  await publishPrComment({env:{...env,GITHUB_RUN_ID:'102'},event,report:breaking,fetchImpl:fixture.fetchImpl});
  assert.equal(await publishPrComment({env,event,report:breaking,fetchImpl:fixture.fetchImpl}),'skipped-stale');
  const stale=async()=>ok({state:'open',head:{...event.pull_request.head,sha:'d'.repeat(40)},base:event.pull_request.base});
  assert.equal(await publishPrComment({env,event,report:breaking,fetchImpl:stale}),'skipped-stale');
  assert.equal(fixture.calls.filter(c=>c.options.method==='PATCH').length,0);
});

test('pagination finds owned comments and never edits a user impersonating the marker',async()=>{
  const fixture=api();
  const marker=commentMarker(env.GITHUB_WORKFLOW,breaking.root);
  fixture.comments.push(...Array.from({length:100},(_,i)=>({id:i+100,user:{type:'User',login:'person'},body:marker+'\nforged'})));
  fixture.comments.push({id:12,user:{type:'Bot',login:'github-actions[bot]'},body:commentBody(breaking,env)});
  const fetchImpl=(url,options)=>options.method==='GET' && url.includes('/issues/1/comments')
    ? Promise.resolve(ok(url.endsWith('page=1')?fixture.comments.slice(0,100):fixture.comments.slice(100))) : fixture.fetchImpl(url,options);
  assert.equal(await publishPrComment({env,event,report:breaking,fetchImpl}),'updated');
  assert.ok(fixture.calls.some(c=>c.url.endsWith('/issues/comments/12') && c.options.method==='PATCH'));
});

test('errors and uncertainty never claim merge allowed; untrusted Markdown stays escaped and body is bounded',()=>{
  for(const code of [2,3]) {
    const body=commentBody({...breaking,exitCode:code},env);
    assert.ok(!body.includes('**Merge allowed by workflow policy'));
    assert.match(body,/failed|Review required/i);
  }
  const input=structuredClone(breaking);input.files[0].findings[0].description='<script>@everyone\n'+ 'x'.repeat(70000);
  const body=commentBody(input,env);
  assert.ok(body.length<60000);assert.ok(!body.includes('<script>'));assert.ok(!body.includes('@everyone'));
  assert.match(body,/Excluded facets: security/);
});

test('missing token and HTTP failures are explicit without echoing credentials or API response content',async()=>{
  await assert.rejects(publishPrComment({env:{...env,GUARD_GITHUB_TOKEN:''},event,report:breaking}),/token/);
  await assert.rejects(publishPrComment({env,event,report:breaking,fetchImpl:async()=>({ok:false,status:403,json:async()=>({message:'fixture-token'})})}),error=>error.message.includes('403') && !error.message.includes('fixture-token'));
});

test('rechecks the head before writing and rejects redirects or insecure API origins',async()=>{
  const fixture=api();let reads=0;
  const fetchImpl=(url,options)=>{
    assert.equal(options.redirect,'error');
    if(url.endsWith('/pulls/1') && ++reads===2)return Promise.resolve(ok({state:'closed'}));
    return fixture.fetchImpl(url,options);
  };
  assert.equal(await publishPrComment({env,event,report:breaking,fetchImpl}),'skipped-stale');
  assert.equal(fixture.comments.length,0);
  await assert.rejects(publishPrComment({env:{...env,GITHUB_API_URL:'http://evil.invalid'},event,report:breaking}),/HTTPS|origin/);
});
