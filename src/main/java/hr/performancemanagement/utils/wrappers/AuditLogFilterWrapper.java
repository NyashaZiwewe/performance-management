package hr.performancemanagement.utils.wrappers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Getter
@Setter
public class AuditLogFilterWrapper {
    private String userName;
    private String action;
    private String tableName;
    private String recordId;
    private String keyword;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date startDate;

    @DateTimeFormat(pattern = "dd/MM/yyyy")
    private Date endDate;
}
