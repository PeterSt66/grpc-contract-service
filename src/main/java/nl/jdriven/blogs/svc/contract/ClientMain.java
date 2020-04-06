package nl.jdriven.blogs.svc.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import nl.jdriven.blogs.svc.contract.api.Transformer;
import nl.jdriven.blogs.svc.contract.proto.*;

import java.math.BigDecimal;
import java.math.MathContext;

public class ClientMain {
    public static void main(String[] args) throws Exception{
        var channel = ManagedChannelBuilder
                .forAddress("localhost", 53000)
                .usePlaintext() // disable TLS which is enabled by default and requires certificates
                .build();
        var client = ContractServiceGrpc.newBlockingStub(channel);

        // Start with a quote
        NewQuoteResponse aqresp = prepareQuote(client);
        var cid = aqresp.getQuoteId();

        // Add some work to quote - should fail
        AddSomeWork(client, cid, "Bad work", 10L);

        // Add empty work to quote - should fail
        tryEmptyWork(client);

        // promote unfound quote to contract - should fail
        promoteQuoteToContract(client, "Bogus", "unknown quote");

        // promote quote to contract
        promoteQuoteToContract(client, cid.getId(), "proper quote");

        // Add some work to contract
        AddSomeWork(client, cid, "Constructed new plumbing and installed all cabinets including the kitchen sink", 1200L);
        AddSomeWork(client, cid, "Installed all appliances", 1000L);

        finalizeContract(client, cid);

        findContract(client, cid);

        // avoid nastiness on the serverside due to the sudden death of the connection
        ((ManagedChannel)client.getChannel()).shutdownNow();
    }

    private static void findContract(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid) {
        var found = client.find(cid);
        System.out.println("Find contract gave: "+ found);
    }

    private static void finalizeContract(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid) {
        var fcResp = client.finalizeContract(cid);
        System.out.println("Finalize contract gave: "+ fcResp);
    }

    private static void promoteQuoteToContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc) {
        var quoteRequest = ContractId.newBuilder().setId(cid).build();
        var pqrespWrong = client.promoteQuote(quoteRequest);
        System.out.println("Promote " + desc + " gave: " + pqrespWrong);
    }

    private static NewQuoteResponse prepareQuote(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var price = new BigDecimal(2500L, new MathContext(2));
        var quotedPrice = Transformer.transform(price);
        NewQuoteRequest request = NewQuoteRequest.newBuilder()
                .setFullNameOfParticipant("Man on the moon")
                .setDescriptionOfWorkRequested("Please give me a quote for installing a kitchen, everything will be delivered @ the house, including all appliances.")
                .setQuotedPrice(quotedPrice)
                .build();
        var aqresp = client.newQuote(request);
        System.out.println("Prepare quote gave: " + aqresp);
        return aqresp;
    }

    private static void AddSomeWork(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid, String desc, long amountInEur) {
        var cost = Transformer.transform(new BigDecimal(amountInEur, new MathContext(2)));
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(cost)
                .setDescriptionOfWorkDone(desc);
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId(cid)
                .setWork(workdone)
                .build();
        var wdresp = client.addWorkDone(adr);
        System.out.println("Add work gave: " + wdresp);
    }
    private static void tryEmptyWork(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var cost = Transformer.transform(new BigDecimal(0L, new MathContext(2)));
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(cost)
                .setDescriptionOfWorkDone("");
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId(ContractId.newBuilder().build())
                .setWork(workdone)
                .build();
        var wdresp = client.addWorkDone(adr);
        System.out.println("Add work gave: " + wdresp);
    }
}
