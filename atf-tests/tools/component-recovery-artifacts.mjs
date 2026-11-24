import {digest} from './source-input-identity.mjs';

const hash=/^[0-9a-f]{64}$/;
const allowed=new Set(['README.md','attempts/original-report.html','attempts/retry-report.html','attempts/original-checkpoint.json']);

export function recoveryArtifactRows(execution) {
  if(execution.recovery===undefined) return [];
  const recovery=execution.recovery;
  if(recovery?.schemaVersion!==1 || !hash.test(recovery.originalCheckpointSha256??'')
      || !Array.isArray(recovery.artifacts) || !recovery.artifacts.length
      || !Array.isArray(recovery.reusedInvocationIds) || !Array.isArray(recovery.executedInvocationIds)) throw Error('COMPONENT_RECOVERY_SCHEMA');
  const ids=[...recovery.reusedInvocationIds,...recovery.executedInvocationIds];
  if(new Set(ids).size!==ids.length || ids.length!==execution.invocations?.length
      || execution.invocations.some(row=>!ids.includes(row.id))) throw Error('COMPONENT_RECOVERY_POPULATION');
  const seen=new Set();
  for(const artifact of recovery.artifacts) {
    if(!artifact || !allowed.has(artifact.path) || seen.has(artifact.path) || !hash.test(artifact.sha256??'')) throw Error('COMPONENT_RECOVERY_ARTIFACT');
    seen.add(artifact.path);
    if(artifact.path==='attempts/original-checkpoint.json' && artifact.sha256!==recovery.originalCheckpointSha256) throw Error('COMPONENT_RECOVERY_CHECKPOINT');
  }
  if(!seen.has('README.md') || !seen.has('attempts/original-checkpoint.json')
      || recovery.executedInvocationIds.length && !seen.has('attempts/retry-report.html')) throw Error('COMPONENT_RECOVERY_ARTIFACT_MISSING');
  return recovery.artifacts;
}

export async function readRecoveryArtifacts(execution,read) {
  const result=[];
  for(const artifact of recoveryArtifactRows(execution)) {
    const bytes=await read(artifact.path);
    if(digest(bytes)!==artifact.sha256) throw Error('COMPONENT_RECOVERY_ARTIFACT_HASH');
    result.push({...artifact,bytes});
  }
  return result;
}
