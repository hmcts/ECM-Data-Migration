package uk.gov.hmcts.reform.collections.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.collections.diff.config.ApplicationParams;
import uk.gov.hmcts.reform.collections.diff.models.CaseDataEntity;
import uk.gov.hmcts.reform.collections.diff.models.CaseEventEntity;
import uk.gov.hmcts.reform.collections.diff.repositories.CaseDataRepository;
import uk.gov.hmcts.reform.collections.diff.repositories.CaseEventRepository;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

@SpringBootApplication
@Slf4j
public class Application  implements CommandLineRunner {

    private static final Clock UTC_CLOCK = Clock.systemUTC();
    public static final String PUBLIC = "PUBLIC";
    private static final String CLAIM_SERVED_DATE = "claimServedDate";
    public static final String GENERATE_CORRESPONDENCE = "generateCorrespondence";
    public static String DESCRIPTION = "";
    public static String SUMMARY = "";

    private static final List<String> EMPLOYMENT_SINGLE_CASE_TYPES = List.of("Bristol", "Leeds", "LondonCentral",
            "LondonEast", "LondonSouth", "Manchester", "MidlandsEast", "MidlandsWest", "Newcastle", "Scotland",
            "Wales", "Watford");

    @Autowired
    private ApplicationParams applicationParams;

    @Autowired
    private CaseDataRepository caseDataRepository;

    @Autowired
    private CaseEventRepository caseEventRepository;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws ParseException, JsonProcessingException {
        log.info("Starting..");

        var isDryRun = applicationParams.isDryRun();
        var startDate = new SimpleDateFormat("yyyy-MM-dd").parse(applicationParams.getStartDate());
        var endDate = new SimpleDateFormat("yyyy-MM-dd").parse(applicationParams.getEndDate());
        Timestamp startDateTs = new Timestamp(startDate.getTime());
        Timestamp endDateTs = new Timestamp(endDate.getTime());

        log.info("Dry run: {}", isDryRun);
        log.info("When dry run = true, dry run will make NO changes.");
        log.info("------------------------------------");
        insertTopLevelFieldBetweenTwoDates(startDateTs, endDateTs);
//        findAndReplaceValue(startDateTs);
    }

    private void findAndReplaceValue(Timestamp startDate) throws JsonProcessingException {
        List<CaseDataEntity> caseDataEntityList = new ArrayList<>();
        List<String> judgesName = List.of("%A Judge%", "%C Judge%");
        for (var office : EMPLOYMENT_SINGLE_CASE_TYPES) { // Will limit to certain offices when running
            for (var judge : judgesName) {
                caseDataEntityList.addAll(caseDataRepository.findCaseDataEntityByValue(office, judge, startDate));
            }
        }
        log.info("Found {} cases created since {}", caseDataEntityList.size(), startDate);
        if (!CollectionUtils.isEmpty(caseDataEntityList)) {
            log.info("------------------------------------");
            for (var caseDataEntity : caseDataEntityList) {
                checkPayloadForValue(caseDataEntity);
            }
            log.info("------------------------------------");
        }
    }

    private void checkPayloadForValue(CaseDataEntity caseDataEntity) throws JsonProcessingException {
        DESCRIPTION = "Correcting Judges ITCO References";
        SUMMARY = "Corrected Judges ITCO references for hearings on the case";
        var oldJudge = "C Judge";
        var newJudge = "0000_ITCO Judge";

        JsonNode caseData = caseDataEntity.getData();
        ObjectNode objectNode = (ObjectNode) caseData;
        var caseReference = caseDataEntity.getReference();
        log.info("Old Values for " + caseReference + " : " + caseData.findValues("judge"));
        var caseDataStr = objectNode.toPrettyString();
        caseDataStr = caseDataStr.replace(oldJudge, newJudge);
        ObjectMapper mapper = new ObjectMapper();
        caseData = mapper.readTree(caseDataStr);
        log.info("New Values for " + caseReference + " : " + caseData.findValues("judge"));
        caseDataEntity.setData(caseData);

        log.info(applicationParams.isDryRun() ? "Application IS in dry-run and WILL NOT write data to the database"
                : "Application IS NOT is dry-run and WILL write data to database");
        if (!applicationParams.isDryRun()) {
            findLatestEventAndCreateNew(caseDataEntity);
            caseDataRepository.save(caseDataEntity);
        }
    }

