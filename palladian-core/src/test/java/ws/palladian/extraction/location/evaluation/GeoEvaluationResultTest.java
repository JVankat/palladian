package ws.palladian.extraction.location.evaluation;

import org.junit.Test;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationBuilder;
import ws.palladian.extraction.location.LocationType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class GeoEvaluationResultTest {

    private static final double DELTA = 0.0001;

    @Test
    public void testGeoEvaluationResult() {
        //  Paris, Texas is a city located 98 miles northeast of Dallas in Lamar County, Texas, in
        // the United States. The Sam Bell Maxey House is a historic house in Paris.

        // String text = "Paris, Texas is a city located 98 miles northeast of Dallas in Lamar County, Texas, in the United States. The Sam Bell Maxey House is a historic house in Paris.";

        List<LocationAnnotation> goldStandard = new ArrayList<>();
        List<LocationAnnotation> result = new ArrayList<>();

        goldStandard.add(new LocationAnnotation(0, "Paris", new LocationBuilder().setPrimaryName("Paris").setType(LocationType.CITY).setCoordinate(33.6625, -95.5477).create()));
        goldStandard.add(new LocationAnnotation(7, "Texas", new LocationBuilder().setPrimaryName("Texas").setType(LocationType.UNIT).setCoordinate(31, -100).create()));
        goldStandard.add(new LocationAnnotation(53, "Dallas", new LocationBuilder().setPrimaryName("Dallas").setType(LocationType.CITY).setCoordinate(32.7758, -96.7967).create()));
        goldStandard.add(new LocationAnnotation(63, "Lamar County", new LocationBuilder().setPrimaryName("Dallas")
                .setType(LocationType.UNIT)
                .setCoordinate(33.67, -95.57)
                .create()));
        goldStandard.add(new LocationAnnotation(77, "Texas", new LocationBuilder().setPrimaryName("Texas").setType(LocationType.UNIT).setCoordinate(31, -100).create()));
        goldStandard.add(new LocationAnnotation(91, "United States", new LocationBuilder().setPrimaryName("United States")
                .setType(LocationType.COUNTRY)
                .setCoordinate(39.76, -98.5)
                .create()));
        goldStandard.add(new LocationAnnotation(110, "Sam Bell Maxey House", new LocationBuilder().setPrimaryName("Sam Bell Maxey House")
                .setType(LocationType.POI)
                .setCoordinate(33.6539, -95.555)
                .create()));
        goldStandard.add(new LocationAnnotation(154, "Paris", new LocationBuilder().setPrimaryName("Paris").setType(LocationType.CITY).setCoordinate(33.6625, -95.5477).create()));

        result.add(new LocationAnnotation(0, "Paris", new LocationBuilder().setPrimaryName("Paris").setType(LocationType.CITY).setCoordinate(8.8534, 2.3488).create()));
        result.add(new LocationAnnotation(7, "Texas", new LocationBuilder().setPrimaryName("Texas").setType(LocationType.UNIT).setCoordinate(31.2504, -99.2506).create()));
        result.add(new LocationAnnotation(53, "Dallas", new LocationBuilder().setPrimaryName("Dallas").setType(LocationType.CITY).setCoordinate(32.7758, -96.7967).create()));
        result.add(new LocationAnnotation(63, "Lamar County", new LocationBuilder().setPrimaryName("Dallas").setType(LocationType.UNIT).setCoordinate(33.6668, -95.5836).create()));
        result.add(new LocationAnnotation(77, "Texas", new LocationBuilder().setPrimaryName("Texas").setType(LocationType.UNIT).setCoordinate(31.2504, -99.2506).create()));
        result.add(new LocationAnnotation(91, "United States", new LocationBuilder().setPrimaryName("United States")
                .setType(LocationType.COUNTRY)
                .setCoordinate(37.0902, -95.7129)
                .create()));
        // TODO no coordinate == no annotation? re-think this
        //        result.add(new LocationAnnotation(110, "Sam Bell Maxey House", new LocationBuilder().setPrimaryName("Sam Bell Maxey House").setType(LocationType.POI).setCoordinate(null).create()));
        result.add(new LocationAnnotation(154, "Paris", new LocationBuilder().setPrimaryName("Paris").setType(LocationType.CITY).setCoordinate(8.8534, 2.3488).create()));

        GeoEvaluationResult evaluationResult = new GeoEvaluationResult("test", "/dev/null");
        evaluationResult.addResultFromDocument("test", goldStandard, result);

        // System.out.println(evaluationResult.getSummary());

        assertEquals(4, evaluationResult.getRelevant());
        assertEquals(1, evaluationResult.getCorrect());
        assertEquals(3, evaluationResult.getRetrieved());

        assertEquals(1. / 3, evaluationResult.getPrecision(), DELTA);
        assertEquals(1. / 4, evaluationResult.getRecall(), DELTA);
    }

}
