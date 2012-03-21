/**
 * <p>
 * This part of the preprocessing functionality of Palladian is responsible for processing natural language texts (NLP).
 * It provides the following functionality:
 * <ul>
 * <li>Splitting sentences into chunks (i.e. words)</li>
 * <li>Splitting text into sentences</li>
 * <li>Annotating a text or sentence with Part of Speech (PoS) tags</li>
 * </ul>
 * </p>
 */
/**
 * <p>
 * This package contains classes realizing a {@code ProcessingPipeline}. The pipeline is realized using the Command GoF
 * pattern. Command objects are called {@link ws.palladian.extraction.PipelineProcessor}s. They are executed by the
 * {@link ws.palladian.extraction.ProcessingPipeline}. The pipeline processes
 * {@link ws.palladian.extraction.PipelineDocument} objects, extracting features and modifying the documents content.
 * </p>
 * <p>
 * There are several implementations of {@code PipelineProcessor}s available but you can create your own if you need.
 * </p>
 * <p>
 * Provided processors are for example:
 * <ul>
 * <li>{@link ws.palladian.extraction.helper.StopWordRemover}</li>
 * <li>{@link ws.palladian.extraction.helper.WordCounter}</li>
 * <li>{@link ws.palladian.preprocessing.nlp.QuestionAnnotator}</li>
 * <li>{@link ws.palladian.extraction.sentence.LingPipeSentenceDetector}</li>
 * <li>{@link ws.palladian.extraction.sentence.OpenNlpSentenceDetector}</li>
 * </ul>
 * </p>
 */
package ws.palladian.extraction;