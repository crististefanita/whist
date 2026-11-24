package ro.whistlab.app.persistence.nativecomponent;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.util.AtomicFile;
import android.view.accessibility.AccessibilityEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import org.json.JSONObject;
import ro.whistlab.app.MainActivity;
import ro.whistlab.app.highscores.AndroidHighScoreStorage;
import ro.whistlab.app.highscores.HighScoreRepository;
import ro.whistlab.app.localization.WhistUiText;
import ro.whistlab.app.persistence.GameSessionStore;
import ro.whistlab.app.profile.PlayerPreferences;
import ro.whistlab.core.config.GameConfig;
import ro.whistlab.core.rules.HowToPlay;
import ro.whistlab.core.session.GameSession;

final class NativeStorageRecovery {
    private NativeStorageRecovery() { }
    static void prepare(NativeStorageChecks c) throws Exception {
        var profile = new PlayerPreferences(c.context); profile.saveName("AuditȘtef"); profile.rankingLimit(10);
        var game = GameSession.newGame(GameConfig.defaults(2718));
        game.submitBid(game.snapshot().biddingSeat(), 0);
        GameSessionStore.save(c.context, game, false, "native-recovery-2718");
        new HighScoreRepository(new AndroidHighScoreStorage(c.context), new Random(7)).boards();
        var reference = new JSONObject().put("state", GameSessionStore.encode(game.saveState()).toString())
                .put("profile", profile.name()).put("ranking", NativeStorageChecks.text(c.ranking()))
                .put("checkpointHash", NativeStorageChecks.hash(c.preferences()));
        NativeStorageChecks.write(c.fixture(), reference.toString());
        interruptedWrite(c.preferences()); interruptedWrite(c.ranking());
        c.compare("interrupted-checkpoint", "Checkpoint write stopped with a committed backup", true, new java.io.File(c.preferences()+".bak").isFile());
        c.compare("interrupted-ranking", "Ranking write stopped with a committed backup", true, new java.io.File(c.ranking()+".bak").isFile());
    }
    private static void interruptedWrite(java.io.File file) throws Exception {
        // Intentionally omit finishWrite/failWrite; the host then stops this process before recovery.
        var stream = new AtomicFile(file).startWrite();
        stream.write("<partial".getBytes(StandardCharsets.UTF_8)); stream.flush(); stream.getFD().sync(); stream.close();
    }
    static void recover(NativeStorageChecks c) throws Exception {
        var reference = c.reference(); var game = GameSessionStore.load(c.context);
        c.compare("checkpoint-recovered", "The original engine state returns after process interruption", reference.getString("state"),
                game == null ? "MISSING" : GameSessionStore.encode(game.saveState()).toString());
        c.compare("checkpoint-identity", "Match identity and ranking exclusion survive recovery", "native-recovery-2718:false",
                GameSessionStore.matchId(c.context)+":"+GameSessionStore.rankingEligible(c.context));
        c.compare("checkpoint-bytes", "Android restores the original committed preference bytes", reference.getString("checkpointHash"), NativeStorageChecks.hash(c.preferences()));
        var storage = new AndroidHighScoreStorage(c.context);
        c.compare("ranking-recovered", "AtomicFile restores the complete original ranking", reference.getString("ranking"), storage.read());
        var preferences = c.context.getSharedPreferences("whist_lab_game", Context.MODE_PRIVATE);
        preferences.edit().remove(NativeStorageChecks.KEY).commit();
        c.compare("valid-empty-preferences", "A valid empty Android map remains an absent checkpoint", true, GameSessionStore.load(c.context) == null);
        preferences.edit().putString(NativeStorageChecks.KEY, "{invalid").commit();
        String hash = NativeStorageChecks.hash(c.preferences());
        c.compare("invalid-json", "Invalid checkpoint JSON raises an explicit diagnostic", "java.lang.IllegalStateException",
                NativeStorageChecks.exception(() -> GameSessionStore.load(c.context)));
        c.compare("invalid-json-kept", "Diagnosis does not remove invalid checkpoint bytes", hash, NativeStorageChecks.hash(c.preferences()));
        preferences.edit().putInt(NativeStorageChecks.KEY, 3).commit();
        c.compare("invalid-type", "Wrong preference type is a localized storage failure boundary", "java.lang.IllegalStateException",
                NativeStorageChecks.exception(() -> GameSessionStore.load(c.context)));
        storage.write("{invalid"); hash = NativeStorageChecks.hash(c.ranking());
        c.compare("invalid-ranking", "A corrupt ranking cannot be replaced by sample entries", "java.io.IOException",
                NativeStorageChecks.exception(() -> new HighScoreRepository(storage, new Random(8)).boards()));
        c.compare("invalid-ranking-kept", "Ranking diagnosis leaves the original corrupt bytes intact", hash, NativeStorageChecks.hash(c.ranking()));
        storage.write(reference.getString("ranking"));
        c.compare("profile-after-recovery", "Checkpoint and ranking recovery preserve the local profile", reference.getString("profile"), new PlayerPreferences(c.context).name());
    }
    static void corruptXml(NativeStorageChecks c) throws Exception {
        NativeStorageChecks.write(c.preferences(), "<map><string");
        c.compare("xml-corruption-prepared", "Truncated XML is prepared before a separate process reads it", "<map><string", NativeStorageChecks.text(c.preferences()));
    }
    static void verifyXml(Instrumentation runner, NativeStorageChecks c) throws Exception {
        String hash = NativeStorageChecks.hash(c.preferences());
        c.compare("invalid-xml", "Android's unreadable XML is not silently treated as an absent game", "java.lang.IllegalStateException",
                NativeStorageChecks.exception(() -> GameSessionStore.load(c.context)));
        for (var language : new HowToPlay.Language[]{HowToPlay.Language.EN, HowToPlay.Language.RO}) {
            var copy = new WhistUiText(language);
            var intent = new Intent(c.context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("debug_language", language.name());
            var activity = new java.util.concurrent.atomic.AtomicReference<android.app.Activity>();
            var event = runner.getUiAutomation().executeAndWaitForEvent(() -> activity.set(runner.startActivitySync(intent)),
                    observed -> observed.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                            && observed.getText().toString().contains(copy.savedGameReadError()), 15000);
            c.compare("read-error-"+language.name().toLowerCase(java.util.Locale.ROOT), "The application exposes the read error in "+language.name(),
                    copy.savedGameReadError(), event.getText().isEmpty()?"MISSING":event.getText().get(0).toString());
            runner.runOnMainSync(() -> activity.get().finish());
        }
        c.compare("invalid-xml-kept", "Opening and closing the application retains the corrupt checkpoint", hash, NativeStorageChecks.hash(c.preferences()));
        c.compare("profile-after-error", "Read errors preserve the user's profile", c.reference().getString("profile"), new PlayerPreferences(c.context).name());
    }
}
