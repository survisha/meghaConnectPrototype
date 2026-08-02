package com.survisha.meghaconnect.formextraction.validation;

import com.survisha.meghaconnect.formextraction.config.FormExtractionProperties;
import com.survisha.meghaconnect.formextraction.dto.ImageQualityResult;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VisitorFormImagePreprocessorTest {
    @Test void noOpKeepsSmallerOriginal() throws Exception {
        byte[] original=image(960,1280,"jpg");
        var result=processor().process(source(original,"image/jpeg",960,1280));
        assertSame(original,result.bytes());
        assertEquals("image/jpeg",result.mimeType());
    }

    @Test void resizeLimitsLongestSideAndProducesJpeg() throws Exception {
        byte[] original=image(2000,1000,"png");
        var result=processor().process(source(original,"image/png",2000,1000));
        assertEquals(1280,result.width()); assertEquals(640,result.height());
        assertEquals("image/jpeg",result.mimeType()); assertTrue(result.bytes().length<1_048_576);
    }

    @Test void largerReencodeFallsBackToOriginal() throws Exception {
        byte[] original=image(320,240,"jpg");
        var result=processor().process(source(original,"image/jpeg",320,240));
        assertTrue(result.bytes().length<=original.length);
    }

    @Test void orientationCorrectionSwapsDimensions() {
        BufferedImage source=new BufferedImage(100,200,BufferedImage.TYPE_INT_RGB);
        BufferedImage rotated=processor().rotate(source,6);
        assertEquals(200,rotated.getWidth()); assertEquals(100,rotated.getHeight());
    }

    private VisitorFormImagePreprocessor processor() { return new VisitorFormImagePreprocessor(new FormExtractionProperties()); }
    private VisitorFormImageValidator.ValidatedImage source(byte[] bytes,String mime,int width,int height) {
        return new VisitorFormImageValidator.ValidatedImage(bytes,mime,width,height,new ImageQualityResult(true,List.of()));
    }
    private byte[] image(int width,int height,String format) throws Exception {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB); Graphics2D g=image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,width,height); g.setColor(Color.BLACK); g.drawString("Handwritten visitor form",20,40); g.dispose();
        ByteArrayOutputStream out=new ByteArrayOutputStream(); ImageIO.write(image,format,out); return out.toByteArray();
    }
}
