package issueManagement.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ResolutionType {

    SOLVED("solved"),
    FIXED("fixed"),
    WONT_FIX("");

    @Getter
    private final String resolution;

}
