package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.helper.SqlHelper;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;

public class FeedPollRowConverter implements RowConverter<PollMetaInformation> {

    @Override
    public PollMetaInformation convert(ResultSet resultSet) throws SQLException {

        PollMetaInformation pollMetaInfo = new PollMetaInformation();
        pollMetaInfo.setFeedID(resultSet.getInt("id"));
        pollMetaInfo.setPollTimestamp(resultSet.getTimestamp("pollTimestamp"));
        pollMetaInfo.setHttpETag(resultSet.getString("httpETag"));
        pollMetaInfo.setHttpDate(resultSet.getTimestamp("httpDate"));
        pollMetaInfo.setHttpLastModified(resultSet.getTimestamp("httpLastModified"));
        pollMetaInfo.setHttpExpires(resultSet.getTimestamp("httpExpires"));
        pollMetaInfo.setNewestItemTimestamp(resultSet.getTimestamp("newestItemTimestamp"));
        pollMetaInfo.setNumberNewItems(SqlHelper.getInteger(resultSet, "numberNewItems"));
        pollMetaInfo.setWindowSize(SqlHelper.getInteger(resultSet, "windowSize"));
        pollMetaInfo.setHttpStatusCode(SqlHelper.getInteger(resultSet, "httpStatusCode"));
        return pollMetaInfo;
    }

}
