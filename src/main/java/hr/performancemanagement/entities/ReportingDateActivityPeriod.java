package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(
        name = "reporting_date_activity_period",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_reporting_date_activity",
                columnNames = {"reporting_date_id", "activity_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportingDateActivityPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "reporting_date_id", nullable = false)
    private ReportingDate reportingDate;

    @Column(name = "activity_type", nullable = false, length = 40)
    private String activityType;

    @Column(nullable = false, length = 10)
    private String startDate;

    @Column(nullable = false, length = 10)
    private String endDate;

    @Column(length = 10)
    private String lastReminderDate;

    @Column(length = 10)
    private String lastCutoffReminderDate;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;

    @UpdateTimestamp
    private Date dateUpdated;
}
