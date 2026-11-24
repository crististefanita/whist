package ro.cardtable.ui.score;

import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import static atf.gradle.audit.assertions.GradleAssertions.*;

public final class ScoreTypographyComponentTest extends atf.gradle.audit.GradleAuditedTest {
    @Test public void nativeScoreRolesDefineTheWebScaleExactlyOnce() {
        var sizes = List.of(25, 12, 13, 16, 18, 14, 16, 14, 11, 13, 12, 20, 16, 16, 14);
        atf.gradle.audit.GradleInputs.fixture("Score typography adapters", "native logical sp", sizes,
                "Web root pixels", List.of(16, 24, 32), "desktop scale", List.of(.75, 1.0, 1.25));
        var names = new HashSet<String>();
        int index = 0;
        for (var role : ScoreTypography.values()) {
            assertEquals("Existing native text role " + role, sizes.get(index++).intValue(), role.sp());
            assertTrue("Each role has a distinct CSS token " + role, names.add(role.cssName()));
            double rem = Double.parseDouble(role.rem().replace("rem", ""));
            for (int root : List.of(16, 24, 32)) {
                double nativeLogical = role.sp() * root / 16.0;
                assertEquals("Font enlargement once for " + role + " at " + root, nativeLogical, rem * root, .00001);
            }
        }
        assertEquals("All score roles are covered", sizes.size(), index);
    }

    @Test public void menuAndResultCommandsShareOneRoleBeforeAccessibilityScaling() {
        atf.gradle.audit.GradleInputs.fixture("Semantic action typography", "command/supporting logical units", List.of(16, 14),
                "accessible roots", List.of(16, 20, 24, 32));
        int command = ro.cardtable.ui.menu.MenuTypography.ACTION.logicalPixels();
        int supporting = ro.cardtable.ui.menu.MenuTypography.HELPER.logicalPixels();
        assertEquals("Menu commands use the shared role", 16, command);
        assertEquals("Supporting labels use the smaller shared role", 14, supporting);
        for (int root : List.of(16, 20, 24, 32)) {
            double scale = root / 16.0;
            assertEquals("Filled score commands match menu commands", command * scale,
                    ScoreTypography.ACTION_PRIMARY.sp() * scale, .00001);
            assertEquals("Quiet command styling does not reduce its role", command * scale,
                    Double.parseDouble(ScoreTypography.ACTION_SECONDARY.rem().replace("rem", "")) * root, .00001);
            assertEquals("Scoring details match supporting home labels", supporting * scale,
                    Double.parseDouble(ScoreTypography.ACTION_EXPLANATION.rem().replace("rem", "")) * root, .00001);
        }
    }

    @Test public void denseScoreCellsAndGenericDialogsKeepLegacyRoles() {
        atf.gradle.audit.GradleInputs.fixture("Independent score roles", "column/value/body/dialog/quiet",
                List.of(11, 13, 16, 20, 14));
        assertEquals("Compact column headings do not inherit action enlargement", 11, ScoreTypography.COLUMN.sp());
        assertEquals("Numeric cells retain their own role", 13, ScoreTypography.VALUE.sp());
        assertEquals("Generic body text is unchanged", 16, ScoreTypography.BODY.sp());
        assertEquals("Dialog headings are unchanged", 20, ScoreTypography.DIALOG_TITLE.sp());
        assertEquals("Existing quiet dialogs stay independent", 14, ScoreTypography.QUIET.sp());
    }
}
