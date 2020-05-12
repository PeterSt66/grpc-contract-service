package nl.jdriven.blogs.svc.contract.api;

import nl.jdriven.blogs.svc.contract.model.exception.PreConditionNotMetException;
import nl.jdriven.blogs.svc.contract.proto.*;
import nl.jdriven.blogs.svc.contract.service.ContractService;
import org.apache.commons.lang3.StringUtils;

/**
 * Handles all gRPC functions by translating input and output from/to the model classes of the service
 * to make the service itself protocol-agnostic.
 * It's debatable whether constraint checking should be done here but hey, it's an example for ProtoBuf.
 *
 * @see ContractService
 */
public class ContractServiceApiHandler {
    private static final ContractService contractService = new ContractService();

    public NewQuoteResponse newQuote(NewQuoteRequest request) {
        String reason = "";
        if (StringUtils.isBlank(request.getDescriptionOfWorkRequested())) {
            reason = "Input.DescriptionOfWorkRequested.mandatory";
        } else if (StringUtils.isBlank(request.getFullNameOfCustomer())) {
            reason = StringUtils.joinWith(";", reason, "Input.FullNameOfCustomer.mandatory");
        } else if (!request.hasQuotedPrice()) {
            reason = StringUtils.joinWith(";", reason, "Input.QuotedPrice.mandatory");
        }
        if (reason.length() > 0) {
            throw new PreConditionNotMetException(reason);
        }

        String id = contractService.addQuote(request.getFullNameOfCustomer(),
                Transformer.transform(request.getQuotedPrice()),
                request.getDescriptionOfWorkRequested());

        return NewQuoteResponse.newBuilder().setContractId(id).build();
    }

    public AddWorkDoneResponse addWorkDone(AddWorkDoneRequest request) {
        String reasons = "";
        if (StringUtils.isBlank(request.getContractId())) {
            reasons = "Input.ContractId.mandatory";
        }
        if (!request.hasWork()) {
            reasons = StringUtils.joinWith(";", reasons, "Input.Work.mandatory");
        } else {
            // check the work package
            if (StringUtils.isBlank(request.getWork().getDescriptionOfWorkDone())) {
                reasons = StringUtils.joinWith(";", reasons, "Input.Work.Description.mandatory");
            }
            if (!request.getWork().hasCostOfWork()) {
                reasons = StringUtils.joinWith(";", reasons, "Input.Work.Cost.mandatory");
            }
        }
        if (reasons.length() > 0) {
            throw new PreConditionNotMetException(reasons);
        }

        var workDone = Transformer.transform(request.getWork());
        contractService.addWorkDone(request.getContractId(), workDone);

        return AddWorkDoneResponse.getDefaultInstance();
    }

    public FinalizeContractResponse finalizeContract(FinalizeContractRequest request) {
        checkContractIdPresent(request.getContractId());

        var profitMade = contractService.finalizeContract(request.getContractId());

        return FinalizeContractResponse.newBuilder()
                .setProfitMade(Transformer.transform(profitMade))
                .build();
    }

    public FindContractResponse find(FindContractRequest request) {
        checkContractIdPresent(request.getContractId());

        var contract = contractService.find(request.getContractId());

        return FindContractResponse.newBuilder().setContract(Transformer.transform(contract)).build();
    }

    public void promoteQuote(PromoteQuoteRequest request) {
        checkContractIdPresent(request.getContractId());

        contractService.promoteQuote(request.getContractId());
    }

    private void checkContractIdPresent(String contractId) {
        if (StringUtils.isBlank(contractId)) {
            throw new PreConditionNotMetException("Input.ContractId.mandatory");
        }
    }

}
