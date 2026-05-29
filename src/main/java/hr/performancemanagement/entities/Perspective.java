package hr.performancemanagement.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.*;

@Entity
public class Perspective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(updatable = false)
    @Positive(message = "Client ID must be positive")
    private long clientId;

    @NotBlank(message = "Perspective name is required")
    @Size(min = 2, max = 200, message = "Perspective name must be between 2 and 200 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Graph color must be a valid hex color")
    private String graphColor;

    @Size(max = 50, message = "Fill cannot exceed 50 characters")
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

    public void setGraphColor(String graphColor) {
        this.graphColor = graphColor;
    }

    public String getFill() {
        return fill;
    }

    public void setFill(String fill) {
        this.fill = fill;
    }
}
