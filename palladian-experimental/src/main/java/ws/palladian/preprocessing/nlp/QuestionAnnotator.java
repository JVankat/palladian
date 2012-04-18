package ws.palladian.preprocessing.nlp;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} detecting 1H5W questions and questions ending on "?" from natural language texts. This is
 * the most simple way to find questions but usually sufficient. The 1H5W words are:
 * <ul>
 * <li>How</li>
 * <li>Who</li>
 * <li>What</li>
 * <li>Where</li>
 * <li>When</li>
 * <li>Why</li>
 * </ul>
 * </p>
 * <p>
 * As input the question annotator requires annotated sentences as produced by any {@link AbstractSentenceDetector}
 * processor. So if you need to detect questions from a text create a new {@link ProcessingPipeline} with an
 * {@code AbstractSentenceDetector} followed by a {@code QuestionAnnotator}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 */
public final class QuestionAnnotator extends AbstractPipelineProcessor {

    /**
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     */
    private static final long serialVersionUID = -2998306515098026978L;
    /**
     * The world wide unique identifier of the {@link Feature}s created by this annotator.
     */
    public final static String FEATURE_IDENTIFIER = "ws.palladian.features.question";

    @Override
    public void processDocument(PipelineDocument document) {
        Feature<List<Annotation>> sentences = document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> questions = new ArrayList<Annotation>();
        for (Annotation sentence : sentences.getValue()) {
            String coveredText = sentence.getValue();
            if (coveredText.endsWith("?") || coveredText.toLowerCase().startsWith("what")
                    || coveredText.toLowerCase().startsWith("who") || coveredText.toLowerCase().startsWith("where")
                    || coveredText.toLowerCase().startsWith("how ") || coveredText.toLowerCase().startsWith("why")) {

                questions.add(createQuestion(sentence));
            }
        }
        Feature<List<Annotation>> questionsFeature = new Feature<List<Annotation>>(FEATURE_IDENTIFIER, questions);
        document.getFeatureVector().add(questionsFeature);
    }

    /**
     * <p>
     * Creates a new question {@code Annotation} based on an existing sentence {@code Annotation}.
     * </p>
     * 
     * @param sentence The sentence {@code Annotation} representing the new question.
     * @return A new annotation of the question type spanning the same area as the provided sentence.
     */
    private Annotation createQuestion(Annotation sentence) {
        Annotation ret = new PositionAnnotation(sentence.getDocument(), sentence.getViewName(),
                sentence.getStartPosition(), sentence.getEndPosition());
        return ret;
    }

}
