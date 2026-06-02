package hr.performancemanagement.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProbationKpiComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "probation_kpi_id")
    private ProbationKpi probationKpi;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private Account sender;

    @Column(length = 3000)
    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
