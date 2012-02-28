package ws.palladian.retrieval;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * <p>
 * The {@link DocumentRetriever} allows to download pages from the Web or the hard disk. The focus of its functionality
 * has evolved over time. HTTP specific methods like GETting data from the web are now provided via
 * {@link HttpRetriever}. The parsing functionalities for obtaining DOM documents have been moved to separate classes
 * implementing {@link DocumentParser}, which can be obtained using {@link ParserFactory}.
 * </p>
 * 
 * <p>
 * The intention of this class is to provide a convenient wrapper for obtaining XML, (X)HTML and JSON data from the web
 * and from local resources. This class throws no exceptions, when IO or parse errors occures, but follows a
 * <code>null</code> return policy, which means, the return values should be checked for <code>null</code> values under
 * all circumstances. Errors are logged using the {@link Logger}.
 * </p>
 * 
 * <p>
 * If you need more control, e.g. when you need access to the HTTP headers for data downloaded from the web, want to
 * react to specific errors, etc. consider using the more specialized classes like {@link HttpRetriever},
 * {@link DocumentParser}, etc., which provide less convenience, but more control.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DocumentRetriever {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DocumentRetriever.class);

    /** The {@link HttpRetriever} used for HTTP operations. */
    private final HttpRetriever httpRetriever;

    /** The number of threads for downloading in parallel. */
    public static final int DEFAULT_NUM_THREADS = 10;

    /** The maximum number of threads to use. */
    private int numThreads = DEFAULT_NUM_THREADS;

    /** The filter for the retriever. */
    private DownloadFilter downloadFilter;

    /** The callbacks that are called after each parsed page. */
    private final List<RetrieverCallback> retrieverCallbacks;

    /**
     * <p>
     * Instantiate a new {@link DocumentRetriever} using a {@link HttpRetriever} obtained by the
     * {@link HttpRetrieverFactory}. If you need to configure the {@link HttpRetriever} individually, use
     * {@link #DocumentRetriever(HttpRetriever)} to inject you own instance.
     * </p>
     */
    public DocumentRetriever() {
        this(HttpRetrieverFactory.getHttpRetriever());
    }

    /**
     * <p>
     * Instantiate a new {@link DocumentRetriever} using the specified {@link HttpRetriever}. This way, you can
     * configure the {@link HttpRetriever} to you specific needs.
     * </p>
     * 
     * @param httpRetriever
     */
    public DocumentRetriever(HttpRetriever httpRetriever) {
        this.httpRetriever = httpRetriever;
        downloadFilter = new DownloadFilter();
        retrieverCallbacks = new ArrayList<RetrieverCallback>();
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing (X)HTML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Get a web page ((X)HTML document).
     * </p>
     * 
     * @param url The URL or file path of the web page.
     * @return The W3C document, or <code>null</code> in case of any error.
     */
    public Document getWebDocument(String url) {
        return getDocument(url, false);
    }

    /**
     * <p>
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked. The number of
     * simultaneous threads for downloading and parsing can be defined using {@link #setNumThreads(int)}.
     * </p>
     * 
     * @param urls the URLs to download.
     * @param callback the callback to be called for each finished download.
     */
    public void getWebDocuments(Collection<String> urls, RetrieverCallback callback) {

        BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(urls);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Runnable runnable = new DocumentRetrieverThread(urlQueue, callback, this);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * <p>
     * Get multiple URLs in parallel. The number of simultaneous threads for downloading and parsing can be defined
     * using {@link #setNumThreads(int)}.
     * </p>
     * 
     * @param urls the URLs to download.
     * @return set with the downloaded documents, documents which could not be downloaded or parsed successfully, are
     *         not included.
     */
    public Set<Document> getWebDocuments(Collection<String> urls) {
        final Set<Document> result = new HashSet<Document>();
        getWebDocuments(urls, new RetrieverCallback<Document>() {
            @Override
            public void onFinishRetrieval(Document document) {
                synchronized (result) {
                    result.add(document);
                }
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing XML documents
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Get XML document from a URL. The XML document must be well-formed.
     * </p>
     * 
     * @param url The URL or file path pointing to the XML document.
     * @return The XML document, or <code>null</code> in case of any error.
     */
    public Document getXmlDocument(String url) {
        return getDocument(url, true);
    }

    // ////////////////////////////////////////////////////////////////
    // methods for retrieving + parsing JSON data
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Get a JSON object from a URL. The retrieved contents must return a valid JSON object.
     * </p>
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON object, or <code>null</code> in case of any error.
     */
    public JSONObject getJsonObject(String url) {
        String json = getText(url);

        if (json != null) {
            json = json.trim();
            
            // delicous feeds return the whole JSON object wrapped in [square brackets],
            // altough this seems to be valid, our parser doesn't like this, so we remove
            // those brackets before parsing -- Philipp, 2010-07-04
            
            // this was stupid, therefore removed it again. Clients should use getJsonArray instead --
            // Philipp, 2011-12-29
            
            /*if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
            }*/

            JSONObject jsonOBJ = null;

            if (json.length() > 0) {
                try {
                    jsonOBJ = new JSONObject(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonOBJ;
        }
        return null;
    }

    /**
     * <p>
     * Get a JSON array from a URL. The retrieved contents must return a valid JSON array.
     * </p>
     * 
     * @param url the URL pointing to the JSON string.
     * @return the JSON array, or <code>null</code> in case of any error.
     */
    public JSONArray getJsonArray(String url) {
        String json = getText(url);

        // since we know this string should be an JSON array,
        // we will directly parse it
        if (json != null) {
            json = json.trim();

            JSONArray jsonAR = null;

            if (json.length() > 0) {
                try {
                    jsonAR = new JSONArray(json);
                } catch (JSONException e) {
                    LOGGER.error(url + ", " + e.getMessage(), e);
                }
            }

            return jsonAR;

        }
        return null;

    }

    // ////////////////////////////////////////////////////////////////
    // method for retrieving plain text
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Download the contents that are retrieved from the given URL.
     * </p>
     * 
     * @param url The URL of the desired contents.
     * @return The contents as a string, or <code>null</code> if contents could no be retrieved. See the error log for
     *         possible errors.
     */
    public String getText(String url) {

        String contentString = null;
        Reader reader = null;

        if (downloadFilter.isAcceptedFileType(url)) {
            try {
                if (isFile(url)) {
                    reader = new FileReader(url);
                    contentString = IOUtils.toString(reader);
                } else {
                    HttpResult httpResult = httpRetriever.httpGet(url);
                    contentString = new String(httpResult.getContent());
                }
            } catch (IOException e) {
                LOGGER.error(url + ", " + e.getMessage());
            } catch (Exception e) {
                LOGGER.error(url + ", " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }

        return contentString;
    }

    /**
     * <p>
     * Get multiple URLs in parallel, for each finished download the supplied callback is invoked. The number of
     * simultaneous threads for downloading and parsing can be defined using {@link #setNumThreads(int)}.
     * </p>
     * 
     * @param urls The URLs to download.
     * @param callback The callback to be called for each finished download.
     */
    public void getTexts(Collection<String> urls, RetrieverCallback<String> callback) {

        BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(urls);

        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Runnable runnable = new TextRetrieverThread(urlQueue, callback, this);
            threads[i] = new Thread(runnable);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * <p>
     * Get multiple URLs in parallel. The number of simultaneous threads for downloading and parsing can be defined
     * using {@link #setNumThreads(int)}.
     * </p>
     * 
     * @param urls The URLs to download.
     * @return Set with the downloaded texts. Texts which could not be downloaded or parsed successfully, are not
     *         included.
     */
    public Set<String> getTexts(Collection<String> urls) {
        final Set<String> result = new HashSet<String>();
        getTexts(urls, new RetrieverCallback<String>() {
            @Override
            public void onFinishRetrieval(String text) {
                synchronized (result) {
                    result.add(text);
                }
            }
        });
        return result;
    }

    // ////////////////////////////////////////////////////////////////
    // internal methods
    // ////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Multi-purpose method to get a {@link Document}, either by downloading it from the Web, or by reading it from
     * disk. The document may be parsed using an XML parser or a dedicated (X)HTML parser.
     * </p>
     * 
     * @param url the URL of the document to retriever or the file path.
     * @param xml indicate whether the document is well-formed XML or needs to be processed using an (X)HTML parser.
     * @return the parsed document, or <code>null</code> if any kind of error occurred or the document was filtered by
     *         {@link DownloadFilter}.
     */
    private Document getDocument(String url, boolean xml) {

        Document document = null;
        String cleanUrl = url.trim();
        InputStream inputStream = null;

        if (downloadFilter.isAcceptedFileType(cleanUrl)) {

            try {

                if (isFile(cleanUrl)) {
                    File file = new File(cleanUrl);
                    inputStream = new BufferedInputStream(new FileInputStream(new File(cleanUrl)));
                    document = parse(inputStream, xml);
                    document.setDocumentURI(file.toURI().toString());
                } else {
                    HttpResult httpResult = httpRetriever.httpGet(cleanUrl);
                    document = parse(new ByteArrayInputStream(httpResult.getContent()), xml);
                    document.setDocumentURI(cleanUrl);
                }

                callRetrieverCallback(document);

            } catch (FileNotFoundException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (DOMException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (ParserException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } catch (HttpException e) {
                LOGGER.error(url + ", " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return document;
    }

    private static boolean isFile(String url) {
        boolean isFile = false;
        if (url.indexOf("http://") == -1 && url.indexOf("https://") == -1) {
            isFile = true;
        }
        return isFile;
    }

    /**
     * <p>
     * Parses an {@link InputStream} to a {@link Document}.
     * </p>
     * 
     * @param inputStream the stream to parse.
     * @param xml <code>true</code> if this document is an XML document, <code>false</code> if HTML document.
     * @throws ParserException if parsing failed.
     */
    private Document parse(InputStream inputStream, boolean xml) throws ParserException {
        Document document = null;
        DocumentParser parser;

        if (xml) {
            parser = ParserFactory.createXmlParser();
        } else {
            parser = ParserFactory.createHtmlParser();
        }

        document = parser.parse(inputStream);
        return document;
    }

    /**
     * <p>
     * Set the maximum number of simultaneous threads for downloading, when using {@link #getWebDocuments(Collection)}
     * and {@link #getWebDocuments(Collection, RetrieverCallback)}.
     * </p>
     * 
     * @param numThreads the number of threads to use.
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public void setDownloadFilter(DownloadFilter downloadFilter) {
        this.downloadFilter = downloadFilter;
    }

    public DownloadFilter getDownloadFilter() {
        return downloadFilter;
    }

    // ////////////////////////////////////////////////////////////////
    // Callbacks
    // ////////////////////////////////////////////////////////////////

    private void callRetrieverCallback(Document document) {
        for (RetrieverCallback retrieverCallback : retrieverCallbacks) {
            retrieverCallback.onFinishRetrieval(document);
        }
    }

    public List<RetrieverCallback> getRetrieverCallbacks() {
        return retrieverCallbacks;
    }

    public void addRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.add(retrieverCallback);
    }

    public void removeRetrieverCallback(RetrieverCallback retrieverCallback) {
        retrieverCallbacks.remove(retrieverCallback);
    }

    // ////////////////////////////////////////////////////////////////
    // main method
    // ////////////////////////////////////////////////////////////////

    /**
     * The main method for testing and usage purposes.
     * 
     * @param args The arguments.
     */
    public static void main(String[] args) throws Exception {
        DocumentRetriever retriever = new DocumentRetriever();

        // // speed test download and parse documents vs. text only retrieval, result: almost no difference, about 10ms
        // per
        // document for parsing
        // StopWatch sw = new StopWatch();
        // BingSearcher bingSearcher = new BingSearcher(ConfigHolder.getInstance().getConfig());
        // List<String> urls1 = bingSearcher.searchUrls("Jim Carrey", 20);
        // System.out.println("searched in " + sw.getElapsedTimeString());
        // sw.start();
        // // Set<Document> webDocuments = retriever.getWebDocuments(urls1);
        // Set<String> webTexts = retriever.getTexts(urls1);
        // System.out.println("downloaded in " + sw.getElapsedTimeString());
        // System.out.println("total: " + sw.getTotalElapsedTimeString());
        System.exit(0);

        // HttpResult result = retriever.httpGet(url);
        // String eTag = result.getHeaderString("Last-Modified");
        //
        // Map<String, String> header = new HashMap<String, String>();
        // header.put("If-Modified-Since", eTag);
        //
        // retriever.httpGet(url, header);
        // System.exit(0);
        //
        // // download and save a web page including their headers in a gzipped file
        // retriever.downloadAndSave("http://cinefreaks.com", "data/temp/cf_no_headers.gz", new HashMap<String,
        // String>(),
        // true);

        // create a retriever that is triggered for every retrieved page
        RetrieverCallback crawlerCallback = new RetrieverCallback<Document>() {
            @Override
            public void onFinishRetrieval(Document document) {
                // do something with the page
                LOGGER.info(document.getDocumentURI());
            }
        };
        retriever.addRetrieverCallback(crawlerCallback);

        // give the retriever a list of URLs to download
        Set<String> urls = new HashSet<String>();
        urls.add("http://www.cinefreaks.com");
        urls.add("http://www.imdb.com");

        // set the maximum number of threads to 10
        retriever.setNumThreads(10);

        // download documents
        Set<Document> documents = retriever.getWebDocuments(urls);
        CollectionHelper.print(documents);

        // or just get one document
        Document webPage = retriever.getWebDocument("http://www.cinefreaks.com");
        LOGGER.info(webPage.getDocumentURI());

    }
}