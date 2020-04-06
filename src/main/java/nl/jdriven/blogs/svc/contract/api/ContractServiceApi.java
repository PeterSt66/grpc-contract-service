package nl.jdriven.blogs.svc.contract.api;

import io.grpc.stub.StreamObserver;
import nl.jdriven.blogs.svc.contract.model.api.DetailedResult;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.proto.*;
import nl.jdriven.blogs.svc.contract.service.ContractService;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

import static nl.jdriven.blogs.svc.contract.model.api.DetailedResult.Result.FAILED;

public class ContractServiceApi extends ContractServiceGrpc.ContractServiceImplBase {

    private static final ContractService handler = new ContractService();

    @Override
    public void newQuote(NewQuoteRequest request, StreamObserver<NewQuoteResponse> responseObserver) {
        returnResponse(responseObserver, newQuote(request));
    }

    private NewQuoteResponse newQuote(NewQuoteRequest request) {
        String reason = "";
        if (StringUtils.isBlank(request.getDescriptionOfWorkRequested())) {
            reason = "Input.DescriptionOfWorkRequested.mandatory";
        } else if (StringUtils.isBlank(request.getFullNameOfParticipant())) {
            reason = StringUtils.joinWith(";", reason, "Input.FullNameOfParticipant.mandatory");
        } else if (!request.hasQuotedPrice() || Transformer.transform(request.getQuotedPrice()).isEmpty()) {
            reason = StringUtils.joinWith(";", reason, "Input.QuotedPrice.mandatory");
        }
        if (reason.length() > 0) {
            return NewQuoteResponse.newBuilder().setStatus(Result.FAILED).setReason(reason).build();
        }

        String id = handler.addQuote(request.getFullNameOfParticipant(),
                Transformer.transform(request.getQuotedPrice()).get(),
                request.getDescriptionOfWorkRequested());

        return NewQuoteResponse.newBuilder()
                .setQuoteId(ContractId.newBuilder().setId(id).build())
                .setStatus(Result.OK)
                .setReason("Quote.added")
                .build();
    }

    @Override
    public void addWorkDone(AddWorkDoneRequest request, StreamObserver<AddWorkDoneResponse> responseObserver) {
        returnResponse(responseObserver, addWorkDone(request));
    }

    private AddWorkDoneResponse addWorkDone(AddWorkDoneRequest request) {
        String reason = "";
        if (!request.hasContractId() || StringUtils.isBlank(request.getContractId().getId())) {
            reason = "Input.ContractId.mandatory";
        }
        if (!request.hasWork()) {
            reason = StringUtils.joinWith(";", reason, "Input.Work.mandatory");
        }
        else {
            // check the work package
            if (StringUtils.isBlank(request.getWork().getDescriptionOfWorkDone())) {
                reason = StringUtils.joinWith(";", reason, "Input.Work.Description.mandatory");
            }
            if (!request.getWork().hasCostOfWork() || Transformer.transform(request.getWork().getCostOfWork()).isEmpty()) {
                reason = StringUtils.joinWith(";", reason, "Input.Work.Cost.mandatory");
            }
        }
        if (reason.length() > 0) {
            return AddWorkDoneResponse.newBuilder().setStatus(Result.FAILED).setReason(reason).build();
        }

        var workDone = Transformer.transform(request.getWork());
        DetailedResult result = handler.addWorkDone(request.getContractId().getId(), workDone);

        return AddWorkDoneResponse.newBuilder()
                .setStatus(Transformer.transform(result.getResult()))
                .setReason(result.getReason())
                .build();
    }

    @Override
    public void finalizeContract(ContractId request, StreamObserver<FinalizeContractResponse> responseObserver) {
        returnResponse(responseObserver, finalizeContract(request));
    }

    private FinalizeContractResponse finalizeContract(ContractId request) {
        var reason = "";
        if (StringUtils.isBlank(request.getId())) {
            reason = "Input.ContractId.mandatory";
        }
        if (reason.length() > 0) {
            return FinalizeContractResponse.newBuilder().setStatus(Result.FAILED).setReason(reason).build();
        }
        Response<BigDecimal> response = handler.finalizeContract(request.getId());

        return FinalizeContractResponse.newBuilder()
                .setStatus(Transformer.transform(response.getDetailedResult().getResult()))
                .setReason(response.getDetailedResult().getReason())
                .setProfitMade(Transformer.transform(response.get()))
                .build();
    }


    @Override
    public void find(ContractId request, StreamObserver<ContractResponse> responseObserver) {
        returnResponse(responseObserver, find(request));
    }

    private ContractResponse find(ContractId request) {
        var reason = "";
        if (StringUtils.isBlank(request.getId())) {
            reason = "Input.ContractId.mandatory";
        }
        if (reason.length() > 0) {
            return ContractResponse.newBuilder().setStatus(Result.FAILED).setReason(reason).build();
        }

        Response<Contract> response = handler.find(request.getId());

        return ContractResponse.newBuilder()
                .setContract(Transformer.transform(response.get()))
                .setStatus(Transformer.transform(response.getDetailedResult().getResult()))
                .setReason(response.getDetailedResult().getReason())
                .build();
    }

    @Override
    public void promoteQuote(ContractId request, StreamObserver<PromoteQuoteResponse> responseObserver) {
        var result = promoteQuote(request);
        returnResponse(responseObserver,
                PromoteQuoteResponse.newBuilder()
                        .setStatus(Transformer.transform(result.getResult()))
                        .setReason(result.getReason())
                        .build());
    }

    private DetailedResult promoteQuote(ContractId request) {
        var reason = "";
        if (StringUtils.isBlank(request.getId())) {
            reason = "Input.ContractId.mandatory";
        }
        if (reason.length() > 0) {
            return DetailedResult.of(FAILED, reason);
        }
        return handler.promoteQuote(request.getId());
    }

    private <T> void returnResponse(StreamObserver<T> responseObserver, T response) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
