package nl.jdriven.blogs.svc.contract.api;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import nl.jdriven.blogs.svc.contract.model.exception.NotFoundException;
import nl.jdriven.blogs.svc.contract.model.exception.PreConditionNotMetException;
import nl.jdriven.blogs.svc.contract.proto.*;

/**
 * This class Implements the gRPC API calls by handling the communication and
 * relaying the call to the ContractServiceApiHandler.
 * @see ContractServiceApiHandler
 */
public class ContractServiceApi extends ContractServiceGrpc.ContractServiceImplBase {

    private static final ContractServiceApiHandler handler = new ContractServiceApiHandler();

    @Override
    public void newQuote(NewQuoteRequest request, StreamObserver<NewQuoteResponse> responseObserver) {
        try {
            returnResponse(responseObserver, handler.newQuote(request));
        } catch (Exception e) {
            returnError(responseObserver, e);
        }
    }

    @Override
    public void addWorkDone(AddWorkDoneRequest request, StreamObserver<AddWorkDoneResponse> responseObserver) {
        try {
            returnResponse(responseObserver, handler.addWorkDone(request));
        } catch (Exception e) {
            returnError(responseObserver, e);
        }
    }

    @Override
    public void finalizeContract(FinalizeContractRequest request, StreamObserver<FinalizeContractResponse> responseObserver) {
        try {
            returnResponse(responseObserver, handler.finalizeContract(request));
        } catch (Exception e) {
            returnError(responseObserver, e);
        }
    }

    @Override
    public void findContract(FindContractRequest request, StreamObserver<FindContractResponse> responseObserver) {
        try {
            returnResponse(responseObserver, handler.find(request));
        } catch (Exception e) {
            returnError(responseObserver, e);
        }
    }

    @Override
    public void promoteQuote(PromoteQuoteRequest request, StreamObserver<PromoteQuoteResponse> responseObserver) {
        try {
            handler.promoteQuote(request);
            returnResponse(responseObserver, PromoteQuoteResponse.getDefaultInstance());
        } catch (Exception e) {
            returnError(responseObserver, e);
        }
    }

    private <T> void returnResponse(StreamObserver<T> responseObserver, T response) {
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void returnError(StreamObserver<?> responseObserver, Throwable exc) {
        if (exc instanceof PreConditionNotMetException) {
            var e = (PreConditionNotMetException) exc;
            var status = Status.FAILED_PRECONDITION
                    .withCause(exc)
                    .withDescription(e.getConditionFailuresAsString());
            responseObserver.onError(status.asRuntimeException());
        }
        if (exc instanceof NotFoundException) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
        var status = Status.fromThrowable(exc);
        responseObserver.onError(status.asRuntimeException());
    }

}
