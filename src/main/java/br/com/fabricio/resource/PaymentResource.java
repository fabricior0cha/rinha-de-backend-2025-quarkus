package br.com.fabricio.resource;

import br.com.fabricio.model.Payment;
import br.com.fabricio.queue.PaymentQueue;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/payments")
public class PaymentResource {

    @Inject
    PaymentQueue queue;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void receivePayment(Payment payment) {
        queue.enqueue(payment);
    }

}
