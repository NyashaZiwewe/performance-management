package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.sql.Date;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(updatable = false)
    @Positive(message = "Client ID must be positive")
    private long clientId;

    @NotBlank(message = "Start date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Start date must be in format YYYY-MM-DD")
    private String startDate;

    @NotBlank(message = "End date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "End date must be in format YYYY-MM-DD")
    private String endDate;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|IN_ACTIVE|INACTIVE|ARCHIVED", message = "Invalid status")
    private String status;

    @NotBlank(message = "Model is required")
    @Pattern(regexp = "gear|programme|standard", message = "Model must be gear, programme, or standard")
    private String model;
    @CreationTimestamp
    private Date date;
    @OneToMany(mappedBy = "reportingPeriod", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReportingDate> reportingDates;
//    @OneToMany(mappedBy = "reportingPeriod", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
//    private List<Goal> goals;

}
