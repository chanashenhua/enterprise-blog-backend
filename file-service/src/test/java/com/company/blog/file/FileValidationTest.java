package com.company.blog.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FileValidationTest {
    private final FileValidation validation = new FileValidation(
            Set.of("image/png", "image/jpeg", "application/pdf"),
            10 * 1024 * 1024
    );

    @Test
    void rejectsExecutableUpload() {
        FileValidationResult result = validation.validate("script.exe", "application/x-msdownload", 1024);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    void rejectsAllowedMimeTypeWithUnexpectedExtension() {
        FileValidationResult result = validation.validate("script.exe", "image/png", 1024);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("FILE_TYPE_NOT_ALLOWED");
    }
}
