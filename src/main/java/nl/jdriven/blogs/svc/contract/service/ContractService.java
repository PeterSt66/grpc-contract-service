package nl.jdriven.blogs.svc.contract.service;

import nl.jdriven.blogs.svc.contract.model.api.DetailedResult;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContractService {
    private static final Map<String, Contract> contracts = new HashMap<>();

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

    public DetailedResult addWorkDone(String id, WorkDone workDone) {
        var c = contracts.get(id);
        if (c == null) {
            return DetailedResult.NOTFOUND;
        }
        if (c.getStatus() != Contract.Status.ATWORK) {
            return DetailedResult.of(DetailedResult.Result.FAILED, "Not.at.work");
        }
        c.addWorkDone(workDone);
        return DetailedResult.OK;
    }

    public DetailedResult promoteQuote(String id) {
        var c = contracts.get(id);
        if (c == null) {
            return DetailedResult.NOTFOUND;
        }
        if (c.getStatus() != Contract.Status.QUOTE) {
            return DetailedResult.of(DetailedResult.Result.FAILED, "Not.a.quote");
        }
        c.setStatus(Contract.Status.ATWORK);
        return DetailedResult.OK;
    }

    public Response<BigDecimal> finalizeContract(String id) {
        DetailedResult result = null;
        var c = contracts.get(id);
        if (c == null) {
            result = DetailedResult.NOTFOUND;
        } else if (c.getStatus() != Contract.Status.ATWORK) {
            result = DetailedResult.of(DetailedResult.Result.FAILED, "Not.at.work");
        }
        if (result != null) {
            return Response.of(result);
        }

        c.setStatus(Contract.Status.FINALIZED);
        // total expenditure is all work costs combined
        var workCosts = c.getWorkDone().stream()
                .map(WorkDone::getCostOfWork)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO); // add() always delivers, so the default is for the compilerchecks

        var profit = c.getQuotedPrice().subtract(workCosts);
        return Response.of(DetailedResult.OK, profit);
    }

    public Response<Contract> find(String id) {
        var c = contracts.get(id);
        if (c == null) {
            return Response.of(DetailedResult.NOTFOUND);
        }
        return Response.of(DetailedResult.OK, c);
    }

}
