package nl.jdriven.blogs.svc.contract.service;

import nl.jdriven.blogs.svc.contract.model.exception.NotFoundException;
import nl.jdriven.blogs.svc.contract.model.exception.PreConditionNotMetException;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * this service implements the business logic of the Contract service.
 */
public class ContractService {
    private static final Map<String, Contract> contracts = new HashMap<>();

    /**
     * Start a new contact as a quote with a quoted fixed price and some info.
     * @return contract id.
     */
    public String addQuote(String fullNameOfParticipant, BigDecimal quotedPrice, String descriptionOfWorkRequested) {
        var contract = new Contract(
                UUID.randomUUID().toString(),
                quotedPrice,
                descriptionOfWorkRequested,
                fullNameOfParticipant);
        contracts.put(contract.getId(), contract);
        System.out.println("Added contract as a quote: " + contract);
        return contract.getId();
    }

    /**
     * Add some work that was done to the contract; the contract needs to be in status ATWORK.
     * @throws PreConditionNotMetException
     * @throws  NotFoundException
     */
    public void addWorkDone(String id, WorkDone workDone) {
        var c = contracts.get(id);
        if (c == null) {
            throw new NotFoundException();
        }
        if (c.getStatus() != Contract.Status.ATWORK) {
            throw new PreConditionNotMetException("Not.at.work");
        }
        c.addWorkDone(workDone);
    }

    /**
     * Promote the contract to ATWORK, can only be done on a contract that is a quote
     * and is done when the quote has been accepted.
     * @throws PreConditionNotMetException
     * @throws  NotFoundException
     */
    public void promoteQuote(String id) {
        var c = contracts.get(id);
        if (c == null) {
            throw new NotFoundException();
        }
        if (c.getStatus() != Contract.Status.QUOTE) {
            throw new PreConditionNotMetException("Not.a.quote");
        }
        c.setStatus(Contract.Status.ATWORK);
    }

    /**
     * Finalize the contract and calculate the profit made, can only be done on a contract that is ATWORK.
     * @return calculated profit
     * @throws PreConditionNotMetException
     */
    public BigDecimal finalizeContract(String id) {
        var c = contracts.get(id);
        if (c == null) {
            throw new NotFoundException();
        }
        if (c.getStatus() != Contract.Status.ATWORK) {
            throw new PreConditionNotMetException("Not.at.work");
        }

        c.setStatus(Contract.Status.FINALIZED);
        // total expenditure is all work costs combined
        var workCosts = c.getWorkDone().stream()
                .map(WorkDone::getCostOfWork)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO); // add() always delivers, so the default is for the compilerchecks

        return c.getQuotedPrice().subtract(workCosts);
    }

    /**
     * Search for the contract with given contract id.
     * @return contract found, otherwise:
     * @throws NotFoundException
     */
    public Contract find(String id) {
        var c = contracts.get(id);
        if (c == null) {
            throw new NotFoundException();
        }
        return c;
    }

}
