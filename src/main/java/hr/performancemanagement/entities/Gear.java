package hr.performancemanagement.entities;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Gear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private String description;
    private String graphColor;
    private String fill;
    @OneToMany(mappedBy = "gear", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Goal> goals;
    @Transient
    private double totalAllocatedWeight;
    @CreationTimestamp
    @Column(updatable = false)
    private Date date;

}
