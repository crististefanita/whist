package atf.android.evidence.component.snapshot;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

/** One immutable file per completed capture; checkpoints bind its exact bytes independently. */
public final class ComponentCaptureJournal {
    public static final String DIRECTORY="atf.component.capture-directory";
    private static final ObjectMapper JSON=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
    private final Path directory;
    public ComponentCaptureJournal(Path directory) throws IOException {
        this.directory=directory.toAbsolutePath().normalize();
        direct(this.directory); Files.createDirectories(this.directory); direct(this.directory);
    }
    public void append(CapturedInvocation invocation) throws IOException {
        Path file=directory.resolve(name(invocation.id()));
        byte[] bytes=JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(invocation);
        Path temporary=Files.createTempFile(directory,"capture-",".writing");
        try {
            Files.write(temporary,bytes); direct(file);
            if(Files.exists(file)) throw new IOException("COMPONENT_CAPTURE_DUPLICATE: "+invocation.id());
            Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }
    public static CapturedInvocation read(Path directory,String id,String expectedHash) throws IOException {
        Path file=directory.resolve(name(id)); direct(file);
        byte[] bytes=Files.readAllBytes(file);
        if(!sha256(bytes).equals(expectedHash)) throw new IOException("COMPONENT_CAPTURE_HASH: "+id);
        var value=JSON.readValue(bytes,CapturedInvocation.class);
        if(!value.id().equals(id)) throw new IOException("COMPONENT_CAPTURE_ID: "+id);
        return value;
    }
    public static String name(String id) { return sha256(id.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".json"; }
    public static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public static void direct(Path file) throws IOException {
        for(Path part=file.toAbsolutePath().normalize();part!=null;part=part.getParent())
            if(Files.isSymbolicLink(part) || Files.exists(part,LinkOption.NOFOLLOW_LINKS) && !part.toRealPath().equals(part))
                throw new IOException("COMPONENT_CAPTURE_INDIRECT: "+part);
    }
}
