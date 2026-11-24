import assert from 'node:assert/strict';
import {tableSanityPlan,verifySanityFrames} from './sanity-plan.mjs';

/** Admission precedes directories, browser startup and user actions. Matrices belong to components. */
export function admitFlowPlan(profiles, selection) {
  assert.ok(selection !== 'all', 'E2E_EXPLICIT_FLOW: choose one sanity flow, not a resolution matrix');
  const plan = profiles.find(profile => profile.id === selection);
  assert.ok(plan, 'E2E_UNKNOWN_FLOW');
  assert.ok(Array.isArray(plan.deals) && plan.deals.length > 0 && plan.deals.length <= 2,
    'E2E_HAND_BUDGET: a primary flow contains one or two hands');
  assert.ok(plan.deals.every(cards => Number.isInteger(cards) && cards > 0), 'E2E_ILLEGAL_DEAL');
  assert.deepEqual(plan.deals,tableSanityPlan.deals,'E2E_EXECUTION_SEQUENCE');
  return Object.freeze({...plan, scenario:tableSanityPlan, deals: Object.freeze([...plan.deals])});
}

export function verifyFlowExecution(plan, rounds, captures, frames, completed = true) {
  assert.ok((completed ? rounds.length === plan.deals.length : rounds.length <= plan.deals.length) && rounds.every((round, index) =>
    round.hand === index + 1 && round.cards === plan.deals[index]), 'E2E_EXECUTION_SEQUENCE');
  assert.ok(captures.filter(c=>c.role!=='failure').length <= 4 && captures.filter(c=>c.role==='failure').length <= 1
    && new Set(captures.map(capture => capture.file)).size === captures.length,
    'E2E_CAPTURE_BUDGET: retain at most four normal checkpoints');
  const observed=verifySanityFrames(plan.scenario,frames,completed);
  assert.deepEqual([...captures.map(c=>c.file)].sort(),frames.filter(f=>f.capture).map(f=>f.capture).sort(),'E2E_CAPTURE_OWNERSHIP');
  for(const image of captures)assert.ok(frames.some(f=>f.capture===image.file&&f.id===image.role),'E2E_CAPTURE_OWNERSHIP');
  return {contract: 'MINIMAL_TABLE_FLOW_V1', ...observed, plan:plan.scenario, frames, hands: rounds.length,
    maximumHands: 2, maximumNormalCaptures: 4};
}
