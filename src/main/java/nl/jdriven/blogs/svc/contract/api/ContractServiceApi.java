package nl.jdriven.blogs.svc.contract.api;

import io.grpc.stub.StreamObserver;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneRequest;
import nl.jdriven.blogs.svc.contract.proto.AddWorkDoneResponse;
import nl.jdriven.blogs.svc.contract.proto.ContractId;
import nl.jdriven.blogs.svc.contract.proto.ContractServiceGrpc;
import nl.jdriven.blogs.svc.contract.proto.Error;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractRequest;
import nl.jdriven.blogs.svc.contract.proto.FinalizeContractResponse;
import nl.jdriven.blogs.svc.contract.proto.FindContractsRequest;
import nl.jdriven.blogs.svc.contract.proto.FindContractsResponse;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.NewQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteRequest;
import nl.jdriven.blogs.svc.contract.proto.PromoteQuoteResponse;
import nl.jdriven.blogs.svc.contract.proto.ResponseStatus;
import nl.jdriven.blogs.svc.contract.proto.Statuscode;
import nl.jdriven.blogs.svc.contract.service.ContractService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.jdriven.blogs.svc.contract.model.api.Response.Result.*;
import static nl.jdriven.blogs.svc.contract.proto.BoolOption.UNDEF;

public class ContractServiceApi extends ContractServiceGrpc.ContractServiceImplBase {

   private static final ContractService handler = new ContractService();

   @Override
   public void newQuote(NewQuoteRequest request, StreamObserver<NewQuoteResponse> responseObserver) {
      returnResponse(responseObserver, newQuote(request));
   }

   private NewQuoteResponse newQuote(NewQuoteRequest request) {
      if (StringUtils.isBlank(request.getDescriptionOfWorkRequested())) {
         return NewQuoteResponse.newBuilder()
                 .setStatus(asStatus(Statuscode.FAILED, "Validation failed", "DescriptionOfWorkRequested", "mandatory"))
                 .build();
      }

      var result = handler.addQuote(request.getFullNameOfParticipant(),
              Transformer.transform(request.getQuotedPrice()),
              request.getDescriptionOfWorkRequested());

      var newQuoteResponseBuilder = NewQuoteResponse.newBuilder().setStatus(Transformer.transform(result));

      if (result.getResult() == Response.Result.OK) {
         var id = result.getResultObject();
         newQuoteResponseBuilder.setQuoteId(ContractId.newBuilder().setId(id).build());
      }

      return newQuoteResponseBuilder.build();
   }


   @Override
   public void addWorkDone(AddWorkDoneRequest request, StreamObserver<AddWorkDoneResponse> responseObserver) {
      returnResponse(responseObserver, addWorkDone(request));
   }

   private AddWorkDoneResponse addWorkDone(AddWorkDoneRequest request) {
      // validate the part of the input that is 'non-functional'
      var errors = new ArrayList<Error>();
      if (!request.hasContractId() || StringUtils.isBlank(request.getContractId().getId())) {
         errors.add(asError("ContractId", "Mandatory"));
      }
      if (!request.hasWork()) {
         errors.add(asError("Work", "Mandatory"));
      }
      if (!errors.isEmpty()) {
         return AddWorkDoneResponse.newBuilder()
                 .setStatus(asStatus(Statuscode.FAILED, "Validation failed", errors))
                 .build();
      }

      var workDone = Transformer.transform(request.getWork());
      var result = handler.addWorkDone(request.getContractId().getId(), workDone);

      return AddWorkDoneResponse.newBuilder()
              .setStatus(Transformer.transform(result))
              .build();
   }

   @Override
   public void finalizeContract(FinalizeContractRequest request, StreamObserver<FinalizeContractResponse> responseObserver) {
      returnResponse(responseObserver, finalizeContract(request));
   }

