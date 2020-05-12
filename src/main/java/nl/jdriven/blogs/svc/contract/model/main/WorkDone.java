package nl.jdriven.blogs.svc.contract.model.main;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.money.Money;

public class WorkDone {
    private final Money costOfWork;
    private final String descriptionOfWorkDone;

    public WorkDone(Money costOfWork, String descriptionOfWorkDone) {
        this.costOfWork = costOfWork;
        this.descriptionOfWorkDone = descriptionOfWorkDone;
    }

    public Money getCostOfWork() {
        return costOfWork;
    }

    public String getDescriptionOfWorkDone() {
        return descriptionOfWorkDone;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
