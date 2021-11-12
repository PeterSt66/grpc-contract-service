package nl.jdriven.blogs.svc.contract;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import nl.jdriven.blogs.svc.contract.api.Transformer;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneRequest;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneResponse;
import nl.jdriven.blogs.svc.contract.proto.BoolOption;
import nl.jdriven.blogs.svc.contract.proto.Contract;
import nl.jdriven.blogs.svc.contract.proto.ContractId;
import nl.jdriven.blogs.svc.contract.proto.ContractServiceGrpc;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractRequest;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractResponse;
import nl.jdriven.blogs.svc.contract.proto.FindContractsRequest;
import nl.jdriven.blogs.svc.contract.proto.FindContractsResponse;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.Options;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.ResponseStatus;
import nl.jdriven.blogs.svc.contract.proto.WorkDone;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

public class ClientMain {

   public static void main(String[] args) {
      var channel = ManagedChannelBuilder
              .forAddress("localhost", 53000)
              .usePlaintext() // disable TLS which is enabled by default and requires certificates
              .build();
      var client = ContractServiceGrpc.newBlockingStub(channel);

      // Start with a quote
      var aqresp = prepareQuote(client);
      assertStatus("prepareQuote", aqresp.getStatus(), ResponseStatus.Code.OK);
      var cid = aqresp.getQuoteId();

      // Add some work to quote - should fail
      var response = AddSomeWork(client, cid, "Bad work", 200L);
      assertStatus("AddSomeWork-WhileNotAtWork", response.getStatus(), ResponseStatus.Code.VALIDATION_ERR, "Contract.not.workable", "Not.at.work/Contract");


      // Add empty work to quote - should fail
      var ewresp = tryEmptyWork(client);
      assertStatus("AddSomeWork-ValidationErrors", ewresp.getStatus(),
              ResponseStatus.Code.VALIDATION_ERR, "Validation failed", "mandatory/WorkDone.Description", "Cost.Below.Minimal.Amount/WorkDone.Cost");

      // promote unfound quote to contract - should fail
      var pqresp = promoteQuoteToContract(client, "Bogus", "unknown quote");
      assertStatus("promoteQuoteToContract-ContractNotFound", pqresp.getStatus(), ResponseStatus.Code.NOTFOUND, "Notfound");

      // promote quote to contract
      var pqresp2 = promoteQuoteToContract(client, cid.getId(), "proper quote");
      assertStatus("promoteQuoteToContract-Ok", pqresp2.getStatus(), ResponseStatus.Code.OK);

      // Add some work to contract
      var aswresp2 = AddSomeWork(client, cid, "Constructed new plumbing and installed all cabinets including the kitchen sink", 1200L);
      assertStatus("AddSomeWork-Ok", aswresp2.getStatus(), ResponseStatus.Code.OK, "Done");

      // Add some more work to contract
      var aswresp3 = AddSomeWork(client, cid, "Installed all appliances", 1000L);
      assertStatus("AddSomeWork-Ok", aswresp3.getStatus(), ResponseStatus.Code.OK, "Done");

      var fcresp = finalizeContract(client, cid);
      assertStatus("finalizeContract-Ok", fcresp.getStatus(), ResponseStatus.Code.OK, "Ok");
      System.out.println("Finalize contract: profit=" + fcresp.getProfitMade().getCurrencyCode() + " " + fcresp.getProfitMade().getUnits());

      var fcresp2 = findContracts(client, cid, "body");
      assertStatus("findContracts-on-id-Ok", fcresp2.getStatus(), ResponseStatus.Code.OK, "Ok");
      dumpContractList(fcresp2.getContractsList(), "on ids");
      //System.out.println("First contract: " + fcresp2.getContracts(0));


      // make some additional contracts for filtering
      prepareQuote(client);

      aqresp = prepareQuote(client);
      promoteQuoteToContract(client, aqresp.getQuoteId().getId(), "evolve");

      aqresp = prepareQuote(client);
      promoteQuoteToContract(client, aqresp.getQuoteId().getId(), "evolve");
      AddSomeWork(client, aqresp.getQuoteId(), "Foo work", 1000L);

      fcresp2 = findContracts(client, true, true, true, "quote");
      assertStatus("findContracts-Ok-all", fcresp2.getStatus(), ResponseStatus.Code.OK, "Ok");
      dumpContractList(fcresp2.getContractsList(), "All");

      fcresp2 = findContracts(client, true, false, true, "workdone");
      assertStatus("findContracts-Ok-no-work", fcresp2.getStatus(), ResponseStatus.Code.OK, "Ok");
      dumpContractList(fcresp2.getContractsList(), "Only quote and final");

      fcresp2 = findContracts(client, false, false, true, "");
      assertStatus("findContracts-Ok-no-work-quote", fcresp2.getStatus(), ResponseStatus.Code.OK, "Ok");
      dumpContractList(fcresp2.getContractsList(), "Only final");

      // avoid nastiness on the serverside due to the sudden death of the connection
      ((ManagedChannel) client.getChannel()).shutdownNow();
   }

