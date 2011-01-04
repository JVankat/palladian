package tud.iir.extraction.snippet;

import java.util.Date;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.extraction.ExtractionProcessManager;
import tud.iir.extraction.Extractor;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.KnowledgeManager;
import tud.iir.persistence.DatabaseManager;

/**
 * <p>
 * The SnippetExtractor class extends the Extractor singleton class, retrieves all entities from the knowledge base and
 * schedules k thread runs in parallel, where k is the number of entities.
 * </p>
 * 
 * <p>
 * For each entity a separate thread is started. Each thread is a subclass of EntitySnippetExtractionThread. To avoid
 * overloading the system, a threading queue allows to only run i threads in parallel.
 * </p>
 * 
 * @author David Urbanksy
 * @author Christopher Friedrich
 */
public class SnippetExtractor extends Extractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SnippetExtractor.class);

    /** The maximum number of extraction threads. */
    protected static final int MAX_EXTRACTION_THREADS = 10;

    /** The number of results that should be retrieved per snippet query. */
    public static final int RESULTS_PER_SNIPPET = 20;

    /** Number of events to extracts before saving them. */
    private static final int SAVE_COUNT = 5;

    /** The path to the part-of-speech model. */
    static String POS_MODEL_PATH;

    private SnippetExtractor() {

        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            POS_MODEL_PATH = config.getString("models.lingpipe.en.postag");

        } else {
            POS_MODEL_PATH = "";
        }

    }

    static class SingletonHolder {
        static SnippetExtractor instance = new SnippetExtractor();
    }

    public static SnippetExtractor getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * Start extraction of snippets for entities that are fetched from the knowledge base. Continue from last
     * extraction.
     */
    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueFromLastExtraction) {

        LOGGER.info("start snippet extraction");

        // reset stopped command
        setStopped(false);

        // load concepts and attributes from ontology (and rdb) and to know what to extract
        if (!isBenchmark()) {
            KnowledgeManager km = DatabaseManager.getInstance().loadOntology();
            setKnowledgeManager(km);
        } else {
            KnowledgeManager km = new KnowledgeManager();
            km.createSnippetBenchmarks();
            setKnowledgeManager(km);
        }

        // loop until exit called
        while (!isStopped()) {

            // concepts
            List<Concept> concepts = knowledgeManager.getConcepts(true); // TODO?

            // iterate through all concepts
            for (Concept currentConcept : concepts) {

                LOGGER.info("Concept: " + currentConcept.getName());

                if (isStopped()) {
                    LOGGER.info("snippet extraction process stopped");
                    break;
                }

                // iterate through all entities for current concept
                if (!isBenchmark()) {
                    currentConcept.loadEntities(continueFromLastExtraction);
                }
                List<Entity> conceptEntities;
                if (continueFromLastExtraction) {
                    conceptEntities = currentConcept.getEntitiesByDate();
                } else {
                    conceptEntities = currentConcept.getEntities();
                }

                // wait for a certain time when no entities were found, then restart
                if (conceptEntities.size() == 0) {
                    LOGGER.info("no entities for snippet extraction, continue with next concept");
                    continue;
                }

                extractionThreadGroup = new ThreadGroup("snippetExtractionThreadGroup");

                for (Entity currentEntity : conceptEntities) {

                    if (isStopped()) {
                        LOGGER.info("snippet extraction process stopped");
                        break;
                    }

                    // update live status
                    ExtractionProcessManager.liveStatus.setCurrentAction("Search for snippets for "
                            + currentEntity.getName() + " (" + currentConcept.getName() + ")");

                    currentEntity.setLastSearched(new Date(System.currentTimeMillis()));

                    LOGGER.info("  start snippet extraction process for entity \"" + currentEntity.getName() + "\" ("
                            + currentEntity.getConcept().getName() + ")");

                    Thread snippetThread = new EntitySnippetExtractionThread(extractionThreadGroup,
                            currentEntity.getSafeName() + "SnippetExtractionThread", currentEntity);
                    snippetThread.start();

                    LOGGER.info("THREAD STARTED (" + getThreadCount() + "): " + currentEntity.getName());

                    int c = 0;
                    while (getThreadCount() >= MAX_EXTRACTION_THREADS) {
                        LOGGER.info("NEED TO WAIT FOR FREE THREAD SLOT (" + getThreadCount() + ") "
                                + extractionThreadGroup.activeCount() + "," + extractionThreadGroup.activeGroupCount());

                        if (extractionThreadGroup.activeCount() + extractionThreadGroup.activeGroupCount() == 0) {
                            LOGGER.warn("apparently " + getThreadCount()
                                    + " threads have not finished correctly but thread group is empty, continuing...");
                            resetThreadCount();
                            break;
                        }

                        ThreadHelper.sleep(WAIT_FOR_FREE_THREAD_SLOT);
                        if (isStopped()) {
                            c++;
                        }

                        if (c > 25) {
                            LOGGER.info("waited 25 iterations after stop has been called, breaking now");
                            break;
                        }
                    }

                    if (getExtractedSnippetCount() == SAVE_COUNT) {
                        saveExtractions(true);
                    }

                }
            }

            // save extraction results after each full loop
            if (!isBenchmark()) {
                getKnowledgeManager().saveExtractions();
            } else {
                getKnowledgeManager().evaluateBenchmarkExtractions();
                LOGGER.info("finished benchmark");
            }

            if (isStopped()) {
                break;
            }
        }
    }

    private int getExtractedSnippetCount() {

        int snippetCount = 0;

        for (Concept concept : getKnowledgeManager().getConcepts()) {
            for (Entity entity : concept.getEntities()) {
                snippetCount += entity.getSnippets().size();
            }
        }

        return snippetCount;
    }

    @Override
    protected void saveExtractions(boolean saveResults) {
        if (saveResults && !isBenchmark()) {
            LOGGER.info("save snippet extractions now");
            getKnowledgeManager().saveExtractions();
        }
    }

    public static void main(String[] abc) {
        // Controller.getInstance();

        long t1 = System.currentTimeMillis();
        SnippetExtractor se = SnippetExtractor.getInstance();
        se.setKnowledgeManager(DatabaseManager.getInstance().loadOntology());
        // se.setBenchmark(true);
        se.startExtraction(false);
        // se.stopExtraction(true);
        DateHelper.getRuntime(t1, System.currentTimeMillis(), true);
    }
}