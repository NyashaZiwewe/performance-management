package hr.performancemanagement.entities;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private String description;
    private String measureOfSuccess;
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Account manager;
    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    private ReportingPeriod reportingPeriod;
    private String startDate;
    private String endDate;
    private double progress;
    private String status;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;
    @OneToMany(mappedBy = "actionPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Task> taskList;
    @OneToMany(mappedBy = "actionPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Issue> issueList;

}
