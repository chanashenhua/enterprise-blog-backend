package com.company.blog.file.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

@Component
public class UploadedContentValidator {
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;

    public void validate(String contentType, byte[] content) {
        boolean valid = switch (contentType) {
            case "image/png" -> isDecodableImage(content, "png");
            case "image/jpeg" -> isDecodableImage(content, "jpeg");
            case "application/pdf" -> isDecodablePdf(content);
            default -> false;
        };
        if (!valid) {
            throw new FileValidationException("UPLOADED_FILE_CONTENT_MISMATCH");
        }
    }

    private static boolean isDecodableImage(byte[] content, String expectedFormat) {
        try {
            ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content));
            if (input == null) {
                return false;
            }
            try (input) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) {
                    return false;
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    if (!expectedFormat.equals(reader.getFormatName().toLowerCase(Locale.ROOT))) {
                        return false;
                    }
                    long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                    if (pixels <= 0 || pixels > MAX_IMAGE_PIXELS) {
                        return false;
                    }
                    BufferedImage image = reader.read(0);
                    return image != null;
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean isDecodablePdf(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            document.getNumberOfPages();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
