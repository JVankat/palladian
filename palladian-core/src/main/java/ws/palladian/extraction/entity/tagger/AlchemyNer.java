package ws.palladian.extraction.entity.tagger;

import java.util.*;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * 
 * <p>
 * The Alchemy service for Named Entity Recognition. This class uses the Alchemy API and therefore requires the
 * application to have access to the Internet.<br>
 * <a href="http://www.alchemyapi.com/api/entity/textc.html">http://www.alchemyapi.com/api/entity/textc.html</a>
 * </p>
 * 
 * <p>
 * Alchemy can recognize the following entities:<br>
 * <ul>
 * <li>Anniversary</li>
 * <li>City</li>
 * <li>Company</li>
 * <li>Continent</li>
 * <li>Country</li>
 * <li>EntertainmentAward</li>
 * <li>Facility</li>
 * <li>FieldTerminology</li>
 * <li>FinancialMarketIndex</li>
 * <li>GeographicFeature</li>
 * <li>HealthCondition</li>
 * <li>Holiday</li>
 * <li>Movie</li>
 * <li>MusicGroup</li>
 * <li>NaturalDisaster</li>
 * <li>Organization</li>
 * <li>Person</li>
 * <li>PrintMedia</li>
 * <li>RadioProgram</li>
 * <li>RadioStation</li>
 * <li>Region</li>
 * <li>Sport</li>
 * <li>StateOrCounty</li>
 * <li>Technology</li>
 * <li>TelevisionShow</li>
 * <li>TelevisionStation</li>
 * <li>AircraftManufacturer</li>
 * <li>Airline</li>
 * <li>AirportOperator</li>
 * <li>ArchitectureFirm</li>
 * <li>AutomobileCompany</li>
 * <li>BicycleManufacturer</li>
 * <li>BottledWater</li>
 * <li>BreweryBrandOfBeer</li>
 * <li>BroadcastDistributor</li>
 * <li>CandyBarManufacturer</li>
 * <li>ComicBookPublisher</li>
 * <li>ComputerManufacturerBrand</li>
 * <li>Distillery</li>
 * <li>EngineeringFirm</li>
 * <li>FashionLabel</li>
 * <li>FilmCompany</li>
 * <li>FilmDistributor</li>
 * <li>GamePublisher</li>
 * <li>ManufacturingPlant</li>
 * <li>MusicalInstrumentCompany</li>
 * <li>OperatingSystemDeveloper</li>
 * <li>ProcessorManufacturer</li>
 * <li>ProductionCompany</li>
 * <li>RadioNetwork</li>
 * <li>RecordLabel</li>
 * <li>Restaurant</li>
 * <li>RocketEngineDesigner</li>
 * <li>RocketManufacturer</li>
 * <li>ShipBuilder</li>
 * <li>SoftwareDeveloper</li>
 * <li>SpacecraftManufacturer</li>
 * <li>SpiritBottler</li>
 * <li>SpiritProductManufacturer</li>
 * <li>TransportOperator</li>
 * <li>TVNetwork</li>
 * <li>VentureFundedCompany</li>
 * <li>VentureInvestor</li>
 * <li>VideoGameDeveloper</li>
 * <li>VideoGameEngineDeveloper</li>
 * <li>VideoGamePublisher</li>
 * <li>WineProducer</li>
 * <li>Airport</li>
 * <li>Bridge</li>
 * <li>HistoricPlace</li>
 * <li>Hospital</li>
 * <li>Lighthouse</li>
 * <li>ShoppingMall</li>
 * <li>SkiArea</li>
 * <li>Skyscraper</li>
 * <li>Stadium</li>
 * <li>Station</li>
 * <li>BodyOfWater</li>
 * <li>Cave</li>
 * <li>GeologicalFormation</li>
 * <li>Glacier</li>
 * <li>Island</li>
 * <li>IslandGroup</li>
 * <li>Lake</li>
 * <li>Mountain</li>
 * <li>MountainPass</li>
 * <li>MountainRange</li>
 * <li>OilField</li>
 * <li>Park</li>
 * <li>ProtectedArea</li>
 * <li>River</li>
 * <li>Waterfall</li>
 * <li>Cave</li>
 * <li>Island</li>
 * <li>Lake</li>
 * <li>Mountain</li>
 * <li>Park</li>
 * <li>ProtectedArea</li>
 * <li>River</li>
 * <li>TropicalCyclone</li>
 * <li>AstronomicalSurveyProjectOrganization</li>
 * <li>AwardPresentingOrganization</li>
 * <li>Club</li>
 * <li>CollegeUniversity</li>
 * <li>CricketAdministrativeBody</li>
 * <li>FinancialSupportProvider</li>
 * <li>FootballOrganization</li>
 * <li>FraternitySorority</li>
 * <li>GovernmentAgency</li>
 * <li>LegislativeCommittee</li>
 * <li>Legislature</li>
 * <li>MartialArtsOrganization</li>
 * <li>MembershipOrganization</li>
 * <li>NaturalOrCulturalPreservationAgency</li>
 * <li>Non-ProfitOrganisation</li>
 * <li>OrganizationCommittee</li>
 * <li>PeriodicalPublisher</li>
 * <li>PoliticalParty</li>
 * <li>ReligiousOrder</li>
 * <li>ReligiousOrganization</li>
 * <li>ReportIssuingInstitution</li>
 * <li>SoccerClub</li>
 * <li>SpaceAgency</li>
 * <li>SportsAssociation</li>
 * <li>StudentOrganization</li>
 * <li>TopLevelDomainRegistry</li>
 * <li>TradeUnion</li>
 * <li>FootballTeam</li>
 * <li>HockeyTeam</li>
 * <li>Legislature</li>
 * <li>MilitaryUnit</li>
 * <li>Non-ProfitOrganisation</li>
 * <li>RecordLabel</li>
 * <li>School</li>
 * <li>SoccerClub</li>
 * <li>TradeUnion</li>
 * <li>Academic</li>
 * <li>AircraftDesigner</li>
 * <li>Appointee</li>
 * <li>Architect</li>
 * <li>ArchitectureFirmPartner</li>
 * <li>Astronaut</li>
 * <li>Astronomer</li>
 * <li>Author</li>
 * <li>AutomotiveDesigner</li>
 * <li>AwardJudge</li>
 * <li>AwardNominee</li>
 * <li>AwardWinner</li>
 * <li>BasketballCoach</li>
 * <li>BasketballPlayer</li>
 * <li>Bassist</li>
 * <li>Blogger</li>
 * <li>BoardMember</li>
 * <li>Boxer</li>
 * <li>BroadcastArtist</li>
 * <li>Celebrity</li>
 * <li>Chef</li>
 * <li>ChessPlayer</li>
 * <li>ChivalricOrderFounder</li>
 * <li>ChivalricOrderMember</li>
 * <li>ChivalricOrderOfficer</li>
 * <li>Collector</li>
 * <li>ComicBookColorist</li>
 * <li>ComicBookCreator</li>
 * <li>ComicBookEditor</li>
 * <li>ComicBookInker</li>
 * <li>ComicBookLetterer</li>
 * <li>ComicBookPenciler</li>
 * <li>ComicBookWriter</li>
 * <li>ComicStripArtist</li>
 * <li>ComicStripCharacter</li>
 * <li>ComicStripCreator</li>
 * <li>CompanyAdvisor</li>
 * <li>CompanyFounder</li>
 * <li>CompanyShareholder</li>
 * <li>Composer</li>
 * <li>ComputerDesigner</li>
 * <li>ComputerScientist</li>
 * <li>ConductedEnsemble</li>
 * <li>Conductor</li>
 * <li>CricketBowler</li>
 * <li>CricketCoach</li>
 * <li>CricketPlayer</li>
 * <li>CricketUmpire</li>
 * <li>Cyclist</li>
 * <li>Dedicatee</li>
 * <li>Dedicator</li>
 * <li>Deity</li>
 * <li>DietFollower</li>
 * <li>DisasterSurvivor</li>
 * <li>DisasterVictim</li>
 * <li>Drummer</li>
 * <li>ElementDiscoverer</li>
 * <li>FashionDesigner</li>
 * <li>FictionalCreature</li>
 * <li>FictionalUniverseCreator</li>
 * <li>FilmActor</li>
 * <li>FilmArtDirector</li>
 * <li>FilmCastingDirector</li>
 * <li>FilmCharacter</li>
 * <li>FilmCinematographer</li>
 * <li>FilmCostumerDesigner</li>
 * <li>FilmCrewmember</li>
 * <li>FilmCritic</li>
 * <li>FilmDirector</li>
 * <li>FilmEditor</li>
 * <li>FilmMusicContributor</li>
 * <li>FilmProducer</li>
 * <li>FilmProductionDesigner</li>
 * <li>FilmSetDesigner</li>
 * <li>FilmTheorist</li>
 * <li>FilmWriter</li>
 * <li>FootballCoach</li>
 * <li>FootballPlayer</li>
 * <li>FootballReferee</li>
 * <li>FootballTeamManager</li>
 * <li>FoundingFigure</li>
 * <li>GameDesigner</li>
 * <li>Golfer</li>
 * <li>Guitarist</li>
 * <li>HallOfFameInductee</li>
 * <li>Hobbyist</li>
 * <li>HockeyCoach</li>
 * <li>HockeyPlayer</li>
 * <li>HonoraryDegreeRecipient</li>
 * <li>Illustrator</li>
 * <li>Interviewer</li>
 * <li>Inventor</li>
 * <li>LandscapeArchitect</li>
 * <li>LanguageCreator</li>
 * <li>Lyricist</li>
 * <li>MartialArtist</li>
 * <li>MilitaryCommander</li>
 * <li>MilitaryPerson</li>
 * <li>Monarch</li>
 * <li>Mountaineer</li>
 * <li>MusicalArtist</li>
 * <li>MusicalGroupMember</li>
 * <li>NoblePerson</li>
 * <li>NobleTitle</li>
 * <li>OlympicAthlete</li>
 * <li>OperaCharacter</li>
 * <li>OperaDirector</li>
 * <li>OperaLibretto</li>
 * <li>OperaSinger</li>
 * <li>PeriodicalEditor</li>
 * <li>Physician</li>
 * <li>PoliticalAppointer</li>
 * <li>Politician</li>
 * <li>ProAthlete</li>
 * <li>ProgrammingLanguageDesigner</li>
 * <li>ProgrammingLanguageDeveloper</li>
 * <li>ProjectParticipant</li>
 * <li>RecordingEngineer</li>
 * <li>RecordProducer</li>
 * <li>ReligiousLeader</li>
 * <li>SchoolFounder</li>
 * <li>ShipDesigner</li>
 * <li>Songwriter</li>
 * <li>SportsLeagueAwardWinner</li>
 * <li>SportsOfficial</li>
 * <li>Surgeon</li>
 * <li>TennisPlayer</li>
 * <li>TennisTournamentChampion</li>
 * <li>TheaterActor</li>
 * <li>TheaterCharacter</li>
 * <li>TheaterChoreographer</li>
 * <li>TheaterDesigner</li>
 * <li>TheaterDirector</li>
 * <li>TheaterProducer</li>
 * <li>TheatricalComposer</li>
 * <li>TheatricalLyricist</li>
 * <li>Translator</li>
 * <li>TVActor</li>
 * <li>TVCharacter</li>
 * <li>TVDirector</li>
 * <li>TVPersonality</li>
 * <li>TVProducer</li>
 * <li>TVProgramCreator</li>
 * <li>TVWriter</li>
 * <li>U.S.Congressperson</li>
 * <li>USPresident</li>
 * <li>USVicePresident</li>
 * <li>VideoGameActor</li>
 * <li>VideoGameDesigner</li>
 * <li>VisualArtist</li>
 * <li>Actor</li>
 * <li>Architect</li>
 * <li>Astronaut</li>
 * <li>Athlete</li>
 * <li>BritishRoyalty</li>
 * <li>Cardinal</li>
 * <li>ChristianBishop</li>
 * <li>CollegeCoach</li>
 * <li>Comedian</li>
 * <li>ComicsCreator</li>
 * <li>Congressman</li>
 * <li>Criminal</li>
 * <li>FootballManager</li>
 * <li>Journalist</li>
 * <li>MilitaryPerson</li>
 * <li>Model</li>
 * <li>Monarch</li>
 * <li>MusicalArtist</li>
 * <li>Philosopher</li>
 * <li>Politician</li>
 * <li>Saint</li>
 * <li>Scientist</li>
 * <li>Writer</li>
 * <li>Magazine</li>
 * <li>Newspaper</li>
 * <li>SchoolNewspaper</li>
 * <li>EnglishRegion</li>
 * <li>FrenchRegion</li>
 * <li>ItalianRegion</li>
 * <li>VideoGameRegion</li>
 * <li>WineRegion</li>
 * <li>MartialArt</li>
 * <li>PoliticalDistrict</li>
 * <li>AdministrativeDivision</li>
 * <li>GovernmentalJurisdiction</li>
 * </ul>
 * </p>
 * 
 * @see <a href="http://www.alchemyapi.com/api/entity/types.html">http://www.alchemyapi.com/api/entity/types.html</a>
 * @author David Urbansky
 */
