package ws.palladian.extraction.location;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.location.persistence.LocationDatabase;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.processing.features.Annotated;

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class PalladianLocationExtractor extends LocationExtractor {

    private final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger();

    private final AnnotationFilter filter = new AnnotationFilter();

    private final LocationSource locationSource;

    private final LocationDisambiguation disambiguation;

    public PalladianLocationExtractor(LocationSource locationSource) {
        this.locationSource = locationSource;
        // this.disambiguation = new FirstDisambiguation(locationSource);
        // this.disambiguation = new BaselineDisambiguation();
        this.disambiguation = new ProximityDisambiguation();
        // this.disambiguation = new ClusteringDisambiguation();
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String text) {
        List<Annotated> taggedEntities = tagger.getAnnotations(text);
        taggedEntities = filter.filter(taggedEntities);

        MultiMap<String, Location> locations = fetchLocations(taggedEntities);

        List<LocationAnnotation> locationEntities = disambiguation.disambiguate(taggedEntities, locations);

        // last step, recognize streets. For also extracting ZIP codes, this needs to be better integrated into above's
        // workflow. We should use the CITY annotations, to search for neighboring ZIP codes.
        AddressTagger addressTagger = new AddressTagger();
        List<LocationAnnotation> annotatedStreets = addressTagger.getAnnotations(text);
        locationEntities.addAll(annotatedStreets);

        return locationEntities;
    }

    private MultiMap<String, Location> fetchLocations(List<? extends Annotated> annotations) {
        Set<String> valuesToRetrieve = CollectionHelper.newHashSet();
        for (Annotated annotation : annotations) {
            String entityValue = LocationExtractorUtils.normalizeName(annotation.getValue());
            valuesToRetrieve.add(entityValue);
        }
        return locationSource.getLocations(valuesToRetrieve, EnumSet.of(Language.ENGLISH));
    }

    @Override
    public String getName() {
        return String.format("PalladianLocationExtractor:%s", disambiguation);
    }

    public static void main(String[] args) throws PageContentExtractorException {
        LocationDatabase database = DatabaseManagerFactory.create(LocationDatabase.class, "locations");
        PalladianLocationExtractor extractor = new PalladianLocationExtractor(database);
        String rawText = FileHelper.readFileToString("/Users/pk/Desktop/LocationLab/TUD-Loc-2013_V2/text6.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38822240.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38765806.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38812825.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543488.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543534.txt");
        // .readFileToString("/Users/pk/Desktop/temp_lgl/text_38543581.txt");
        String cleanText = HtmlHelper.stripHtmlTags(rawText);
        List<LocationAnnotation> locations = extractor.getAnnotations(cleanText);
        CollectionHelper.print(locations);
    }

}
