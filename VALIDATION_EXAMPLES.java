// =====================================================
// VALIDATION EXAMPLES - Copy and adapt to your entities
// =====================================================

package hr.performancemanagement.entities;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.sql.Date;
import java.time.LocalDateTime;

// =====================================================
// EXAMPLE 1: Scorecard Entity with Validation
// =====================================================

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Scorecard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(updatable = false)
    @NotNull(message = "Client ID is required")
    private long clientId;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    @NotNull(message = "Owner is required")
    private Account owner;

    @ManyToOne
    @JoinColumn(name = "reporting_period_id")
    @NotNull(message = "Reporting period is required")
    private ReportingPeriod reportingPeriod;

    @ManyToOne
    @JoinColumn(name = "scorecard_model_id")
    @NotNull(message = "Scorecard model is required")
    private ScorecardModel scorecardModel;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DELETED", message = "Invalid status")
    private String status;

    @DecimalMin(value = "0.0", message = "Employee score must be non-negative")
    @DecimalMax(value = "5.0", message = "Employee score cannot exceed 5.0")
    private double employeeScore;

    @DecimalMin(value = "0.0", message = "Manager score must be non-negative")
    @DecimalMax(value = "5.0", message = "Manager score cannot exceed 5.0")
    private double managerScore;

    @DecimalMin(value = "0.0", message = "Agreed score must be non-negative")
    @DecimalMax(value = "5.0", message = "Agreed score cannot exceed 5.0")
    private double agreedScore;

    @DecimalMin(value = "0.0", message = "Moderated score must be non-negative")
    @DecimalMax(value = "5.0", message = "Moderated score cannot exceed 5.0")
    private double moderatedScore;

    @DecimalMin(value = "0.0", message = "Weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Weighted score cannot exceed 100")
    private double weightedScore;

    @Column(columnDefinition = "varchar(50) default 'NEW'")
    @NotBlank(message = "Approval status is required")
    @Pattern(regexp = "NEW|PENDING|APPROVED|REJECTED|RETURNED", message = "Invalid approval status")
    private String approvalStatus;

    @Size(max = 1000, message = "Owner comment cannot exceed 1000 characters")
    private String ownerComment;

    @Size(max = 1000, message = "Supervisor comment cannot exceed 1000 characters")
    private String supervisorComment;

    @Size(max = 1000, message = "Moderator comment cannot exceed 1000 characters")
    private String moderatorComment;

    @UpdateTimestamp
    private Date lastUpdate;

    @CreationTimestamp
    private Date date;

    @NotBlank(message = "Lock status is required")
    @Pattern(regexp = "OPEN|LOCKED|CLOSED", message = "Invalid lock status")
    private String lockStatus;

    @Transient
    private OverallScore overallScore;

    // Audit fields
    private Long createdBy;
    private String createdByName;
    private Long lastModifiedBy;
    private String lastModifiedByName;

    // Soft delete fields
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}

// =====================================================
// EXAMPLE 2: Account Entity with Validation
// =====================================================

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String fullName;

    // Password validation - only checked on creation/update, not on read
    @Size(min = 8, message = "Password must be at least 8 characters")
    // Note: Complex password validation should be done in service layer
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|USER|SUPERVISOR|MODERATOR", message = "Invalid role")
    private String role;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|SUSPENDED|DELETED", message = "Invalid status")
    private String status;

    @ManyToOne
    @JoinColumn(name = "supervisor_id")
    private Account supervisor;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    // Security fields
    private LocalDateTime passwordChangedAt;
    private LocalDateTime passwordExpiresAt;

    @Min(value = 0, message = "Failed login attempts cannot be negative")
    @Max(value = 10, message = "Failed login attempts exceeded maximum")
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;

    @Size(max = 50, message = "IP address too long")
    private String lastLoginIp;

    // Audit fields
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Soft delete
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}

// =====================================================
// EXAMPLE 3: Target Entity with Validation
// =====================================================

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Target {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "scorecard_id")
    @NotNull(message = "Scorecard is required")
    private Scorecard scorecard;

    @ManyToOne
    @JoinColumn(name = "goal_id")
    @NotNull(message = "Goal is required")
    private Goal goal;

    @NotBlank(message = "Measure is required")
    @Size(min = 3, max = 1000, message = "Measure must be between 3 and 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String measure;

    @NotBlank(message = "Unit is required")
    @Size(max = 100, message = "Unit cannot exceed 100 characters")
    private String unit;

    @NotNull(message = "Normal target is required")
    private Double normalTarget;

    @DecimalMin(value = "0.0", message = "Allocated weight must be non-negative")
    @DecimalMax(value = "100.0", message = "Allocated weight cannot exceed 100")
    @NotNull(message = "Allocated weight is required")
    private Double allocatedWeight;

    // For standard scorecard
    private Double baseTarget;
    private Double stretchTarget;
    private Double actual;

    // For value-based scorecard
    @DecimalMin(value = "0.0", inclusive = false, message = "Employee score must be greater than 0")
    @DecimalMax(value = "5.0", message = "Employee score cannot exceed 5.0")
    private Double employeeScore;

    @DecimalMin(value = "0.0", inclusive = false, message = "Manager score must be greater than 0")
    @DecimalMax(value = "5.0", message = "Manager score cannot exceed 5.0")
    private Double managerScore;

    @DecimalMin(value = "0.0", message = "Weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Weighted score cannot exceed 100")
    private Double weightedScore;

    @Size(max = 2000, message = "Justification cannot exceed 2000 characters")
    @Column(columnDefinition = "TEXT")
    private String justification;

    @Size(max = 500, message = "Evidence URL too long")
    private String evidence;

    private String attachment;

    // Audit fields
    private Long createdBy;
    private Long lastModifiedBy;

    // Soft delete
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}

