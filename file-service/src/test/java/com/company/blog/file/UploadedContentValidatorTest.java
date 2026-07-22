package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.blog.file.api.FileValidationException;
import com.company.blog.file.api.UploadedContentValidator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class UploadedContentValidatorTest {
    private final UploadedContentValidator validator = new UploadedContentValidator();

    @Test
    void acceptsDecodableImagesAndPdfDocuments() throws Exception {
        assertThatCode(() -> validator.validate("image/png", image("png"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("image/jpeg", image("jpeg"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("application/pdf", pdf())).doesNotThrowAnyException();
    }

    @Test
    void rejectsContentThatOnlyPretendsToHaveAnAllowedFileHeader() {
        assertThatThrownBy(() -> validator.validate(
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00}
        ))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("UPLOADED_FILE_CONTENT_MISMATCH");
        assertThatThrownBy(() -> validator.validate(
                "application/pdf",
                "%PDF-1.7 not a real document".getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        ))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("UPLOADED_FILE_CONTENT_MISMATCH");
    }

    private static byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.BLUE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static byte[] pdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return output.toByteArray();
        }
    }
}
