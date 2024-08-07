package org.example.crawler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Web crawler to analyze word importance per page. It takes a starting url and
 * scans pages up to a certain depth for a given amount of time.
 */
public class Crawler {
    private final int maxDepth;
    private final long maxTime; // milliseconds
    private ExecutorService pool;
    static final int MAX_THREADS = 150;
    private final Set<String> visitedPages;
    private final AtomicInteger nTasks; // keep track of the number of non-finished tasks
    private long startTime;
    private final AtomicBoolean running;

    /**
     * Crawler constructor
     * @param maxDepth The steps to go from the current depth
     * @param maxTime The maximum running time for the crawler
     */
    public Crawler(int maxDepth, long maxTime) {
        this.maxDepth = maxDepth;
        this.maxTime = maxTime;
        this.visitedPages = Collections.synchronizedSet(new HashSet<>());
        this.nTasks = new AtomicInteger(0);
        this.running = new AtomicBoolean(false);
    }

    /**
     * Starts crawling from a given startUrl if it was not already crawling. Returns immediately.
     * @param startUrl the url of the first page to be crawled
     */
    public void startCrawling(String startUrl) {
        boolean wasRunning = this.running.getAndSet(true);
        if (wasRunning) return;
        this.visitedPages.clear();
        this.nTasks.set(0);
        this.startTime = System.currentTimeMillis();
        this.pool = Executors.newFixedThreadPool(MAX_THREADS);
        submitTask(new CrawlTask(this.maxDepth, startUrl, this));
    }

    /**
     * Stops the crawling process if it has not already been stopped.
     */
    public void stopCrawling() {
        boolean wasRunning = this.running.getAndSet(false);
        if (!wasRunning) return;
        this.pool.shutdownNow();
    }

    /**
     * Submits a given task for execution as long as the maximum crawl time has
     * not been exceeded and the url associated to the task has not been visited before.
     * @param task the task to be submitted for execution
     */
    public void submitTask(CrawlTask task){
        if(System.currentTimeMillis() - startTime > maxTime) {
            stopCrawling();
            return;
        }
        if (this.visitedPages.contains(task.getUri())) return;
        this.nTasks.incrementAndGet();
        this.visitedPages.add(task.getUri());
        this.pool.execute(() -> {
            if(System.currentTimeMillis() - startTime > maxTime) {
                stopCrawling();
                return;
            }
            task.doTask();
            int n = this.nTasks.getAndDecrement();
            if (n == 0) stopCrawling();
        });
    }

}
