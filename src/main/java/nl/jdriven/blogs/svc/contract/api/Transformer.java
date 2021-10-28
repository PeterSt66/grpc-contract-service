package nl.jdriven.blogs.svc.contract.api;

import com.google.type.Money;
import nl.jdriven.blogs.svc.contract.model.api.BooleanOption;
import nl.jdriven.blogs.svc.contract.model.api.Error;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Transformer {
   private Transformer() {
   }

   // unverified cut-and-grab job, might work but who knows.
   public static Optional<BigDecimal> transform(final Money amount) {
      // only accept EUR or no code (default is EUR)
      var currencyCode = amount.getCurrencyCode();
      if (StringUtils.isNotBlank(currencyCode) && !"EUR".equalsIgnoreCase(currencyCode)) {
         return Optional.empty();
      }
      var bd = new BigDecimal(amount.getUnits()).add(new BigDecimal(amount.getNanos(), new MathContext(9)));
      return Optional.of(bd);
   }

   // quick hack, does not support cents (why will the Internet not help me?)
   public static Money transform(BigDecimal amount) {
      return Money.newBuilder()
              .setCurrencyCode("EUR")
              .setUnits(amount.longValue())
              .build();

   }

   public static WorkDone transform(nl.jdriven.blogs.svc.contract.proto.WorkDone work) {
      var costOfWork = transform(work.getCostOfWork()).orElse(null);
      return new WorkDone(costOfWork, work.getDescriptionOfWorkDone());
   }

   public static nl.jdriven.blogs.svc.contract.proto.WorkDone transform(WorkDone work) {
      return nl.jdriven.blogs.svc.contract.proto.WorkDone.newBuilder()
              .setCostOfWork(transform(work.getCostOfWork()))
              .setDescriptionOfWorkDone(work.getDescriptionOfWorkDone())
              .build();
   }

   public static nl.jdriven.blogs.svc.contract.proto.Contract transform(Contract c) {
      return transform(c, true, true);
   }

   public static nl.jdriven.blogs.svc.contract.proto.Contract transform(Contract c, boolean includeQuote, boolean includeWork) {
      var builder = nl.jdriven.blogs.svc.contract.proto.Contract.newBuilder()
              .setContractId(nl.jdriven.blogs.svc.contract.proto.ContractId.newBuilder().setId(c.getId()))
              .setStatus(c.getStatus().name());

      if (includeQuote) {
         var quote = nl.jdriven.blogs.svc.contract.proto.Quote.newBuilder()
                 .setDescriptionOfWorkRequested(c.getDescriptionOfWorkRequested())
                 .setFullNameOfParticipant(c.getFullNameOfParticipant())
                 .setQuotedPrice(Transformer.transform(c.getQuotedPrice()));
         builder.setQuote(quote);
      }

      if (includeWork) {
         var workDone = c.getWorkDone().stream().map(Transformer::transform).collect(Collectors.toUnmodifiableList());
         builder.addAllWork(workDone);
      }

      return builder.build();
   }

   public static List<nl.jdriven.blogs.svc.contract.proto.Contract> transform(List<Contract> contracts, boolean includeQuote, boolean includeWork) {
      if (contracts == null) {
         return new ArrayList<>();
      }
      return contracts.stream()
              .map(c -> transform(c, includeQuote, includeWork))
              .collect(Collectors.toList());
   }

   public static nl.jdriven.blogs.svc.contract.proto.Statuscode transform(Response.Result result) {
      return nl.jdriven.blogs.svc.contract.proto.Statuscode.forNumber(result.getNumber());
   }

   public static nl.jdriven.blogs.svc.contract.proto.ResponseStatus transform(Response<?> result, String withWarning) {
      return nl.jdriven.blogs.svc.contract.proto.ResponseStatus.newBuilder()
              .setStatus(transform(result.getResult()))
              .setReason(result.getReason())
              .setWarning(withWarning)
              .addAllErrors(transform(result.getErrors()))
              .build();
   }

   public static nl.jdriven.blogs.svc.contract.proto.ResponseStatus transform(Response<?> result) {
      return nl.jdriven.blogs.svc.contract.proto.ResponseStatus.newBuilder()
              .setStatus(transform(result.getResult()))
              .setReason(result.getReason())
              .addAllErrors(transform(result.getErrors()))
              .build();
   }

   private static Iterable<nl.jdriven.blogs.svc.contract.proto.Error> transform(Collection<Error> errors) {
      return errors.stream()
              .filter(e -> !Objects.isNull(e))
              .map(Transformer::transform)
              .collect(Collectors.toList());
   }

   private static nl.jdriven.blogs.svc.contract.proto.Error transform(Error error) {
      return nl.jdriven.blogs.svc.contract.proto.Error.newBuilder()
              .setErrorCode(error.getCode())
              .setLocation(error.getLocation())
              .addAllArgs(error.getArgs())
              .build();
   }

   public static BooleanOption transform(nl.jdriven.blogs.svc.contract.proto.BoolOption option) {
      if (option == null) {
         return BooleanOption.UNSET;
      }
      switch (option) {
         case YES:
            return BooleanOption.YES;
         case NO:
            return BooleanOption.NO;
         default:
            return BooleanOption.UNSET;
      }
   }

}
