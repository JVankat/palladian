package ws.palladian.retrieval;

import org.openqa.selenium.Cookie;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public abstract class JsEnabledDocumentRetriever extends WebDocumentRetriever {
    protected int timeoutSeconds = 10;
    protected Consumer<WaitException> waitExceptionCallback;
    protected List<Cookie> cookies = new ArrayList<>();

    /**
     * We can configure the retriever to wait for certain elements on certain URLs that match the given pattern.
     */
    protected Map<Pattern, String> waitForElementMap = new HashMap<>();

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<Pattern, String> getWaitForElementMap() {
        return waitForElementMap;
    }

    public void setWaitForElementMap(Map<Pattern, String> waitForElementMap) {
        this.waitForElementMap = waitForElementMap;
    }

    public Consumer<WaitException> getWaitExceptionCallback() {
        return waitExceptionCallback;
    }

    public void setWaitExceptionCallback(Consumer<WaitException> waitExceptionCallback) {
        this.waitExceptionCallback = waitExceptionCallback;
    }

    public void addCookie(String name, String value, String domain) {
        Cookie cookie = new Cookie.Builder(name, value)
                .domain(domain)
                .expiresOn(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)))
                .isHttpOnly(false)
                .isSecure(false)
                .path("/")
                .build();
        addCookie(cookie);
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public void clearCookies() {
        this.cookies.clear();
    }
}
