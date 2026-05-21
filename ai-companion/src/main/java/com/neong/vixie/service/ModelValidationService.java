package com.neong.vixie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Validates uploaded Live2D model zip files.
 * Checks for required files: .model3.json, textures, and at least one .motion3.json.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelValidationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate a Live2D model zip file.
     *
     * @param zipStream input stream of the zip file
     * @return validation result with pass/fail and error list
     */
    public ValidationResult validate(InputStream zipStream) {
        List<String> errors = new ArrayList<>();
        Set<String> allEntries = new HashSet<>();
        String model3JsonContent = null;
        String model3JsonPath = null;
        boolean hasMotion3 = false;

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                allEntries.add(name);

                if (name.endsWith(".model3.json") && model3JsonContent == null) {
                    model3JsonPath = name;
                    model3JsonContent = readEntryContent(zis);
                }

                if (name.endsWith(".motion3.json")) {
                    hasMotion3 = true;
                }

                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to read zip file", e);
            errors.add("Invalid or corrupted zip file");
            return new ValidationResult(false, errors);
        }

        // Check .model3.json exists
        if (model3JsonContent == null) {
            errors.add("Missing .model3.json file — every Live2D model requires one");
            return new ValidationResult(false, errors);
        }

        // Validate .model3.json is valid JSON
        try {
            JsonNode modelJson = objectMapper.readTree(model3JsonContent);

            // Check for texture references
            JsonNode texturesNode = modelJson.at("/FileReferences/Textures");
            if (texturesNode.isMissingNode() || !texturesNode.isArray() || texturesNode.isEmpty()) {
                errors.add("model3.json has no texture references under FileReferences.Textures");
            } else {
                // Verify each referenced texture exists in the zip
                String basePath = model3JsonPath.contains("/")
                        ? model3JsonPath.substring(0, model3JsonPath.lastIndexOf('/') + 1)
                        : "";

                for (JsonNode texNode : texturesNode) {
                    String texturePath = basePath + texNode.asText();
                    // Normalize path separators
                    texturePath = texturePath.replace("\\", "/");
                    if (!allEntries.contains(texturePath)) {
                        errors.add("Referenced texture not found in zip: " + texNode.asText());
                    }
                }
            }
        } catch (IOException e) {
            errors.add(".model3.json is not valid JSON: " + e.getMessage());
        }

        // Check motion files
        if (!hasMotion3) {
            errors.add("No .motion3.json files found — at least one motion is required");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private String readEntryContent(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        return baos.toString("UTF-8");
    }

    /**
     * Result of model validation.
     */
    public record ValidationResult(boolean valid, List<String> errors) {}
}
