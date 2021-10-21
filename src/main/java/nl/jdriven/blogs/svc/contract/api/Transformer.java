package nl.jdriven.blogs.svc.contract.api;

import com.google.type.Money;
import nl.jdriven.blogs.svc.contract.model.api.Response;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import nl.jdriven.blogs.svc.contract.proto.ContractId;
import nl.jdriven.blogs.svc.contract.proto.Error;
import nl.jdriven.blogs.svc.contract.proto.Quote;
import nl.jdriven.blogs.svc.contract.proto.Status;
import nl.jdriven.blogs.svc.contract.proto.Statuscode;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
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
      var quote = Quote.newBuilder()
              .setDescriptionOfWorkRequested(c.getDescriptionOfWorkRequested())
              .setFullNameOfParticipant(c.getFullNameOfParticipant())
              .setQuotedPrice(Transformer.transform(c.getQuotedPrice()))
              .build();

      var allWork = c.getWorkDone().stream().map(Transformer::transform).collect(Collectors.toUnmodifiableList());

      return nl.jdriven.blogs.svc.contract.proto.Contract.newBuilder()
              .setContractId(ContractId.newBuilder().setId(c.getId()))
              .setQuote(quote)
              .addAllWork(allWork)
              .build();
   }

   public static Statuscode transform(Response.Result result) {
      return Statuscode.forNumber(result.getNumber());
   }

   public static Status transform(Response<?> result) {
      return Status.newBuilder()
              .setStatus(transform(result.getResult()))
              .setReason(result.getReason())
              .addAllErrors(transform(result.getErrors()))
              .build();
   }

   private static Iterable<Error> transform(List<nl.jdriven.blogs.svc.contract.model.api.Error> errors) {
      return errors.stream()
              .filter(e-> !Objects.isNull(e))
              .map(Transformer::transform)
              .collect(Collectors.toList());
   }

   private static Error transform(nl.jdriven.blogs.svc.contract.model.api.Error e) {
      return Error.newBuilder()
              .setErrorCode(e.getCode())
              .setLocation(e.getLocation())
              .addAllArgs(e.getArgs())
              .build();
   }

}
