package ws.palladian.retrieval;

import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Function;

final class ApacheCookieStoreAdapter implements CookieStore {

    private final ws.palladian.retrieval.CookieStore adapted;

    ApacheCookieStoreAdapter(ws.palladian.retrieval.CookieStore adapted) {
        this.adapted = adapted;
    }

    @Override
    public void addCookie(Cookie cookie) {
        adapted.addCookie(new ImmutableCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(), cookie.getPath()));
    }

    @Override
    public List<Cookie> getCookies() {
        return CollectionHelper.convertList(adapted.getCookies(),
                new Function<ws.palladian.retrieval.Cookie, Cookie>() {
                    @Override
                    public Cookie compute(ws.palladian.retrieval.Cookie input) {
                        BasicClientCookie cookie = new BasicClientCookie(input.getName(), input.getValue());
                        cookie.setDomain(input.getDomain());
                        cookie.setPath(cookie.getPath());
                        return cookie;
                    }
                });
    }

    @Override
    public boolean clearExpired(Date date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
