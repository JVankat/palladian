package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of backlinks to the domain using the OpenLinkProfiler index.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class OpenLinkProfiler extends AbstractRankingService implements RankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "openlinkprofiler";

    /** The ranking value types of this service **/
    public static final RankingType BACKLINKS_DOMAIN = new RankingType("openlinkprofilertotal", "Backlinks Total",
            "The Total Number of Backlinks to the Domain");
    public static final RankingType BACKLINKS_DOMAIN_UNIQUE = new RankingType("openlinkprofilerunique",
            "Unique Backlinks", "The Number of Unique Backlinks to the Domain");

    /** All available ranking types by {@link OpenLinkProfiler}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(BACKLINKS_DOMAIN, BACKLINKS_DOMAIN_UNIQUE);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        String requestUrl = "http://www.openlinkprofiler.org/r/" + UrlHelper.getDomain(url, false);
        try {
            DocumentRetriever documentRetriever = new DocumentRetriever(retriever);
            Document document = documentRetriever.getWebDocument(requestUrl);

            Node node1 = XPathHelper.getXhtmlNode(document,
                    "//div/div[contains(@class,'topinfobox') and contains(@class,'help')]/p");
            long backlinksDomain = Long.parseLong(node1.getTextContent().replaceAll("[,+]", ""));
            long backlinksDomainUnique = Long.parseLong(XPathHelper
                    .getXhtmlNode(document, "//div/div[contains(@class,'topinfobox') and contains(@class,'2')][1]/p")
                    .getTextContent().replaceAll("[,+]", ""));

            builder.add(BACKLINKS_DOMAIN, backlinksDomain);
            builder.add(BACKLINKS_DOMAIN_UNIQUE, backlinksDomainUnique);

        } catch (Exception e) {
            throw new RankingServiceException("Error while parsing the response (\"" + url + "\")", e);
        }
        return builder.create();
    }


    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        OpenLinkProfiler gpl = new OpenLinkProfiler();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com/");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(OpenLinkProfiler.BACKLINKS_DOMAIN) + " backlinks to the domain");
        System.out
        .println(ranking.getValues().get(OpenLinkProfiler.BACKLINKS_DOMAIN_UNIQUE) + " backlinks to the page");
    }

}
