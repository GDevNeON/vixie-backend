package com.neong.vixie.helpers;

import com.neong.vixie.helpers.api.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    @Test
    void sanitize_nullInput_returnsNull() {
        assertNull(HtmlSanitizer.sanitize(null));
    }

    @Test
    void sanitize_nullBytes_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> HtmlSanitizer.sanitize("hello\0world"));
    }

    @Test
    void sanitize_htmlTags_stripsAll() {
        String input = "<b>hello</b><script>alert('x')</script>";
        String result = HtmlSanitizer.sanitize(input);
        assertEquals("helloalert('x')", result);
    }

    @Test
    void sanitize_whitespace_trims() {
        assertEquals("hello", HtmlSanitizer.sanitize("  hello  "));
    }

    @Test
    void sanitize_cleanInput_returnsUnchanged() {
        assertEquals("clean text", HtmlSanitizer.sanitize("clean text"));
    }
}
