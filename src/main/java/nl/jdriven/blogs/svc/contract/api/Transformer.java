package nl.jdriven.blogs.svc.contract.api;

import com.google.type.Money;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import nl.jdriven.blogs.svc.contract.proto.Quote;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Transformer between gRPC classes and Model classes.
 */
public class Transformer {
    private Transformer() {
    }

    // unverified cut-and-grab job, might work but who knows.
    public static Optional<BigDecimal> transform(final Money amount) {
        // only accept EUR or no code (default is EUR)
        var  currencyCode = amount.getCurrencyCode();
        if (currencyCode != null && currencyCode.length() > 0 && !"EUR".equalsIgnoreCase(currencyCode)) {
            return Optional.empty();
        }
        var  bd = new BigDecimal(amount.getUnits()).add(new BigDecimal(amount.getNanos(), new MathContext(9)));
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
        var  costOfWork = transform(work.getCostOfWork()).orElse(null);
        return new WorkDone(costOfWork, work.getDescriptionOfWorkDone());
    }

    public static nl.jdriven.blogs.svc.contract.proto.WorkDone transform(WorkDone work ) {
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

        var allWork =c.getWorkDone().stream().map(Transformer::transform).collect(Collectors.toUnmodifiableList());

        return nl.jdriven.blogs.svc.contract.proto.Contract.newBuilder()
                .setContractId(c.getId())
                .setQuote(quote)
                .addAllWork(allWork)
                .build();
    }
}
