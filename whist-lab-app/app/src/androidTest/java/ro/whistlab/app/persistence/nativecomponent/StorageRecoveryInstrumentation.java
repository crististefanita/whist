package ro.whistlab.app.persistence.nativecomponent;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;

/** Separate process invocations exercise Android recovery, without playing an artificial UI match. */
public final class StorageRecoveryInstrumentation extends Instrumentation {
    private String phase;
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        phase = arguments == null ? "" : arguments.getString("phase", "");
        start();
    }
    @Override public void onStart() {
        var result = new Bundle(); var checks = new NativeStorageChecks(getTargetContext());
        try {
            switch (phase) {
                case "prepare" -> NativeStorageRecovery.prepare(checks);
                case "recover" -> NativeStorageRecovery.recover(checks);
                case "corrupt-xml" -> NativeStorageRecovery.corruptXml(checks);
                case "verify-xml" -> NativeStorageRecovery.verifyXml(this, checks);
                default -> throw new IllegalArgumentException("Unknown storage phase: " + phase);
            }
            var receipt = new JSONObject().put("schemaVersion", 1).put("phase", phase)
                    .put("cases", checks.cases).put("failures", checks.failures);
            result.putString("stream", "\nATF_NATIVE_STORAGE=" + receipt + "\n");
            finish(checks.failures == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
        } catch (Exception original) {
            try {
                result.putString("stream", "\nATF_NATIVE_STORAGE=" + new JSONObject()
                        .put("schemaVersion", 1).put("phase", phase).put("cases", checks.cases)
                        .put("failures", checks.failures + 1).put("executionError", android.util.Log.getStackTraceString(original)) + "\n");
            } catch (org.json.JSONException encoding) {
                result.putString("stream", "Native storage execution failed: " + original + "; receipt: " + encoding);
            }
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
