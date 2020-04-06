package nl.jdriven.blogs.svc.contract.model.main;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;

public class WorkDone {
    private final BigDecimal costOfWork;
    private final String descriptionOfWorkDone;

    public WorkDone(BigDecimal costOfWork, String descriptionOfWorkDone) {
        this.costOfWork = costOfWork;
        this.descriptionOfWorkDone = descriptionOfWorkDone;
    }

    public BigDecimal getCostOfWork() {
        return costOfWork;
    }

    public String getDescriptionOfWorkDone() {
        return descriptionOfWorkDone;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
