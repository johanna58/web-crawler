package org.example.crawler;

import org.example.processor.PageProcessor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The task to be executed for each crawled page. That includes text processing and creating new tasks
 * for outgoing links found on a page.
 */
public class CrawlTask {
    private final int maxDepth;
    private final String uri;
    private final Crawler crawler;


    /**
     * CrawlTask constructor
     * @param maxDepth the maximum number of steps possible from the current depth
     * @param uri the uri for the page to be crawled
     * @param crawler a reference to the crawler that created the initial task
     */
    public CrawlTask(int maxDepth, String uri, Crawler crawler) {
        this.maxDepth = maxDepth;
        this.uri = uri;
        this.crawler = crawler;
    }

    /**
     * Retrieves the html content of the page to be crawled
     * @return doc if the data was retrieved successfully and null otherwise
     */
    private Document retrievePage() {
        Document doc = null;
        try {
            doc = Jsoup.connect(this.uri).get();
        }
        catch (IOException e) {
            System.out.printf("Could not retrieve page, %s\n", e.getMessage());
        }
        return doc;
    }

    /**
     * For each given link, create a new CrawlTask to submit to the crawler
     * @param links the hyperlinks found on the current page
     */
    private void submitTasksForLinks(Elements links){
        for (Element link : links) {
            String normalizedUrl;
            try {
                URI uri = new URI(link.attr("abs:href"));
                URI normalizedUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), null);
                normalizedUrl = normalizedUri.toString();
            } catch (URISyntaxException e) {
                return;
            }
            CrawlTask task = new CrawlTask(this.maxDepth -1, normalizedUrl, this.crawler);
            crawler.submitTask(task);
        }
    }

    /**
     * Retrieves the content of the webpage corresponding to the CrawlTask, processes it, and submits new
     * CrawlTasks to the Crawler if the maximum depth has not been reached.
     */
    public void doTask() {
        if (this.maxDepth < 0) return;
        Document doc = retrievePage();
        if (doc == null) return;
        System.out.printf("%s, %s%n", doc.title(), this.uri);
        PageProcessor processor = PageProcessor.getInstance();
        processor.processPage(doc, this.uri);

        // do not process any links on this page if maximum depth has been reached
        if (this.maxDepth == 0) return;
        Elements links = doc.select("a[href]");
//          System.out.println(String.format("depth %d, links %d", depth, links.size()));
        submitTasksForLinks(links);
    }

    /**
     * Getter for uri belonging to the CrawlTask
     * @return uri
     */
    public String getUri() {
        return this.uri;
    }
}
