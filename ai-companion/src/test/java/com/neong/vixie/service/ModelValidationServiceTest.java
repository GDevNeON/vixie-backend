package com.neong.vixie.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ModelValidationServiceTest {

    private final ModelValidationService service = new ModelValidationService();

    @Test
    void testValidModel() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // .model3.json
            zos.putNextEntry(new ZipEntry("haru.model3.json"));
            zos.write("{\"FileReferences\":{\"Textures\":[\"texture_00.png\"]}}".getBytes());
            zos.closeEntry();
            
            // Texture
            zos.putNextEntry(new ZipEntry("texture_00.png"));
            zos.write("dummy".getBytes());
            zos.closeEntry();
            
            // Motion
            zos.putNextEntry(new ZipEntry("haru.motion3.json"));
            zos.write("{}".getBytes());
            zos.closeEntry();
        }
        
        var result = service.validate(new ByteArrayInputStream(baos.toByteArray()));
        assertTrue(result.valid(), "Should be valid: " + result.errors());
    }

    @Test
    void testMissingModel3Json() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("texture_00.png"));
            zos.write("dummy".getBytes());
            zos.closeEntry();
        }
        var result = service.validate(new ByteArrayInputStream(baos.toByteArray()));
        assertFalse(result.valid());
        assertTrue(result.errors().contains("Missing .model3.json file — every Live2D model requires one"));
    }
}
