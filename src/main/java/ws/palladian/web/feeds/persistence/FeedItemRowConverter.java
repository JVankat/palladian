package ws.palladian.web.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.web.feeds.FeedItem;

public class FeedItemRowConverter implements RowConverter<FeedItem> {

    @Override
    public FeedItem convert(ResultSet resultSet) throws SQLException {
        
        FeedItem entry = new FeedItem();

        entry.setId(resultSet.getInt("id"));
        entry.setFeedId(resultSet.getInt("feedId"));
        entry.setTitle(resultSet.getString("title"));
        entry.setLink(resultSet.getString("link"));
        entry.setRawId(resultSet.getString("rawId"));
        entry.setPublished(resultSet.getTimestamp("published"));
        entry.setItemDescription(resultSet.getString("description"));
        entry.setItemText(resultSet.getString("text"));
        entry.setPageText(resultSet.getString("pageText"));
        entry.setAdded(resultSet.getTimestamp("added"));
        entry.setAuthors(resultSet.getString("authors"));

        return entry;
        
    }

}
