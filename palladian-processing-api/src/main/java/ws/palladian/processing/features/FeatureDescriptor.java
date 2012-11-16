package ws.palladian.processing.features;

import ws.palladian.processing.PipelineProcessor;

/**
 * <p>
 * This interface provides meta-data for a specific {@link Feature}. {@link PipelineProcessor}s providing or requiring
 * specific {@link Feature}s can use this meta-data when querying a {@link FeatureVector}. By providing run-time type
 * information about the concrete feature, unnecessary casts in client code can be avoided, as the {@link FeatureVector}
 * can dynamically cast the requested feature to the correct type.
 * </p>
 * 
 * <p>
 * {@link PipelineProcessor}s which attach a certain feature to a {@link FeatureVector} should provide a static field
 * named e.g. <code>PROVIDED_FEATURE_DESCRIPTOR</code>. This constant can then be used for invoking
 * {@link FeatureVector#get(FeatureDescriptor)}. {@link FeatureDescriptor}s can be created using
 * {@link FeatureDescriptorBuilder#build(String, Class)}. {@link PipelineProcessor}s which need to supply
 * {@link FeatureDescriptor}s dynamically (for example a dictionary based annotator which can be configured with
 * different dictionaries for each instance) implement the interface {@link FeatureProvider}.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> The specific feature type which this {@link FeatureDescriptor} describes.
 * @deprecated Not supported any longer.
 */
@Deprecated
public interface FeatureDescriptor<T extends Feature<?>> {

    /**
     * <p>
     * Return the unique identifier for this feature, e.g. <code>ws.palladian.features.tokens</code>
     * </p>
     * 
     * @return
     */
    String getIdentifier();

    /**
     * <p>
     * The run-time type of this feature.
     * </p>
     * 
     * @return
     */
    Class<T> getType();

}
