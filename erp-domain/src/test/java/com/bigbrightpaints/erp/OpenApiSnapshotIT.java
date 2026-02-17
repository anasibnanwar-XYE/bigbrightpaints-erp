package com.bigbrightpaints.erp;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "erp.security.swagger-public=true")
public class OpenApiSnapshotIT extends AbstractIntegrationTest {

    private static final String SNAPSHOT_VERIFY_PROPERTY = "erp.openapi.snapshot.verify";
    private static final String SNAPSHOT_VERIFY_ENV = "ERP_OPENAPI_SNAPSHOT_VERIFY";
    private static final String SNAPSHOT_REFRESH_PROPERTY = "erp.openapi.snapshot.refresh";
    private static final String SNAPSHOT_REFRESH_ENV = "ERP_OPENAPI_SNAPSHOT_REFRESH";
    private static final ObjectMapper CANONICAL_JSON = new ObjectMapper()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Autowired
    private TestRestTemplate rest;

    @Test
    void openapi_snapshot_matches_repository_contract() throws IOException {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30_000);
        requestFactory.setReadTimeout(120_000);
        rest.getRestTemplate().setRequestFactory(requestFactory);

        ResponseEntity<String> json = rest.getForEntity("/v3/api-docs", String.class);
        assertThat(json.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.getBody()).as("OpenAPI payload").isNotBlank();

        if (!verifyRequested()) {
            return;
        }

        Path openApiSnapshotPath = resolveRepoRoot().resolve("openapi.json");
        String currentSpec = canonicalizeJson(json.getBody());
        if (refreshRequested()) {
            Files.writeString(openApiSnapshotPath, currentSpec, StandardCharsets.UTF_8);
            return;
        }

        assertThat(Files.exists(openApiSnapshotPath))
                .withFailMessage("Missing OpenAPI snapshot at %s. Remediation: rerun intentionally with -D%s=true "
                                + "or %s=true (with -D%s=true or %s=true) to generate it.",
                        openApiSnapshotPath,
                        SNAPSHOT_REFRESH_PROPERTY,
                        SNAPSHOT_REFRESH_ENV,
                        SNAPSHOT_VERIFY_PROPERTY,
                        SNAPSHOT_VERIFY_ENV)
                .isTrue();

        String snapshotSpec = canonicalizeJson(Files.readString(openApiSnapshotPath, StandardCharsets.UTF_8));
        assertThat(currentSpec)
                .withFailMessage("OpenAPI snapshot drift detected at %s. Verify mode is non-mutating unless refresh is enabled. "
                                + "Remediation: rerun intentionally with -D%s=true (or %s=true) and -D%s=true "
                                + "(or %s=true), then commit updated openapi.json.",
                        openApiSnapshotPath,
                        SNAPSHOT_VERIFY_PROPERTY,
                        SNAPSHOT_VERIFY_ENV,
                        SNAPSHOT_REFRESH_PROPERTY,
                        SNAPSHOT_REFRESH_ENV)
                .isEqualTo(snapshotSpec);
    }

    private static boolean verifyRequested() {
        return Boolean.parseBoolean(System.getProperty(
                SNAPSHOT_VERIFY_PROPERTY,
                System.getenv().getOrDefault(SNAPSHOT_VERIFY_ENV, "false")));
    }

    private static boolean refreshRequested() {
        return Boolean.parseBoolean(System.getProperty(
                SNAPSHOT_REFRESH_PROPERTY,
                System.getenv().getOrDefault(SNAPSHOT_REFRESH_ENV, "false")));
    }

    private static Path resolveRepoRoot() {
        Path moduleRoot = Path.of("").toAbsolutePath().normalize();
        if (moduleRoot.getFileName() != null && "erp-domain".equals(moduleRoot.getFileName().toString())) {
            Path parent = moduleRoot.getParent();
            if (parent != null) {
                return parent;
            }
        }
        return moduleRoot;
    }

    private static String canonicalizeJson(String spec) throws IOException {
        return CANONICAL_JSON.writeValueAsString(CANONICAL_JSON.readTree(spec));
    }
}
