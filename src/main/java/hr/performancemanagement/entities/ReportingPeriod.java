package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
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
    private long clientId;
    @Column(unique = true)
    private String startDate;
    @Column(unique = true)
    private String endDate;
    private String status;
    @CreationTimestamp
    private Date date;
    @OneToMany(mappedBy = "reportingPeriod", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReportingDate> reportingDates;

}
