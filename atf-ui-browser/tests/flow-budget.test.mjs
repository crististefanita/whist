import assert from 'node:assert/strict';
import test from 'node:test';
import {admitFlowPlan, verifyFlowExecution} from './flow-budget.mjs';

const plans = [{id:'sanity', deals:[1,1]}, {id:'long',deals:[1,2,3]}];
test('one explicit short flow is admitted and its observed hands reconcile', () => {
  const plan = admitFlowPlan(plans, 'sanity');
  const result = verifyFlowExecution(plan, [{hand:1,cards:1},{hand:2,cards:1}], [{file:'before.png'},{file:'score.png'}]);
  assert.equal(result.hands, 2); assert.equal(result.normalCaptures, 2);
  assert.ok(Object.isFrozen(plan.deals));
  console.log('Flow budget observations: explicit selection, immutable plan, actual hand and capture reconciliation');
});
test('matrices long games understated execution and excessive captures are rejected before publication', () => {
  assert.throws(() => admitFlowPlan(plans,'all'), /E2E_EXPLICIT_FLOW/);
  assert.throws(() => admitFlowPlan(plans,'missing'), /E2E_UNKNOWN_FLOW/);
  assert.throws(() => admitFlowPlan(plans,'long'), /E2E_HAND_BUDGET/);
  assert.throws(() => admitFlowPlan([{id:'bad',deals:[0]}],'bad'), /E2E_ILLEGAL_DEAL/);
  const plan = admitFlowPlan(plans,'sanity');
  assert.throws(() => verifyFlowExecution(plan,[{hand:1,cards:1}],[]), /E2E_EXECUTION_SEQUENCE/);
  assert.throws(() => verifyFlowExecution(plan,[{hand:1,cards:1},{hand:2,cards:2}],[]), /E2E_EXECUTION_SEQUENCE/);
  const rounds=[{hand:1,cards:1},{hand:2,cards:1}];
  assert.throws(() => verifyFlowExecution(plan,rounds,Array.from({length:5},(_,i)=>({file:i+'.png'}))), /E2E_CAPTURE_BUDGET/);
  assert.throws(() => verifyFlowExecution(plan,rounds,[{file:'same.png'},{file:'same.png'}]), /E2E_CAPTURE_BUDGET/);
  console.log('Flow budget rejection observations: precise selection, sequence and capture causes');
});
