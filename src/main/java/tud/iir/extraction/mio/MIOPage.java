/**
 * 
 * @author Martin Werner
 */
package tud.iir.extraction.mio;

import tud.iir.web.Crawler;

/**
 * An webpage which contains mio(s).
 * 
 * @author Martin Werner
 */
public class MIOPage {

    /** The url. */
    private String url;

    /** The hostname. */
    private String hostname;

    /** The content. */
    private String content = "";

    /** The title of a MIOPage. */
    private String title = "";

    /** To check if this MIOPage was linked. */
    private boolean isLinkedPage = false;

    /** The name of the link. */
    private String linkName = "";

    /** The title of the link. */
    private String linkTitle = "";

    /** The parent page that linked to this MIOPage. */
    private String linkParentPage = "";

    /** To check if this MIOPage is an iframe-source. */
    private boolean isIFrameSource = false;

    /** The iframe-Page that embeds this MIOPage. */
    private String iframeParentPage = "";

    /** The title of the iframe-page that embeds this MIOpage. */
    private String iframeParentPageTitle = "";

    /** The dedicated page trust. */
    private double dedicatedPageTrust = 0;

    /**
     * Instantiates a new mIO page.
     * 
     * @param url the url
     * @param content the content
     */
    public MIOPage(String url, String content) {
        this.url = url;
        this.content = content;
        Crawler crawler = new Crawler();
        crawler.setDocument(url);
        this.hostname = Crawler.getDomain(url, false);
        this.title = Crawler.extractTitle(crawler.getDocument());

    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     * 
     * @param url the new url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the hostname.
     * 
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the hostname.
     * 
     * @return true, if is i frame source
     */
    // private void setHostname(String pageURL) {
    // Crawler crawler = new Crawler(5000, 5000, 10000);
    // this.hostname = Crawler.getDomain(pageURL, false);
    // }

    /**
     * Checks if is i frame source.
     * 
     * @return true, if is i frame source
     */
    public boolean isIFrameSource() {
        return isIFrameSource;
    }

    /**
     * Sets the i frame source.
     * 
     * @param isIFrameSource the new i frame source
     */
    public void setIFrameSource(boolean isIFrameSource) {
        this.isIFrameSource = isIFrameSource;
    }

    /**
     * Gets the content.
     * 
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     * 
     * @param content the new content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the link name.
     * 
     * @return the link name
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Sets the link name.
     * 
     * @param linkName the new link name
     */
    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    /**
     * Gets the link parent page.
     * 
     * @return the link parent page
     */
    public String getLinkParentPage() {
        return linkParentPage;
    }

    /**
     * Sets the link parent page.
     * 
     * @param linkParentPage the new link parent page
     */
    public void setLinkParentPage(String linkParentPage) {
        this.linkParentPage = linkParentPage;
    }

    /**
     * Checks if is linked page.
     * 
     * @return true, if is linked page
     */
    public boolean isLinkedPage() {
        return isLinkedPage;
    }

    /**
     * Sets the linked page.
     * 
     * @param isLinkedPage the new linked page
     */
    public void setLinkedPage(boolean isLinkedPage) {
        this.isLinkedPage = isLinkedPage;
    }

    /**
     * Gets the link title.
     * 
     * @return the link title
     */
    public String getLinkTitle() {
        return linkTitle;
    }

    /**
     * Sets the link title.
     * 
     * @param linkTitle the new link title
     */
    public void setLinkTitle(String linkTitle) {
        this.linkTitle = linkTitle;
    }

    // private void getHostname(String url) {
    // Crawler crawler = new Crawler(5000, 5000, 10000);
    //
    // setHostname(crawler.getDomain(url, false));
    // }

    /**
     * Gets the dedicated page trust.
     * 
     * @return the dedicated page trust
     */
    public double getDedicatedPageTrust() {
        return dedicatedPageTrust;
    }

    /**
     * Sets the dedicated page trust.
     * 
     * @param dedicatedPageTrust the new dedicated page trust
     */
    public void setDedicatedPageTrust(double dedicatedPageTrust) {
        this.dedicatedPageTrust = dedicatedPageTrust;
    }

    /**
     * Gets the iframe parent page.
     * 
     * @return the iframe parent page
     */
    public String getIframeParentPage() {
        return iframeParentPage;
    }

    /**
     * Sets the iframe parent page.
     * 
     * @param iframeParentPage the new iframe parent page
     */
    public void setIframeParentPage(String iframeParentPage) {
        this.iframeParentPage = iframeParentPage;
    }

    /**
     * Sets the iframe parent page title.
     * 
     * @param iframeParentPageTitle the new iframe parent page title
     */
    public void setIframeParentPageTitle(String iframeParentPageTitle) {
        this.iframeParentPageTitle = iframeParentPageTitle;
    }

    /**
     * Gets the iframe parent page title.
     * 
     * @return the iframe parent page title
     */
    public String getIframeParentPageTitle() {
        return iframeParentPageTitle;
    }

    /**
     * Gets the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

}
