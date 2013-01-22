package ws.palladian.extraction.content;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.helper.JsonObjectWrapper;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;

/**
 * <p>
 * The ViewTextContentExtractor extracts clean sentences from (English) texts.
 * </p>
 * 
 * @author David Urbansky
 * @see http://viewtext.org
 */
public class ViewTextContentExtractor extends WebPageContentExtractor {

    /** For performing HTTP requests. */
    private final HttpRetriever httpRetriever;

    private Node resultNode = null;
    private String extractedTitle = "";
    private String extractedResult = "";

    public ViewTextContentExtractor() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public WebPageContentExtractor setDocument(String documentLocation) throws PageContentExtractorException {

        String requestUrl = buildRequestUrl(documentLocation);

        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(requestUrl);
        } catch (HttpException e) {
            throw new PageContentExtractorException("Error when contacting API for URL \"" + documentLocation + "\": "
                    + e.getMessage(), e);
        }

        extractedResult = HttpHelper.getStringContent(httpResult);

        JsonObjectWrapper json = new JsonObjectWrapper(extractedResult);
        extractedResult = json.getString("content");

        NekoHtmlParser parser = new NekoHtmlParser();
        try {
            resultNode = parser.parse(new StringInputStream(extractedResult));
            extractedResult = HtmlHelper.documentToReadableText(resultNode);
        } catch (ParserException e) {
            e.printStackTrace();
        }

        return this;
    }

    @Override
    public WebPageContentExtractor setDocument(Document document) throws PageContentExtractorException {
        String docUrl = document.getDocumentURI();
        return setDocument(docUrl);
    }

    private String buildRequestUrl(String docUrl) {
        String requestUrl = String.format(
                "http://viewtext.org/api/text?url=%s&format=json",
                UrlHelper.encodeParameter(docUrl));

        return requestUrl;
    }

    @Override
    public Node getResultNode() {
        return resultNode;
    }

    @Override
    public String getResultText() {
        return extractedResult;
    }

    @Override
    public String getResultTitle() {
        return extractedTitle;
    }

    @Override
    public String getExtractorName() {
        return "ViewText Content Extractor";
    }

    public static void main(String[] args) {
        ViewTextContentExtractor ce = new ViewTextContentExtractor();
        String resultTitle = ce.getResultTitle();
        String resultText = ce.getResultText("http://www.bbc.co.uk/news/world-asia-17116595");

        System.out.println("title: " + resultTitle);
        System.out.println("text: " + resultText);
    }

}