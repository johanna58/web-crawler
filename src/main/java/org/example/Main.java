package org.example;

import org.example.crawler.Crawler;


public class Main {

    public static void main(String[] args) {
        String startUrl = "https://en.wikipedia.org/wiki/Open-source_intelligence";
        int maxDepth = 3;
        int maxTime = 60000; // milliseconds

        Crawler crawler = new Crawler(maxDepth, maxTime);
        crawler.startCrawling(startUrl);

    }
}