// =====================================================
// EXAMPLE 4: ReportingPeriod Entity with Validation
// =====================================================

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
    @NotNull(message = "Client ID is required")
    private long clientId;

    @Column(unique = true)
    @NotBlank(message = "Start date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Start date must be in format YYYY-MM-DD")
    private String startDate;

    @Column(unique = true)
    @NotBlank(message = "End date is required")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "End date must be in format YYYY-MM-DD")
    private String endDate;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED", message = "Invalid status")
    private String status;

    @NotBlank(message = "Model is required")
    @Pattern(regexp = "gear|programme|standard", message = "Model must be gear, programme, or standard")
    private String model;

    @CreationTimestamp
    private Date date;

    @OneToMany(mappedBy = "reportingPeriod", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ReportingDate> reportingDates;

    // Audit fields
    private Long createdBy;
    private String createdByName;
    private Long lastModifiedBy;
    private String lastModifiedByName;

    // Custom validation: endDate must be after startDate
    @AssertTrue(message = "End date must be after start date")
    private boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotBlank handle null check
        }
        try {
            return endDate.compareTo(startDate) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

// =====================================================
// EXAMPLE 5: Controller with Validation
// =====================================================

@RestController
@RequestMapping("/api/scorecards")
public class ScorecardResource {
    private static final Logger log = LoggerFactory.getLogger(ScorecardResource.class);

    @Autowired
    private ScorecardService scorecardService;

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ScorecardDTO>> getScorecard(
            @PathVariable @Positive(message = "ID must be positive") Long id) {

        ScorecardDTO scorecard = scorecardService.getScorecardById(id);
        return ResponseEntity.ok(CommonResponse.<ScorecardDTO>builder()
                .isSuccess(true)
                .statusCode(200)
                .message("Scorecard retrieved successfully")
                .data(scorecard)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ScorecardDTO>> createScorecard(
            @Valid @RequestBody ScorecardDTO scorecardDTO,
            BindingResult result) {

        if (result.hasErrors()) {
            String errorMessage = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.badRequest().body(CommonResponse.<ScorecardDTO>builder()
                    .isSuccess(false)
                    .statusCode(400)
                    .message(errorMessage)
                    .build());
        }

        ScorecardDTO created = scorecardService.createScorecard(scorecardDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.<ScorecardDTO>builder()
                        .isSuccess(true)
                        .statusCode(201)
                        .message("Scorecard created successfully")
                        .data(created)
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ScorecardDTO>> updateScorecard(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ScorecardDTO scorecardDTO) {

        ScorecardDTO updated = scorecardService.updateScorecard(id, scorecardDTO);
        return ResponseEntity.ok(CommonResponse.<ScorecardDTO>builder()
                .isSuccess(true)
                .statusCode(200)
                .message("Scorecard updated successfully")
                .data(updated)
                .build());
    }

    @PostMapping("/{scorecardId}/scores")
    public ResponseEntity<CommonResponse<Void>> saveScore(
            @PathVariable @Positive Long scorecardId,
            @RequestParam @Positive Long targetId,
            @RequestParam
            @DecimalMin(value = "0.0", message = "Score must be at least 0")
            @DecimalMax(value = "5.0", message = "Score cannot exceed 5")
            @NotNull(message = "Score is required")
            Double score) {

        scorecardService.saveScore(targetId, score);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(200)
                .message("Score saved successfully")
                .build());
    }
}

// =====================================================
// EXAMPLE 6: Custom Validator for Complex Rules
// =====================================================

@Component
public class ScorecardValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return Scorecard.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Scorecard scorecard = (Scorecard) target;

        // Business rule: Total weight of targets must not exceed 100%
        double totalWeight = scorecard.getTargets().stream()
                .mapToDouble(Target::getAllocatedWeight)
                .sum();

        if (totalWeight > 100.0) {
            errors.reject("totalWeight.exceeded",
                    "Total allocated weight cannot exceed 100%. Current total: " + totalWeight);
        }

        // Business rule: Cannot approve scorecard without all scores
        if ("APPROVED".equals(scorecard.getApprovalStatus())) {
            boolean hasEmptyScores = scorecard.getTargets().stream()
                    .anyMatch(t -> t.getEmployeeScore() == null || t.getManagerScore() == null);

            if (hasEmptyScores) {
                errors.reject("scores.incomplete",
                        "Cannot approve scorecard with incomplete scores");
            }
        }

        // Business rule: Only one active scorecard per owner per period
        // This would be checked in service layer with database query
    }
}

// Usage in controller:
@InitBinder
protected void initBinder(WebDataBinder binder) {
    binder.addValidators(scorecardValidator);
}

// =====================================================
// EXAMPLE 7: DTO with Validation
// =====================================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScorecardDTO {

    private Long id;

    @NotNull(message = "Owner ID is required")
    @Positive(message = "Owner ID must be positive")
    private Long ownerId;

    @NotBlank(message = "Owner name is required")
    private String ownerName;

    @NotNull(message = "Reporting period ID is required")
    @Positive(message = "Reporting period ID must be positive")
    private Long reportingPeriodId;

    @NotBlank(message = "Reporting period label is required")
    private String reportingPeriodLabel;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED", message = "Invalid status")
    private String status;

    @DecimalMin(value = "0.0", message = "Weighted score must be non-negative")
    @DecimalMax(value = "100.0", message = "Weighted score cannot exceed 100")
    private Double weightedScore;

    @Valid // Validates nested list
    private List<TargetDTO> targets;
}
