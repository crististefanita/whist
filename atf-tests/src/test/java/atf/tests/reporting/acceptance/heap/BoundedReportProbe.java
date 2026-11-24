package atf.tests.reporting.acceptance.heap;

import atf.android.evidence.component.publication.ComponentReportPublication;
import atf.reporting.artifact.ReportArtifactProfile;
import atf.reporting.artifact.ReportArtifactInspector;
import atf.reporting.contract.*;
import atf.reporting.model.ReportObservation;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** Isolated physical-JAR qualification; synthetic comparisons are not additional JUnit invocations. */
public final class BoundedReportProbe {
    private BoundedReportProbe() { }
    public static void main(String[] arguments) throws Exception {
        Path directory=Path.of(arguments[0]);Files.createDirectories(directory);
        int population=Integer.parseInt(arguments[1]), characters=Integer.parseInt(arguments[2]);
        long maximum=Long.parseLong(arguments[3]);Path report=directory.resolve("bounded.html");
        var settings=new atf.reporting.configuration.ReportingSettings(240,Integer.parseInt(arguments[4]));
        String png=syntheticPng();
        var required=new ArrayList<ReportArtifactProfile.Evidence>();
        ComponentReportPublication.publish(report,maximum,staged->{
            try(var session=ReportHeapComponentTest.openFixtureRenderer(staged,settings)){
                var scenario=session.startScenario("HeapContract","controlled", "bounded-"+population,List.of("synthetic-contract"));
                    try(var phase=scenario.startPhase("TEST BODY")){
                        for(int offset=0;offset<population;offset+=64){
                            int end=Math.min(population,offset+64);var rows=new ArrayList<AtfAssertion>();
                            var comparisons=new ArrayList<ReportArtifactProfile.Comparison>();var raw=new StringBuilder();
                            for(int index=offset;index<end;index++){
                                boolean passed=index!=population-1;String name="Comparison "+index;
                                rows.add(new AtfAssertion(name,"expected-"+index,passed?"expected-"+index:"actual-final",passed));
                                comparisons.add(new ReportArtifactProfile.Comparison(name,passed));
                                raw.append("owner=").append(index).append("; value=").append("x".repeat(characters))
                                        .append("; final=").append(index).append(';').append('\n');
                            }
                            String title="Captured comparisons "+offset;
                            try(var step=phase.startAssertionStep(title)){step.assertions(rows);step.complete(end==population?AtfReportStatus.FAIL:AtfReportStatus.PASS);}
                            required.add(new ReportArtifactProfile.Assertions(title,comparisons));
                            var bodies=new ArrayList<ReportObservation.Body>();var labels=new ArrayList<String>();
                            for(String part:atf.android.evidence.text.EvidenceTextParts.split(raw.toString(),settings.bodyMaxDisplayCharacters())){
                                String label="Full values "+offset+" / "+bodies.size();labels.add(label);
                                bodies.add(new ReportObservation.Body(label,part,ReportObservation.Format.TEXT));
                            }
                            String bodyTitle="Original values "+offset;
                            phase.observation(new ReportObservation(bodyTitle,List.of(),bodies,List.of()));
                            required.add(new ReportArtifactProfile.Observation(bodyTitle,List.of(),labels));
                        }
                        phase.complete(AtfReportStatus.FAIL);
                    }
                try(var media=atf.reporting.ui.UiExtentReportSession.adaptScenario(scenario,
                        atf.reporting.evidence.profile.ReportEvidenceProfile.forScreenshotPolicy(
                                atf.reporting.evidence.ReportScreenshotPolicy.EACH_CHECKPOINT),"Synthetic PNG retention")){
                    try(var phase=media.startPhase("Synthetic media")){
                        for(int image=0;image<2;image++)phase.uiValidationAsMedia("Synthetic payload "+image,
                                "Controlled 3 by 2 pixel image, not an application capture",png,
                                List.of(new AtfAssertion("PNG signature "+image,"true",
                                        Boolean.toString(atf.reporting.model.ReportScreenshot.hasPngSignature(png)),
                                        atf.reporting.model.ReportScreenshot.hasPngSignature(png))),List.of(),AtfReportStatus.PASS);
                        phase.complete(AtfReportStatus.PASS);
                    }media.complete(AtfReportStatus.FAIL);
                }
            }
        },staged->{
            var profile=new ReportArtifactProfile(ReportArtifactProfile.Renderer.EXTENT,List.of(
                    new ReportArtifactProfile.Scenario("HeapContract","bounded-"+population,AtfReportStatus.FAIL,List.of(
                            new ReportArtifactProfile.Phase("TEST BODY",AtfReportStatus.FAIL,required),
                            new ReportArtifactProfile.Phase("Synthetic media",AtfReportStatus.PASS,mediaProfile())))));
            var result=ReportArtifactInspector.inspect(staged,profile);
            if(!result.violations().isEmpty())throw new java.io.IOException("PROFILE_PARITY: "+result.violations());
            if(!contains(staged,png))throw new java.io.IOException("MANDATORY_INLINE_PNG_MISSING");
            CapturedTextProbe.verify(staged,population,characters);
        });
        var json=new ObjectMapper();var result=json.readTree(report.resolveSibling("bounded.html.publication.json").toFile());
        var output=json.createObjectNode();output.put("status","PASS");output.put("population",population);
        output.put("comparisons",population);output.put("capturedFailures",1);output.put("valueCharacters",characters);
        output.put("profileParity",true);output.set("publication",result);
        output.put("mandatoryPngRetained",true);
        output.put("mediaComparisons",2);
        output.put("completeValuesVerified",population);
        System.out.println(json.writeValueAsString(output));
    }
    private static List<ReportArtifactProfile.Evidence> mediaProfile() {
        var required=new ArrayList<ReportArtifactProfile.Evidence>();
        for(int image=0;image<2;image++)required.add(new ReportArtifactProfile.HttpExchange(
                AtfGherkinKeyword.THEN.text()+" Synthetic payload "+image,false,false,List.of(),
                List.of(new ReportArtifactProfile.Assertions("UI validation",
                        List.of(new ReportArtifactProfile.Comparison("PNG signature "+image,true)))),AtfReportStatus.PASS));
        return List.copyOf(required);
    }
    private static String syntheticPng() throws Exception {
        var image=new java.awt.image.BufferedImage(3,2,java.awt.image.BufferedImage.TYPE_INT_RGB);
        image.setRGB(0,0,0xff0000);image.setRGB(2,1,0x00ff00);
        var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }
    private static boolean contains(Path file,String needle) throws java.io.IOException {
        try(var reader=Files.newBufferedReader(file)){
            char[] chunk=new char[8192];String tail="";int count;
            while((count=reader.read(chunk))!=-1){String window=tail+new String(chunk,0,count);
                if(window.contains(needle))return true;tail=window.substring(Math.max(0,window.length()-needle.length()+1));}
            return false;
        }
    }
}
