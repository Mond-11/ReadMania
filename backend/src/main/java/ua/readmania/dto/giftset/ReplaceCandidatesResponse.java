package ua.readmania.dto.giftset;

import java.math.BigDecimal;
import java.util.List;

public class ReplaceCandidatesResponse {

    private List<GiftSetBookItem> candidates;
    private BigDecimal maxPriceAllowed;

    public ReplaceCandidatesResponse() {
    }

    public ReplaceCandidatesResponse(List<GiftSetBookItem> candidates, BigDecimal maxPriceAllowed) {
        this.candidates = candidates;
        this.maxPriceAllowed = maxPriceAllowed;
    }

    public List<GiftSetBookItem> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<GiftSetBookItem> candidates) {
        this.candidates = candidates;
    }

    public BigDecimal getMaxPriceAllowed() {
        return maxPriceAllowed;
    }

    public void setMaxPriceAllowed(BigDecimal maxPriceAllowed) {
        this.maxPriceAllowed = maxPriceAllowed;
    }
}