public class AlchemyNer extends NamedEntityRecognizer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AlchemyNer.AlchemyAnnotation.class);

    /**
     * <p>
     * Specific {@link Annotation}, which provides access to the sub types delivered from Alchemy.
     * </p>
     */
    public static final class AlchemyAnnotation extends ImmutableAnnotation {

        private final List<String> subtypes;

        public AlchemyAnnotation(int startPosition, String value, String tag, List<String> subtypes) {
            super(startPosition, value, tag);
            this.subtypes = subtypes;
        }

        public List<String> getSubtypes() {
            return Collections.unmodifiableList(subtypes);
        }

    }

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.alchemy.key";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    /** The maximum number of characters allowed to send per request. */
    private final int MAXIMUM_TEXT_LENGTH = 15000;

    /** Turns coreference resolution on/off. */
    private boolean coreferenceResolution = false;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    /**
     * <p>
     * Create a new {@link AlchemyNer} with an API key provided by the supplied {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The configuration providing the API key via {@value #CONFIG_API_KEY}, not <code>null</code>.
     */
    public AlchemyNer(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link AlchemyNer} with the specified API key.
     * </p>
     * 
     * @param apiKey The API key to use for connecting with Alchemy API, not <code>null</code> or empty.
     */
    public AlchemyNer(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    public void setCoreferenceResolution(boolean value) {
        coreferenceResolution = value;
    }

    @Override
    public List<AlchemyAnnotation> getAnnotations(String inputText) {

        Annotations<AlchemyAnnotation> annotations = new Annotations<AlchemyAnnotation>();
        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);
        LOGGER.debug("sending {} text chunks, total text length {}", textChunks.size(), inputText.length());

        Set<String> checkedEntities = new HashSet<>();
        for (String textChunk : textChunks) {

            try {
                HttpResult httpResult = getHttpResult(textChunk.toString());
                String response = httpResult.getStringContent();
                if (response.contains("daily-transaction-limit-exceeded")) {
                    throw new IllegalStateException("API daily transaction limit exceeded");
                }
                JsonObject json = new JsonObject(response);
                JsonArray entities = json.getJsonArray("entities");
                for (int i = 0; i < entities.size(); i++) {
                    JsonObject entity = entities.getJsonObject(i);
                    String entityName = entity.getString("text");
                    String entityType = entity.getString("type");
                    List<String> subTypeList = new ArrayList<>();
                    if (entity.get("disambiguated") != null) {
                        JsonObject disambiguated = entity.getJsonObject("disambiguated");
                        if (disambiguated.get("subType") != null) {
                            JsonArray subTypes = disambiguated.getJsonArray("subType");
                            for (int j = 0; j < subTypes.size(); j++) {
                                subTypeList.add(subTypes.getString(j));
                            }
                        }
                    }
                    // skip entities that have been processed already
                    if (!checkedEntities.add(entityName)) {
                        continue;
                    }
                    // get locations of named entity
                    List<Integer> entityOffsets = NerHelper.getEntityOffsets(inputText, entityName);
                    for (Integer offset : entityOffsets) {
                        annotations.add(new AlchemyAnnotation(offset, entityName, entityType, subTypeList));
                    }
                }
            } catch (HttpException e) {
                throw new IllegalStateException("Error performing HTTP POST", e);
            } catch (JsonException e) {
                throw new IllegalStateException("Error while parsing JSON response", e);
            }
        }
        annotations.sort();
        return annotations;
    }

    @Override
    public String getName() {
        return "Alchemy API NER";
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST,
                "http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");
        request.addParameter("text", inputText);
        request.addParameter("apikey", apiKey);
        request.addParameter("outputMode", "json");
        request.addParameter("disambiguate", "1");
        request.addParameter("maxRetrieve", "500");
        request.addParameter("coreference", coreferenceResolution ? "1" : "0");
        return httpRetriever.execute(request);
    }

    public static void main(String[] args) {

        AlchemyNer tagger = new AlchemyNer("");

        // // HOW TO USE ////
        System.out
                .println(tagger
                        .tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. Some of them are also made in Salt Lake City or Cameron."));
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");
        System.exit(0);

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

    }

}
