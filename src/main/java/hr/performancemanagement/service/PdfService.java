package hr.performancemanagement.service;

import hr.performancemanagement.utils.constants.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@Service
public class PdfService {

    private static final String PDF_RESOURCES = "/pdf-resources/";
//    private static final String PDF_RESOURCES = "/static/";

    @Autowired
    private AccountService accountService;
    @Autowired
    private SpringTemplateEngine templateEngine;

    public File generatePdf() throws Exception{
        Context context = getContext();
        String html = loadAndFillTemplate(context);
        return renderPdf(html);
    }



    private File renderPdf(String html) throws Exception {
        File file = File.createTempFile("accounts", ".pdf");
        OutputStream outputStream = new FileOutputStream(file);
//        ITextRenderer renderer = new ITextRenderer(20f * 4f / 3f, 20);
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html, new ClassPathResource(PDF_RESOURCES).getURL().toExternalForm());
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
        file.deleteOnExit();
        return file;
    }


    private Context getContext() {
        Context context = new Context();
        context.setVariable("accounts", accountService.listAllAccounts());
        return context;
    }


    private String loadAndFillTemplate(Context context) {
        return templateEngine.process(Pages.PDF_ACCOUNTS, context);
    }


}
