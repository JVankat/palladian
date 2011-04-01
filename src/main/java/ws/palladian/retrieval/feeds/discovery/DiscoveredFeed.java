package ws.palladian.retrieval.feeds.discovery;

public class DiscoveredFeed {
    
    public static enum Type {
        RSS, ATOM
    }
    
    private Type feedType;
    private String feedLink;
    private String feedTitle;
    private String pageLink;
    public Type getFeedType() {
        return feedType;
    }
    public void setFeedType(Type feedType) {
        this.feedType = feedType;
    }
    public String getFeedLink() {
        return feedLink;
    }
    public void setFeedLink(String feedLink) {
        this.feedLink = feedLink;
    }
    public String getFeedTitle() {
        return feedTitle;
    }
    public void setFeedTitle(String feedTitle) {
        this.feedTitle = feedTitle;
    }
    public String getPageLink() {
        return pageLink;
    }
    public void setPageLink(String pageLink) {
        this.pageLink = pageLink;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DiscoveredFeed [feedType=");
        builder.append(feedType);
        builder.append(", feedLink=");
        builder.append(feedLink);
        builder.append(", feedTitle=");
        builder.append(feedTitle);
        builder.append(", pageLink=");
        builder.append(pageLink);
        builder.append("]");
        return builder.toString();
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feedLink == null) ? 0 : feedLink.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DiscoveredFeed other = (DiscoveredFeed) obj;
        if (feedLink == null) {
            if (other.feedLink != null)
                return false;
        } else if (!feedLink.equals(other.feedLink))
            return false;
        return true;
    }
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(feedLink).append(";");
        sb.append(feedType).append(";");
        sb.append(pageLink);
        return sb.toString();
    }
}
