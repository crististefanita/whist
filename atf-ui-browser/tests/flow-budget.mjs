import assert from 'node:assert/strict';

/** Admission precedes directories, browser startup and user actions. Matrices belong to components. */
export function admitFlowPlan(profiles, selection) {
  assert.ok(selection !== 'all', 'E2E_EXPLICIT_FLOW: choose one sanity flow, not a resolution matrix');
  const plan = profiles.find(profile => profile.id === selection);
  assert.ok(plan, 'E2E_UNKNOWN_FLOW');
  assert.ok(Array.isArray(plan.deals) && plan.deals.length > 0 && plan.deals.length <= 2,
    'E2E_HAND_BUDGET: a primary flow contains one or two hands');
  assert.ok(plan.deals.every(cards => Number.isInteger(cards) && cards > 0), 'E2E_ILLEGAL_DEAL');
  return Object.freeze({...plan, deals: Object.freeze([...plan.deals])});
}

export function verifyFlowExecution(plan, rounds, captures) {
  assert.ok(rounds.length === plan.deals.length && rounds.every((round, index) =>
    round.hand === index + 1 && round.cards === plan.deals[index]), 'E2E_EXECUTION_SEQUENCE');
  assert.ok(captures.length <= 4 && new Set(captures.map(capture => capture.file)).size === captures.length,
    'E2E_CAPTURE_BUDGET: retain at most four normal checkpoints');
  return {contract: 'MINIMAL_TABLE_FLOW_V1', hands: rounds.length, normalCaptures: captures.length,
    maximumHands: 2, maximumNormalCaptures: 4};
}
