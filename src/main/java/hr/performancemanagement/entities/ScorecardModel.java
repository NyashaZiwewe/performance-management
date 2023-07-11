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
import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ScorecardModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    public long clientId;
    private String name;
    private String status;
    @CreationTimestamp
    private Date date;
}
