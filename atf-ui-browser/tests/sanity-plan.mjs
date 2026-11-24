import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const integer = n => Number.isInteger(n) && n >= -2147483648 && n <= 2147483647;
export function admitSanityContract(spec) {
  assert.ok(spec && typeof spec === 'object' && !Array.isArray(spec) && Object.keys(spec).sort().join() ===
    ['schemaVersion','id','steps','captureSteps','optionalSteps','deals','maximumNormalCaptures','maximumFailureCaptures'].sort().join()
    && spec.schemaVersion === 1 && /^[A-Z][A-Z0-9-]{3,79}$/.test(spec.id), 'E2E_PLAN_SCHEMA');
  const validSteps = values => Array.isArray(values) && values.every(s => typeof s === 'string' && /^[a-z][a-z0-9-]*$/.test(s))
    && new Set(values).size === values.length;
  assert.ok(validSteps(spec.steps) && spec.steps.length > 0 && spec.steps.length <= 20 && !spec.steps.includes('failure'), 'E2E_STEP_PLAN');
  assert.ok(validSteps(spec.optionalSteps) && spec.optionalSteps.every(s=>!spec.steps.includes(s)&&s!=='failure'),'E2E_STEP_PLAN');
  assert.ok(Array.isArray(spec.deals) && spec.deals.length > 0 && spec.deals.length <= 2
    && spec.deals.every(n => integer(n) && n > 0 && n <= 17), 'E2E_HAND_BUDGET');
  assert.equal(spec.steps.filter(s => /(?:^|-)score$/.test(s)).length, spec.deals.length, 'E2E_EXECUTION_SEQUENCE');
  assert.ok(integer(spec.maximumNormalCaptures) && spec.maximumNormalCaptures > 0 && spec.maximumNormalCaptures <= 4
    && spec.maximumFailureCaptures === 1 && validSteps(spec.captureSteps) && spec.captureSteps.length > 0
    && spec.captureSteps.length <= spec.maximumNormalCaptures && spec.captureSteps.every(s => spec.steps.includes(s)), 'E2E_CAPTURE_BUDGET');
  return Object.freeze({...spec, steps:Object.freeze([...spec.steps]), captureSteps:Object.freeze([...spec.captureSteps]), optionalSteps:Object.freeze([...spec.optionalSteps]), deals:Object.freeze([...spec.deals])});
}

export const tableSanityPlan = admitSanityContract(JSON.parse(readFileSync(new URL('../../config/table-sanity-plan.json', import.meta.url), 'utf8')));

export async function executeSanityPlan(plan, driver) {
  const admitted = admitSanityContract(plan);
  for (const step of admitted.steps) await driver(step);
}

export function verifySanityFrames(spec, frames, complete = true) {
  const plan = admitSanityContract(spec);
  assert.ok(Array.isArray(frames), 'E2E_EXECUTION_SEQUENCE');
  let ordinal=0, normal=0, failures=0, failed=false;
  const captures=new Set();
  for (const frame of frames) {
    assert.ok(frame && typeof frame.id === 'string', 'E2E_EXECUTION_SEQUENCE');
    const captured=typeof frame.capture === 'string' && frame.capture.trim() !== '';
    if(captured){ assert.ok(!captures.has(frame.capture),'E2E_DUPLICATE_CAPTURE');captures.add(frame.capture); }
    if(frame.id==='failure'){
      assert.ok(!complete&&!failed,'E2E_FAILURE_CAPTURE_BUDGET');failed=true;if(captured)failures++;
    }else if(plan.optionalSteps.includes(frame.id)){
      assert.ok(!failed&&!captured,'E2E_CAPTURE_OWNERSHIP');
    }else{
      assert.ok(!failed&&ordinal<plan.steps.length&&plan.steps[ordinal++]===frame.id,'E2E_EXECUTION_SEQUENCE');
      assert.equal(captured,plan.captureSteps.includes(frame.id),'E2E_CAPTURE_OWNERSHIP');if(captured)normal++;
    }
  }
  assert.ok(normal<=plan.maximumNormalCaptures&&failures<=plan.maximumFailureCaptures,'E2E_CAPTURE_BUDGET');
  assert.ok(!complete||ordinal===plan.steps.length,'E2E_INCOMPLETE_FLOW');
  return {scenarioId:plan.id,steps:ordinal,normalCaptures:normal,failureCaptures:failures};
}
