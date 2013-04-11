package ws.palladian.extraction.entity;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.Tagger;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Tag dates and times in a text.
 * </p>
 * 
 * @author David Urbansky
 */
public class DateAndTimeTagger implements Tagger {

    /** The tag name for URLs. */
    public static final String DATETIME_TAG_NAME = "DATETIME";

    /** All date formats defined by default, plus additionally years in context. */
    private static final DateFormat[] ALL_DATES_WITH_YEARS = ArrayUtils.addAll(RegExp.ALL_DATE_FORMATS,
            RegExp.DATE_CONTEXT_YYYY);

    @Override
    public List<Annotated> getAnnotations(String text) {
        return tagDateAndTime(text, ALL_DATES_WITH_YEARS);
    }

    public List<Annotated> tagDateAndTime(String inputText, DateFormat[] dateFormats) {

        List<Annotated> annotations = CollectionHelper.newArrayList();

        List<ExtractedDate> allDates = DateParser.findDates(inputText, dateFormats);

        for (ExtractedDate dateTime : allDates) {

            // get the offset
            List<Integer> occurrenceIndices = StringHelper.getOccurrenceIndices(inputText, dateTime.getDateString());

            for (Integer integer : occurrenceIndices) {
                Annotation annotation = new Annotation(integer, dateTime.getDateString(), DATETIME_TAG_NAME);
                annotations.add(annotation);
            }

        }

        return annotations;
    }

}
