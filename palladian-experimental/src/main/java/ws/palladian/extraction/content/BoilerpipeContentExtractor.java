package ws.palladian.extraction.content;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.ExtractorBase;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ws.palladian.helper.html.HtmlHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;

/**
 * <p>
 * Boilerpipe, as described in "Boilerplate Detection using Shallow Text Features"; Kohlschütter, Christian; Fankhauser,
 * Peter; Nejdl, Wolfgang; 2010.
 * </p>
 *
 * @author Ruchit Beri
 * @author Philipp Katz
 * @see http://code.google.com/p/boilerpipe/ and
 * @see http://www.l3s.de/~kohlschuetter/boilerplate/
 */
public class BoilerpipeContentExtractor extends WebPageContentExtractor {
    private final ExtractorBase extractor;
    private TextDocument textDocument;

    public BoilerpipeContentExtractor() {
        this(ArticleExtractor.INSTANCE);
    }

    public BoilerpipeContentExtractor(ExtractorBase extractor) {
        Validate.notNull(extractor, "extractor must not be null");
        this.extractor = extractor;
    }

    @Override
    public WebPageContentExtractor setDocument(File file, boolean parse) throws PageContentExtractorException {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            setDocument(inputSource);
        } catch (FileNotFoundException e) {
            throw new PageContentExtractorException(e);
        }
        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        return setDocument(document, true);
    }

    @Override
    public WebPageContentExtractor setDocument(Document document, boolean parse) throws PageContentExtractorException {
        StringReader stringReader = new StringReader(HtmlHelper.xmlToString(document, false));
        InputSource inputSource = new InputSource(stringReader);
        setDocument(inputSource);
        return this;
    }

    // TODO pull up?
    public BoilerpipeContentExtractor setDocument(InputSource inputSource) throws PageContentExtractorException {
        try {
            BoilerpipeSAXInput boilerpipeInput = new BoilerpipeSAXInput(inputSource);
            textDocument = boilerpipeInput.getTextDocument();
            extractor.process(textDocument);
        } catch (SAXException e) {
            throw new PageContentExtractorException(e);
        } catch (BoilerpipeProcessingException e) {
            throw new PageContentExtractorException(e);
        }
        return this;
    }

    @Override
    public Node getResultNode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getResultText() {
        return textDocument.getContent();

    }

    @Override
    public String getResultTitle() {
        return textDocument.getTitle();
    }

    @Override
    public String getExtractorName() {
        return "BoilerpipeContentExtractor(" + extractor.getClass().getSimpleName() + ")";
    }

}
