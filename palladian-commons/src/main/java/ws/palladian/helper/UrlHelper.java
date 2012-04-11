package ws.palladian.helper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;

/**
 * <p>
 * Various helper methods for working with URLs.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Sandro Reichert
 * @author Julien Schmehl
 */
public class UrlHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(UrlHelper.class);

    /** Names of attributes in (X)HTML containing links. */
    private static final List<String> LINK_ATTRIBUTES = Arrays.asList("href", "src");

    /** RegEx pattern defining a session ID. */
    private static final Pattern SESSION_ID_PATTERN = Pattern
            .compile("(?<!\\w)(jsessionid=|s=|sid=|PHPSESSID=|sessionid=)[a-f0-9]{32}(?!\\w)");
    
    /** List of top level domains. */
    private static final String TOP_LEVEL_DOMAINS = "ac|ad|ae|aero|af|ag|ai|al|am|an|ao|aq|ar|as|asia|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|biz|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cat|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|com|coop|cr|cs|cu|cv|cx|cy|cz|dd|de|dj|dk|dm|do|dz|ec|edu|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gov|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|info|int|io|iq|ir|is|it|je|jm|jo|jobs|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mil|mk|ml|mm|mn|mo|mobi|mp|mq|mr|ms|mt|mu|museum|mv|mw|mx|my|mz|na|name|nc|ne|net|nf|ng|ni|nl|no|np|nr|nu|nz|om|org|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pro|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|ss|st|su|sv|sy|sz|tc|td|tel|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|travel|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|xxx|ye|yt|yu|za|zm|zw";

    // adapted version from <http://daringfireball.net/2010/07/improved_regex_for_matching_urls>
    // this is able to match URLs, containing (brackets), but does not include trailing brackets 
    public static final Pattern URL_PATTERN = Pattern
            .compile(
                    "\\b(?:https?://)?[\\w.-]{1,63}?\\.(?:"
                            + TOP_LEVEL_DOMAINS
                            + ")(?:[?/](?:\\([^\\s()<>\\[\\]]{0,255}\\)|[^\\s()<>\\[\\]]{0,255})+(?:\\([^\\s()<>\\[\\]]{0,255}\\)|[^\\s.,;!?:()<>\\[\\]])|/|\\b)",
                    Pattern.CASE_INSENSITIVE);

    private UrlHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Tries to remove a session ID from URL if it can be found.
     * </p>
     * 
     * @param original The URL to remove the sessionID from.
     * @return The URL without the sessionID if it could be found or the original URL else wise. <code>null</code> if
     *         original was <code>null</code>.
     */
    public static URL removeSessionId(URL original) {
        URL replacedURL = original;
        if (original != null) {
            String origURL = original.toString();
            Matcher matcher = SESSION_ID_PATTERN.matcher(origURL);
            String sessionID = null;
            String newURL = null;
            while (matcher.find()) {
                sessionID = matcher.group();
                LOGGER.debug("   sessionID : " + sessionID);
                newURL = origURL.replaceAll(SESSION_ID_PATTERN.toString(), "");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Original URL: " + origURL);
                LOGGER.debug("Cleaned URL : " + newURL);
            }
            if (newURL != null) {
                try {
                    replacedURL = new URL(newURL);
                } catch (MalformedURLException e) {
                    LOGGER.error("Could not replace sessionID in URL \"" + origURL + "\", returning original value.");
                }
            }
        }
        return replacedURL;
    }

    /**
     * <p>
     * Convenience method to remove a session ID from a URL string if it can be found.
     * </p>
     * 
     * @param originalUrl The URL to remove the sessionID from.
     * @param silent If <code>true</code>, do not log errors.
     * @return The string representation of the url without the sessionID if it could be found or the original string
     *         else wise. <code>null</code> if original was <code>null</code>.
     */
    public static String removeSessionId(String originalUrl, boolean silent) {
        String replacedURL = originalUrl;
        if (originalUrl != null) {
            try {
                replacedURL = removeSessionId(new URL(originalUrl)).toString();
            } catch (MalformedURLException e) {
                if (!silent) {
                    LOGGER.error("Could not create URL from \"" + originalUrl + "\". " + e.getLocalizedMessage());
                }
            }
        }
        return replacedURL;
    }

    /**
     * <p>
     * Transforms all relative URLs (i.e. in attributes <tt>href</tt> and <tt>src</tt>) of an (X)HTML {@link Document}
     * to full, absolute URLs. The document's URL should be specified using {@link Document#setDocumentURI(String)}.
     * </p>
     * 
     * @param document The document for which to create absolute URLs, this will be modified in-place
     */
    public static void makeAbsoluteUrls(Document document) {

        String documentUrl = document.getDocumentURI();
        String baseUrl = getBaseUrl(document);

        for (String attribute : LINK_ATTRIBUTES) {
            String xpath = "//*[@" + attribute + "]";
            List<Node> nodes = XPathHelper.getXhtmlChildNodes(document, xpath);
            for (Node node : nodes) {
                Node attributeNode = node.getAttributes().getNamedItem(attribute);
                String value = attributeNode.getNodeValue();
                String fullValue = makeFullUrl(documentUrl, baseUrl, value);
                if (!fullValue.equals(value)) {
                    LOGGER.debug(value + " -> " + fullValue);
                    attributeNode.setNodeValue(fullValue);
                }
            }
        }
    }

    /**
     * <p>
     * Get the Base URL of the supplied (X)HTML document, which is specified via the <tt>base</tt> tag in the document's
     * header.
     * </p>
     * 
     * @param document
     * @return The base URL, if present, <code>null</code> otherwise.
     */
    public static String getBaseUrl(Document document) {
        String baseHref = null;
        Node baseNode = XPathHelper.getXhtmlNode(document, "//head/base/@href");
        if (baseNode != null) {
            baseHref = baseNode.getTextContent();
        }
        return baseHref;
    }

    /**
     * <p>
     * Try to create a full/absolute URL based on the specified parameters. Handling links in HTML documents can be
     * tricky. If no absolute URL is specified in the link itself, there are two factors for which we have to take care:
     * </p>
     * <ol>
     * <li>The document's URL</li>
     * <li>If provided, a base URL inside the document, which can be as well be absolute or relative to the document's
     * URL</li>
     * </ol>
     * 
     * @see <a href="http://www.mediaevent.de/xhtml/base.html">HTML base • Basis-Adresse einer Webseite</a>
     * 
     * @param pageUrl actual URL of the document, can be <code>null</code>.
     * @param baseUrl base URL defined in document's header, can be <code>null</code> if no base URL is specified.
     * @param linkUrl link URL from the document to be made absolute, not <code>null</code>.
     * @return the absolute URL, empty String, if URL cannot be created, never <code>null</code>.
     * 
     * @author Philipp Katz
     */
    public static String makeFullUrl(String pageUrl, String baseUrl, String linkUrl) {
        if (linkUrl == null) {
            throw new NullPointerException("linkUrl must not be null");
        }
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl.concat("/");
        }
        String contextUrl;
        if (pageUrl != null && baseUrl != null) {
            contextUrl = makeFullUrl(pageUrl, baseUrl);
        } else if (pageUrl != null) {
            contextUrl = pageUrl;
        } else {
            contextUrl = baseUrl;
        }
        return makeFullUrl(contextUrl, linkUrl);
    }

    public static String makeFullUrl(String contextUrl, String linkUrl) {
        String result = linkUrl;
        if (contextUrl != null) {
            try {
                result = new URL(new URL(contextUrl), linkUrl).toString();
            } catch (MalformedURLException e) {
                // don't care
            }
        }
        return result;
        // return makeFullUrl(pageUrl, null, linkUrl);
    }

    public static String getCleanUrl(String url) {
        if (url == null) {
            url = "";
        }
        if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        if (url.startsWith("www.")) {
            url = url.substring(4);
        }
        // if (url.endsWith("/")) url = url.substring(0,url.length()-1);
        return url;
    }

    public static String removeAnchors(String url) {
        return url.replaceAll("#.*", "");
    }

    /**
     * <p>
     * Check if a URL is in a valid form.
     * </p>
     * 
     * @param url the URL
     * @return true, if is a valid URL
     * @author Martin Werner
     */
    // TODO too specific, move to WebKnox
    public static boolean isValidUrl(String url) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_2_SLASHES);
        return urlValidator.isValid(url);
    }

    /**
     * <p>
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>.
     * </p>
     * 
     * @param url
     * @param includeProtocol include protocol prefix, e.g. "http://"
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url, boolean includeProtocol) {
        String result = "";
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            if (!host.isEmpty()) {
                if (includeProtocol) {
                    result = urlObj.getProtocol() + "://";
                }
                result += urlObj.getHost();
                LOGGER.trace("root url for " + url + " -> " + result);
            } else {
                LOGGER.trace("no domain specified " + url);
            }
        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine domain " + url);
        }
        return result;
    }

    /**
     * <p>
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>.
     * </p>
     * 
     * @param url
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url) {
        return getDomain(url, true);
    }

    /**
     * <p>
     * Check URL for validness and eventually modify e.g. relative path.
     * </p>
     * 
     * @param urlCandidate the URLCandidate
     * @param pageUrl the pageURL
     * @return the verified URL
     */
    // TODO too specific, move to WebKnox
    public static String verifyUrl(final String urlCandidate, final String pageUrl) {

        String returnValue = "";

        final String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (isValidUrl(modUrlCandidate)) {
                returnValue = modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                final String modifiedURL = makeFullUrl(pageUrl, modUrlCandidate);
                if (isValidUrl(modifiedURL)) {
                    returnValue = modifiedURL;
                }
            }

        }
        return returnValue;
    }

    /**
     * <p>
     * Returns the canonical URL. This URL is lowercase, with trailing slash and no index.htm* and is the redirected URL
     * if input URL is redirecting.
     * </p>
     * 
     * @param url
     * @return canonical URL, or empty String if URL cannot be determined, never <code>null</code>
     * 
     */
    public static String getCanonicalUrl(String url) {

        if (url == null) {
            return "";
        }

        try {

            // get redirect url if it exists and continue with this url FIXME resolve redirects somewhere else
            // HttpRetriever retriever = new HttpRetriever();
            // String redirectUrl = retriever.getRedirectUrl(url);
            // if (isValidUrl(redirectUrl))
            // url = redirectUrl;

            URL urlObj = new URL(url);

            // get all url parts
            String protocol = urlObj.getProtocol();
            String port = "";
            if (urlObj.getPort() != -1 && urlObj.getPort() != urlObj.getDefaultPort())
                port = ":" + urlObj.getPort();
            String host = urlObj.getHost().toLowerCase();
            String path = urlObj.getPath();
            String query = "";
            if (urlObj.getQuery() != null)
                query = "?" + urlObj.getQuery();

            // correct path to eliminate ".." and recreate path accordingly
            String[] parts = path.split("/");
            path = "/";

            if (parts.length > 0) {
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                    // throw away ".." and a directory above it
                    if (parts[i].equals("..")) {
                        parts[i] = "";
                        // if there is a directory above this one in the path
                        if (parts.length > 1 && i > 0) {
                            parts[i - 1] = "";
                        }
                    }
                }
                for (int i = 0; i < parts.length; i++)
                    if (parts[i].length() > 0)
                        path += parts[i] + "/";

                // delete trailing slash if path ends with a file
                if (parts[parts.length - 1].contains("."))
                    path = path.substring(0, path.length() - 1);
                // delete index.* if there is no query
                if (parts[parts.length - 1].contains("index") && query.isEmpty())
                    if (query.isEmpty())
                        path = path.replaceAll("index\\..+$", "");

            }

            return protocol + "://" + port + host + path + query;

        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine canonical url for" + url);
            return "";
        }

    }

    /**
     * <p>
     * URLDecode a String.
     * </p>
     * 
     * @param string
     * @return
     */
    public static String urlDecode(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage());
            return string;
        }
    }

    /**
     * <p>
     * URLEncode a String.
     * </p>
     * 
     * @param string
     * @return
     */
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // this will not happen, as we always use UTF-8 as encoding
            throw new IllegalStateException("houston, we have a problem");
        }
    }

    /**
     * <p>
     * Extracts all recognizable URLs from a given text.
     * </p>
     * 
     * @param text
     * @return List of extracted URLs, or empty List if no URLs were found, never <code>null</code>.
     */
    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<String>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(0));
        }
        return urls;
    }

    public static boolean isLocalFile(URL url) {
        String protocol = url.getProtocol();
        String host = url.getHost();

        boolean hasHost = host != null && !"".equals(host);

        return "file".equalsIgnoreCase(protocol) && !hasHost;
    }
}
