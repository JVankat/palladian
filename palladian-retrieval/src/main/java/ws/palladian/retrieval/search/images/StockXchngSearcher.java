package ws.palladian.retrieval.search.images;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.resources.BasicWebImage;
import ws.palladian.retrieval.resources.WebImage;
import ws.palladian.retrieval.search.AbstractSearcher;
import ws.palladian.retrieval.search.License;
import ws.palladian.retrieval.search.SearcherException;

/**
 * <p>
 * Search for royality free images on <a href="http://www.sxc.hu">Stock xchng</a>.
 * </p>
 * 
 * @author David Urbansky
 */
public class StockXchngSearcher extends AbstractSearcher<WebImage> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(StockXchngSearcher.class);

    @Override
    public List<WebImage> search(String query, int resultCount, Language language) throws SearcherException {
        List<WebImage> results = new ArrayList<>();

        resultCount = Math.min(1000, resultCount);

        DocumentRetriever documentRetriever = new DocumentRetriever();

        ol: for (int page = 1; page <= 100; page++) {

            String requestUrl = buildRequest(query, page);

            Document webDocument = documentRetriever.getWebDocument(requestUrl);
            List<Node> targetNodes = XPathHelper.getXhtmlNodes(webDocument,
                    "//div[@id='thumbs']/div[@class='thumb_row']/div[@class='thumb']");

            if (targetNodes.isEmpty()) {
                break;
            }

            for (Node node : targetNodes) {

                BasicWebImage.Builder builder = new BasicWebImage.Builder();
                Node dimensionNode = XPathHelper.getXhtmlNode(node, ".//li[@class='t_size']");
                if (dimensionNode != null) {
                    String[] split = dimensionNode.getTextContent().split("\\*");
                    builder.setWidth(Integer.parseInt(split[0]));
                    builder.setHeight(Integer.parseInt(split[1]));
                }
                Node titleNode = XPathHelper.getXhtmlNode(node, ".//li[@class='t_title0']");
                if (titleNode != null) {
                    builder.setTitle(titleNode.getTextContent());
                }

                Node imageNode = XPathHelper.getXhtmlNode(node, ".//img/@src");
                if (imageNode != null) {
                    builder.setThumbnailUrl("http://www.sxc.hu/" + imageNode.getTextContent());
                }

                String imageUrl = XPathHelper.getXhtmlNode(node, ".//a/@href").getTextContent();
                builder.setUrl("http://www.sxc.hu/" + imageUrl);
                imageUrl = imageUrl.replace("photo/", "");
                imageUrl = "http://www.sxc.hu/browse.phtml?f=download&id=" + imageUrl;
                builder.setImageUrl(imageUrl);

                builder.setLicense(License.ATTRIBUTION);
                builder.setLicenseLink("http://www.sxc.hu/help/7_2");
                builder.setImageType(ImageType.PHOTO);

                results.add(builder.create());

                if (results.size() >= resultCount) {
                    break ol;
                }
            }

        }

        return results;
    }

    private String buildRequest(String searchTerms, int page) {
        String[] terms = searchTerms.split("\\s");
        String q1 = "";
        String q2 = "";
        String q3 = "";
        q1 = UrlHelper.encodeParameter(terms[0]);
        if (terms.length > 1) {
            q2 = UrlHelper.encodeParameter(terms[1]);
        }
        if (terms.length > 2) {
            q3 = UrlHelper.encodeParameter(terms[2]);
        }
        String url = "http://www.sxc.hu/browse.phtml?f=advanced_search&q1=" + q1 + "&q2=" + q2 + "&q3=" + q3
                + "&cat=0&r=0&t=0&p=" + page;

        LOGGER.debug(url);

        return url;
    }

    @Override
    public String getName() {
        return "Stock Xchng";
    }

    /**
     * @param args
     * @throws SearcherException
     */
    public static void main(String[] args) throws SearcherException {
        StockXchngSearcher searcher = new StockXchngSearcher();
        List<WebImage> results = searcher.search("planet earth", 10);
        CollectionHelper.print(results);
        System.out.println(results.get(0).getThumbnailUrl());
    }
}
