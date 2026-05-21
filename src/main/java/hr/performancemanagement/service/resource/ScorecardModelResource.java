package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.ScorecardModel;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.ScorecardModelService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scorecard-models")
@RequiredArgsConstructor
public class ScorecardModelResource {

    private final ScorecardModelService scorecardModelService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<ScorecardModel>>> listByClientId(@PathVariable long clientId) {
        List<ScorecardModel> models = scorecardModelService.getAllScorecardModels(clientId);
        List<ScorecardModel> filteredModels = new ArrayList<>();
        for (ScorecardModel model : models) {
            if (model != null && model.getClientId() == clientId) {
                filteredModels.add(model);
            }
        }

        return ResponseEntity.ok(CommonResponse.<List<ScorecardModel>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard models retrieved successfully")
                .data(filteredModels)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ScorecardModel>> getById(@PathVariable long id) {
        ScorecardModel model = scorecardModelService.getScorecardModelById(id);
        if (model == null) {
            throw new ResourceNotFoundException("Scorecard model not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<ScorecardModel>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard model retrieved successfully")
                .data(model)
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<CommonResponse<ScorecardModel>> getActiveModel() {
        ScorecardModel activeModel = scorecardModelService.getActiveScorecardModel();
        if (activeModel == null) {
            throw new ResourceNotFoundException("No active scorecard model found");
        }
        return ResponseEntity.ok(CommonResponse.<ScorecardModel>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Active scorecard model retrieved successfully")
                .data(activeModel)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<ScorecardModel>> save(@RequestBody ScorecardModel model) {
        scorecardModelService.saveScorecardModel(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<ScorecardModel>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Scorecard model saved successfully")
                .data(model)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        ScorecardModel model = scorecardModelService.getScorecardModelById(id);
        if (model == null) {
            throw new ResourceNotFoundException("Scorecard model not found with id " + id);
        }

        int response = scorecardModelService.deleteScorecardModel(model);
        if (response == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.<Void>builder()
                    .isSuccess(false)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Cannot delete the only scorecard model. Add another one first.")
                    .data(null)
                    .build());
        }

        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Scorecard model deleted successfully")
                .data(null)
                .build());
    }
}
