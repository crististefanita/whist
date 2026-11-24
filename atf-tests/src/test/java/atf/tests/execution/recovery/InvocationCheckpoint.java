package atf.tests.execution.recovery;

import atf.android.evidence.component.snapshot.ComponentCaptureJournal;
import atf.tests.execution.components.ComponentExecutionListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;

/** Independent JUnit state, replaced after every registration/outcome; captured values have separate seals. */
public final class InvocationCheckpoint {
    private static final ObjectMapper JSON=new ObjectMapper();
    private final Path output;
    private final String suite,provenanceHash;
    private final JsonNode context;
    private IOException failure;
    public InvocationCheckpoint(Path output,String suite,byte[] provenance) throws IOException {
        this.output=output; this.suite=suite; provenanceHash=ComponentCaptureJournal.sha256(provenance);
        context=JSON.readTree(provenance);
    }
    public void save(ComponentExecutionListener listener) {
        save(listener.invocations(),listener.populationComplete(),listener.containerFailures());
    }
    public void save(java.util.List<atf.tests.reporting.aggregate.ComponentReportCoverage.Invocation> invocations,
            boolean complete,java.util.List<String> failures) {
        try {
            var rows=new ArrayList<Map<String,Object>>();
            for(var row:invocations) {
                var item=new LinkedHashMap<String,Object>(); item.put("id",row.id());item.put("outcome",row.outcome());
                Path capture=output.resolve("captures").resolve(ComponentCaptureJournal.name(row.id()));
                if(Files.isRegularFile(capture)) item.put("captureSha256",ComponentCaptureJournal.sha256(Files.readAllBytes(capture)));
                rows.add(item);
            }
            var value=new LinkedHashMap<String,Object>();
            value.put("schemaVersion",1);value.put("kind","COMPONENT_INVOCATION_CHECKPOINT");value.put("suite",suite);
            value.put("provenanceSha256",provenanceHash);value.put("sourceContext",context);
            value.put("populationComplete",complete);value.put("containerFailures",failures);value.put("invocations",rows);
            Path target=output.resolve("invocation-checkpoint.json"),temporary=output.resolve("invocation-checkpoint.writing");
            JSON.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(),value);
            Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } catch(IOException error) {if(failure==null) failure=error;throw new UncheckedIOException(error);}
    }
    public void requireSaved() throws IOException {if(failure!=null) throw new IOException("COMPONENT_INVOCATION_CHECKPOINT_WRITE",failure);}
}
