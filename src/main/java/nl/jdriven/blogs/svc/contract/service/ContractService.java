package nl.jdriven.blogs.svc.contract.service;

import nl.jdriven.blogs.svc.contract.model.api.Error;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static nl.jdriven.blogs.svc.contract.model.api.Response.Result.NOTFOUND;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ContractService {
   private static final Map<String, Contract> contracts = new HashMap<>();

   public Response<String> addQuote(String fullNameOfParticipant, Optional<BigDecimal> quotedPrice, String descriptionOfWorkRequested) {
      // validate the quote
      List<Error> errors = new ArrayList<>();
      if (StringUtils.isBlank(descriptionOfWorkRequested)) {
         errors.add(Error.of("DescriptionOfWorkRequested", "mandatory"));
      }
      if (StringUtils.isBlank(fullNameOfParticipant)) {
         errors.add(Error.of("FullNameOfParticipant", "mandatory"));
      }
      if (quotedPrice.isEmpty()) {
         errors.add(Error.of("QuotedPrice", "mandatory"));
      }
      if (!errors.isEmpty()) {
         return Response.of(Response.Result.FAILED, "Input incorrect", errors);
      }
      @SuppressWarnings("OptionalGetWithoutIsPresent")
      var contract = new Contract(
              UUID.randomUUID().toString(),
              quotedPrice.get(),
              descriptionOfWorkRequested,
              fullNameOfParticipant);
      contracts.put(contract.getId(), contract);
      System.out.println("Added contract as a quote: " + contract);
      return Response.of(contract.getId());
   }

   public Response<?> addWorkDone(String id, WorkDone workDone) {
      // validate the work package
      List<Error> errors = new ArrayList<>();
      if (StringUtils.isBlank(workDone.getDescriptionOfWorkDone())) {
         errors.add(Error.of("mandatory", "WorkDone.Description"));
      }
      if (workDone.getCostOfWork() == null || workDone.getCostOfWork().compareTo(BigDecimal.ZERO) <= 0) {
         errors.add(Error.of("mandatory", "WorkDone.Cost"));
      }
      if (workDone.getCostOfWork().compareTo(BigDecimal.valueOf(100L)) < 0) {
         errors.add(Error.of("Cost.Below.Minimal.Amount", "WorkDone.Cost", workDone.getCostOfWork().toString()));
      }
      if (!errors.isEmpty()) {
         return Response.of(Response.Result.FAILED, "Validation failed", errors);
      }

      var c = contracts.get(id);
      if (c == null) {
         return Response.NOTFOUND;
      }
      if (c.getStatus() != Contract.Status.ATWORK) {
         return Response.of(Response.Result.FAILED, "Contract.not.workable", Error.of("Not.at.work","Contract", id));
      }
      c.addWorkDone(workDone);
      return Response.OK;
   }

   public Response<?> promoteQuote(String id) {
      var c = contracts.get(id);
      if (c == null) {
         return Response.NOTFOUND;
      }
      if (c.getStatus() != Contract.Status.QUOTE) {
         return Response.of(Response.Result.FAILED, "Promotion.not.possible", Error.of("Not.a.quote", "Contract.Status", id));
      }
      c.setStatus(Contract.Status.ATWORK);
      return Response.OK;
   }

   public Response<BigDecimal> finalizeContract(String id) {
      var c = contracts.get(id);
      if (c == null) {
         return Response.of(NOTFOUND, "Contract.Not.Found", Error.of("Not.Found", "Contract", id));
      } else if (c.getStatus() != Contract.Status.ATWORK) {
         return Response.of(Response.Result.FAILED, "Finalize.not.possible", Error.of("Not.at.work", "Contract", id));
      }

      c.setStatus(Contract.Status.FINALIZED);
      // total expenditure is all work costs combined
      var workCosts = c.getWorkDone().stream()
              .map(WorkDone::getCostOfWork)
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO); // add() always delivers, so this is to appease the compilerchecks

      var profit = c.getQuotedPrice().subtract(workCosts);
      return Response.of(profit);
   }

   public Response<Contract> find(String id) {
      var c = contracts.get(id);
      if (c == null) {
         return Response.of(NOTFOUND, "Contract.Not.Found", Error.of("Not.Found", "Contract", id));
      }
      return Response.of(c);
   }

}
