package nl.jdriven.blogs.svc.contract.api;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.perfmark.traceviewer.TraceEventViewer;
import lombok.extern.slf4j.Slf4j;
import nl.jdriven.blogs.svc.contract.model.exception.NotFoundException;
import nl.jdriven.blogs.svc.contract.model.exception.PreConditionNotMetException;
import nl.jdriven.blogs.svc.contract.proto.*;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * This class Implements the gRPC API calls by handling the communication and
 * relaying the call to the ContractServiceApiHandler.
 * @see ContractServiceApiHandler
 */
@Slf4j
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

    @Override
    public void serverCommand(ServerCommandRequest request, StreamObserver<ServerCommandResponse> responseObserver) {
        switch(request.getCmd()) {
            case "STOP" : {
                log.warn("Server stop command received");
                System.exit(0);
            }
            case "CYCLEPERFMARK" : {
                log.warn("Cycle PerfMark command received");
                try {
                    Writer writer = new StringWriter();
                    TraceEventViewer.writeTraceHtml(writer);
                    returnResponse(responseObserver, ServerCommandResponse.newBuilder().setResultDescription(writer.toString()).build());
                } catch (IOException e) {
                    log.warn("Could not write trace html: "+e,e);
                }
                log.warn("Cycle PerfMark - done");
                break;
            }
            default: throw new IllegalArgumentException("Cmd not recognized");
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
        else if (exc instanceof NotFoundException) {
            responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
        }
        else {
            var status = Status.fromThrowable(exc);
            responseObserver.onError(status.asRuntimeException());
        }
    }

}
