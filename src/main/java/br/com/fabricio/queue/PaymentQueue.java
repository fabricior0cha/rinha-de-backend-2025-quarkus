package br.com.fabricio.queue;

import br.com.fabricio.model.Payment;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.concurrent.LinkedBlockingQueue;

@ApplicationScoped
public class PaymentQueue {

    private final LinkedBlockingQueue<Payment> paymentQueue = new LinkedBlockingQueue<>();


    public void enqueue(Payment payment) {
        payment.setRequestedAt(Instant.now());
        paymentQueue.offer(payment);
    }

    public Payment dequeue() {
        try {
            return paymentQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
