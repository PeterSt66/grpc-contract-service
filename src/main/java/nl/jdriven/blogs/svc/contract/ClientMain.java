package nl.jdriven.blogs.svc.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.perfmark.PerfMark;
import lombok.extern.slf4j.Slf4j;
import nl.jdriven.blogs.svc.contract.api.Transformer;
import nl.jdriven.blogs.svc.contract.proto.*;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.io.FileWriter;

@Slf4j
public class ClientMain {
    public static void main(String[] args) {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        PerfMark.setEnabled(true);

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

        dumpTrace(client);

        stopServer(client);

        // avoid nastiness on the serverside due to the sudden death of the connection
        ((ManagedChannel) client.getChannel()).shutdownNow();
    }

    private static void findContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid) {
        var fcr = FindContractRequest.newBuilder().setContractId(cid).build();
        var found = client.findContract(fcr);
        log.info("Find contract gave: \n{}", found);
    }

    private static void finalizeContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid) {
        var fcr = FinalizeContractRequest.newBuilder().setContractId(cid).build();
        var fcResp = client.finalizeContract(fcr);
        log.info("Finalize contract gave: \n{}", fcResp);
    }

    private static void promoteQuoteToContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc) {
        var quoteRequest = PromoteQuoteRequest.newBuilder().setContractId(cid).build();

        try {
            var pqrespWrong = client.promoteQuote(quoteRequest);
            log.info("Promote {} gave: \n{}", desc, pqrespWrong);
        } catch (StatusRuntimeException sre) {
            log.info("Promote failed with: \n{}", sre.getStatus().getCode() + " because: \n{}", sre.getStatus().getDescription());
        }
    }

    private static NewQuoteResponse prepareQuote(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var quotedPrice = Money.ofMajor(CurrencyUnit.EUR, 2500L);
        NewQuoteRequest request = NewQuoteRequest.newBuilder()
                .setFullNameOfCustomer("Man on the moon")
                .setDescriptionOfWorkRequested("Please give me a quote for installing a kitchen, everything is already delivered @ the house, including all appliances.")
                .setQuotedPrice(Transformer.transform(quotedPrice))
                .build();
        var aqresp = client.newQuote(request);
        log.info("Prepare quote gave: \n{}", aqresp);
        return aqresp;
    }

    private static void AddSomeWork(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc, long amountInEur) {
        var cost = Money.ofMajor(CurrencyUnit.EUR, amountInEur);
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(Transformer.transform(cost))
                .setDescriptionOfWorkDone(desc);
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId(cid)
                .setWork(workdone)
                .build();

        try {
            var wdresp = client.addWorkDone(adr);
            log.info("Add work gave: \n{}", wdresp);
        } catch (StatusRuntimeException sre) {
            log.info("Add work failed with: {} because of: {}", sre.getStatus().getCode(), sre.getStatus().getDescription());
        }
    }

    private static void tryEmptyWork(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var cost = Money.ofMajor(CurrencyUnit.EUR, 0L);
        var workdone = WorkDone.newBuilder()
                .setCostOfWork(Transformer.transform(cost))
                .setDescriptionOfWorkDone("");
        AddWorkDoneRequest adr = AddWorkDoneRequest.newBuilder()
                .setContractId("")
                .setWork(workdone)
                .build();

        try {
            client.addWorkDone(adr);
        } catch (StatusRuntimeException sre) {
            log.info("Add work failed with: {} because of: {}", sre.getStatus().getCode(), sre.getStatus().getDescription());
        }
    }

    private static void dumpTrace(ContractServiceGrpc.ContractServiceBlockingStub client) {
        var scresp = client.serverCommand(ServerCommandRequest.newBuilder().setCmd("CYCLEPERFMARK").build());
        try {
            var tf = File.createTempFile("Perfmark", ".html");
            var fw = new FileWriter(tf);
            fw.write(scresp.getResultDescription());
            fw.close();
            log.info("PerfMark trace written to: " + tf.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Could not write perfmark tracefile:" + e, e);
        }
    }

    private static void stopServer(ContractServiceGrpc.ContractServiceBlockingStub client) {
        client.serverCommand(ServerCommandRequest.newBuilder().setCmd("STOP").build());
    }
}