    private void insertTopLevelFieldBetweenTwoDates(Timestamp startDateTs, Timestamp endDateTs) {
        List<CaseEventEntity> caseEventEntityList = caseEventRepository.findCaseEventByEventIdAndState(
                GENERATE_CORRESPONDENCE,
                "Accepted",
                startDateTs,
                endDateTs);
        log.info("Found {} occurrences of {} between {} and {}", caseEventEntityList.size(), GENERATE_CORRESPONDENCE, startDateTs, endDateTs);
        Set<Long> caseIdSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(caseEventEntityList)) {
            for (var caseEventEntity : caseEventEntityList) {
                var caseId = caseEventEntity.getCaseDataId();
                var eventDate = caseEventEntity.getCreatedDate();
                if (!caseIdSet.contains(caseId)) {
                    log.info("------------------------------------");
                    caseIdSet.add(caseId);
                    addFieldToPayload(caseId, eventDate);
                    log.info("------------------------------------");
                } else {
                    log.info("Skipping case {} as it has already been checked" , caseDataRepository.findCaseDataById(caseId).get().getReference());
                }
            }
        }
    }

    private void addFieldToPayload(Long caseId, LocalDateTime eventDate) {
        DESCRIPTION = "Adding ClaimServedDate to case";
        SUMMARY = "Adding ClaimServedDate to case";
        Optional<CaseDataEntity> caseDataEntityOpt = caseDataRepository.findCaseDataById(caseId);
        if (caseDataEntityOpt.isEmpty()) {
            log.error(caseId + "not found");
            return;
        }

        CaseDataEntity caseDataEntity = caseDataEntityOpt.get();
        JsonNode caseData = caseDataEntity.getData();
        var caseReference = caseDataEntity.getReference();
        JsonNode dataClassification = caseDataEntity.getDataClassification();
        if (isNullOrEmpty(caseData.findValue(CLAIM_SERVED_DATE).toString())) {
            log.info("Adding field to case " + caseReference);
            ((ObjectNode) caseData).put(CLAIM_SERVED_DATE, eventDate.toLocalDate().toString());
            ((ObjectNode) dataClassification).put(CLAIM_SERVED_DATE, PUBLIC);

            caseDataEntity.setData(caseData);
            caseDataEntity.setDataClassification(dataClassification);
            if (!caseDataEntity.getData().findValue(CLAIM_SERVED_DATE).textValue().equals(eventDate.toLocalDate().toString())) {
                log.info("Data has not been inserted correctly");
                return;
            }
            log.info("Data has been inserted correctly");
            log.info(applicationParams.isDryRun() ? "Application IS in dry-run and WILL NOT write data to the database"
                    : "Application IS NOT is dry-run and WILL write data to database");
            if (!applicationParams.isDryRun()) {
                findLatestEventAndCreateNew(caseDataEntity);
                caseDataRepository.save(caseDataEntity);
            }
        } else {
            log.info(CLAIM_SERVED_DATE + " already exists for case " + caseReference);
        }
    }

    private void findLatestEventAndCreateNew(CaseDataEntity caseDataEntity) {
        List<CaseEventEntity> latestEventsSingletonList = caseEventRepository
                .findLatestEvents(caseDataEntity.getId(), PageRequest.of(0, 1));
        if (CollectionUtils.isEmpty(latestEventsSingletonList)) {
            log.error("Cannot find latest event for case " + caseDataEntity.getReference());
            return;
        }
        log.info("Creating new history event for case " + caseDataEntity.getReference() );
        CaseEventEntity newHistoryRecord = createHistoryEvent(caseDataEntity, latestEventsSingletonList.get(0));
        caseEventRepository.save(newHistoryRecord);
        log.info(caseDataEntity.getReference() + " updated from admin event");
    }

    private CaseEventEntity createHistoryEvent(CaseDataEntity caseDataEntity, CaseEventEntity caseEventEntity) {
        return CaseEventEntity.builder()
                .eventId("fixCaseAPI")
                .eventName("Update to case data")
                .caseDataId(caseDataEntity.getId())
                .caseTypeId(caseEventEntity.getCaseTypeId())
                .caseTypeVersion(caseEventEntity.getCaseTypeVersion())
                .createdDate(LocalDateTime.now(UTC_CLOCK))
                .data(caseDataEntity.getData())
                .dataClassification(caseDataEntity.getDataClassification())
                .stateId(caseEventEntity.getStateId())
                .stateName(caseEventEntity.getStateName())
                .userId("123456")
                .description(DESCRIPTION)
                .summary(SUMMARY)
                .userFirstName("ECM")
                .userLastName("Local Dev (Stub)")
                .securityClassification(caseEventEntity.getSecurityClassification())
                .build();

    }
}
