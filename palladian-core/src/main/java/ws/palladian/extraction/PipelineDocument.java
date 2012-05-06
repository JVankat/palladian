package ws.palladian.extraction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * Represents a document processed by a {@link PipelineProcessor}. These documents are the input for pipelines. They
 * contain the content that is processed by the pipeline and the features extracted from that content as well as some
 * modified content.
 * </p>
 * <p>
 * This class represents documents as {@code String}s and thus can be used for text documents at the moment only.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class PipelineDocument<T> {

    /**
     * <p>
     * A vector of all features extracted for this document by some pipeline.
     * </p>
     */
    private FeatureVector featureVector;

    /**
     * <p>
     * A map storing different views on the content of a document. By default this map contains an entry for the
     * documents original content as well as modified content. Modified content is initialized to be the same as the
     * original content but might be changed to represent a cleaned or extended representation.
     * </p>
     */
    private final Map<String, T> views;

    /**
     * <p>
     * Creates a new {@code PipelineDocument} with initialized content. This instance is ready to be processed by a
     * {@link ProcessingPipeline}.
     * </p>
     * 
     * @param originalContent The content of this {@code PipelineDocument}.
     */
    public PipelineDocument(T originalContent) {
        super();
        this.views = new HashMap<String, T>();
        this.views.put("originalContent", originalContent);
        this.views.put("modifiedContent", originalContent);
        this.featureVector = new FeatureVector();
    }

    /**
     * <p>
     * Provides a special structured representation of a document as used by classifiers or clusterers.
     * </p>
     * 
     * @return A vector of all features extracted for this document by some pipeline.
     */
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    /**
     * <p>
     * Resets this documents {@code FeatureVector} overwriting all features previously extracted.
     * </p>
     * 
     * @param featureVector The new {@code FeatureVector} of this document.
     */
    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    /**
     * <p>
     * Provides the original content of the processed document as retrieved from the web, the local file system or any
     * other source providing documents.
     * </p>
     * 
     * @return The unmodified original content representing the document.
     */
    public T getOriginalContent() {
        return this.views.get("originalContent");
    }

    /**
     * <p>
     * Resets this documents content completely overwriting any previous original content.
     * </p>
     * 
     * @param originalContent The new unmodified original content representing the document.
     */
    public void setOriginalContent(T originalContent) {
        this.views.put("originalContent", originalContent);
    }

    /**
     * <p>
     * Provides the modified content of this document. Modified content is usually inserted by {@link PipelineProcessor}
     * s.
     * </p>
     * 
     * @return The modified content of the document or {@code null} if no modified content is available yet.
     */
    public T getModifiedContent() {
        return this.views.get("modifiedContent");
    }

    /**
     * <p>
     * Resets the modified content completely overwriting any old modified content.
     * </p>
     * 
     * @param modifiedContent The content of this document modified by some {@link PipelineProcessor}.
     */
    public void setModifiedContent(T modifiedContent) {
        this.views.put("modifiedContent", modifiedContent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PipelineDocument [featureVector=");
        builder.append(featureVector);
        builder.append(", originalContent=");
        builder.append(getOriginalContent());
        builder.append(", modifiedContent=");
        builder.append(getModifiedContent());
        builder.append("]");
        return builder.toString();
    }

    /**
     * <p>
     * Resets and overrides the content of a named view or initializes the view of it didn't exist yet.
     * </p>
     * 
     * @param viewName The name of the view.
     * @param content The text content as the new view of the document.
     */
    public void putView(String viewName, T content) {
        this.views.put(viewName, content);
    }

    /**
     * <p>
     * Provides the content of a named view.
     * </p>
     * 
     * @param viewName The name of the view, providing the requested content.
     * @return The views content or {@code null} if there is no such view available.
     */
    public T getView(String viewName) {
        return this.views.get(viewName);
    }

    /**
     * <p>
     * Checks whether this document provides a view with the provided name.
     * </p>
     * 
     * @param inputViewName The name of the requested view.
     * @return {@code true} if the document provides the requested view; {@code false} otherwise.
     */
    public boolean providesView(String inputViewName) {
        return this.views.containsKey(inputViewName);
    }

    /**
     * <p>
     * Returns a set of the names of all views this document provides currently on its content.
     * </p>
     * 
     * @return The set of all provided view names.
     */
    public Set<String> getProvidedViewNames() {
        return Collections.unmodifiableSet(this.views.keySet());
    }
}
