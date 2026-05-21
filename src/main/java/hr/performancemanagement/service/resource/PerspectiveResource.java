package hr.performancemanagement.service.resource;

import hr.performancemanagement.entities.Perspective;
import hr.performancemanagement.exception.ResourceNotFoundException;
import hr.performancemanagement.service.api.PerspectiveService;
import hr.performancemanagement.utils.dto.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/perspectives")
@RequiredArgsConstructor
public class PerspectiveResource {

    private final PerspectiveService perspectiveService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<CommonResponse<List<Perspective>>> listByClientId(@PathVariable long clientId) {
        List<Perspective> perspectives = perspectiveService.listAllPerspectives(clientId);
        return ResponseEntity.ok(CommonResponse.<List<Perspective>>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Perspectives retrieved successfully")
                .data(perspectives)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Perspective>> getById(@PathVariable long id) {
        Perspective perspective = perspectiveService.getPerspectiveById(id);
        if (perspective == null) {
            throw new ResourceNotFoundException("Perspective not found with id " + id);
        }
        return ResponseEntity.ok(CommonResponse.<Perspective>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Perspective retrieved successfully")
                .data(perspective)
                .build());
    }

    @PostMapping
    public ResponseEntity<CommonResponse<Perspective>> save(@RequestBody Perspective perspective) {
        Perspective savedPerspective = perspectiveService.savePerspective(perspective);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.<Perspective>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.CREATED.value())
                .message("Perspective saved successfully")
                .data(savedPerspective)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable long id) {
        Perspective perspective = perspectiveService.getPerspectiveById(id);
        if (perspective == null) {
            throw new ResourceNotFoundException("Perspective not found with id " + id);
        }
        perspectiveService.deletePerspective(perspective);
        return ResponseEntity.ok(CommonResponse.<Void>builder()
                .isSuccess(true)
                .statusCode(HttpStatus.OK.value())
                .message("Perspective deleted successfully")
                .data(null)
                .build());
    }
}
