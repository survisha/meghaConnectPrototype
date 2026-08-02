package com.survisha.meghaconnect.formextraction.validation;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.exception.FormExtractionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

@Slf4j @Component @RequiredArgsConstructor
public class VisitorFormImagePreprocessor {
    private final FormExtractionProperties properties;

    public ProcessedImage process(VisitorFormImageValidator.ValidatedImage source) {
        long started = System.nanoTime();
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(source.bytes()));
            int limit = properties.getImage().getMaxLongestSide();
            double scale = Math.min(1d, (double) limit / Math.max(original.getWidth(), original.getHeight()));
            int width = Math.max(1, (int)Math.round(original.getWidth()*scale));
            int height = Math.max(1, (int)Math.round(original.getHeight()*scale));
            BufferedImage output = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setColor(Color.WHITE); graphics.fillRect(0,0,width,height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original,0,0,width,height,null); graphics.dispose();
            byte[] bytes = jpeg(output, (float)properties.getImage().getJpegQuality());
            if(bytes.length>properties.getImage().getMaxProcessedSizeBytes())
                throw new FormExtractionException("FORM_IMAGE_PROCESSED_TOO_LARGE","The optimized form image exceeds the configured size limit.",413);
            log.info("Visitor form image preprocessed originalWidth={} originalHeight={} processedWidth={} processedHeight={} originalBytes={} processedBytes={} durationMs={}",
                    source.width(),source.height(),width,height,source.bytes().length,bytes.length,(System.nanoTime()-started)/1_000_000);
            return new ProcessedImage(bytes,"image/jpeg",width,height);
        } catch(FormExtractionException ex) { throw ex; }
        catch(IOException ex) { throw new FormExtractionException("FORM_IMAGE_PREPROCESSING_FAILED","The form image could not be optimized.",400,ex); }
    }
    private byte[] jpeg(BufferedImage image,float quality) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        try(ImageOutputStream stream=ImageIO.createImageOutputStream(out)) { writer.setOutput(stream); ImageWriteParam p=writer.getDefaultWriteParam(); p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); p.setCompressionQuality(quality); writer.write(null,new IIOImage(image,null,null),p); }
        finally { writer.dispose(); }
        return out.toByteArray();
    }
    public record ProcessedImage(byte[] bytes,String mimeType,int width,int height) {}
}
