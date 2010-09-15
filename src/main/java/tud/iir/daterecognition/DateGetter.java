package tud.iir.daterecognition;

import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Document;

import tud.iir.helper.ArrayHelper;
import tud.iir.web.Crawler;

/**
 * DateGetter provides methods for getting dates from URL and rate them.
 * 
 * @author Martin Gregor (mail@m-gregor.de)
 * @param <T>
 * 
 * 
 */
public class DateGetter {

    private boolean tech_HTTP = true;
    private boolean tech_URL = true;
    private boolean tech_HTML_head = true;
    private boolean tech_HTML_struct = true;
    private boolean tech_HTML_content = true;
    private boolean tech_reference = true;
    private boolean tech_archive = true;

    /** URL that will be called */
    private String url;

    private Document document;

    public DateGetter() {
        super();
    }

    /**
     * Constructor creates a new DateGetter with a given URL.
     * 
     * @param url URL that will be analyzed
     */
    public DateGetter(final String url) {
        this.url = url;
    }

    public DateGetter(final Document document) {
        this.document = document;
    }

    public DateGetter(final String url, Document document) {
        this.url = url;
        this.document = document;
    }

    /**
     * Analyzes a webpage by different techniques to find dates. The techniques are found in DateGetterHelper. <br>
     * Type of the found dates are ExtractedDate.
     * 
     * @param <T>
     * 
     * @return A array of ExtractedDates.
     */
    public <T> ArrayList<T> getDate() {

        ArrayList<T> dates = new ArrayList<T>();
        Crawler crawler = new Crawler();

        if (url != null) {

            // final Document document = crawler.getWebDocument(this.url, false);

            if (tech_HTTP) {
                dates.addAll((Collection<? extends T>) DateGetterHelper.getHTTPHeaderDate(url));
            }
            if (tech_URL) {
                dates.add((T) DateGetterHelper.getURLDate(url));
            }
            if (tech_HTML_head || tech_HTML_struct || tech_HTML_content || tech_reference) {
                Document document = this.document;
                if (document == null) {
                    document = crawler.getWebDocument(url);
                }
                if (document != null) {
                    if (tech_HTML_head) {
                        dates.addAll((Collection<? extends T>) DateGetterHelper.getHeadDates(document));
                    }
                    if (tech_HTML_struct) {
                        dates.addAll((Collection<? extends T>) DateGetterHelper.getStructureDate(document));
                    }
                    if (tech_HTML_content) {
                        dates.addAll((Collection<? extends T>) DateGetterHelper.getContentDates(document));
                    }
                    if (tech_reference) {
                        dates.addAll((Collection<? extends T>) DateGetterHelper.getReferenceDates(document));
                        // TODO: evaluate each link, so only the best date for each link is left.
                    }
                }
            }
        }
        dates = ArrayHelper.removeNullElements(dates);
        return dates;

    }

    /**
     * Getter for global variable URL.
     * 
     * @return URL.
     */
    public String getURL() {
        return this.url;
    }

    /**
     * Setter for global variable URL.
     * 
     * @return URL.
     */
    public void setURL(final String url) {
        this.url = url;
    }

    public void setTechHTTP(boolean value) {
        tech_HTTP = value;
    }

    public void setTechURL(boolean value) {
        tech_URL = value;
    }

    public void setTechHTMLHead(boolean value) {
        tech_HTML_head = value;
    }

    public void setTechHTMLStruct(boolean value) {
        tech_HTML_struct = value;
    }

    public void setTechHTMLContent(boolean value) {
        tech_HTML_content = value;
    }

    public void setTechReference(boolean value) {
        tech_reference = value;
    }

    public void setTechArchive(boolean value) {
        tech_archive = value;
    }

    public void setAllFalse() {
        tech_HTTP = false;
        tech_URL = false;
        tech_HTML_head = false;
        tech_HTML_struct = false;
        tech_HTML_content = false;
        tech_reference = false;
        tech_archive = false;
    }

    public void setAllTrue() {
        tech_HTTP = true;
        tech_URL = true;
        tech_HTML_head = true;
        tech_HTML_struct = true;
        tech_HTML_content = true;
        tech_reference = true;
        tech_archive = true;
    }

}
