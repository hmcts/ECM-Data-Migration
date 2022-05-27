package uk.gov.hmcts.reform.collections.diff.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.collections.diff.models.CaseEventEntity;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
public interface CaseEventRepository extends PagingAndSortingRepository<CaseEventEntity, Long> {

    @Query("SELECT ce FROM CaseEventEntity ce"
        + " WHERE ce.caseDataId = :caseDataId"
        + " AND ce.createdDate < :createdDate"
        + " ORDER BY ce.createdDate DESC")
    List<CaseEventEntity> findEventPriorTo(LocalDateTime createdDate, Long caseDataId, Pageable pageable);

    @Query("SELECT ce FROM CaseEventEntity ce"
        + " WHERE ce.caseDataId = :caseDataId"
        + " ORDER BY ce.createdDate DESC")
    List<CaseEventEntity> findLatestEvents(Long caseDataId, Pageable pageable);

    @Query(value =
            "select * " +
            "from case_event " +
            "where event_id = :eventId " +
            "and state_id = :state " +
            "and created_date > :startDate " +
            "and created_date < :endDate " +
            "order by created_date", nativeQuery = true)
    List<CaseEventEntity> findCaseEventByEventIdAndState(String eventId, String state,
                                                         Timestamp startDate , Timestamp endDate);
}