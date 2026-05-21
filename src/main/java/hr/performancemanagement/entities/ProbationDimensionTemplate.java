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
public class ProbationDimensionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String code;
    private String title;
    @Column(length = 3000)
    private String description;
    private Integer displayOrder;
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp date;
}
