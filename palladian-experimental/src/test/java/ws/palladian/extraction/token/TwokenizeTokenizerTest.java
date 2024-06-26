package ws.palladian.extraction.token;

import org.junit.Before;
import org.junit.Test;
import ws.palladian.core.Token;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Test for {@link TwokenizeTokenizer}.
 * </p>
 *
 * @author Philipp Katz
 */
public class TwokenizeTokenizerTest {

    private static final String TWEET = "I predict I won't win a single game I bet on. Got Cliff Lee today, so if he loses its on me RT @e_one: Texas (cont) http://tl.gd/6meogh";
    private static final String TOKENS[] = {"I", "predict", "I", "won't", "win", "a", "single", "game", "I", "bet", "on", ".", "Got", "Cliff", "Lee", "today", ",", "so", "if",
            "he", "loses", "its", "on", "me", "RT", "@e_one", ":", "Texas", "(", "cont", ")", "http://tl.gd/6meogh"};

    private static final String TWEET2 = "Funny! But I wonder why? Hmmm ~~&gt; RT @MarketWatch: Diamond feared Barclays nationalization in 2008 http://t.co/EbRcgLYf";

    private TwokenizeTokenizer tokenizer;

    @Before
    public void setUp() {
        tokenizer = new TwokenizeTokenizer();
    }

    @Test
    public void testTwokenizeTokenizer() {
        Iterator<Token> spans = tokenizer.iterateTokens(TWEET);
        int i = 0;
        while (spans.hasNext()) {
            Token span = spans.next();
            assertEquals(TOKENS[i++], span.getValue());
        }
    }

    @Test
    public void testTwokenizeProblem() {
        // see comment in TwokenizeTokenizer class, line 35
        tokenizer.iterateTokens(TWEET2);
    }

}
