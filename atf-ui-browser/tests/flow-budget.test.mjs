import assert from 'node:assert/strict';
import test from 'node:test';
import {admitFlowPlan, verifyFlowExecution} from './flow-budget.mjs';

const plans = [{id:'sanity', deals:[1,1]}, {id:'long',deals:[1,2,3]}];
const roles=['bidding','before-human','after-human','manual-score','auto-bidding','auto-score','return-control'];
const captured=new Set(['bidding','before-human','after-human','auto-score']);
const frames=roles.map(id=>({id,capture:captured.has(id)?id+'.png':null}));
const images=frames.filter(f=>f.capture).map(f=>({file:f.capture,role:f.id}));
test('one explicit short flow is admitted and its observed hands reconcile', () => {
  const plan = admitFlowPlan(plans, 'sanity');
  const result = verifyFlowExecution(plan, [{hand:1,cards:1},{hand:2,cards:1}], images,frames);
  assert.equal(result.hands, 2); assert.equal(result.normalCaptures, 4);
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
  assert.throws(() => verifyFlowExecution(plan,rounds,images,frames.slice(1)),/E2E_EXECUTION_SEQUENCE/);
  assert.throws(() => verifyFlowExecution(plan,rounds,images.map(i=>({...i,role:'foreign'})),frames),/E2E_CAPTURE_OWNERSHIP/);
  console.log('Flow budget rejection observations: precise selection, sequence and capture causes');
});
