package nl.jdriven.blogs.svc.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import nl.jdriven.blogs.svc.contract.api.Transformer;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneRequest;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneResponse;
import nl.jdriven.blogs.svc.contract.proto.ContractId;
import nl.jdriven.blogs.svc.contract.proto.ContractServiceGrpc;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractRequest;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractResponse;
import nl.jdriven.blogs.svc.contract.proto.FindContractRequest;
import nl.jdriven.blogs.svc.contract.proto.FindContractResponse;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.Status;
import nl.jdriven.blogs.svc.contract.proto.Statuscode;
import nl.jdriven.blogs.svc.contract.proto.WorkDone;
import org.apache.commons.lang3.StringUtils;

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
      var aqresp = prepareQuote(client);
      assertStatus("prepareQuote", aqresp.getStatus(), Statuscode.OK);
      var cid = aqresp.getQuoteId();

      // Add some work to quote - should fail
      var response = AddSomeWork(client, cid, "Bad work", 200L);
      assertStatus("AddSomeWork-WhileNotAtWork", response.getStatus(), Statuscode.FAILED, "Contract.not.workable", "Contract/Not.at.work");


      // Add empty work to quote - should fail
      var ewresp = tryEmptyWork(client);
      assertStatus("AddSomeWork-ValidationErrors", ewresp.getStatus(),
              Statuscode.FAILED, "Validation failed", "mandatory/WorkDone.Description", "Cost.Below.Minimal.Amount/WorkDone.Cost");

      // promote unfound quote to contract - should fail
      var pqresp = promoteQuoteToContract(client, "Bogus", "unknown quote");
      assertStatus("promoteQuoteToContract-ContractNotFound", pqresp.getStatus(), Statuscode.NOTFOUND, "Notfound");

      // promote quote to contract
      var pqresp2 = promoteQuoteToContract(client, cid.getId(), "proper quote");
      assertStatus("promoteQuoteToContract-Ok", pqresp2.getStatus(), Statuscode.OK);

      // Add some work to contract
      var aswresp2 = AddSomeWork(client, cid, "Constructed new plumbing and installed all cabinets including the kitchen sink", 1200L);
      assertStatus("AddSomeWork-Ok", aswresp2.getStatus(), Statuscode.OK, "Done");

      // Add some more work to contract
      var aswresp3 = AddSomeWork(client, cid, "Installed all appliances", 1000L);
      assertStatus("AddSomeWork-Ok", aswresp3.getStatus(), Statuscode.OK, "Done");

      var fcresp = finalizeContract(client, cid);
      assertStatus("finalizeContract-Ok", fcresp.getStatus(), Statuscode.OK, "Ok");
      System.out.println("Finalize contract: profit=" + fcresp.getProfitMade().getCurrencyCode() + " " + fcresp.getProfitMade().getUnits());

      var fcresp2 = findContract(client, cid);
      assertStatus("findContract-Ok", fcresp2.getStatus(), Statuscode.OK, "Ok");
      System.out.println("Find contract gave: " + fcresp2.getContract());

      // avoid nastiness on the serverside due to the sudden death of the connection
      ((ManagedChannel) client.getChannel()).shutdownNow();
   }


   private static FindContractResponse findContract(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid) {
      var request = FindContractRequest.newBuilder()
              .setContractId(cid)
              .build();
      return client.findContract(request);
   }

   private static FinalizeContractResponse finalizeContract(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid) {
      var request = FinalizeContractRequest.newBuilder()
              .setContractId(cid)
              .build();
      return client.finalizeContract(request);
   }

   private static PromoteQuoteResponse promoteQuoteToContract(ContractServiceGrpc.ContractServiceBlockingStub client, String cid, String desc) {
      var quoteRequest = PromoteQuoteRequest.newBuilder()
              .setContractId(ContractId.newBuilder().setId(cid).build())
              .build();
      var pqrespWrong = client.promoteQuote(quoteRequest);
      return pqrespWrong;
   }

   private static NewQuoteResponse prepareQuote(ContractServiceGrpc.ContractServiceBlockingStub client) {
      var price = new BigDecimal(2500L, new MathContext(2));
      var quotedPrice = Transformer.transform(price);
      var request = NewQuoteRequest.newBuilder()
              .setFullNameOfParticipant("Man on the moon")
              .setDescriptionOfWorkRequested("Please give me a quote for installing a kitchen, everything will be delivered @ the house, including all appliances.")
              .setQuotedPrice(quotedPrice)
              .build();
      var aqresp = client.newQuote(request);
      return aqresp;
   }

   private static AddWorkDoneResponse AddSomeWork(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid, String desc, long amountInEur) {
      var cost = Transformer.transform(new BigDecimal(amountInEur, new MathContext(2)));
      var workdone = WorkDone.newBuilder()
              .setCostOfWork(cost)
              .setDescriptionOfWorkDone(desc);
      var adr = AddWorkDoneRequest.newBuilder()
              .setContractId(cid)
              .setWork(workdone)
              .build();
      var wdresp = client.addWorkDone(adr);
      return wdresp;

   }

   private static AddWorkDoneResponse tryEmptyWork(ContractServiceGrpc.ContractServiceBlockingStub client) {
      var cost = Transformer.transform(new BigDecimal(10L, new MathContext(2)));
      var workdone = WorkDone.newBuilder()
              .setCostOfWork(cost)
              .setDescriptionOfWorkDone("");
      var adr = AddWorkDoneRequest.newBuilder()
              .setContractId(ContractId.newBuilder().setId("Bogus").build())
              .setWork(workdone)
              .build();
      var wdresp = client.addWorkDone(adr);
      return wdresp;
   }

   private static void assertStatus(String action, Status status, Statuscode expectedStatuscode) {
      assertStatus(action, status, expectedStatuscode, null);
   }

   private static void assertStatus(String action, Status status, Statuscode expectedStatuscode, String expectedReason, String... expectedErrorCodes) {
      System.out.println("  -- " + action + " result : " + status.getStatus() + " " + status.getReason() + " "+ status.getErrorsList());

      if (status.getStatus() != expectedStatuscode) {
         throw new IllegalStateException("Expected statuscode " + expectedStatuscode + " but found " + status);
      }
      if (expectedReason != null && !expectedReason.equalsIgnoreCase(status.getReason())) {
         throw new IllegalStateException("Expected reason " + expectedReason + " but found " + status);
      }

      if (expectedErrorCodes != null && expectedErrorCodes.length > 0) {
         if (expectedErrorCodes.length != status.getErrorsCount()) {
            throw new IllegalStateException("Nr of errors do not match, expected " + expectedErrorCodes + " but found " + status);
         }
         for (int i = 0; i < expectedErrorCodes.length; i++) {
            var expErrConcat = expectedErrorCodes[i];
            var foundErr = status.getErrors(i);
            var foundErrConcat = foundErr.getErrorCode() + "/" + foundErr.getLocation();
            if (!StringUtils.equalsIgnoreCase(foundErrConcat, expErrConcat)) {
               throw new IllegalStateException("Error " + i + " mismatched, expected " + expErrConcat + " but found " + foundErrConcat);
            }
         }
      }
   }
}
