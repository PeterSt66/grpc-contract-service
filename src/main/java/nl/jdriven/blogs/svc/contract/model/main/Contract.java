package nl.jdriven.blogs.svc.contract.model.main;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Contract {
    public static enum Status {QUOTE, ATWORK, FINALIZED, ABANDONED}

    private Status status = Status.QUOTE;
    private final String id;
    private final BigDecimal quotedPrice;
    private final String descriptionOfWorkRequested;
    private final String fullNameOfParticipant;
    private final List<WorkDone> workDone = new ArrayList<>();

    public Contract(String id, BigDecimal quotedPrice, String descriptionOfWorkRequested, String fullNameOfParticipant) {
        this.id = id;
        this.quotedPrice = quotedPrice;
        this.descriptionOfWorkRequested = descriptionOfWorkRequested;
        this.fullNameOfParticipant = fullNameOfParticipant;
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getQuotedPrice() {
        return quotedPrice;
    }

    public String getDescriptionOfWorkRequested() {
        return descriptionOfWorkRequested;
    }

    public String getFullNameOfParticipant() {
        return fullNameOfParticipant;
    }

    public List<WorkDone> getWorkDone() {
        return Collections.unmodifiableList(workDone);
    }

    public void addWorkDone(WorkDone work) {
        workDone.add(work);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
