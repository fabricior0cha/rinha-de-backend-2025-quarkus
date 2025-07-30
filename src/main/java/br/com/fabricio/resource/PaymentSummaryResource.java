package br.com.fabricio.resource;

import br.com.fabricio.service.PaymentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.time.Instant;

@Path("/payments-summary")
public class PaymentSummaryResource {

    @Inject
    PaymentService service;

    @GET
    @Produces("application/json")
    public Response summary(@QueryParam(value = "to") Instant to, @QueryParam(value = "from") Instant from) {
        return Response.ok(service.getSummary(to, from)).build();
    }
}
