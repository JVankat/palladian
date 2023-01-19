package ws.palladian.utils;

import org.junit.Test;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;

import static org.junit.Assert.assertEquals;
import static ws.palladian.helper.functional.Predicates.regex;

public class InstanceValueTransformerTest {

    private static final Instance TEST_INSTANCE;

    static {
        InstanceBuilder builder = new InstanceBuilder();
        builder.set("word-a", 3);
        builder.set("word-b", 5);
        builder.set("word-c", 2);
        builder.set("word-d", 0);
        builder.set("other-value", 10);
        TEST_INSTANCE = builder.create(false);
    }

    @Test
    public void testBinarizer() {
        InstanceValueBinarizer transformer = new InstanceValueBinarizer(regex("word.*"));
        Instance result = transformer.apply(TEST_INSTANCE);
        assertEquals(ImmutableBooleanValue.TRUE, result.getVector().get("word-a"));
        assertEquals(ImmutableBooleanValue.FALSE, result.getVector().get("word-d"));
    }

    @Test
    public void testRelativizer() {
        InstanceValueRelativizer transformer = new InstanceValueRelativizer(regex("word.*"));
        Instance result = transformer.apply(TEST_INSTANCE);
        assertEquals(new ImmutableDoubleValue(0.3), result.getVector().get("word-a"));
        assertEquals(new ImmutableDoubleValue(0), result.getVector().get("word-d"));
    }

}
