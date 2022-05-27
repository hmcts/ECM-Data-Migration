package uk.gov.hmcts.reform.collections.diff.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Getter
public class ApplicationParams {

    @Value("${event-list-file}")
    private String eventListFile;

    @Value("${dry-run}")
    private boolean dryRun;

    @Value("${start-date}")
    private String startDate;

    @Value("${end-date}")
    private String endDate;


}
