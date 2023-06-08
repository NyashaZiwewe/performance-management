package hr.performancemanagement.entities;


import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
public class Perspective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(updatable = false)
    private long clientId;
    private String name;
    private String description;
    private String graphColor;
    private String fill;

    public Perspective(long id, long clientId, String name, String description, String graphColor, String fill) {
        this.id = id;
        this.clientId = clientId;
        this.name = name;
        this.description = description;
        this.graphColor = graphColor;
        this.fill = fill;
    }

    public Perspective() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGraphColor() {
        return graphColor;
    }

    public void setGraphColor(String graph_color) {
        this.graphColor = graph_color;
    }

    public String getFill() {
        return fill;
    }

    public void setFill(String fill) {
        this.fill = fill;
    }
}
