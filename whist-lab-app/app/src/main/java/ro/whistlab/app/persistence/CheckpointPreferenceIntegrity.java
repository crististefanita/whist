package ro.whistlab.app.persistence;

import java.io.File;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Supplier;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Distinguishes an absent checkpoint from a preference document Android could not read. */
public final class CheckpointPreferenceIntegrity {
    private CheckpointPreferenceIntegrity() { }

    public static String read(Supplier<String> source, File document, String key) {
        final String encoded;
        try { encoded = source.get(); }
        catch (ClassCastException wrongType) { throw new IllegalStateException("Saved checkpoint has the wrong preference type", wrongType); }
        if (encoded != null) {
            if (encoded.isBlank()) throw new IllegalStateException("Saved checkpoint is empty");
            return encoded;
        }
        if (!document.exists()) return null;
        try {
            String xml = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(Files.readAllBytes(document.toPath()))).toString();
            // SharedPreferences writes UTF-8 without a DTD. Reject declarations before parsing,
            // avoiding optional SAX security features unsupported by Android's Expat factory.
            if (xml.contains("<!DOCTYPE") || xml.contains("<!ENTITY")) throw new SAXException("Checkpoint declarations are forbidden");
            var factory = SAXParserFactory.newInstance();
            var reader = factory.newSAXParser().getXMLReader();
            var handler = new PreferenceDocument(key);
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.setEntityResolver((publicId, systemId) -> { throw new SAXException("External checkpoint entity"); });
            reader.parse(new InputSource(new StringReader(xml)));
            if (!handler.root || handler.checkpoint) throw new SAXException("Checkpoint preference is unreadable");
            return null;
        } catch (Exception invalid) {
            throw new IllegalStateException("Could not read the saved checkpoint preference document", invalid);
        }
    }

    private static final class PreferenceDocument extends DefaultHandler {
        private final String key;
        private boolean root, checkpoint;
        private PreferenceDocument(String key) { this.key = key; }
        @Override public void startElement(String uri, String local, String name, Attributes attributes) throws SAXException {
            if (!root) {
                if (!name.equals("map")) throw new SAXException("Expected a preferences map");
                root = true;
            }
            if (key.equals(attributes.getValue("name"))) checkpoint = true;
        }
        @Override public void error(org.xml.sax.SAXParseException failure) throws SAXException { throw failure; }
        @Override public void fatalError(org.xml.sax.SAXParseException failure) throws SAXException { throw failure; }
    }
}
