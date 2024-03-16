package com.iablonski.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final Logger LOGGER = LoggerFactory.getLogger(CrptApi.class);

    private final long intervalMillis;
    private final int requestLimit;
    private int requestCount;
    private long lastRequestTime;

    private final Lock lock;

    private final Gson gson;
    private final HttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.intervalMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.gson = new GsonBuilder().create();
        this.httpClient = HttpClient.newHttpClient();
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
        this.lock = new ReentrantLock();
    }

    public void createDocument(CrptDocument document, String signature) throws InterruptedException, IOException {
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= intervalMillis) {
                // Если интервал истёк
                requestCount = 0;
                lastRequestTime = currentTime;
            }
            if (requestCount >= requestLimit) {
                // Если достигли лимита запросов
                long timeLeft = lastRequestTime + intervalMillis - currentTime;
                if (timeLeft > 0) {
                    TimeUnit.MILLISECONDS.sleep(timeLeft);
                    lastRequestTime = System.currentTimeMillis();
                }
                requestCount = 0;
            }
            LOGGER.info("Запрос за интервал {}", requestCount);
            requestCount++;
            LOGGER.info("Making API request...");
            makeRequest(document, signature);
            LOGGER.info("Request finished");
        } finally {
            lock.unlock();
        }
    }

    // Запрос
    private void makeRequest(CrptDocument document, String signature) throws IOException, InterruptedException {
        String requestBody = gson.toJson(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            LOGGER.info("Request was successful {} {}", response.body(), signature);
        } else {
            LOGGER.error("Request failed with status code: {} {}", response.statusCode(), response.body());
        }
    }

    // Классы для документа
    public static class CrptDocument {
        public static class Description {
            public String participantInn;

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }
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

            public Product(String certificate_document, String certificate_document_date,
                           String certificate_document_number, String owner_inn,
                           String producer_inn, String production_date,
                           String tnved_code, String uit_code, String uitu_code) {
                this.certificate_document = certificate_document;
                this.certificate_document_date = certificate_document_date;
                this.certificate_document_number = certificate_document_number;
                this.owner_inn = owner_inn;
                this.producer_inn = producer_inn;
                this.production_date = production_date;
                this.tnved_code = tnved_code;
                this.uit_code = uit_code;
                this.uitu_code = uitu_code;
            }
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

        public CrptDocument(Description description, String doc_id, String doc_status, String doc_type,
                            boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                            String production_date, String production_type, List<Product> products,
                            String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }
    }


    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);

        // Пример создания документа
        CrptDocument.Description description = new CrptDocument.Description("string");
        CrptDocument.Product product = new CrptDocument.Product(
                "string", "2020-01-23",
                "string", "string",
                "string", "2020-01-23",
                "string", "string", "string");
        CrptDocument document = new CrptDocument(description, "string", "string",
                "LP_INTRODUCE_GOODS", true, "string",
                "string", "string",
                "2020-01-23", "string", List.of(product),
                "2020-01-23", "string");

        String signature = "testSignature";

        // Тест для проверки
        try {
            for (int i = 1; i <= 23; i++) {
                LOGGER.info("Запрос: {}", i);
                api.createDocument(document, signature);
                Thread.sleep(500);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}