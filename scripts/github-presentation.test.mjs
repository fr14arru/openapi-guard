import test from 'node:test';
import assert from 'node:assert/strict';
import {annotations, findingSummary, consoleSummary} from './github-presentation.mjs';

const finding = {id:'stable-id',ruleId:'REQUIRED_PARAMETER_ADDED',severity:'ERROR',source:'candidate',description:'Required parameter',positions:[{line:27,column:3,approximate:false},{line:89,column:5,approximate:false}]};
const report = files => ({base:'abc',candidate:'def',exitCode:1,files});
const file = changes => ({path:'contracts/new.yaml',candidatePath:'contracts/new.yaml',kind:'RENAMED',exitCode:1,findings:changes,report:{profileId:'profile-v1',status:'COMPLETED',verdict:'BREAKING',excludedFacets:['security']}});

test('annotations associate separate positions with one finding in the new repository path', () => {
  const result = annotations(report([file([finding])]));
  assert.equal(result.length,2);
  assert.match(result[0],/^::error file=contracts\/new.yaml,line=27,col=3,title=/);
  assert.match(result[1],/line=89,col=5/);
  assert.ok(result.every(line => line.includes('stable-id')));
  assert.ok(result.every(line => !line.includes('endLine=')));
});

test('untrusted content cannot inject workflow commands or properties', () => {
  const malicious = {...finding,description:'bad%\r\n::error::injected\u001b',ruleId:'rule,title=fake'};
  const changed = {...file([malicious]),candidatePath:'contracts/a,b%\n.yaml'};
  const output = annotations(report([changed])).join('\n');
  assert.equal(output.split('\n').length,2);
  assert.match(output,/%25/); assert.match(output,/%0A/); assert.match(output,/%2C/);
  assert.ok(!output.includes('\u001b'));
  assert.ok(!output.includes(',title=fake'));
});

test('baseline diagnostics and removed files never claim candidate line one', () => {
  const baseline = {...finding,source:'baseline',positions:[{line:27,column:3}],severity:'WARNING'};
  const result = annotations(report([file([baseline]),{path:'contracts/old.yaml',kind:'REMOVED',exitCode:1,ruleId:'CONTRACT_FILE_REMOVED',description:'Removed'}]));
  assert.equal(result.length,2);
  assert.ok(result.every(line => !line.includes('file=') && !line.includes('line=')));
  assert.match(result[0],/^::warning /); assert.match(result[1],/CONTRACT_FILE_REMOVED/);
});

test('summary preserves uncertainty coverage and logical finding count with escaped content', () => {
  const summary = findingSummary(report([file([{...finding,description:'<script>|[link](url)'}])]));
  assert.match(summary,/27, 89/); assert.match(summary,/profile-v1/); assert.match(summary,/security/);
  assert.match(summary,/1 finding/); assert.ok(!summary.includes('<script>')); assert.ok(!summary.includes('[link](url)'));
});

test('annotation cap announces omitted positions while summary retains findings', () => {
  const many = Array.from({length:60},(_,i)=>({...finding,id:`id-${i}`}));
  const result = annotations(report([file(many)]));
  assert.equal(result.filter(line=>line.startsWith('::error ')).length,50);
  assert.match(result.at(-1),/^::notice::/);
  assert.match(findingSummary(report([file(many)])),/60 findings/);
});

test('compatible empty results produce no annotations and clear summary', () => {
  const compatible = {base:'a',candidate:'b',exitCode:0,files:[]};
  assert.deepEqual(annotations(compatible),[]);
  assert.match(findingSummary(compatible),/0 findings/);
});

test('execution errors remain visible alongside already collected findings', () => {
  const failed = report([{...file([finding]),exitCode:2,error:'Invalid comparator response'}]);
  assert.ok(annotations(failed).some(line=>line.includes('EXECUTION_FAILED')));
  assert.match(findingSummary(failed),/Invalid comparator response/);
});

test('presentation identifies operation and response using semantic report locations even with older CI findings', () => {
  const located = file([finding]);
  located.report.changes = [{...finding, location:{method:'POST',path:'/orders',direction:'RESPONSE',responseCode:'201',mediaType:'application/json'}}];
  assert.match(findingSummary(report([located])), /POST \/orders.*RESPONSE 201.*application\/json/);
  assert.match(annotations(report([located]))[0], /POST \/orders.*RESPONSE 201/);
});

test('console has one numbered line per logical finding and no Markdown entities or injected commands', () => {
  const input = report([file([{...finding,description:'maximum: 100 -> 10\n::error::injected'}])]);
  const output = consoleSummary(input);
  assert.equal(output.split('\n').filter(line=>line.startsWith('Finding 1 -')).length,1);
  assert.match(output,/Lines 27, 89/);
  assert.match(output,/REQUIRED_PARAMETER_ADDED/);
  assert.ok(!output.includes('&#'));
  assert.ok(!output.includes('\n::error::injected'));
  assert.ok(!output.includes('| ---'));
});

test('summary distinguishes breaking, invalid, review and advisory policy without calling breakage an input error', () => {
  assert.match(findingSummary(report([file([finding])])), /1 breaking change detected/);
  assert.ok(!findingSummary(report([file([finding])])).includes('input errors'));
  assert.match(findingSummary({...report([]),exitCode:2}), /Input or execution errors/);
  assert.match(findingSummary({...report([]),exitCode:3}), /Review required/);
  const advisory = {...report([file([finding])]),policy:{failOnBreaking:false}};
  assert.match(findingSummary(advisory), /Breaking changes are allowed by workflow policy/);
  assert.ok(annotations(advisory).every(line=>line.startsWith('::warning ')));
  assert.match(annotations({...advisory,files:[{...file([]),exitCode:2,error:'invalid'}]})[0], /^::error /);
});
