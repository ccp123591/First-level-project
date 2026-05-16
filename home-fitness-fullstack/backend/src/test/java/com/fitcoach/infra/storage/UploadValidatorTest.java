package com.fitcoach.infra.storage;

import com.fitcoach.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadValidatorTest {

    private final UploadValidator validator = new UploadValidator();

    /** 1x1 PNG 真实字节（IHDR + IDAT + IEND）—— 用于跳过 Tika 嗅探。 */
    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(validator, "maxImageBytes", 5L * 1024 * 1024);
    }

    @Test
    void valid_png_passes_and_normalizes_extension() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.PNG", "image/png", PNG_BYTES);
        String ext = validator.validateImage(file);
        assertThat(ext).isEqualTo("png");
    }

    @Test
    void empty_file_rejected_with_400() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> validator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未上传");
    }

    @Test
    void missing_extension_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "no-ext", "image/png", PNG_BYTES);
        assertThatThrownBy(() -> validator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("扩展名");
    }

    @Test
    void unsupported_extension_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", PNG_BYTES);
        assertThatThrownBy(() -> validator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持");
    }

    @Test
    void exe_disguised_as_png_caught_by_tika() {
        // MZ header = Windows PE executable
        byte[] exe = new byte[] {0x4D, 0x5A, (byte)0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", exe);
        assertThatThrownBy(() -> validator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内容与扩展名不匹配");
    }

    @Test
    void too_large_file_rejected_with_413() {
        ReflectionTestUtils.setField(validator, "maxImageBytes", 10L);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);
        assertThatThrownBy(() -> validator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过大");
    }

    @Test
    void jpeg_extension_normalized_to_jpg() {
        // valid JPEG SOI marker
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10,
                'J', 'F', 'I', 'F', 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                (byte) 0xFF, (byte) 0xD9};
        MockMultipartFile file = new MockMultipartFile("file", "a.jpeg", "image/jpeg", jpeg);
        String ext = validator.validateImage(file);
        assertThat(ext).isEqualTo("jpg");
    }
}
