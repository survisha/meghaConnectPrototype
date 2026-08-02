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
            int orientation=properties.getImage().isAutoRotate()?jpegOrientation(source.bytes()):1;
            boolean rotationApplied=orientation==3||orientation==6||orientation==8;
            BufferedImage oriented=rotationApplied?rotate(original,orientation):original;
            int limit = properties.getImage().getMaxLongestSide();
            double scale = Math.min(1d, (double) limit / Math.max(oriented.getWidth(), oriented.getHeight()));
            int width = Math.max(1, (int)Math.round(oriented.getWidth()*scale));
            int height = Math.max(1, (int)Math.round(oriented.getHeight()*scale));
            boolean resizeApplied=width!=oriented.getWidth()||height!=oriented.getHeight();
            BufferedImage output = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setColor(Color.WHITE); graphics.fillRect(0,0,width,height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(oriented,0,0,width,height,null); graphics.dispose();
            byte[] encoded = jpeg(output, (float)properties.getImage().getJpegQuality());
            boolean meaningfulReduction=encoded.length<source.bytes().length*0.95d;
            boolean originalRetained=properties.getImage().isPreventSizeIncrease()&&!resizeApplied&&!rotationApplied&&!meaningfulReduction;
            byte[] bytes=originalRetained?source.bytes():encoded;
            String outputMime=originalRetained?source.mimeType():"image/jpeg";
            if(bytes.length>properties.getImage().getMaxProcessedSizeBytes())
                throw new FormExtractionException("FORM_IMAGE_PROCESSED_TOO_LARGE","The optimized form image exceeds the configured size limit.",413);
            double reduction=source.bytes().length==0?0d:(source.bytes().length-bytes.length)*100d/source.bytes().length;
            log.info("Visitor form image preprocessed originalMimeType={} outputMimeType={} originalWidth={} originalHeight={} processedWidth={} processedHeight={} originalBytes={} processedBytes={} reductionPercent={} cropApplied=false rotationApplied={} resizeApplied={} reencodeApplied={} originalRetained={} durationMs={}",
                    source.mimeType(),outputMime,source.width(),source.height(),originalRetained?source.width():width,
                    originalRetained?source.height():height,source.bytes().length,bytes.length,String.format(java.util.Locale.ROOT,"%.2f",reduction),
                    rotationApplied,resizeApplied,!originalRetained,originalRetained,(System.nanoTime()-started)/1_000_000);
            return new ProcessedImage(bytes,outputMime,originalRetained?source.width():width,originalRetained?source.height():height);
        } catch(FormExtractionException ex) { throw ex; }
        catch(IOException ex) { throw new FormExtractionException("FORM_IMAGE_PREPROCESSING_FAILED","The form image could not be optimized.",400,ex); }
    }
    private byte[] jpeg(BufferedImage image,float quality) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        try(ImageOutputStream stream=ImageIO.createImageOutputStream(out)) { writer.setOutput(stream); ImageWriteParam p=writer.getDefaultWriteParam(); p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); p.setCompressionQuality(quality); writer.write(null,new IIOImage(image,null,null),p); }
        finally { writer.dispose(); }
        return out.toByteArray();
    }
    BufferedImage rotate(BufferedImage source,int orientation) {
        int width=(orientation==6||orientation==8)?source.getHeight():source.getWidth();
        int height=(orientation==6||orientation==8)?source.getWidth():source.getHeight();
        BufferedImage target=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB); Graphics2D g=target.createGraphics();
        if(orientation==3){ g.translate(width,height); g.rotate(Math.PI); }
        else if(orientation==6){ g.translate(width,0); g.rotate(Math.PI/2); }
        else if(orientation==8){ g.translate(0,height); g.rotate(-Math.PI/2); }
        g.drawImage(source,0,0,null); g.dispose(); return target;
    }
    private int jpegOrientation(byte[] data) {
        try {
            for(int i=2;i+12<data.length;) {
                if((data[i]&255)!=255) break; int marker=data[i+1]&255; int length=((data[i+2]&255)<<8)|(data[i+3]&255);
                if(marker==225&&i+length+2<=data.length&&data[i+4]=='E'&&data[i+5]=='x'&&data[i+6]=='i'&&data[i+7]=='f') {
                    int t=i+10; boolean little=data[t]=='I'&&data[t+1]=='I'; int offset=readInt(data,t+4,little); int dir=t+offset;
                    int count=readShort(data,dir,little);
                    for(int e=0;e<count;e++){ int p=dir+2+e*12; if(readShort(data,p,little)==274) return readShort(data,p+8,little); }
                    return 1;
                }
                if(length<2) break; i+=length+2;
            }
        } catch(IndexOutOfBoundsException ignored) { return 1; }
        return 1;
    }
    private int readShort(byte[] d,int p,boolean little){ return little?(d[p]&255)|((d[p+1]&255)<<8):((d[p]&255)<<8)|(d[p+1]&255); }
    private int readInt(byte[] d,int p,boolean little){ return little?readShort(d,p,true)|(readShort(d,p+2,true)<<16):(readShort(d,p,false)<<16)|readShort(d,p+2,false); }
    public record ProcessedImage(byte[] bytes,String mimeType,int width,int height) {}
}
