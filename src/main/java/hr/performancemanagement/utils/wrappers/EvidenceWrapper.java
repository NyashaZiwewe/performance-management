package hr.performancemanagement.utils.wrappers;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class EvidenceWrapper {

    private Long targetId;
    private String evidence;
    private MultipartFile attachment;

}
