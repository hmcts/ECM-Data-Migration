package uk.gov.hmcts.reform.collections.diff.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "case_data")
@Entity
@TypeDef(
    typeClass = JsonBinaryType.class,
    defaultForType = JsonNode.class
)
@Data
public class CaseDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "jurisdiction")
    private String jurisdiction;

    @Column(name = "reference")
    private Long reference;

    @Column(name = "data", columnDefinition = "jsonb")
    private JsonNode data;

    @Column(name = "data_classification", columnDefinition = "jsonb")
    private JsonNode dataClassification;

    @Column(name = "state")
    private String state;
}
