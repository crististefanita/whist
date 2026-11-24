package atf.android.e2e.support.journal;

import atf.android.e2e.support.flow.AndroidUiFlowEvidence;
import atf.android.evidence.timing.AndroidPhaseTimingCodec;
import atf.reporting.ui.model.AtfUiActionRecord;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;

/** Pure trace encoding and the existing replace-on-success journal boundary. */
public final class AndroidUiFlowJournal {
    private AndroidUiFlowJournal() { }

    public static Properties encode(AndroidUiFlowEvidence trace, String failurePhase, Set<String> runningPhases) {
        var properties = new Properties();
        properties.setProperty("format.version", "1");
        properties.setProperty("flow.id", trace.id());
        properties.setProperty("flow.name", trace.name());
        properties.setProperty("flow.testClass", trace.testClass());
        properties.setProperty("flow.testPackage", trace.testPackage());
        properties.setProperty("flow.appPackage", trace.appPackage());
        properties.setProperty("flow.tags", String.join(",", trace.tags()));
        properties.setProperty("flow.status", trace.status().name());
        properties.setProperty("flow.failure", trace.failure());
        properties.setProperty("flow.failurePhase", failurePhase);
        var phases = trace.phaseSnapshots();
        properties.setProperty("flow.phaseCount", Integer.toString(phases.size()));
        for (int phaseIndex = 0; phaseIndex < phases.size(); phaseIndex++) {
            var phase = phases.get(phaseIndex);
            String prefix = "phase." + phaseIndex;
            properties.setProperty(prefix + ".name", phase.name());
            phase.timing().ifPresent(timing -> AndroidPhaseTimingCodec.write(properties, prefix + ".timing", timing));
            if (runningPhases.contains(phase.name())) AndroidPhaseTimingCodec.running(properties,
                    prefix + ".timing", trace.id(), phase.name(), phaseIndex);
            properties.setProperty(prefix + ".stepCount", Integer.toString(phase.checkpoints().size()));
            for (int stepIndex = 0; stepIndex < phase.checkpoints().size(); stepIndex++) {
                var checkpoint = phase.checkpoints().get(stepIndex);
                String step = prefix + ".step." + stepIndex;
                properties.setProperty(step + ".keyword", checkpoint.keyword());
                properties.setProperty(step + ".title", checkpoint.title());
                properties.setProperty(step + ".target", checkpoint.target());
                properties.setProperty(step + ".screenshot", artifactName(trace.manifest(), checkpoint.screenshot()));
                properties.setProperty(step + ".status", checkpoint.status().name());
                properties.setProperty(step + ".actionCount", Integer.toString(checkpoint.actions().size()));
                for (int index = 0; index < checkpoint.actions().size(); index++)
                    action(properties, step + ".action." + index, checkpoint.actions().get(index));
                properties.setProperty(step + ".assertionCount", Integer.toString(checkpoint.assertions().size()));
                for (int index = 0; index < checkpoint.assertions().size(); index++) {
                    var assertion = checkpoint.assertions().get(index);
                    String key = step + ".assertion." + index;
                    properties.setProperty(key + ".name", assertion.name());
                    properties.setProperty(key + ".expected", assertion.expected());
                    properties.setProperty(key + ".actual", assertion.actual());
                    properties.setProperty(key + ".passed", Boolean.toString(assertion.passed()));
                    properties.setProperty(key + ".status", assertion.resultStatus().name());
                }
                properties.setProperty(step + ".artifactCount", Integer.toString(checkpoint.supportingArtifacts().size()));
                for (int index = 0; index < checkpoint.supportingArtifacts().size(); index++)
                    properties.setProperty(step + ".artifact." + index,
                            artifactName(trace.manifest(), checkpoint.supportingArtifacts().get(index)));
            }
        }
        var pending = trace.pendingActions();
        properties.setProperty("flow.pendingActionCount", Integer.toString(pending.size()));
        for (int index = 0; index < pending.size(); index++)
            action(properties, "flow.pendingAction." + index, pending.get(index));
        return properties;
    }

    public static void write(Path manifest, Properties properties) throws IOException {
        Path temporary = manifest.resolveSibling(manifest.getFileName() + ".tmp");
        try (var output = Files.newOutputStream(temporary)) {
            properties.store(output, "Whist ordered Android UI flow evidence");
        }
        try {
            Files.move(temporary, manifest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, manifest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void action(Properties properties, String prefix, AtfUiActionRecord action) {
        properties.setProperty(prefix + ".sequence", Integer.toString(action.sequence()));
        properties.setProperty(prefix + ".operation", action.operation());
        properties.setProperty(prefix + ".target", action.target());
        properties.setProperty(prefix + ".expected", action.expected());
        properties.setProperty(prefix + ".observed", action.observed());
        properties.setProperty(prefix + ".durationMillis", Long.toString(action.durationMillis()));
        properties.setProperty(prefix + ".status", action.status().name());
        properties.setProperty(prefix + ".source", action.source().name());
    }

    private static String artifactName(Path manifest, Path artifact) {
        if (artifact == null) return "";
        Path root = manifest.toAbsolutePath().normalize().getParent();
        Path file = artifact.toAbsolutePath().normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Android UI journal artifact outside its owner: " + file);
        return root.relativize(file).toString().replace('\\', '/');
    }
}