   private static void dumpContractList(List<Contract> contracts, String filterDesc) {
      StringBuilder sb = new StringBuilder("Find contract gave with filter=(" + filterDesc + "): ");
      contracts.forEach(c ->
              sb.append("\n")
                      .append("   cid=").append(c.getContractId().getId())
                      .append("    st=").append(c.getStatus())
                      .append("    w#=").append(c.getWorkCount())
                      .append("    qt=").append(c.hasQuote() ? "YES" : "NO"));


      System.out.println(sb);
   }


   private static FindContractsResponse findContracts(ContractServiceGrpc.ContractServiceBlockingStub client, ContractId cid, String includeOption) {
      var requestBuilder = FindContractsRequest.newBuilder()
              .addContractId(cid);
      if (StringUtils.isNotBlank(includeOption)) {
         requestBuilder.setOptions(Options.newBuilder().setIncluded(includeOption));
      }
      return client.findContracts(requestBuilder.build());
   }

   private static FindContractsResponse findContracts(ContractServiceGrpc.ContractServiceBlockingStub client, boolean withQuotes, boolean withWork, boolean withFinal, String includeOption) {
      var requestBuilder = FindContractsRequest.newBuilder()
              .setIncludeQuotes(withQuotes ? BoolOption.YES : BoolOption.NO)
              .setIncludeWorking(withWork ? BoolOption.YES : BoolOption.UNDEF)
              .setIncludeFinalized(withFinal ? BoolOption.YES : BoolOption.UNDEF);
      if (StringUtils.isNotBlank(includeOption)) {
         requestBuilder.setOptions(Options.newBuilder().setIncluded(includeOption));
      }
      return client.findContracts(requestBuilder.build());
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
      return client.promoteQuote(quoteRequest);
   }

   private static NewQuoteResponse prepareQuote(ContractServiceGrpc.ContractServiceBlockingStub client) {
      var price = new BigDecimal(2500L, new MathContext(2));
      var quotedPrice = Transformer.transform(price);
      var request = NewQuoteRequest.newBuilder()
              .setFullNameOfParticipant("Man on the moon")
              .setDescriptionOfWorkRequested("Please give me a quote for installing a kitchen, everything will be delivered @ the house, including all appliances.")
              .setQuotedPrice(quotedPrice)
              .build();
      return client.newQuote(request);
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
      return client.addWorkDone(adr);

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
      return client.addWorkDone(adr);
   }

   private static void assertStatus(String action, ResponseStatus status, ResponseStatus.Code expectedCode) {
      assertStatus(action, status, expectedCode, null);
   }

   private static void assertStatus(String action, ResponseStatus status, ResponseStatus.Code expectedCode, String expectedReason, String... expectedErrorCodes) {
      System.out.print("  -- " + action);
      System.out.print(" result : st=" + status.getStatus());
      System.out.print(" rs=" + status.getReason());
      if (StringUtils.isNotBlank(status.getWarning())) {
         System.out.print(" warn=" + status.getWarning());
      }
      System.out.print(" err=" + status.getErrorsList() + "\n");

      if (status.getStatus() != expectedCode) {
         throw new IllegalStateException("Expected ResponseStatus.Code " + expectedCode + " but found " + status);
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
