package com.survisha.meghaconnect.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.survisha.meghaconnect.config.AiNotesProperties;
import com.survisha.meghaconnect.entity.DocumentUpload;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

class AiNotesDocumentProcessorTest {
    @TempDir Path temp;

    @Test void sendsDirectJpegToVisionInsteadOfSyntheticText() throws Exception {
        Path image=temp.resolve("EPICDetails.jpeg"); writeImage(image);
        Fixture f=fixture(image,MediaType.IMAGE_JPEG);
        var result=f.processor.process(f.document);
        assertTrue(result.vision()); assertEquals(1,result.images().size()); assertFalse(result.renderedPdf());
        verifyNoInteractions(f.textExtractor);
    }

    @Test void rendersImageOnlyPdfForVision() throws Exception {
        Path pdf=temp.resolve("Photo.pdf");
        BufferedImage image=image();
        try(PDDocument document=new PDDocument()){
            PDPage page=new PDPage(PDRectangle.A4);document.addPage(page);
            try(PDPageContentStream content=new PDPageContentStream(document,page)){content.drawImage(LosslessFactory.createFromImage(document,image),0,0,page.getMediaBox().getWidth(),page.getMediaBox().getHeight());}
            document.save(pdf.toFile());
        }
        Fixture f=fixture(pdf,MediaType.APPLICATION_PDF);when(f.textExtractor.extractText(f.document)).thenThrow(new IllegalStateException("No extractable text"));
        var result=f.processor.process(f.document);
        assertTrue(result.vision()); assertTrue(result.renderedPdf()); assertEquals(1,result.images().size());
    }

    @Test void keepsMeaningfulTextPdfOnTextPath() throws Exception {
        Path pdf=temp.resolve("text.pdf");Files.write(pdf,"%PDF- placeholder".getBytes());
        Fixture f=fixture(pdf,MediaType.APPLICATION_PDF);String text="This proposal contains enough meaningful appointment document text with names, dates, departments and requested action.";
        when(f.textExtractor.extractText(f.document)).thenReturn(text);
        var result=f.processor.process(f.document);
        assertFalse(result.vision());assertEquals(text,result.text());
    }

    private Fixture fixture(Path path,MediaType type){
        FileStorageService storage=mock(FileStorageService.class);DocumentTextExtractionService extractor=mock(DocumentTextExtractionService.class);
        DocumentUpload document=DocumentUpload.builder().id(119L).originalFilename(path.getFileName().toString()).contentType(type.toString()).build();
        when(storage.resolveDocumentPath(document)).thenReturn(path);when(storage.mediaTypeFromMetadata(document,path)).thenReturn(type);
        AiNotesProperties props=new AiNotesProperties();props.setMaxImageBytes(5_242_880);props.setMinMeaningfulTextChars(40);
        return new Fixture(new AiNotesDocumentProcessor(storage,extractor,props),extractor,document);
    }
    private void writeImage(Path path)throws Exception{ImageIO.write(image(),"jpeg",path.toFile());}
    private BufferedImage image(){BufferedImage i=new BufferedImage(960,1280,BufferedImage.TYPE_INT_RGB);Graphics2D g=i.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,960,1280);g.setColor(Color.BLACK);g.drawString("EPIC No: ABC1234567",100,200);g.dispose();return i;}
    private record Fixture(AiNotesDocumentProcessor processor,DocumentTextExtractionService textExtractor,DocumentUpload document){}
}
