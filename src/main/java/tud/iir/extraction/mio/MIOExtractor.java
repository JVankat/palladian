package tud.iir.extraction.mio;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;

import tud.iir.extraction.Extractor;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;

public class MIOExtractor extends Extractor {

    private static final Logger logger = Logger.getLogger(MIOExtractor.class);

    private static MIOExtractor instance = null;

    protected static final int MAX_EXTRACTION_THREADS = 3;

    private MIOExtractor() {
    }

    public static MIOExtractor getInstance() {
        if (instance == null) {
            instance = new MIOExtractor();
        }
        return instance;
    }

    /**
     * Start extraction of MIOs for entities that are fetched from the knowledge base. Continue from last extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueFromLastExtraction) {

        logger.info("start MIO extraction");
        // http://www.dcs.shef.ac.uk/~sam/stringmetrics.html#overlap
        // OverlapCoefficient oc = new OverlapCoefficient();
        // QGramsDistance qg = new QGramsDistance();
        // CosineSimilarity cs = new CosineSimilarity();
        // BlockDistance bd = new BlockDistance();;
        // DiceSimilarity ds = new DiceSimilarity();
        // EuclideanDistance ed = new EuclideanDistance();
        // JaccardSimilarity js = new JaccardSimilarity();
        // JaroWinkler jw = new JaroWinkler();
        //        
        // System.out.println(js.getSimilarity("Samsung s8500 Wave", " Das Samsung s8500 Wave ist besser als das Samsung S9500!"));
        // System.out.println(js.getSimilarity("Samsung s8500 Wave", " Das Samsung s8500 Wave!"));
        // System.exit(0);
        // // reset stopped command
        // setStopped(false);
        //		
        // // load concepts and attributes from ontology (and rdb) and to know
        // what
        // // to extract
        // if (!isBenchmark()) {
        KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
        // setKnowledgeManager(km);
        // } else {

        // KnowledgeManager km = new KnowledgeManager();
        // km.createBenchmarkConcepts();
        // km.setCorrectValues();
        // setKnowledgeManager(km);
        // }
        //		
        // // loop until exit called
        // // while (!isStopped()) {
        //
        // concepts
        ArrayList<Concept> concepts1 = km.getConcepts(true);
        CollectionHelper.print(concepts1);
        System.exit(0);
        // TODO?

        // create a new concept without databaseusage
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        Concept exampleConcept = new Concept("mobilePhone");
        Entity exampleEntity_1 = new Entity("Samsung S8500 Wave", exampleConcept);
        // Entity exampleEntity_2 = new Entity("HTC Desire", exampleConcept);
        exampleConcept.addEntity(exampleEntity_1);
        // exampleConcept.addEntity(exampleEntity_2);
        concepts.add(exampleConcept);
        //
        // Concept headphoneConcept = new Concept("headphone");
        // Entity headphone1 = new Entity("Razer Megalodon", headphoneConcept);
        // Entity headphone2 = new Entity("Sennheiser HD800", headphoneConcept);
        // headphoneConcept.addEntity(headphone1);
        // headphoneConcept.addEntity(headphone2);
        // concepts.add(headphoneConcept);

        // loadSearchVocabulary
        ConceptSearchVocabulary searchVoc = loadSearchVocabulary();
        // iterate through all concepts
        for (Concept currentConcept : concepts) {

            System.out.println("Concept: " + currentConcept.getName());

            // load concept-specific SearchVocabulary
            // List<String>
            // conceptVocabularyList=searchVoc.getVocByConceptName(currentConcept.getName());

            if (isStopped()) {
                logger.info("mio extraction process stopped");
                break;
            }

            // iterate through all entities for current concept
            // if (!isBenchmark()) {
            // currentConcept.loadEntities(continueFromLastExtraction);
            // }
            ArrayList<Entity> conceptEntities;
            if (continueFromLastExtraction) {
                conceptEntities = currentConcept.getEntitiesByDate();
            } else {
                conceptEntities = currentConcept.getEntities();
            }

            // wait for a certain time when no entities were found, then
            // restart
            if (conceptEntities.size() == 0) {
                logger.info("no entities for mio extraction, continue with next concept");
                continue;
            }

            ThreadGroup extractionThreadGroup = new ThreadGroup("mioExtractionThreadGroup");

            for (Entity currentEntity : conceptEntities) {

                if (isStopped()) {
                    logger.info("mio extraction process stopped");
                    break;
                }

                currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                logger.info("  start mio extraction process for entity \"" + currentEntity.getName() + "\" (" + currentEntity.getConcept().getName() + ")");
                Thread mioThread = new EntityMIOExtractionThread(extractionThreadGroup, currentEntity.getSafeName() + "MIOExtractionThread", currentEntity,
                        searchVoc);
                mioThread.start();

                logger.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());
                System.out.println("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                    logger.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                    System.out.println("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ")");
                    ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                }

            }
        }

        // // save extraction results after each full loop
        // if (!isBenchmark()) {
        // getKnowledgeManager().saveExtractions();
        // } else {
        // getKnowledgeManager().evaluateBenchmarkExtractions();
        // logger.info("finished benchmark");
        // // break;
        // }
        // }

    }

    @Override
    protected void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            System.out.println("save extractions now");
            getKnowledgeManager().saveExtractions();
        }
    }

    /**
     * load the concept-specific SearchVocabulary from .yml-file
     */
    private ConceptSearchVocabulary loadSearchVocabulary() {
        try {
            ConceptSearchVocabulary cSearchVoc = Yaml.loadType(new File("data/knowledgeBase/conceptSearchVocabulary.yml"), ConceptSearchVocabulary.class);

            return cSearchVoc;
        } catch (FileNotFoundException e) {

            logger.error(e.getMessage());
        }
        return null;
    }

    public boolean isURLallowed(String url) {
        super.addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        super.addSuffixesToBlackList(super.URL_TEXTUAL_BLACKLIST);
        return super.isURLallowed(url);
    }

    public static void main(String[] abc) {
        // Controller.getInstance();

        long t1 = System.currentTimeMillis();
        MIOExtractor mioEx = MIOExtractor.getInstance();

        // mioEx.setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        // se.setBenchmark(true);

        mioEx.startExtraction(false);
        // se.stopExtraction(true);

        // DateHelper.getRuntime(t1, true);
    }

}
