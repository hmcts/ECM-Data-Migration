package uk.gov.hmcts.reform.collections.diff.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.collections.diff.models.CaseDataEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Transactional
public interface CaseDataRepository extends CrudRepository<CaseDataEntity, Long> {

    @Query(value = "SELECT * FROM case_data cd WHERE cd.reference = :caseReference", nativeQuery = true)
    Optional<CaseDataEntity> findCaseDataByReference(Long caseReference);

    @Query("SELECT cd FROM CaseDataEntity cd WHERE cd.id = :id")
    Optional<CaseDataEntity> findCaseDataById(Long id);

    @Query(value = "SELECT * FROM case_data cd WHERE cd.created_date > :startDate and cd.case_type_id = :ctID", nativeQuery = true)
    List<CaseDataEntity> findCaseDataEntityByDateCreation(Timestamp startDate, String ctID);

    @Query(value =
            "SELECT * " +
            "FROM case_data cd " +
            "WHERE cd.case_type_id = :ctID " +
            "AND CAST(cd.data as VARCHAR) LIKE :textToFind " +
            "AND cd.created_date > :startDate", nativeQuery = true)
    List<CaseDataEntity> findCaseDataEntityByValue(String ctID, String textToFind, Timestamp startDate);

}
