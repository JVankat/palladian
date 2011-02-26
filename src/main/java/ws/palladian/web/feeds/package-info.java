/**
 * <p>
 * Classes from this package provide functionality to work with Atom and RSS web feeds. The central class is the
 * {@code Feed} class representing a single feed with several {@code FeedItem} objects as children. To get the content
 * of a feed into a {@code Feed} object use the {@code FeedDownloader}. The {@code FeedDownloader} is like a factory for
 * feeds and tries to fetch a feeds content directly over the internet using Palladians {@link ws.palladian.web.Crawler}
 * and the ROME framework. To save the retrieved contents use an instance of
 * {@link ws.palladian.web.feeds.persistence.FeedDatabase}.
 * </p>
 * 
 * <p>
 * Searching for feeds can be achieved using the class {@link ws.palladian.web.feeds.FeedDiscovery}, which can employ
 * arbitrary search engines to find feeds for specified search terms.
 * </p>
 * 
 * <p>
 * A quick overview demonstrating the main use cases of this package in code can be found in
 * {@link ws.palladian.web.feeds.FeedsExamples}.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 */
package ws.palladian.web.feeds;