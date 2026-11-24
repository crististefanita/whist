package atf.tests.reporting.acceptance.heap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/** Streams original PRE text; part boundaries cannot hide a dropped suffix or owner. */
final class CapturedTextProbe extends HTMLEditorKit.ParserCallback {
    private final int population, characters;
    private int pre, matched;
    private String tail="";
    private CapturedTextProbe(int population,int characters){this.population=population;this.characters=characters;}
    static void verify(Path report,int population,int characters) throws IOException {
        var probe=new CapturedTextProbe(population,characters);
        try(var reader=Files.newBufferedReader(report)){new ParserDelegator().parse(reader,probe,true);}
        if(probe.matched!=population)throw new IOException("FULL_VALUE_PARITY: "+probe.matched+" / "+population);
    }
    @Override public void handleStartTag(HTML.Tag tag,MutableAttributeSet attributes,int position){if(tag==HTML.Tag.PRE)pre++;}
    @Override public void handleEndTag(HTML.Tag tag,int position){if(tag==HTML.Tag.PRE)pre--;}
    @Override public void handleText(char[] text,int position){
        if(pre<1||matched==population)return;
        String window=tail+new String(text);int start=0;
        while(matched<population){
            String expected="owner="+matched+"; value="+"x".repeat(characters)+"; final="+matched+";";
            int found=window.indexOf(expected,start);if(found<0)break;
            start=found+expected.length();matched++;
        }
        int retained=characters+100;tail=window.substring(Math.max(start,window.length()-retained));
    }
}
