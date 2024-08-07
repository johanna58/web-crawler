package org.example.processor;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.RuntimeInterruptedException;
import org.jsoup.nodes.Document;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates word frequency document out of jsoup Document.
 */
public class PageProcessor {
    private final Set<String> stopwords;

    /**
     * Holds an instance of PageProcessor to ensure Singleton pattern in a thread-safe manner
     */
    private static final class InstanceHolder {
        private static final PageProcessor instance = new PageProcessor();
    }

    /**
     * Retrieves the PageProcessor
     * @return PageProcessor
     */
    public static PageProcessor getInstance() {
        return InstanceHolder.instance;
    }

    private PageProcessor() {
        stopwords = createStopwords();
    }

    /**
     * Reads a list of stopwords from a file
     * @return a set of stopwords
     */
    private Set<String> createStopwords() {
        InputStream inputStream = getClass().getResourceAsStream("/stopwords/en-stopwords.txt");
        if (inputStream == null) throw new RuntimeException("Failed to load stopwords from file");
        Set<String> stopwords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read stopwords from file", e);
        }
        return stopwords;
    }

    /**
     * Processes the text from a jsoup Document by removing special characters,
     * tokenization and lemmatization to find the most frequent words and store the results.
     */
    public void processPage(Document doc, String url){
        String text = doc.body().text();
        text = prepareText(text);

        StanfordCoreNLP pipeline = createPipeline();
        // create a document object
        CoreDocument document;
        try{
            document = pipeline.processToCoreDocument(text);
        } catch (RuntimeInterruptedException e){
            return;
        }
        // remove stopwords from lemmas
        List<String> filteredLemmas = filterStopwords(document, this.stopwords);
        // find word frequency
        Map<String, Integer> frequencyMap = performFrequencyAnalysis(filteredLemmas);
        // sort in descending order
        List<Map.Entry<String, Integer>> sortedFreq = frequencyMap.entrySet().stream().sorted((Map.Entry.<String, Integer>comparingByValue().reversed())).collect(Collectors.toList());
        // store result
        StorageHandling.saveToCsv(sortedFreq, url);
    }

    /**
     * Removes special characters and converts to lower case
     * @param text the input string
     * @return lowercase text without special characters
     */
    private String prepareText(String text){
        text = text.replaceAll("[^a-zA-Z ]", " ");
        return text.toLowerCase();
    }

    /**
     * Creates StanfordCoreNLP pipeline for tokenization, pos, and lemmatization
     * @return StanfordCoreNLP pipeline
     */
    private StanfordCoreNLP createPipeline(){
        RedwoodConfiguration.current().clear().apply();
        Properties props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,pos,lemma");
        // build pipeline
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        return pipeline;
    }

    /**
     * Removes stopwords from a CoreDocument
     * @param document text that has been processed by a StanfordCoreNLP pipeline
     * @param stopwords set of stopwords
     * @return list of lemmas from a CoreDocument without stopwords
     */
    private List<String> filterStopwords(CoreDocument document, Set<String> stopwords) {
        return document.tokens().stream()
                .filter(label -> !stopwords.contains(label.lemma()))
                .map(label -> label.lemma())
                .collect(Collectors.toList());
    }

    /**
     * Counts the frequency of words in a list
     * @param strings a list of words
     * @return a map object of words and their frequencies
     */
    public Map<String, Integer> performFrequencyAnalysis(List<String> strings) {
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String str : strings) {
            // If the string is already in the map, increment its count
            if (frequencyMap.containsKey(str)) {
                frequencyMap.put(str, frequencyMap.get(str) + 1);
            } else {
                // Otherwise, add the string to the map with count 1
                frequencyMap.put(str, 1);
            }
        }
        return frequencyMap;
    }

}
