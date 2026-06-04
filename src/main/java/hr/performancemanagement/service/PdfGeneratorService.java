package hr.performancemanagement.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

@Service
public class PdfGeneratorService {
    private static final String PDF_RESOURCES = "/static/";

    @Autowired
    private TemplateEngine templateEngine;
    public byte[] generatePdfFromTemplate(String templateName, Context context, boolean clean) throws Exception {
        String htmlContent = loadAndFillTemplate(templateName, context);

        if(clean){
            htmlContent = cleanDocument(htmlContent);
        }
        OutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

//        System.out.println(htmlContent);
        renderer.setDocumentFromString(htmlContent, new ClassPathResource(PDF_RESOURCES).getURL().toExternalForm());
        renderer.layout();
        renderer.createPDF(outputStream, false);
        renderer.finishPDF();

        return ((ByteArrayOutputStream) outputStream).toByteArray();
    }

    private String loadAndFillTemplate(String templateName, Context context) {
        return templateEngine.process(templateName, context);
    }

    public static String cleanDocument(String input) {
        Document document = Jsoup.parse(input, "", Parser.xmlParser());

        Whitelist whitelist = Whitelist.relaxed().addTags("img").addAttributes("img", "src");
        whitelist.addProtocols("img", "src", "data");

        Cleaner cleaner = new Cleaner(whitelist);
        Document cleanedDocument = cleaner.clean(document);

        String result = cleanedDocument.outerHtml();
        return result;
    }
}
