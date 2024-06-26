package ws.palladian.extraction.entity;

import org.junit.Test;
import ws.palladian.core.Annotation;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class StringTaggerTest {

    @Test
    public void testTagString() {

        List<Annotation> annotations = null;
        String text = "";
        StringTagger tagger = StringTagger.INSTANCE;

        // abbreviations
        text = "the United States of America (USA) are often called the USA, the U.S.A., or simply the U.S., the U.S.S. Enterprise is a space ship.";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(6, annotations.size());
        assertEquals("United States of America", annotations.get(0).getValue());
        assertEquals("USA", annotations.get(1).getValue());
        assertEquals("USA", annotations.get(2).getValue());
        assertEquals("U.S.A.", annotations.get(3).getValue());
        assertEquals("U.S.", annotations.get(4).getValue());
        assertEquals("U.S.S. Enterprise", annotations.get(5).getValue());

        text = "The outfit that stages the festival, Black Rock City LLC, is now a $23 million-per-year concern with 40 full-time employees, hundreds of volunteers, and a non-profit arts foundation that doles out grants.";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(2, annotations.size());
        assertEquals("Black Rock City LLC", annotations.get(1).getValue());

        // names
        text = "Mr. Yakomoto, John J. Smith, and Bill Drody cooperate with T. Shéff, L.Carding, T.O'Brian, Harry O'Sullivan and O'Brody. they are partying on Saturday's night special, Friday's Night special or THURSDAY'S, in St. Petersburg there is Dr. Mark Litwin";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(14, annotations.size());
        assertEquals("Mr. Yakomoto", annotations.get(0).getValue());
        assertEquals("John J. Smith", annotations.get(1).getValue());
        assertEquals("Bill Drody", annotations.get(2).getValue());
        assertEquals("T. Shéff", annotations.get(3).getValue());
        assertEquals("L.Carding", annotations.get(4).getValue());
        assertEquals("T.O'Brian", annotations.get(5).getValue());
        assertEquals("Harry O'Sullivan", annotations.get(6).getValue());
        assertEquals("O'Brody", annotations.get(7).getValue());
        assertEquals("Saturday", annotations.get(8).getValue());
        assertEquals("Friday", annotations.get(9).getValue());
        assertEquals("Night", annotations.get(10).getValue());
        assertEquals("THURSDAY", annotations.get(11).getValue());
        assertEquals("St. Petersburg", annotations.get(12).getValue());
        assertEquals("Dr. Mark Litwin", annotations.get(13).getValue());
        // assertEquals("Google Inc.", annotations.get(12).getValue());

        // long names
        text = "Mayor Bobby E. Horton called the Special Council Meeting of the West Columbia Council to order on Monday, March 16, 2009 at 6 pm.";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(5, annotations.size());
        assertEquals("Mayor Bobby E. Horton", annotations.get(0).getValue());

        // composites
        text = "Dolce & Gabana as well as S&P are companies.";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(2, annotations.size());
        assertEquals("Dolce & Gabana", annotations.get(0).getValue());
        assertEquals("S&P", annotations.get(1).getValue());

        // containing numbers TODO make work, code in Stringtagger.tagString2 before revision r1952
        // taggedText =
        // "the Interstate 80 is dangerous, the Sony Playstation 3 looks more stylish than Microsoft's Xbox 360. the 1961 Ford Mustang is fast, H2 database just 30 ist not to tag though";
        //
        // taggedText = StringTagger.tagString2(taggedText);
        // annotations = FileFormatParser.getAnnotationsFromXMLText(taggedText);
        // CollectionHelper.print(annotations);
        //
        // assertEquals(6, annotations.size());
        // assertEquals("Interstate 80", annotations.get(0).getValue());
        // assertEquals("Sony Playstation 3", annotations.get(1).getValue());
        // assertEquals("Microsoft", annotations.get(2).getValue());
        // assertEquals("Xbox 360", annotations.get(3).getValue());
        // assertEquals("1961 Ford Mustang", annotations.get(4).getValue());
        // assertEquals("H2", annotations.get(5).getValue());

        // fill words
        text = "the Republic of Ireland, and Return of King Arthur, the National Bank of Scotland, Erin Purcell of Boston-based Reagan Communications";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(6, annotations.size());
        assertEquals("Republic of Ireland", annotations.get(0).getValue());
        assertEquals("Return of King Arthur", annotations.get(1).getValue());
        assertEquals("National Bank of Scotland", annotations.get(2).getValue());
        assertEquals("Erin Purcell", annotations.get(3).getValue());
        assertEquals("Boston-based", annotations.get(4).getValue());
        assertEquals("Reagan Communications", annotations.get(5).getValue());

        // dashes
        text = "Maria-Hillary Johnson lives on Chester-le-Street and Ontario-based Victor Vool, the All-England Club and Patricia Djate-Taillard were in the United Nations-sponsored ceasfire with St. Louis-based NFL coach trains in MG-Gym (MG-GYM), the Real- Rumble, TOTALLY FREE- Choice, Australia-- Germany";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(17, annotations.size());
        assertEquals("Maria-Hillary Johnson", annotations.get(0).getValue());
        assertEquals("Chester-le-Street", annotations.get(1).getValue());
        assertEquals("Ontario-based", annotations.get(2).getValue());
        assertEquals("Victor Vool", annotations.get(3).getValue());
        assertEquals("All-England Club", annotations.get(4).getValue());
        assertEquals("Patricia Djate-Taillard", annotations.get(5).getValue());
        assertEquals("United Nations-sponsored", annotations.get(6).getValue());
        assertEquals("St. Louis-based", annotations.get(7).getValue());
        assertEquals("NFL", annotations.get(8).getValue());
        assertEquals("MG-Gym", annotations.get(9).getValue());
        assertEquals("MG-GYM", annotations.get(10).getValue());
        assertEquals("Real", annotations.get(11).getValue());
        assertEquals("Rumble", annotations.get(12).getValue());
        assertEquals("TOTALLY FREE", annotations.get(13).getValue());
        assertEquals("Choice", annotations.get(14).getValue());
        assertEquals("Australia", annotations.get(15).getValue());
        assertEquals("Germany", annotations.get(16).getValue());

        // apostrophes
        text = "Early in 1939, Georgia O’Keeffe, the artist most famous for depicting the arid Southwest, suddenly decided to paint America’s diametrically opposite landscape — the lush tropical valleys of Hawaii.";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);
        assertEquals(5, annotations.size());
        assertEquals("Early", annotations.get(0).getValue());
        assertEquals("Georgia O’Keeffe", annotations.get(1).getValue());
        assertEquals("Southwest", annotations.get(2).getValue());
        assertEquals("America", annotations.get(3).getValue());
        assertEquals("Hawaii", annotations.get(4).getValue());

        text = "The Adam Opel GmbH unit is based in Germany.";
        annotations = tagger.getAnnotations(text);
        assertEquals("The Adam Opel GmbH", annotations.get(0).getValue());
        // CollectionHelper.print(annotations);

        text = "In 2009, GM scrapped a plan — favored by Berlin — to sell a majority stake to a consortium led by Canadian auto parts maker Magna International Inc. and decided to restructure the brands itself instead.";
        annotations = tagger.getAnnotations(text);
        assertEquals("Magna International Inc.", annotations.get(4).getValue());
        // CollectionHelper.print(annotations);

        text = "General Motors Co.'s Opel unit said Monday that it plans to end car production at one plant in Germany in 2016, but a slimmed-down factory may continue to make components.";
        annotations = tagger.getAnnotations(text);
        assertEquals("General Motors Co.", annotations.get(0).getValue());
        // CollectionHelper.print(annotations);

        // starting small and camel case
        text = "the last ex-England, mid-SCORER player, al-Rama is a person Rami al-Sadani, the iPhone 4 is a phone. Veronica Swenston VENICE alternative Frank HERALD which was found at Universal Orlando® Resort";

        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        assertEquals(10, annotations.size());
        assertEquals("ex-England", annotations.get(0).getValue());
        assertEquals("mid-SCORER", annotations.get(1).getValue());
        assertEquals("al-Rama", annotations.get(2).getValue());
        assertEquals("Rami al-Sadani", annotations.get(3).getValue());
        assertEquals("iPhone 4", annotations.get(4).getValue());
        assertEquals("Veronica Swenston", annotations.get(5).getValue());
        assertEquals("VENICE", annotations.get(6).getValue());
        assertEquals("Frank", annotations.get(7).getValue());
        assertEquals("HERALD", annotations.get(8).getValue());
        assertEquals("Universal Orlando® Resort", annotations.get(9).getValue());

        text = "Sterling Cooper is located on Madison Ave. in New York City.";
        annotations = tagger.getAnnotations(text);
        assertEquals(3, annotations.size());
        assertEquals("Madison Ave.", annotations.get(1).getValue());

        // accents
        text = "the city is called Yaoundé and that's a fact";
        annotations = tagger.getAnnotations(text);
        assertEquals(1, annotations.size());
        assertEquals("Yaoundé", annotations.get(0).getValue());

        // XXX see https://bitbucket.org/palladian/palladian/issue/205/stringtagger-and-initials
        text = "Mayor Bobby E. Horton called the Special Council Meeting of the West Columbia Council to order on Monday, March 16, 2009 at 6 pm.";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        // XXX
        text = "ST. PARIS, Ohio";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        // XXX
        text = "McDonald's, McDonald’s, Kentucky Fried Chicken and a Pizza Hut!";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        // XXX
        text = "3 Ind. fugitives transferred to another Neb. jail.";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        // XXX
        text = "Sahuyé is a town and commune in Côte d'Ivoire.";
        annotations = tagger.getAnnotations(text);
        // CollectionHelper.print(annotations);

        // XXX
        text = "Marina del Rey is an affluent unincorporated seaside community and census-designated place (CDP) in Los Angeles County, California.";
        annotations = tagger.getAnnotations(text);
        // should extract "Marina del Rey"
        // CollectionHelper.print(annotations);

        // note, that there is a non-breaking space in between "West Virginia"; this needs dedicted treatment.
        text = "Up to 300,000 people in West Virginia have been told not to drink tap water after the spill of a dangerous chemical sparked a federal emergency.";
        annotations = tagger.getAnnotations(text);
        assertEquals(2, annotations.size());
        assertEquals("West Virginia", annotations.get(1).getValue());
        // CollectionHelper.print(annotations);

        text = "A Short History of L’Arc de Triomphe.";
        annotations = tagger.getAnnotations(text);
        assertEquals(2, annotations.size());
        assertEquals("L’Arc de Triomphe", annotations.get(1).getValue());
    }

}
