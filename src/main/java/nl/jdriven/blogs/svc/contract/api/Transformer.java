package nl.jdriven.blogs.svc.contract.api;

import com.google.type.Money;
import nl.jdriven.blogs.svc.contract.model.main.Contract;
import nl.jdriven.blogs.svc.contract.model.main.WorkDone;
import nl.jdriven.blogs.svc.contract.proto.Quote;
import org.apache.commons.lang3.StringUtils;
import org.joda.money.CurrencyUnit;

import java.util.stream.Collectors;

/**
 * Transformer between gRPC classes and Model classes.
 */
public class Transformer {
    private Transformer() {
    }

    public static org.joda.money.Money transform(final Money amount) {
        var  currencyCode = CurrencyUnit.EUR;
        if (!StringUtils.isBlank(amount.getCurrencyCode())) {
            currencyCode = CurrencyUnit.of(amount.getCurrencyCode());
        }
        return org.joda.money.Money.ofMajor(currencyCode, amount.getUnits()).plusMinor(amount.getNanos());
    }

    public static Money transform(org.joda.money.Money amount) {
        return Money.newBuilder()
                .setCurrencyCode(amount.getCurrencyUnit().getCode())
                .setUnits(amount.getAmountMajorLong())
                .setNanos(amount.getMinorPart())
                .build();
    }

    public static WorkDone transform(nl.jdriven.blogs.svc.contract.proto.WorkDone work) {
        return new WorkDone(transform(work.getCostOfWork()), work.getDescriptionOfWorkDone());
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
                .setFullNameOfCustomer(c.getFullNameOfCustomer())
                .setQuotedPrice(Transformer.transform(c.getQuotedPrice()))
                .build();

        var allWork =c.getWorkDone().stream()
                .map(Transformer::transform)
                .collect(Collectors.toUnmodifiableList());

        return nl.jdriven.blogs.svc.contract.proto.Contract.newBuilder()
                .setContractId(c.getId())
                .setQuote(quote)
                .addAllWork(allWork)
                .build();
    }
}
