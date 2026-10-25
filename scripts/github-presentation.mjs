// GitHub presentation and check policy: preserve the engine's report and original exit code.
// Workflow command escaping is deliberately separate from Markdown escaping.
function clean(value) { return String(value ?? '').replace(/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x9f]/g, ' '); }
function command(value, property=false) {
  let result=clean(value).replace(/%/g,'%25').replace(/\r/g,'%0D').replace(/\n/g,'%0A');
  if (property) result=result.replace(/:/g,'%3A').replace(/,/g,'%2C');
  return result;
}
export function markdown(value) {
  return clean(value).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/[|`\[\]()*_\\]/g,c=>`&#${c.charCodeAt(0)};`).replace(/[\r\n]/g,' ');
}
// Keep repository paths; absolute paths, traversal and absent candidate files cannot be annotated.
function candidatePath(file) {
  const value=file.candidatePath;
  return typeof value==='string' && !value.startsWith('/') && !value.includes('\\')
    && !/^[A-Za-z]:/.test(value) && !value.split('/').some(part=>part==='..' || !part) ? value : null;
}
function findings(file) {
  const integration=file.ruleId || file.error ? [{id:'integration',ruleId:file.error?'EXECUTION_FAILED':file.ruleId,severity:'ERROR',source:'integration',description:file.error ?? file.description,positions:[]}] : [];
  if (file.findings?.length) return [...file.findings,...integration];
  // Preserve visibility for an older/custom comparator without positional findings.
  return [...(file.report?.changes ?? []).map(change=>({...change,source:'candidate',positions:[]})),
    ...(file.report?.diagnostics ?? []).map(diagnostic=>({id:'diagnostic',ruleId:diagnostic.code,source:diagnostic.source,description:diagnostic.description,severity:file.exitCode===2?'ERROR':'WARNING',positions:[]})),...integration];
}
function positions(file, finding) {
  if (finding.source!=='candidate' || !candidatePath(file)) return [];
  const seen=new Set();
  return (finding.positions ?? []).filter(p=>Number.isSafeInteger(p.line) && p.line>0
    && Number.isSafeInteger(p.column) && p.column>0 && !seen.has(p.line) && seen.add(p.line));
}

// Older packaged CI envelopes omit location; join by stable ID rather than parsing descriptions.
function operation(file, finding) {
  const location=finding.location ?? file.report?.changes?.find(change=>change.id===finding.id)?.location;
  if (!location) return 'Not applicable';
  return [location.method,location.path,location.direction,
    location.responseCode,location.mediaType,
    location.parameter ? `${location.parameter.in}: ${location.parameter.name}` : null].filter(Boolean).join(' ');
}

/** A deliberate break may pass; an incomplete comparison still needs review, even with exit 1. */
export function checkExitCode(report) {
  if (report.exitCode!==1 || report.policy?.failOnBreaking!==false) return report.exitCode;
  return report.files.some(file=>file.exitCode===3 || file.report?.status==='PARTIAL') ? 3 : 0;
}
function outcome(report) {
  const count=report.files.reduce((total,file)=>total+(file.report?.changes?.length
    ?? findings(file).filter(f=>file.exitCode===1 && f.severity==='ERROR' && !f.id?.startsWith('diagnostic')).length),0);
  const breaking=`${count} breaking change${count===1?'':'s'} detected.`;
  const state=report.exitCode===0?'Passed within the selected coverage.'
    :report.exitCode===2?'Input or execution errors; comparison could not be completed reliably.'
    :report.exitCode===3?'Review required; compatibility could not be established.'
    :breaking;
  const partial=report.files.some(file=>file.report?.status==='PARTIAL');
  return state+(report.exitCode===1 && partial?' Review also required for incomplete coverage.':'')
    +(report.policy?.failOnBreaking===false?' Breaking changes are allowed by workflow policy; input errors and review still fail the check.':'');
}

/** Human log: one physical line per logical finding, independent of Markdown escaping. */
export function consoleSummary(report) {
  const lines=[`OpenAPI contract guard | Exit code: ${report.exitCode} | Check exit code: ${checkExitCode(report)} | Profile: full`,
    `Base: ${clean(report.base)}`,`Candidate: ${clean(report.candidate)}`,outcome(report)];
  if(report.error)lines.push(`Execution error: ${report.error}`);
  let number=0;
  for (const file of report.files) for (const finding of findings(file)) {
    const points=positions(file,finding);
    const location=points.length?`Lines ${points.map(p=>p.line).join(', ')}${points.some(p=>p.approximate)?' (container)':''}`:'Lines unavailable';
    lines.push(`Finding ${++number} - ${location} | ${file.path} | ${operation(file,finding)} | ${finding.ruleId} | ${finding.description} [${finding.source}]`);
  }
  if(!number)lines.push('No findings.');
  for(const file of report.files.filter(file=>file.report))lines.push(`Coverage: ${file.path} | ${file.report.verdict} / ${file.report.status} | Excluded: ${(file.report.excludedFacets ?? []).join(', ')}`);
  lines.push('See summary.md and report.json. Compatibility is limited to the published profile.');
  // A hostile path/description must never start a new workflow command in stdout.
  return lines.map(line=>clean(line).replace(/[\r\n]/g,' ')).join('\n');
}

/** Emit at most 50 positional/global annotations; all logical findings remain in report.json. */
export function annotations(report) {
  const result=[];
  const files=report.error ? [...report.files,{error:report.error,path:'comparison'}] : report.files;
  for (const file of files) for (const finding of findings(file)) {
    const points=positions(file,finding);
    for (const point of points.length ? points : [null]) {
      const advisory=report.policy?.failOnBreaking===false && file.exitCode===1
        && finding.ruleId!=='EXECUTION_FAILED' && !finding.id?.startsWith('diagnostic');
      const level=finding.severity==='ERROR' && !advisory?'error':'warning';
      const location=point ? `file=${command(candidatePath(file),true)},line=${point.line},col=${point.column},` : '';
      const title=command(`${finding.ruleId} [${finding.id}]`,true);
      const context=point?.approximate ? ' (existing container; removed element)' : point?.role==='context' ? ' (schema context)' : '';
      const message=command(`${file.path} | ${operation(file,finding)}: ${finding.description}${context} [${finding.source}]`);
      result.push(`::${level} ${location}title=${title}::${message}`);
    }
  }
  return result.length>50 ? [...result.slice(0,50),`::notice::${result.length-50} annotations omitted; see the full summary and report.json.`] : result;
}

/** Markdown summary groups separate positions under a single logical finding. */
export function findingSummary(report) {
  const rows=[];
  for (const file of report.files) for (const finding of findings(file)) {
    const points=positions(file,finding);
    const lines=points.length ? points.map(p=>p.line).join(', ')+(points.some(p=>p.approximate)?' (container)':'') : 'Unavailable';
    rows.push(`| ${rows.length+1} | ${markdown(file.path)} | ${lines} | ${markdown(operation(file,finding))} | ${markdown(finding.ruleId)} | ${markdown(finding.description)} | ${markdown(finding.source)} |`);
  }
  const coverage=report.files.filter(file=>file.report).map(file=>
    `- ${markdown(file.path)}: ${markdown(file.report.verdict)} / ${markdown(file.report.status)}; profile ${markdown(file.report.profileId)}; excluded: ${markdown((file.report.excludedFacets ?? []).join(', '))}.`).join('\n');
  return `\n### Findings\n\n${rows.length} finding${rows.length===1?'':'s'}. ${markdown(outcome(report))}\n\n`
    + `${report.error?markdown(report.error)+'\n\n':''}| # | Candidate contract / affected file | Lines | Operation / direction / response | Rule | Reason | Source |\n| --- | --- | --- | --- | --- | --- | --- |\n`
    + rows.slice(0,200).join('\n')+`\n\n${rows.length>200?'Table truncated at 200 findings; full results in report.json.\n\n':''}`
    + `### Coverage\n\n${coverage || 'No engine comparison was performed.'}\n\nPositions refer to the analyzed candidate revision. Containers and schema context are navigation aids, not an exact diff. Compatibility never certifies the whole API.\n`;
}
