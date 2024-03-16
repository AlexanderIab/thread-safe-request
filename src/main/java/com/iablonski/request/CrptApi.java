package com.iablonski.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi1 {
    private final long intervalMillis;
    private final int requestLimit;
    private int requestCount;
    private long lastRequestTime;
    private final Lock lock;
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Gson gson;
    private final HttpClient httpClient;


    public CrptApi1(TimeUnit timeUnit, int requestLimit) {
        this.intervalMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.gson = new GsonBuilder().create();
        this.httpClient = HttpClient.newHttpClient();;
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
        this.lock = new ReentrantLock();
    }

    public void createDocument(CrptDocument document, String signature) throws InterruptedException {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= intervalMillis) {
                // If interval has elapsed, reset the request count and time
                requestCount = 0;
                lastRequestTime = currentTime;
            }
            if (requestCount >= requestLimit) {
                // If reached the request limit, wait until interval expires
                long remainingTime = lastRequestTime + intervalMillis - currentTime;
                if (remainingTime > 0) {
                    TimeUnit.MILLISECONDS.sleep(remainingTime);
                    lastRequestTime = System.currentTimeMillis();
                }
                requestCount = 0; // Reset request count after sleep
            }
            requestCount++;
            System.out.println("Making API request...");
            makeRequest(document, signature);
        } finally {
            lock.unlock();
        }
    }

    private void makeRequest(CrptDocument document, String signature) {
        try {
            String requestBody = gson.toJson(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class CrptDocument {
        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }

        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public List<Product> products;
        public String reg_date;
        public String reg_number;
    }

    public static void main(String[] args) {
        String signature = "your_signature_here"; // В этом примере подпись задается явно, но в реальном приложении она должна быть получена из заголовка запроса или откуда-то еще

    }

}