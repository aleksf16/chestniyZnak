package com.mailing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CrptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger requestCount;

    // Эти два поля оставлены для возможного расширения(например получения ограничений для каких либо целей)
    private final int requestLimit;
    private final TimeUnit timeUnit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.requestCount = new AtomicInteger(0);
        this.timeUnit = timeUnit;

        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
            requestCount.set(0);
        }, 0, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException, URISyntaxException {
        semaphore.acquire();

        try {
            String requestBody = objectMapper.writeValueAsString(document);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to create document: " + response.body());
            }
        } finally {
            semaphore.release();
        }
    }

    public static class Document {
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
        public Product[] products;
        public String reg_date;
        public String reg_number;

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
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

        Document document = new Document();
        document.description = new Document.Description();
        document.description.participantInn = "string";
        document.doc_id = "string";
        document.doc_status = "string";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "string";
        document.participant_inn = "string";
        document.producer_inn = "string";
        document.production_date = "2020-01-23";
        document.production_type = "string";
        document.products = new Document.Product[1];
        document.products[0] = new Document.Product();
        document.products[0].certificate_document = "string";
        document.products[0].certificate_document_date = "2020-01-23";
        document.products[0].certificate_document_number = "string";
        document.products[0].owner_inn = "string";
        document.products[0].producer_inn = "string";
        document.products[0].production_date = "2020-01-23";
        document.products[0].tnved_code = "string";
        document.products[0].uit_code = "string";
        document.products[0].uitu_code = "string";
        document.reg_date = "2020-01-23";
        document.reg_number = "string";

        try {
            api.createDocument(document, "your_signature_here");
        } catch (IOException | InterruptedException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
