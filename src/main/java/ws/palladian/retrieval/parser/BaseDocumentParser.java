
package ws.palladian.retrieval.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Base implementation for document parsers.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseDocumentParser implements DocumentParser {

    @Override
    public Document parse(InputStream inputStream) throws ParserException {
        return parse(new InputSource(inputStream));
    }

    @Override
    public Document parse(HttpResult httpResult) throws ParserException {
        byte[] content = httpResult.getContent();
        if (content.length == 0) {
            throw new ParserException("HttpResult has no content");
        }
        Document document = parse(new ByteArrayInputStream(content));
        document.setDocumentURI(httpResult.getUrl());
        return document;
    }
    
    @Override
    public Document parse(File file) throws ParserException {
        InputSource inputSource;
        try {
            inputSource = new InputSource(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new ParserException("File " + file + " not found", e);
        }
        Document document = parse(inputSource);
        String documentUri = file.toURI().toString();
        document.setDocumentURI(documentUri);
        return document;
    }

}
