package ws.palladian.core;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.value.*;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.LinkedHashMap;
import java.util.Map;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

/**
 * A builder for conveniently creating {@link Instance}s and {@link FeatureVector}s.
 *
 * @author Philipp Katz
 */
public final class InstanceBuilder {
    private final Map<String, Value> valueMap = new LinkedHashMap<>();

    private int weight = 1;

    /**
     * Set a double value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, double value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, new ImmutableDoubleValue(value));
        return this;
    }

    /**
     * Set a float value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, float value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, new ImmutableFloatValue(value));
        return this;
    }

    /**
     * Set a long value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, long value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, ImmutableLongValue.valueOf(value));
        return this;
    }

    /**
     * Set an integer value (overwrite an existing value with the same name, in
     * case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, int value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, ImmutableIntegerValue.valueOf(value));
        return this;
    }

    /**
     * Set a String value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set, not <code>null</code>.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, String value) {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(value, "value must not be null");
        valueMap.put(name, ImmutableStringValue.valueOf(value));
        return this;
    }

    /**
     * Set a boolean value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, boolean value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, ImmutableBooleanValue.create(value));
        return this;
    }

    /**
     * Set a value (overwrite an existing value with the same name, in case it exists).
     *
     * @param name  Name of the value to set, not <code>null</code> or empty.
     * @param value Value to set, not <code>null</code>.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder set(String name, Value value) {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(value, "value must not be null");
        valueMap.put(name, value);
        return this;
    }

    /**
     * Set the text value (overwrite existing text, in case it exists).
     *
     * @param text The text to set, not <code>null</code> or empty.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder setText(String text) {
        Validate.notNull(text, "text must not be null");
        valueMap.put(VECTOR_TEXT_IDENTIFIER, new ImmutableTextValue(text));
        return this;
    }

    /**
     * Explicitly set a value to {@link NullValue}.
     *
     * @param name Name of the value to set, not <code>null</code> or empty.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder setNull(String name) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, NullValue.NULL);
        return this;
    }

    /**
     * Add all values from a given feature vector (overwriting already existing values with the same names).
     *
     * @param featureVector The featue vector, not <code>null</code>.
     * @return The builder instance for method chaining.
     */
    public InstanceBuilder add(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        for (VectorEntry<String, Value> entry : featureVector) {
            set(entry.key(), entry.value());
        }
        return this;
    }

    /**
     * Set a weight which will be considered when creating an instance using {@link #create(boolean)} or
     * {@link #create(String)}.
     *
     * @param weight The weight, equal/greater one.
     * @return The builder instance for method chaining.
     * @see Instance#getWeight()
     */
    public InstanceBuilder weight(int weight) {
        Validate.isTrue(weight >= 1, "weight must be equal/greater one");
        this.weight = weight;
        return this;
    }

    /**
     * Create a feature vector.
     *
     * @return A new (immutable) feature vector.
     */
    public FeatureVector create() {
        return new ImmutableFeatureVector(new LinkedHashMap<>(valueMap));
    }

    /**
     * Create an instance.
     *
     * @param category The category for the instance, not <code>null</code>.
     * @return A new (immutable) instance.
     */
    public Instance create(String category) {
        Validate.notNull(category, "category must not be null");
        return new ImmutableInstance(create(), category, weight);
    }

    /**
     * Create an instance.
     *
     * @param category The category for the instance.
     * @return A new (immutable) instance.
     */
    public Instance create(boolean category) {
        return create(String.valueOf(category));
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }

}
