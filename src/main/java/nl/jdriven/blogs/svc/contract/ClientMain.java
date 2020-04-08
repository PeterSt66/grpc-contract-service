package nl.jdriven.blogs.svc.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import nl.jdriven.blogs.svc.contract.api.Transformer;
import nl.jdriven.blogs.svc.contract.proto.*;

import java.math.BigDecimal;
import java.math.MathContext;

public class ClientMain {
    public static void main(String[] args) {
        var channel = ManagedChannelBuilder
                .forAddress("localhost", 53000)
                .usePlaintext() // disable TLS which is enabled by default and requires certificates
                .build();
        var client = ContractServiceGrpc.newBlockingStub(channel);

        // Start with a quote
        var pqresp = prepareQuote(client);
        var cid = pqresp.getContractId();

        // Add some work to quote - should fail
        AddSomeWork(client, cid, "Bad work", 10L);

        // Add empty work to quote - should fail
        tryEmptyWork(client);

        // promote unfound quote to contract - should fail
        promoteQuoteToContract(client, "Bogus", "unknown quote");

        // promote quote to contract
        promoteQuoteToContract(client, cid, "proper quote");

        // Add some work to contract
        AddSomeWork(client, cid, "Constructed new plumbing and installed all cabinets including the kitchen sink", 1200L);
        AddSomeWork(client, cid, "Installed all appliances", 1000L);

        finalizeContract(client, cid);

        findContract(client, cid);

        // avoid nastiness on the serverside due to the sudden death of the connection
        ((ManagedChannel) client.getChannel()).shutdownNow();
    }

    private static void findContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid) {
        var fcr = FindContractRequest.newBuilder().setContractId(cid).build();
        var found = client.findContract(fcr);
        System.out.println("Find contract gave: " + found);
    }

    private static void finalizeContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid) {
        var fcr = FinalizeContractRequest.newBuilder().setContractId(cid).build();
        var fcResp = client.finalizeContract(fcr);
        System.out.println("Finalize contract gave: " + fcResp);
    }

    private static void promoteQuoteToContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc) {
        var quoteRequest = PromoteQuoteRequest.newBuilder().setContractId(cid).build();

        try {
            var pqrespWrong = client.promoteQuote(quoteRequest);
            System.out.println("Promote " + desc + " gave: " + pqrespWrong);
        } catch (StatusRuntimeException sre) {
            System.out.println("Promote failed with: " + sre.getStatus().getCode() + " because: " + sre.getStatus().getDescription());
        }
    }

    private static NewQuoteResponse prepareQuote(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var price = new BigDecimal(2500L, new MathContext(2));
        var quotedPrice = Transformer.transform(price);
        NewQuoteRequest request = NewQuoteRequest.newBuilder()
                .setFullNameOfParticipant("Man on the moon")
                .setDescriptionOfWorkRequested("Please give me a quote for installing a kitchen, everything is already delivered @ the house, including all appliances.")
                .setQuotedPrice(quotedPrice)
                .build();
        var aqresp = client.newQuote(request);
        System.out.println("Prepare quote gave: " + aqresp);
        return aqresp;
    }

    private static void AddSomeWork(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc, long amountInEur) {
        var cost = Transformer.transform(new BigDecimal(amountInEur, new MathContext(2)));
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(cost)
                .setDescriptionOfWorkDone(desc);
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId(cid)
                .setWork(workdone)
                .build();

        try {
            var wdresp = client.addWorkDone(adr);
            System.out.println("Add work gave: " + wdresp);
        } catch (StatusRuntimeException sre) {
            System.out.println("Add work failed with: " + sre.getStatus().getCode() + " because: " + sre.getStatus().getDescription());
        }
    }

    private static void tryEmptyWork(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var cost = Transformer.transform(new BigDecimal(0L, new MathContext(2)));
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(cost)
                .setDescriptionOfWorkDone("");
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId("")
                .setWork(workdone)
                .build();

        try {
            var wdresp = client.addWorkDone(adr);
        } catch (StatusRuntimeException sre) {
            System.out.println("Add work failed with: " + sre.getStatus().getCode() + " because: " + sre.getStatus().getDescription());
        }
    }
}
