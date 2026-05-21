package hr.performancemanagement.service.api;

import hr.performancemanagement.utils.constants.Pages;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public interface PdfService {
    File generatePdf() throws Exception;
}
