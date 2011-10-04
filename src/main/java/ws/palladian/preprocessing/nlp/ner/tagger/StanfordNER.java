package ws.palladian.preprocessing.nlp.ner.tagger;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.preprocessing.nlp.ner.Annotations;
import ws.palladian.preprocessing.nlp.ner.FileFormatParser;
import ws.palladian.preprocessing.nlp.ner.NamedEntityRecognizer;
import ws.palladian.preprocessing.nlp.ner.TaggingFormat;
import ws.palladian.preprocessing.nlp.ner.evaluation.EvaluationResult;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.StringUtils;

/**
 * <p>
 * This class wraps the Stanford Named Entity Recognizer which is based on conditional random fields (CRF).<br>
 * The NER has been described in the following paper:
 * </p>
 * 
 * <p>
 * The following models exist already for this recognizer:
 * <ul>
 * <li>Person</li>
 * <li>Location</li>
 * <li>Organization</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Jenny Rose Finkel, Trond Grenager, and Christopher Manning<br>
 * "Incorporating Non-local Information into Information Extraction Systems", 2005<br>
 * Proceedings of the 43nd Annual Meeting of the Association for Computational Linguistics (ACL 2005), pp. 363-370<br>
 * <a href="http://nlp.stanford.edu/~manning/papers/gibbscrf3.pdf">Read Paper</a>
 * </p>
 * 
 * <p>
 * See also <a
 * href="http://www-nlp.stanford.edu/software/crf-faq.shtml">http://www-nlp.stanford.edu/software/crf-faq.shtml</a>
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class StanfordNER extends NamedEntityRecognizer {

    /** Hold the configuration settings here instead of a file. */
    private String configFileContent = "";

    public StanfordNER() {
        setName("Stanford NER");
        buildConfigFile();

    }

    private void buildConfigFile() {
        configFileContent = "";
        configFileContent += "#location of the training file" + "\n";
        configFileContent += "trainFile = ###TRAINING_FILE###" + "\n";
        configFileContent += "#location where you would like to save (serialize to) your" + "\n";
        configFileContent += "#classifier; adding .gz at the end automatically gzips the file," + "\n";
        configFileContent += "#making it faster and smaller" + "\n";
        configFileContent += "serializeTo = ###MODEL_FILE###" + "\n";
        configFileContent += "#structure of your training file; this tells the classifier" + "\n";
        configFileContent += "#that the word is in column 0 and the correct answer is in" + "\n";
        configFileContent += "#column 1" + "\n";
        configFileContent += "map = word=0,answer=1" + "\n";
        configFileContent += "#these are the features we'd like to train with" + "\n";
        configFileContent += "#some are discussed below, the rest can be" + "\n";
        configFileContent += "#understood by looking at NERFeatureFactory" + "\n";
        configFileContent += "useClassFeature=true" + "\n";
        configFileContent += "useWord=true" + "\n";
        configFileContent += "useNGrams=true" + "\n";
        configFileContent += "#no ngrams will be included that do not contain either the" + "\n";
        configFileContent += "#beginning or end of the word" + "\n";
        configFileContent += "noMidNGrams=true" + "\n";
        configFileContent += "useDisjunctive=true" + "\n";
        configFileContent += "maxNGramLeng=6" + "\n";
        configFileContent += "usePrev=true" + "\n";
        configFileContent += "useNext=true" + "\n";
        configFileContent += "useSequences=true" + "\n";
        configFileContent += "usePrevSequences=true" + "\n";
        configFileContent += "maxLeft=1" + "\n";
        configFileContent += "#the next 4 deal with word shape features" + "\n";
        configFileContent += "useTypeSeqs=true" + "\n";
        configFileContent += "useTypeSeqs2=true" + "\n";
        configFileContent += "useTypeySequences=true" + "\n";
        configFileContent += "wordShape=chris2useLC";
    }

    public void demo(String inputText) throws IOException {

        String serializedClassifier = "data/temp/stanfordner/classifiers/ner-eng-ie.crf-3-all2008.ser.gz";

        AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);

        String inputTextPath = "data/temp/inputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        /*
         * For either a file to annotate or for the hardcoded text example,
         * this demo file shows two ways to process the output, for teaching
         * purposes. For the file, it shows both how to run NER on a String
         * and how to run it on a whole file. For the hard-coded String,
         * it shows how to run it on a single sentence, and how to do this
         * and produce an inline XML output format.
         */
        if (inputTextPath.length() > 1) {
            String fileContents = StringUtils.slurpFile(inputTextPath);
            List<List<CoreLabel>> out = classifier.classify(fileContents);
            for (List<CoreLabel> sentence : out) {
                for (CoreLabel word : sentence) {
                    LOGGER.debug(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
                }
            }
            out = classifier.classifyFile(inputTextPath);
            for (List<CoreLabel> sentence : out) {
                for (CoreLabel word : sentence) {
                    LOGGER.debug(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
                }
            }

        } else {
            String s1 = "Good afternoon Rajat Raina, how are you today?";
            String s2 = "I go to school at Stanford University, which is located in California.";
            LOGGER.info(classifier.classifyToString(s1));
            LOGGER.info(classifier.classifyWithInlineXML(s2));
            LOGGER.info(classifier.classifyToString(s2, "xml", true));
        }
    }

    @Override
    public String getModelFileEnding() {
        return "ser.gz";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return true;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {

        String trainingFilePath2 = FileHelper.appendToFileName(trainingFilePath, "_t");
        FileFormatParser.removeWhiteSpaceInFirstColumn(trainingFilePath, trainingFilePath2, "_");

        // set the location to the training and the model file in the configs and save the file
        buildConfigFile();
        configFileContent = configFileContent.replaceAll("###TRAINING_FILE###", trainingFilePath2);
        configFileContent = configFileContent.replaceAll("###MODEL_FILE###", modelFilePath);
        FileHelper.writeToFile("data/temp/stanfordNerConfig.props", configFileContent);

        String[] args = new String[2];
        args[0] = "-props";
        args[1] = "data/temp/stanfordNerConfig.props";

        Properties props = StringUtils.argsToProperties(args);
        CRFClassifier crf = new CRFClassifier(props);
        String loadPath = crf.flags.loadClassifier;
        String loadTextPath = crf.flags.loadTextClassifier;
        String serializeTo = crf.flags.serializeTo;
        String serializeToText = crf.flags.serializeToText;

        if (loadPath != null) {
            crf.loadClassifierNoExceptions(loadPath, props);
        } else if (loadTextPath != null) {
            System.err.println("Warning: this is now only tested for Chinese Segmenter");
            System.err.println("(Sun Dec 23 00:59:39 2007) (pichuan)");
            try {
                crf.loadTextClassifier(loadTextPath, props);
                // System.err.println("DEBUG: out from crf.loadTextClassifier");
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("error loading " + loadTextPath);
            }
        } else if (crf.flags.loadJarClassifier != null) {
            crf.loadJarClassifier(crf.flags.loadJarClassifier, props);
        } else if (crf.flags.trainFile != null || crf.flags.trainFileList != null) {
            crf.train();
        } else {
            crf.loadDefaultClassifier();
        }

        if (serializeTo != null) {
            crf.serializeClassifier(serializeTo);
        }

        if (serializeToText != null) {
            crf.serializeTextClassifier(serializeToText);
        }

        return true;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        StopWatch stopWatch = new StopWatch();

        AbstractSequenceClassifier classifier;

        try {
            classifier = CRFClassifier.getClassifierNoExceptions(configModelFilePath);
        } catch (Exception e) {
            LOGGER.error(getName() + " error in loading model: " + e.getMessage());
            return false;
        }

        setModel(classifier);
        LOGGER.info("model " + configModelFilePath + " successfully loaded in " + stopWatch.getElapsedTimeString());

        return true;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations = new Annotations();

        AbstractSequenceClassifier classifier = (AbstractSequenceClassifier) getModel();

        String inputTextPath = "data/temp/inputText.txt";
        FileHelper.writeToFile(inputTextPath, inputText);

        StringBuilder taggedText = new StringBuilder();
        taggedText.append(classifier.classifyWithInlineXML(inputText));

        String taggedTextFilePath = "data/temp/stanfordNERTaggedText.txt";
        FileHelper.writeToFile(taggedTextFilePath, taggedText);

        annotations = FileFormatParser.getAnnotationsFromXMLFile(taggedTextFilePath);

        annotations.instanceCategoryToClassified();

        FileHelper.writeToFile("data/test/ner/stanfordNEROutput.txt", tagText(inputText, annotations));

        FileHelper.writeToFile("data/test/ner/lingPipeOutput.txt", tagText(inputText, annotations));
        // CollectionHelper.print(annotations);

        return annotations;
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        loadModel(configModelFilePath);
        return getAnnotations(inputText);
    }

    // public void evaluateNER(String modelFilePath, String testFilePath) throws Exception {
    //
    // String[] args = new String[4];
    // args[0] = "-loadClassifier";
    // args[1] = modelFilePath;
    // args[2] = "-testFile";
    // args[3] = testFilePath;
    //
    // Properties props = StringUtils.argsToProperties(args);
    // CRFClassifier crf = new CRFClassifier(props);
    // String testFile = crf.flags.testFile;
    // String loadPath = crf.flags.loadClassifier;
    //
    // if (loadPath != null) {
    // crf.loadClassifierNoExceptions(loadPath, props);
    // } else {
    // crf.loadDefaultClassifier();
    // }
    //
    // if (testFile != null) {
    // if (crf.flags.searchGraphPrefix != null) {
    // crf.classifyAndWriteViterbiSearchGraph(testFile, crf.flags.searchGraphPrefix);
    // } else if (crf.flags.printFirstOrderProbs) {
    // crf.printFirstOrderProbs(testFile);
    // } else if (crf.flags.printProbs) {
    // crf.printProbs(testFile);
    // } else if (crf.flags.useKBest) {
    // int k = crf.flags.kBest;
    // crf.classifyAndWriteAnswersKBest(testFile, k);
    // } else if (crf.flags.printLabelValue) {
    // crf.printLabelInformation(testFile);
    // } else {
    // // crf.classifyAndWriteAnswers(testFile);
    //
    // String testText = FileHelper.readFileToString(testFilePath);
    // String classifiedString = crf.classifyToString(testText, "inlineXML", true);
    // LOGGER.info("cs:" + classifiedString);
    //
    // FileHelper.writeToFile("data/temp/stanfordClassified.xml", classifiedString);
    //
    // FileFormatParser ffp = new FileFormatParser();
    // ffp.xmlToColumn("data/temp/stanfordClassified.xml", "data/temp/stanfordClassifiedColumn.tsv", "\t");
    //
    // /*
    // * List<List<CoreLabel>> out = crf.classify(testFile);
    // * for (List<CoreLabel> sentence : out) {
    // * for (CoreLabel word : sentence) {
    // * System.out.println(word.word());
    // * System.out.println(word.get(AnswerAnnotation.class));
    // * System.out.println(word.value());
    // * System.out.println(word.word() + '/' + word.get(AnswerAnnotation.class) + ' ');
    // * }
    // * System.out.println();
    // * }
    // */
    // }
    // }
    //
    // // port to Java: http://www.cnts.ua.ac.be/conll2002/ner/bin/conlleval.txt
    // }

    /**
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {

        StanfordNER tagger = new StanfordNER();

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("mode").withDescription("whether to tag or train a model")
                    .create());

            OptionGroup modeOptionGroup = new OptionGroup();
            modeOptionGroup.addOption(OptionBuilder.withArgName("tg").withLongOpt("tag").withDescription("tag a text")
                    .create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("tr").withLongOpt("train")
                    .withDescription("train a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("ev").withLongOpt("evaluate")
                    .withDescription("evaluate a model").create());
            modeOptionGroup.addOption(OptionBuilder.withArgName("dm").withLongOpt("demo")
                    .withDescription("demo mode of the tagger").create());
            modeOptionGroup.setRequired(true);
            options.addOptionGroup(modeOptionGroup);

            options.addOption(OptionBuilder.withLongOpt("trainingFile")
                    .withDescription("the path and name of the training file for the tagger (only if mode = train)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder
                    .withLongOpt("testFile")
                    .withDescription(
                            "the path and name of the test file for evaluating the tagger (only if mode = evaluate)")
                    .hasArg().withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("configFile")
                    .withDescription("the path and name of the config file for the tagger").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("inputText")
                    .withDescription("the text that should be tagged (only if mode = tag)").hasArg()
                    .withArgName("text").withType(String.class).create());

            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                if (cmd.hasOption("tag")) {

                    String taggedText = tagger.tag(cmd.getOptionValue("inputText"), cmd.getOptionValue("configFile"));

                    if (cmd.hasOption("outputFile")) {
                        FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                    } else {
                        System.out.println("No output file given so tagged text will be printed to the console:");
                        System.out.println(taggedText);
                    }

                } else if (cmd.hasOption("train")) {

                    tagger.train(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"));

                } else if (cmd.hasOption("evaluate")) {

                    tagger.evaluate(cmd.getOptionValue("trainingFile"), cmd.getOptionValue("configFile"),
                            TaggingFormat.XML);

                } else if (cmd.hasOption("demo")) {

                    try {
                        tagger.demo(cmd.getOptionValue("inputText"));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage());
                    }

                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("StanfordNER", options);
            }

        }

        // // HOW TO USE ////
        // tagger.loadModel("data/models/stanfordner/data/ner-eng-ie.crf-3-all2008.ser.gz");
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");

        // tagger.tag(
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.",
        // "data/models/stanfordner/data/ner-eng-ie.crf-3-all2008.ser.gz");

        // demo
        // st.demo("John J. Smith and the Nexus One location mention Seattle in the text.");
        // learn
        // st.trainNER("data/temp/stanfordner/example/austen.prop");
        // st.trainNER("data/temp/mobilephone.prop");

        // use
        // st.useLearnedNER("data/temp/stanfordner/example/ner-model.ser.gz","John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");
        // tagger.useLearnedNER(
        // "data/temp/ner-model-mobilePhone.ser.gz",
        // "John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");
        // st.useLearnedNER("data/temp/stanfordner/classifiers/ner-eng-ie.crf-3-all2008.ser.gz","John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle.");

        // evaluate
        // st.evaluateNER("data/temp/stanfordner/example/ner-model.ser.gz","data/temp/stanfordner/example/jane-austen-emma-ch2.tsv");
        // st.evaluateNER("data/temp/ner-model-mobilePhone.ser.gz", "data/temp/allUntagged.xml");

        // /////////////////////////// train and test /////////////////////////////
        // tagger.train("data/temp/nerEvaluation/www_eval_2_cleansed/allColumn.txt", "data/temp/stanfordNER.model");
        // tagger.train("data/datasets/ner/conll/training.txt", "data/temp/stanfordNER.model");
        // EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
        // "data/temp/stanfordNER.model", TaggingFormat.COLUMN);

        tagger.train("data/datasets/ner/tud/tud2011_train.txt", "data/temp/stanfordNER2.model");
        EvaluationResult er = tagger.evaluate("data/datasets/ner/tud/tud2011_test.txt", "data/temp/stanfordNER2.model",
                TaggingFormat.COLUMN);

        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

        // Dataset trainingDataset = new Dataset();
        // trainingDataset.setPath("data/datasets/ner/www_test/index_split1.txt");
        // tagger.train(trainingDataset, "data/temp/stanfordner." + tagger.getModelFileEnding());
        //
        // Dataset testingDataset = new Dataset();
        // testingDataset.setPath("data/datasets/ner/www_test/index_split2.txt");
        // EvaluationResult er = tagger.evaluate(testingDataset, "data/temp/stanfordner." +
        // tagger.getModelFileEnding());
        // System.out.println(er.getMUCResultsReadable());
        // System.out.println(er.getExactMatchResultsReadable());
    }

}