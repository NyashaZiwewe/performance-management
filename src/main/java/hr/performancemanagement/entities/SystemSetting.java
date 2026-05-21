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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false)
    private String settingKey;

    @Column(length = 3000)
    private String settingValue;

    @Column(length = 1000)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private Date dateCreated;

    @UpdateTimestamp
    private Date dateUpdated;
}
