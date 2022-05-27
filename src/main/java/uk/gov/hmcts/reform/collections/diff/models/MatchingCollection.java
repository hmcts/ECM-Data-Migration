package uk.gov.hmcts.reform.collections.diff.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MatchingCollection {

    private String collectionItemId;
    private JsonNode collectionValue;
    private Integer index;
}
