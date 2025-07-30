package br.com.fabricio.worker;

import br.com.fabricio.model.Payment;
import br.com.fabricio.queue.PaymentQueue;
import br.com.fabricio.service.PaymentService;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;


@ApplicationScoped
public class PaymentWorker {
    @ConfigProperty(name = "workers.max-threads")
    int MAX_THREADS;

    @ConfigProperty(name = "retry.default.max-attempts")
    int MAX_ATTEMPTS;

    @ConfigProperty(name = "retry.fallback.max-attempts")
    int MAX_FALLBACK_ATTEMPTS;

    @ConfigProperty(name = "processor.default.host")
    String DEFAULT_PROCESSOR_HOST;

    @ConfigProperty(name = "processor.fallback.host")
    String FALLBACK_PROCESSOR_HOST;

    @Inject
    PaymentQueue producer;

    @Inject
    PaymentService service;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build();

    void onStart(@Observes StartupEvent ev) {
    }

    @PostConstruct
    public void init() {

        for (int i = 0; i < MAX_THREADS; i++) {
            Thread.startVirtualThread(this::process);
        }
    }

    public void process() {
        while (true) {
            Payment payment = producer.dequeue();
            boolean success = false;
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                success = sendPayment(payment, true);
                if (success) {
                    payment.setProcessor(1);
                    break;
                }
            }


            if (!success) {
                for (int attempt = 0; attempt < MAX_FALLBACK_ATTEMPTS; attempt++) {
                    success = sendPayment(payment, false);
                    if (success) {
                        payment.setProcessor(2);
                        break;
                    }
                }
            }

            if (success) {
                try {
                    service.addPayment(payment);
                } catch (SQLException e) {
                    System.err.println("Failed to save payment: " + e.getMessage());
                }
            }
        }
    }

    public boolean sendPayment(Payment payment, boolean useDefault) {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder()
                    .add("amount", payment.getAmount())
                    .add("correlationId", payment.getCorrelationId())
                    .add("requestedAt", payment.getRequestedAt().toString());
            JsonObject jsonObject = builder.build();
            String jsonString = jsonObject.toString();
            String urlDefault =  DEFAULT_PROCESSOR_HOST + "/payments";
            String urlFallback =  FALLBACK_PROCESSOR_HOST + "/payments";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(new URI(useDefault ? urlDefault : urlFallback))
                    .version(HttpClient.Version.HTTP_2)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                    .build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return true;
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            return false;
        }

        return false;
    }


}
