package ws.palladian.processing.features;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * A class to describe collections of {@code Feature}s extracted from some document. Based on its {@code FeatureVector}
 * the document can be processed by Information Retrieval components like classifiers or clusterers.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
public class FeatureVector implements Iterable<Feature<?>> {

    /**
     * <p>
     * A map of all {@code Feature}s in this vector. It maps from the {@code Feature}s {@code FeatureVector} wide unique
     * identifier to an actual {@code Feature} instance containing the value. The value might be of any java object
     * type.
     * </p>
     */
    protected final SortedMap<String, List<Feature<?>>> features;

    /**
     * <p>
     * Creates a new empty {@code FeatureVector}. To fill it with {@link Feature}s call {@link #add(String, Feature)}.
     * </p>
     */
    public FeatureVector() {
        features = new TreeMap<String, List<Feature<?>>>();
    }

    /**
     * <p>
     * Creates a new {@link FeatureVector} from the provided FeatureVector, i.e. a copy with all {@link Feature}s.
     * 
     * @param featureVector The feature vector which Features to copy.
     */
    public FeatureVector(FeatureVector featureVector) {
        features = new TreeMap<String, List<Feature<?>>>(featureVector.features);
    }

    /**
     * <p>
     * Adds a new {@code Feature} to this {@code FeatureVector}.
     * </p>
     * 
     * @param newFeature
     *            The actual {@code Feature} instance containing the value.
     */
    public void add(Feature<?> newFeature) {
        List<Feature<?>> list = features.get(newFeature.getName());
        if (list == null) {
            list = new ArrayList<Feature<?>>();
        }
        list.add(newFeature);

        features.put(newFeature.getName(), list);
    }

    /**
     * <p>
     * Provides a {@code Feature} from this {@code FeatureVector}.
     * </p>
     * 
     * @param identifier
     *            The {@code FeatureVector} wide unique identifier of the requested {@code Feature}.
     * @return The {@code Feature} with identifier {@code identifier} or {@code null} if no such {@code Feature} exists.
     * @deprecated Prefer using {@link #get(FeatureDescriptor)} when a {@link FeatureDescriptor} is available. This
     *             improves type safety and avoids unnecessary casting.
     */
    // @Deprecated
    // public <T extends Feature<?>> T getFeature(Class<T> class1, String identifier) {
    // List<T> allFeatures = getAll(class1, identifier);
    // if (allFeatures != null && !allFeatures.isEmpty()) {
    // return allFeatures.get(0);
    // }
    // return null;
    // }

    @Deprecated
    private Feature<?> getFeature(String identifier) {
        List<Feature<?>> allFeatures = features.get(identifier);
        if (allFeatures != null && !allFeatures.isEmpty()) {
            return allFeatures.get(0);
        }
        return null;
    }

    @Deprecated
    public <T extends Feature<?>> T getFeature(Class<T> class1, String identifier) {
        List<T> allFeatures = getAll(class1, identifier);
        if (allFeatures != null && !allFeatures.isEmpty()) {
            return allFeatures.get(0);
        }
        return null;

    }

    @Deprecated
    public <T extends Feature<?>> List<T> getAll(Class<T> class1, String identifier) {
        List<Feature<?>> list = features.get(identifier);

        List<T> selectedFeatures = new ArrayList<T>();
        for (Feature<?> feature : list) {
            selectedFeatures.add(class1.cast(feature));
        }
        return selectedFeatures;
    }

    /**
     * <p>
     * Provides all {@link Feature}s with the specified type from this {@link FeatureVector}.
     * </p>
     * 
     * @param type The type of the {@link Feature}s to retrieve.
     * @return A {@link List} of {@link Feature}s for the specified type or an empty List of no such {@link Feature}s
     *         exist, never <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T extends Feature<?>> List<T> getAll(Class<T> type) {
        List<T> ret = new ArrayList<T>();
        for (List<Feature<?>> featureList : features.values()) {
            for (Feature<?> feature : featureList) {
                if (type.isInstance(feature)) {
                    ret.add((T)feature);
                }
            }
        }
        return ret;
    }

    /**
     * <p>
     * Provides a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
     *            requested {@link Feature}.
     * @return The {@link Feature} for the specified {@link FeatureDescriptor} or <code>null</code> if no such
     *         {@link Feature} exists.
     */
    public <T extends Feature<?>> T get(FeatureDescriptor<T> descriptor) {
        List<Feature<?>> feature = features.get(descriptor.getIdentifier());
        if (feature == null) {
            return null;
        }
        if (feature.size() == 0) {
            return null;
        }
        return descriptor.getType().cast(feature.get(0));
    }

    @Override
    public String toString() {
        return features.values().toString();
    }

    /**
     * <p>
     * Converts this {@code FeatureVector} into an array of {@code Feature}s.
     * </p>
     * 
     * @return The vector as array.
     */
    public Feature<?>[] toArray() {
        // return features.values().toArray(new Feature[features.size()]);
        return getFlat().toArray(new Feature[0]);
    }

    /**
     * <p>
     * Get the dimension of this feature vector, i.e. how many {@link Feature}s the vector contains.
     * </p>
     * 
     * @return The size of this {@code FeatureVector}.
     */
    public int size() {
        return features.size();
    }

    /**
     * <p>
     * Removes a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param identifier
     *            The {@link FeatureVector} wide unique identifier of the {@link Feature} to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean remove(String identifier) {
        return features.remove(identifier) != null;
    }

    /**
     * <p>
     * Removes a {@link Feature} from this {@link FeatureVector}.
     * </p>
     * 
     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and the concrete type of the
     *            {@link Feature} to remove.
     * @return <code>true</code> if the {@link Feature} was removed, <code>false</code> if there was no feature with the
     *         specified identifier to remove.
     */
    public boolean remove(FeatureDescriptor<?> featureDescriptor) {
        return features.remove(featureDescriptor.getIdentifier()) != null;
    }

    /**
     * <p>
     * Adds all features from the provided {@code FeatureVector} to this {@code FeatureVector}.
     * </p>
     * 
     * @param featureVector The {@code FeatureVector} containing the {@link Feature}s to add.
     */
    // public void addAll(final FeatureVector featureVector) {
    // for (Feature<?> feature : featureVector) {
    // this.add(feature);
    // }
    // }

    public List<Feature<?>> getFlat() {
        List<Feature<?>> result = new ArrayList<Feature<?>>();

        Set<Entry<String, List<Feature<?>>>> entrySet = features.entrySet();
        for (Entry<String, List<Feature<?>>> entry : entrySet) {
            result.addAll(entry.getValue());
        }

        return result;
    }

    public <T extends Feature<?>> List<T> getFeatures(Class<T> class1, String featurePath) {

        String[] pathElements = featurePath.split("/");

        for (String pathElement : pathElements) {
            Feature<?> feature = getFeature(pathElement);
            if (feature instanceof AnnotationFeature) {
                return ((AnnotationFeature)feature).getFeatures(class1,
                        featurePath.substring(featurePath.indexOf("/") + 1));
            }
            return getAll(class1, pathElement);
        }

        return null;
    }

    @Override
    public Iterator<Feature<?>> iterator() {
        return getFlat().iterator();
    }
}
