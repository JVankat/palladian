package ws.palladian.retrieval;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * <p>Represents a response for an HttpDownload, e.g. GET or HEAD.</p>
 * 
 * @author Philipp Katz
 * 
 */
public class HttpResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String HEADER_SEPARATOR = "; ";
    
    private final String url;
    private final byte[] content;
    private final Map<String, List<String>> headers;
    private final int statusCode;
    private final long transferedBytes;

    /**
     * <p>Instantiate a new {@link HttpResult}.</p>
     * 
     * @param url the result's URL.
     * @param content the content as byte array; empty byte array for response without content (e.g. HEAD).
     * @param headers the HTTP headers.
     * @param statusCode the HTTP response code.
     * @param transferedBytes the number of transfered bytes.
     */
    public HttpResult(
            String url, 
            byte[] content, 
            Map<String, List<String>> headers, 
            int statusCode,
            long transferedBytes) {
        super();
        this.url = url;
        this.content = content;
        this.headers = headers;
        this.statusCode = statusCode;
        this.transferedBytes = transferedBytes;
    }

    /**
     * @return the result's URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the content as byte array; emtpy byte array for response without content (e.g. HEAD).
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @return the HTTP headers.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * <p>Get the HTTP header for the specified name.</p>
     * @param name the name of the HTTP header to get.
     * @return List of values, or <code>null</code> if no such header name.
     */
    public List<String> getHeader(String name) {
        return headers.get(name);
    }
    
    /**
     * <p>Get the HTTP header for the specified name as String.</p>
     * @param name the name of the HTTP header to get.
     * @return header value, or <code>null</code> if no such header name.
     */
    public String getHeaderString(String name) {
        return StringUtils.join(getHeader(name), HEADER_SEPARATOR);
    }

    /**
     * @return the HTTP response code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the number of transfered bytes.
     */
    public long getTransferedBytes() {
        return transferedBytes;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HttpResult [url=");
        builder.append(url);
        builder.append(", content=");
        builder.append(content.length);
        builder.append(" bytes");
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", statusCode=");
        builder.append(statusCode);
        builder.append(", transferedBytes=");
        builder.append(transferedBytes);
        builder.append("]");
        return builder.toString();
    }

}
