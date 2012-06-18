/**
 * Created on: 27.01.2012 19:43:56
 */
package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.pos.BasePosTagger;
import ws.palladian.extraction.pos.OpenNlpPosTagger;
import ws.palladian.extraction.sentence.AbstractSentenceDetector;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.model.features.PositionAnnotation;
import ws.palladian.model.features.SequentialPattern;
import ws.palladian.model.features.SequentialPatternFeature;

/**
 * <p>
 * Takes a text that is already part of speech tagged and has marked sentences. From this text it extracts Labeled
 * Sequential Patterns [1]. The {@code PipelineProcessor} also requires a list of keywords it should mark in the
 * patterns. If a word is a keyword its value is used in the sequential pattern instead of the part of speech tag. The
 * labels are taken from the feature identified by {@link #LABEL_FEATURE_IDENTIFIER}. To summarize, the following
 * prerequisits are necessary to use this {@code PipelineProcessor}:
 * </p>
 * <ol>
 * <li>Complete text is part of speech tagged. For example by using {@link OpenNlpPosTagger}.
 * <li>Complete text is split into sentences. This may be achieved using an implementation of
 * {@link AbstractSentenceDetector}.
 * <li>The provided {@link PipelineDocument} contains a {@link NominalFeature} denoting the label to use for the
 * sequential patterns. This label is identified using {@code #LABEL_FEATURE_IDENTIFIER}.
 * <li>A list of keywords as parameter for the constructor of this class.
 * </ol>
 * <p>
 * [1] ﻿Cong, G., Wang, L., Lin, C. Y., Song, Y. I., & Sun, Y. (2008). Finding question-answer pairs from online forums.
 * Proceedings of the 31st annual international ACM SIGIR conference on Research and development in information
 * retrieval (pp. 467–474). New York, NY, USA: ACM. doi:10.1145/1390334.1390415
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class SequentialPatternAnnotator extends StringDocumentPipelineProcessor {
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = -1433065329363584974L;

    private Set<String> keywords;

    public static final String PROVIDED_FEATURE = "ws.palladian.lsp";

    public static final FeatureDescriptor<SequentialPatternFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
            .build(PROVIDED_FEATURE, SequentialPatternFeature.class);

    private Integer maxSequentialPatternSize = 0;

    /**
     * <p>
     * Creates a new LSP annotator with a list of keywords. This annotator is ready to use.
     * </p>
     * 
     * @param keywords
     *            The keywords used by this annotator.
     * @param maxSequentialPatternSize
     */
    public SequentialPatternAnnotator(String[] keywords, Integer maxSequentialPatternSize) {
        super();
        this.keywords = new HashSet<String>();
        Collections.addAll(this.keywords, keywords);
        this.maxSequentialPatternSize = maxSequentialPatternSize;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) {
        AnnotationFeature posFeature = document.getFeatureVector().get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        AnnotationFeature sentencesFeature = document.getFeatureVector().get(
                AbstractSentenceDetector.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> posTags = posFeature.getValue();
        List<Annotation> sentences = sentencesFeature.getValue();
        List<Annotation> markedKeywords = markKeywords(document);

        Collections.sort(posTags);
        Collections.sort(sentences);
        Collections.sort(markedKeywords);

        Iterator<Annotation> posTagsIterator = posTags.iterator();
        Iterator<Annotation> markedKeywordsIterator = markedKeywords.iterator();

        Annotation currentMarkedKeyword = markedKeywordsIterator.hasNext() ? markedKeywordsIterator.next() : null;
        Annotation currentPosTag = posTagsIterator.hasNext() ? posTagsIterator.next() : null;

        // create one LSP per sentence in the document.
        for (Annotation sentence : sentences) {
            Integer sentenceStartPosition = sentence.getStartPosition();
            Integer sentenceEndPosition = sentence.getEndPosition();
            List<String> sequentialPattern = new ArrayList<String>();

            Integer i = sentenceStartPosition;
            while (i < sentenceEndPosition) {
                // check for next keyword or part of speech tag.
                if (currentMarkedKeyword != null && currentMarkedKeyword.getStartPosition().equals(i)) {
                    sequentialPattern.add(currentMarkedKeyword.getValue());
                    i = currentMarkedKeyword.getEndPosition();
                } else if (currentPosTag != null && currentPosTag.getStartPosition().equals(i)) {
                    sequentialPattern.add(currentPosTag.getFeatureVector()
                            .get(BasePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue());
                    i = currentPosTag.getEndPosition();
                } else {
                    i++;
                }

                // iterate keywords and part of speech tags.
                while (currentMarkedKeyword != null && currentMarkedKeyword.getStartPosition() < i
                        && markedKeywordsIterator.hasNext()) {
                    currentMarkedKeyword = markedKeywordsIterator.next();
                }

                while (currentPosTag != null && currentPosTag.getStartPosition() < i && posTagsIterator.hasNext()) {
                    currentPosTag = posTagsIterator.next();
                }
            }

            Collection<List<String>> patterns = ws.palladian.extraction.token.Tokenizer.getAllSpans(
                    sequentialPattern.toArray(new String[sequentialPattern.size()]), maxSequentialPatternSize);

            for (List<String> pattern : patterns) {
                Feature<SequentialPattern> lspFeature = new Feature<SequentialPattern>(PROVIDED_FEATURE,
                        new SequentialPattern(pattern));
                sentence.getFeatureVector().add(lspFeature);
            }

        }
    }

    /**
     * <p>
     * Creates a list of annotations of the keywords provided to this {@code PipelineProcessor}.
     * </p>
     * 
     * @param document
     *            The {@link PipelineDocument} to process.
     * @return A {@code List} of {@code Annotation}s
     */
    private List<Annotation> markKeywords(PipelineDocument<String> document) {
        List<Annotation> markedKeywords = new LinkedList<Annotation>();
        String originalContent = document.getContent();
        String originalContentLowerCased = originalContent.toLowerCase();
        for (String keyword : keywords) {
            Pattern keywordPattern = Pattern.compile(keyword.toLowerCase());
            Matcher keywordMatcher = keywordPattern.matcher(originalContentLowerCased);
            if (keywordMatcher.find()) {
                markedKeywords.add(new PositionAnnotation(document, keywordMatcher.start(), keywordMatcher.end()));
            }
        }
        return markedKeywords;
    }
}
