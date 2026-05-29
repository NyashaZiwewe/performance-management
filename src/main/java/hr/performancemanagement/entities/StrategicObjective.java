package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.*;
import java.sql.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StrategicObjective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    @NotNull(message = "Reporting period is required")
    private ReportingPeriod reportingPeriod;

    @NotBlank(message = "Strategic objective name is required")
    @Size(min = 3, max = 500, message = "Strategic objective name must be between 3 and 500 characters")
    private String name;

    @Transient
    private Double weightedScore;

    @CreationTimestamp
    private Date dateAdded;
}
