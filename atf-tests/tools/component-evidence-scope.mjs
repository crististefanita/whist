import {atfGroups, atfOwnershipCompatible, hasAtfCollections} from './atf-component-ownership.mjs';
import {atfDependencySelection} from './atf-component-dependencies.mjs';
import {readFileSync} from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export const nonBuildDocumentation = file => file === 'README.md'
    || (/^(?:docs\/|lib\/README\.)/.test(file) && file.endsWith('.md'))
    || (file.endsWith('.md') && /(?:^|\/)src\//.test(file)
        && !/(?:^|\/)src\/[^/]+\/(?:assets|res|resources)\//.test(file))
    || (!/(?:^|\/)src\//.test(file) && file.endsWith('/README.md'));

export function changedInputs(candidate, previous) {
  const paths = new Set([...candidate.keys(), ...previous.keys()]);
  return [...paths].filter(file => candidate.get(file) !== previous.get(file)).sort();
}

const standaloneGovernanceLaunchers = new Set([
  'scripts/AtfStandaloneJava.ps1',
  'scripts/AtfTestChangeBatch.ps1',
  'scripts/run-atf-governance.ps1',
  'scripts/run-documentation-governance.ps1',
  'scripts/run-jvm-inventory.ps1'
]);
export const isStandaloneGovernanceLauncher = file => standaloneGovernanceLaunchers.has(file);

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const collectionOwners = readFileSync(path.join(projectRoot, 'config/gradle-app-collections.properties'), 'utf8')
  .split(/\r?\n/).map(line => line.trim()).filter(line => line && !line.startsWith('#'))
  .map(line => line.split('=', 2));
if (collectionOwners.some(([owner, group]) => !owner || !group)
    || new Set(collectionOwners.map(([owner]) => owner)).size !== collectionOwners.length) {
  throw Error('INVALID_GRADLE_APP_COLLECTION_OWNERSHIP');
}
const gradleAppPackages = new Map(collectionOwners.map(([owner, group]) => [owner, `whist-lab-app-${group}`]));

const appGroups = [...new Set(gradleAppPackages.values())];
const gradleGroups = [...appGroups, 'whist-lab-app-atf-card-table-ui', 'whist-lab-app-card-table-core', 'whist-lab-app-rules'];
const mavenGroups = ['atf-validation', 'atf-test-android', 'release-tools', 'atf-tests'];
const allGroups = [...mavenGroups, ...gradleGroups];
const modernGroups = [...new Set([...allGroups, ...atfGroups])];
const frameworkOnly = file => nonBuildDocumentation(file)
    || file.startsWith('atf-tests/') || file.startsWith('docs/') || file.startsWith('release/')
    || file.startsWith('atf-ui-browser/') || file === '.github/workflows/whist-components.yml'
    || file === 'config/documentation-editions.json'
    || file.startsWith('release-tools/config/release-platform-rag-policy.')
    || file.startsWith('config/atf-governance-policy.') || file.startsWith('config/atf-test-suites.')
    || file.startsWith('config/test-collections.') || file.startsWith('scripts/')
    || file.startsWith('.atf-signing/') || file.startsWith('release-tools/');

function appPackageOwner(file, sourceSet) {
  const prefix = new RegExp(`^whist-lab-app/(?:app|whist-presentation)/src/${sourceSet}/java/ro/whistlab/app/`).exec(file);
  if (!prefix) return null;
  const packageRoot = file.slice(prefix[0].length).split('/')[0];
  return gradleAppPackages.get(packageRoot) ?? null;
}

function affectedGroups(file) {
  // This inventory is checked by every RAG evaluation. A change to the inventory
  // must not invalidate the independent Maven framework suite by itself.
  if (file === 'config/documentation-editions.json') return [];
  if (file.startsWith('release-tools/')) return ['release-tools', 'atf-tests'];
  if (/^docs\/game\/ATF_Rules(?:Ro|En)\.md$/.test(file)) return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('.github/')) return file === '.github/workflows/whist-components.yml' ? ['atf-tests'] : allGroups;
  if (frameworkOnly(file)) return ['atf-tests'];

  if (file.startsWith('atf-validation/')) return ['atf-validation', 'atf-tests'];
  if (file.startsWith('atf-test-android/')) return ['atf-test-android', 'atf-tests'];
  if (file.startsWith('atf-tests/')) return ['atf-tests'];

  if (file === 'pom.xml' || file.startsWith('.mvn/')) return [...new Set([...mavenGroups, ...atfGroups])];
  if (file.endsWith('/pom.xml') || file.startsWith('lib/')) return allGroups;

  if (file === 'config/gradle-app-collections.properties') return [...appGroups, 'atf-tests'];
  if (file === 'whist-lab-app/gradle/app-component-collections.gradle') return [...appGroups, 'atf-tests'];
  if (file === 'config/gradle-audit-collections.properties' || /^config\/gradle-[^/]+\.properties$/.test(file)
      || file.startsWith('whist-lab-app/gradle/') || file.startsWith('whist-lab-app/gradle/wrapper/')
      || ['whist-lab-app/build.gradle', 'whist-lab-app/settings.gradle', 'whist-lab-app/gradle.properties'].includes(file)
      || file.startsWith('whist-lab-app/test-audit/')) return [...gradleGroups, 'atf-tests'];

  if (/^whist-lab-app\/(?:app|whist-presentation)\/src\/main\/java\/ro\/whistlab\/app\//.test(file)) {
    const owner = appPackageOwner(file, 'main');
    return owner ? [owner, 'atf-tests'] : allGroups;
  }
  if (file.startsWith('whist-lab-app/app/src/test/java/ro/whistlab/app/')) {
    const owner = appPackageOwner(file, 'test');
    return owner ? [owner, 'atf-tests'] : allGroups;
  }
  if (file.startsWith('whist-lab-app/app/src/rasterTest/')) return ['whist-lab-app-history', 'atf-tests'];
  if (file.startsWith('whist-lab-app/app/')) return [...appGroups, 'atf-tests'];
  if (file.startsWith('whist-lab-app/whist-presentation/src/main/achievement-rules/')
      || file === 'whist-lab-app/whist-presentation/achievement-catalog.gradle')
    return ['whist-lab-app-achievements', 'atf-tests'];
  if (file.startsWith('whist-lab-app/whist-presentation/')) return [...appGroups, 'atf-tests'];
  if (/^whist-lab-app\/card-table-core\/src\/(?:test|unitTest)\//.test(file))
    return ['whist-lab-app-card-table-core', 'atf-tests'];
  if (file.startsWith('whist-lab-app/card-table-core/'))
    return [...appGroups, 'whist-lab-app-atf-card-table-ui', 'whist-lab-app-card-table-core', 'atf-tests'];

  // Core, names and the shared presentation library are dependencies of the app collections.
  if (file.startsWith('whist-lab-app/game-core/src/main/java/ro/whistlab/core/rules/HowToPlay.java'))
    return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/game-core/') || file.startsWith('whist-lab-app/card-table-names/'))
    return [...appGroups, 'whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/atf-card-table-ui/'))
    return [...appGroups, 'whist-lab-app-atf-card-table-ui', 'atf-tests'];
  if (file.startsWith('whist-lab-app/ai-lab/')) return ['whist-lab-app-rules', 'atf-tests'];
  if (file.startsWith('whist-lab-app/algorithm-build/') || file.startsWith('whist-lab-app/ios-bridge/'))
    return ['atf-tests'];
  if (file.startsWith('whist-lab-app/')) return [...gradleGroups, 'atf-tests'];

  if (/^config\/whist-[^/]+\.(?:json|ya?ml)$/.test(file)) return [...gradleGroups, 'atf-tests'];
  if (file.startsWith('config/')) return ['atf-tests'];

  // New or unclassified paths fail closed: establish ownership before reusing old evidence.
  return allGroups;
}

export function componentEvidenceScope(group, releaseInputs, executionInputs, {androidVersionMetadataOnly = false} = {}) {
  const changed = changedInputs(releaseInputs, executionInputs);
  const modern = hasAtfCollections(releaseInputs);
  const knownGroups = modern ? modernGroups : allGroups;
  if (changed.length && !knownGroups.includes(group)) {
    return {decision: 'RUN', reason: 'UNKNOWN_SUITE_FAIL_CLOSED', changed, affected: changed, affectedGroups: allGroups};
  }
  const compatible = atfOwnershipCompatible(releaseInputs, executionInputs);
  const classified = changed.map(file => {
    let groups = affectedGroups(file);
    if (modern) {
      // Markdown and dossier bytes have no launcher/runtime dependency. Other code
      // dependencies remain conservative until a narrower edge is qualified.
      if (nonBuildDocumentation(file)) groups = /^docs\/game\/ATF_Rules(?:Ro|En)\.md$/.test(file)
        ? ['atf-tests-documentation', 'whist-lab-app-rules'] : ['atf-tests-documentation'];
      else if (groups.includes('atf-tests') && file !== 'pom.xml') {
        const dependency = atfDependencySelection(file, releaseInputs, executionInputs);
        groups = [...new Set([...groups.filter(group => group !== 'atf-tests'), ...dependency.groups])];
        if (!compatible) groups = [...new Set([...groups, ...atfGroups])];
        return {file, groups, dependency};
      }
      if (/^lib\/README\./.test(file)) groups = [...new Set([...groups, 'atf-tests'])];
      if (!compatible) groups = [...new Set([...groups, ...atfGroups])];
    } else if (!compatible) groups = [...new Set([...groups, 'atf-tests'])];
    return {file, groups};
  });
  const alwaysEvaluatedRagChecks = [...new Set(classified
    .filter(row => row.file === 'config/documentation-editions.json')
    .map(() => 'DOCUMENTATION_EDITIONS'))].sort();
  const metadataOnlyBuildFile = androidVersionMetadataOnly
      && classified.every(row => row.file === 'whist-lab-app/app/build.gradle' || row.file === 'pom.xml' || frameworkOnly(row.file));
  const affected = classified.filter(row => row.groups.includes(group)
      && !(metadataOnlyBuildFile && row.file === 'whist-lab-app/app/build.gradle')).map(row => row.file);
  const unclassified = classified.filter(row => knownGroups.every(group => row.groups.includes(group)));
  const decision = affected.length ? 'RUN' : changed.length ? 'REUSE' : 'MATCH';
  const reason = affected.length
      ? unclassified.some(row => affected.includes(row.file)) ? 'UNKNOWN_INPUT_FAIL_CLOSED' : 'OWNED_OR_SHARED_INPUT_CHANGED'
      : alwaysEvaluatedRagChecks.length ? 'RAG_GATE_ONLY'
        : changed.length ? 'OWNED_INPUTS_UNCHANGED' : 'IDENTICAL_INPUTS';
  const dependencyEvidence = classified.filter(row => row.dependency).map(row => ({path: row.file, ...row.dependency}));
  return {decision, reason, changed, affected,
    affectedGroups: [...new Set(classified.flatMap(row => row.groups))].sort(),
    alwaysEvaluatedRagChecks, ...(dependencyEvidence.length ? {dependencyEvidence} : {})};
}
