package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ThreadHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpMethod;
import ws.palladian.retrieval.HttpRequest2Builder;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find whether a certain URL has been cached by Google.
 * </p>
 * 
 * @author David Urbansky
 */
public final class GoogleCachedPage extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCachedPage.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "Google Cache";

    /** The ranking value types of this service **/
    public static final RankingType GOOGLE_CACHED = new RankingType("googlecached", "Google Indexed",
            "Whether the page is in Google's Cache");

    /** All available ranking types by {@link GoogleCachedPage}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(GOOGLE_CACHED);

    /** Fields to check the service availability. */
    private long sleepTime = TimeUnit.SECONDS.toMillis(10);
    
    /** The time in milliseconds we wait between two requests. */
    private static final RequestThrottle THROTTLE = new FixedIntervalRequestThrottle(1000, TimeUnit.MILLISECONDS); 

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        THROTTLE.hold();
        int indexed = 0;
        String requestUrl = "http://webcache.googleusercontent.com/search?q=cache:" + url;

        try {

            boolean success = false;

            while (!success) {

                HttpResult httpHead = retriever.execute(new HttpRequest2Builder(HttpMethod.HEAD, requestUrl).create());

                success = true;

                if (httpHead.getStatusCode() < 400) {
                    indexed = 1;
                } else if (httpHead.getStatusCode() >= 500) {
                    LOGGER.error("too many frequent requests, we're blocked");
                    success = false;
                    ThreadHelper.deepSleep(TimeUnit.SECONDS.toMillis(sleepTime));
                    sleepTime += Math.random() * 10;
                }

                if (success) {
                    sleepTime = TimeUnit.SECONDS.toMillis(10);
                }

            }

        } catch (HttpException e) {
            throw new RankingServiceException(e);
        }

        builder.add(GOOGLE_CACHED, indexed);
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
        GoogleCachedPage gpl = new GoogleCachedPage();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com/p/best-funny-comic-strips");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(GoogleCachedPage.GOOGLE_CACHED) + " -> indexed");
    }

}
