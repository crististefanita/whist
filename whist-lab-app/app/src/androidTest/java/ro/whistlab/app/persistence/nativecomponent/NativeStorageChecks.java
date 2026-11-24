package ro.whistlab.app.persistence.nativecomponent;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

final class NativeStorageChecks {
    static final String KEY = "current_session_v1";
    final Context context;
    final JSONArray cases = new JSONArray();
    int failures;
    NativeStorageChecks(Context context) { this.context = context; }
    File preferences() { return new File(context.getApplicationInfo().dataDir, "shared_prefs/whist_lab_game.xml"); }
    File fixture() { return new File(context.getCacheDir(), "native-storage-reference.json"); }
    File ranking() { return new File(context.getFilesDir(), "highestScores.json"); }
    JSONObject reference() throws Exception { return new JSONObject(text(fixture())); }
    void compare(String id, String name, Object expected, Object observed) throws Exception {
        boolean passed = java.util.Objects.equals(expected, observed);
        cases.put(new JSONObject().put("id", id).put("name", name).put("expected", expected)
                .put("observed", observed).put("passed", passed));
        if (!passed) failures++;
    }
    static String text(File file) throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
    static void write(File file, String text) throws Exception { Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8)); }
    static String hash(File file) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        var result = new StringBuilder(); for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
    static String exception(Throwing operation) throws Exception {
        try { operation.run(); return "NO_EXCEPTION"; }
        catch (IllegalStateException | java.io.IOException failure) { return failure.getClass().getName(); }
    }
    @FunctionalInterface interface Throwing { void run() throws Exception; }
}