   private FinalizeContractResponse finalizeContract(FinalizeContractRequest request) {
      if (!request.hasContractId() || StringUtils.isBlank(request.getContractId().getId())) {
         return FinalizeContractResponse.newBuilder()
                 .setStatus(asStatus(Statuscode.FAILED, "Validation failed", "ContractId", "mandatory"))
                 .build();
      }

      var response = handler.finalizeContract(request.getContractId().getId());

      return FinalizeContractResponse.newBuilder()
              .setStatus(Transformer.transform(response, "This call has a warning"))
              .setProfitMade(Transformer.transform(response.getResultObject()))
              .build();
   }


   @Override
   public void findContracts(FindContractsRequest request, StreamObserver<FindContractsResponse> responseObserver) {
      returnResponse(responseObserver, find(request));
   }

   private FindContractsResponse find(FindContractsRequest request) {
      var includeQuotes = request.getIncludeQuotes();
      var includeFinalized = request.getIncludeFinalized();
      var includeWorking = request.getIncludeWorking();

      Response<List<Contract>> response;
      var noFiltering = includeQuotes == UNDEF && includeFinalized == UNDEF && includeWorking == UNDEF;
      if (noFiltering) {
         if (request.getContractIdCount() == 0) {
            return FindContractsResponse.newBuilder()
                    .setStatus(asStatus(Statuscode.FAILED, "Validation failed", "ContractIds", "mandatory"))
                    .build();
         }
         var cids = request.getContractIdList().stream()
                 .map(ContractId::getId)
                 .filter(StringUtils::isNotBlank)
                 .collect(Collectors.toList());
         response = handler.findOnIds(cids);
      } else {
         response = handler.findOnIncludes(Transformer.transform(includeQuotes), Transformer.transform(includeWorking), Transformer.transform(includeFinalized));
      }

      var responseBuilder = FindContractsResponse.newBuilder().setStatus(Transformer.transform(response));
      if (response.getResult() == OK) {
         var contractsFound = response.getResultObject();
         // determine which part we need to include in the response objects
         var includeQuotePart = true;
         var includeWorkPart = true;
         if (request.hasOptions() && StringUtils.isNotBlank(request.getOptions().getIncluded())) {
            var included = request.getOptions().getIncluded().toUpperCase();
            includeWorkPart = included.contains("WORKDONE");
            includeQuotePart = included.contains("QUOTE");
         }
         List<nl.jdriven.blogs.svc.contract.proto.Contract> contracts =
                 Transformer.transform(contractsFound, includeQuotePart, includeWorkPart);
         responseBuilder.addAllContracts(contracts);
      }
      return responseBuilder.build();
   }

   @Override
   public void promoteQuote(PromoteQuoteRequest request, StreamObserver<PromoteQuoteResponse> responseObserver) {

      var result = promoteQuote(request);

      returnResponse(responseObserver,
              PromoteQuoteResponse.newBuilder()
                      .setStatus(Transformer.transform(result))
                      .build());
   }

   private Response<?> promoteQuote(PromoteQuoteRequest request) {
      if (!request.hasContractId() || StringUtils.isBlank(request.getContractId().getId())) {
         return Response.of(FAILED, "Validation failed", asError("ContractId", "mandatory"));
      }
      return handler.promoteQuote(request.getContractId().getId());
   }

   // ////////////////////////////////
   //
   // Local helper methods below
   //
   // ////////////////////////////////
   private <T> void returnResponse(StreamObserver<T> responseObserver, T response) {
      responseObserver.onNext(response);
      responseObserver.onCompleted();
   }

   private ResponseStatus asStatus(Statuscode statusCode, String reason, List<Error> errors) {
      return ResponseStatus.newBuilder()
              .setStatus(statusCode)
              .setReason(reason)
              .addAllErrors(errors)
              .build();
   }

   private ResponseStatus asStatus(Statuscode statusCode, String reason, String location, String errCode) {
      return ResponseStatus.newBuilder()
              .setStatus(statusCode)
              .setReason(reason)
              .addErrors(asError(location, errCode))
              .build();
   }

   private Error asError(String location, String errCode) {
      return Error.newBuilder().setLocation(location).setErrorCode(errCode).build();
   }

}
