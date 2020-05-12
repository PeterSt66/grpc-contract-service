package nl.jdriven.blogs.svc.contract.service;

import lombok.extern.slf4j.Slf4j;
import nl.jdriven.blogs.svc.contract.model.exception.NotFoundException;
import nl.jdriven.blogs.svc.contract.model.exception.PreConditionNotMetException;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * this service implements the business logic of the Contract service.
 */
@Slf4j
public class ContractService {
    private static final Map<String, Contract> contracts = new HashMap<>();

    /**
     * Start a new contact as a quote with a quoted fixed price and some info.
     * @return contract id.
     */
    public String addQuote(String fullNameOfCustomer, Money quotedPrice, String descriptionOfWorkRequested) {
        var contract = new Contract(
                UUID.randomUUID().toString(),
                quotedPrice,
                descriptionOfWorkRequested,
                fullNameOfCustomer);
        contracts.put(contract.getId(), contract);
        log.info("Added contract as a quote: \n{}" , contract);
        return contract.getId();
    }

    /**
     * Add some work that was done to the contract; the contract needs to be in status ATWORK.
     * @throws PreConditionNotMetException
     * @throws NotFoundException
     */
    public void addWorkDone(String id, WorkDone workDone) {
        var contract = contracts.get(id);
        if (contract == null) {
            throw new NotFoundException();
        }
        if (contract.getStatus() != Contract.Status.ATWORK) {
            throw new PreConditionNotMetException("Not.at.work");
        }
        contract.addWorkDone(workDone);
        log.info("Added work to contract: \n{}" , contract);
    }

    /**
     * Promote the contract to ATWORK, can only be done on a contract that is a quote
     * and is done when the quote has been accepted.
     * @throws PreConditionNotMetException
     * @throws  NotFoundException
     */
    public void promoteQuote(String id) {
        var contract = contracts.get(id);
        if (contract == null) {
            throw new NotFoundException();
        }
        if (contract.getStatus() != Contract.Status.QUOTE) {
            throw new PreConditionNotMetException("Not.a.quote");
        }
        contract.setStatus(Contract.Status.ATWORK);
        log.info("quote promoted to contract: \n{}", contract);
    }

    /**
     * Finalize the contract and calculate the profit made, can only be done on a contract that is ATWORK.
     * @return calculated profit
     * @throws PreConditionNotMetException
     */
    public Money finalizeContract(String id) {
        var contract = contracts.get(id);
        if (contract == null) {
            throw new NotFoundException();
        }
        if (contract.getStatus() != Contract.Status.ATWORK) {
            throw new PreConditionNotMetException("Not.at.work");
        }

        contract.setStatus(Contract.Status.FINALIZED);
        // total expenditure is all work costs combined
        var workCosts = contract.getWorkDone().stream()
                .map(WorkDone::getCostOfWork)
                .reduce(Money::plus)
                .orElse(Money.zero(CurrencyUnit.EUR)); // add() always delivers, so the default is only for the compilerchecks

        log.info("finalized contract: \nworkCosts={}\nquotedPrice={}", workCosts.toBigMoney(), contract.getQuotedPrice());
        log.info("finalized contract: \n{}", contract);
        return contract.getQuotedPrice().minus(workCosts);
    }

    /**
     * Search for the contract with given contract id.
     * @return contract found, otherwise:
     * @throws NotFoundException
     */
    public Contract find(String id) {
        var contract = contracts.get(id);
        if (contract == null) {
            throw new NotFoundException();
        }
        return contract;
    }

}
