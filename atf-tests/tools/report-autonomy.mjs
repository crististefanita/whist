import assert from 'node:assert/strict';
export function admitReview(catalog, manifest) {
  assert.equal(manifest.schemaVersion, 1, 'REPORT_REVIEW_SCHEMA');
  const required=catalog.producers.filter(p=>p.role==='primary').map(p=>p.source).sort();
  assert.deepEqual(manifest.reports.map(r=>r.producer).sort(),required,'REPORT_REVIEW_PRODUCERS');
  for(const row of manifest.reports) {
    assert.match(row.sha256,/^[a-f0-9]{64}$/,'REPORT_REVIEW_ORIGINAL_SEAL');
    assert.ok(typeof row.file==='string'&&row.file.endsWith('.html'),'REPORT_REVIEW_FILE');
  }
  return manifest.reports;
}
export function qualifyReview(original, observed) {
  assert.equal(observed.sha256, original.sha256,'REPORT_REVIEW_BYTES_CHANGED');
  for(const key of ['overview','owner','lastRow','fullBody','returned','networkBlocked'])
    assert.equal(observed[key],true,'REPORT_REVIEW_'+key);
  assert.equal(observed.image, original.inlineReferences>0 ? true : null,'REPORT_REVIEW_IMAGE');
  return {id:'REPORT-AUTONOMY-'+original.id,name:original.id+' detached HTML navigation',
    layer:'report-component',status:'PASS',images:[],
    expected:{sha256:original.sha256,owner:original.ownerName,lastCaption:original.lastCaption,bodyTail:original.bodyTail,offline:true},observed};
}
