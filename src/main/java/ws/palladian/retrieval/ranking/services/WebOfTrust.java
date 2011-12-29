package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Get ranking from Web of Trust. We just take the "Trustworthiness" factor, not considering "Vendor reliability",
 * "Privacy", or "Child safety". Also we do not cosider the confidence values.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://www.mywot.com/en/api
 * @see http://www.mywot.com/wiki/API
 */
public class WebOfTrust extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(WebOfTrust.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "web_of_trust";

    /** The ranking value types of this service. */
    public static final RankingType TRUSTWORTHINESS = new RankingType("wot_trustworthiness",
            "Web of Trust Trustworthiness", "");

    private static final List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(TRUSTWORTHINESS);
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        String domain = UrlHelper.getDomain(url, false);
        try {
            HttpResult httpResult = retriever.httpGet("http://api.mywot.com/0.4/public_query2?target=" + domain);
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document doc = xmlParser.parse(httpResult);

            Node trustworthiness = XPathHelper.getNode(doc, "//application[@name='0']/@r");
            if (trustworthiness != null) {
                Float trustValue = Float.valueOf(trustworthiness.getTextContent());
                LOGGER.trace("WOT Trustworthiness for " + url + " -> " + trustValue);
                results.put(TRUSTWORTHINESS, trustValue);
            }
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        } catch (ParserException e) {
            LOGGER.error("ParserException " + e.getMessage());
        }

        return ranking;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

}
