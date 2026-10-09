package com.jobtrack.jobtrack.service;

import com.jobtrack.jobtrack.dto.JobDescriptionExtractionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.MockRestServiceServer.bindTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JobDescriptionExtractionServiceTest {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String TEST_TOKEN = "unit-test-token";

    private MockRestServiceServer server;
    private JobDescriptionExtractionService service;

    @BeforeEach
    void setUp() {
        configureService(TEST_TOKEN);
    }

    @Test
    void extractsDextersTechAndPreservesTheExactOriginalDescription() {
        String description = "FULLSTACK WEB DEVELOPMENT INTERN\r\n"
                + "Company: Dexter's TechLocation: RemoteDuration: 3 Months"
                + "Employment Type: Full-Time InternshipStipend: Performance-Based, up to \u20B97,500/-\r\n"
                + "ABOUT THE COMPANY\r\nDexter's Tech builds digital products.\r\n"
                + "ABOUT THE ROLE\r\nWork with the web development team.\r\n"
                + "RESPONSIBILITIES\r\nBuild and maintain web features.\r\n"
                + "ELIGIBILITY\r\nStudents interested in web development.\r\n"
                + "REQUIRED SKILLS\u2022HTML\u2022CSS\u2022JavaScript\u2022React\u2022Git\r\n"
                + "PERKS & BENEFITS\r\nLearn with an experienced team.";
        expectGroqResponse("""
                {"company":"Dexter's Tech","role":"Fullstack Web Development Intern","location":"Remote",
                 "requiredSkills":"HTML, CSS, JavaScript, React, Git","jobUrl":""}
                """);

        JobDescriptionExtractionResponse result = service.extract(description);

        assertEquals("Dexter's Tech", result.company());
        assertEquals("Fullstack Web Development Intern", result.role());
        assertEquals("Remote", result.location());
        assertEquals("HTML, CSS, JavaScript, React, Git", result.requiredSkills());
        assertEquals("", result.jobUrl());
        assertEquals(description, result.jobDescription());
        server.verify();
    }

    @Test
    void missingFieldsRemainEmptyAndValidHttpUrlIsReturned() {
        String description = "Some posting with only a role and a URL.";
        expectGroqResponse("""
                {"role":"Data Analyst","jobUrl":"https://jobs.example.test/posting"}
                """);

        JobDescriptionExtractionResponse result = service.extract(description);

        assertEquals("", result.company());
        assertEquals("Data Analyst", result.role());
        assertEquals("", result.location());
        assertEquals("", result.requiredSkills());
        assertEquals("https://jobs.example.test/posting", result.jobUrl());
        assertEquals(description, result.jobDescription());
        server.verify();
    }

    @Test
    void invalidOrNonHttpUrlIsReturnedAsEmpty() {
        expectGroqResponse("""
                {"company":"Example","role":"","location":"","requiredSkills":"",
                 "jobUrl":"javascript:alert(1)"}
                """);

        JobDescriptionExtractionResponse result = service.extract("A description.");

        assertEquals("", result.jobUrl());
        server.verify();
    }

    @Test
    void missingApiKeyReturnsFriendlyConfigurationErrorWithoutCallingGroq() {
        configureService("");

        JobDescriptionExtractionException exception = assertThrows(
                JobDescriptionExtractionException.class,
                () -> service.extract("A description."));

        assertEquals(true, exception.getMessage().contains("GROQ_API_KEY"));
        server.verify();
    }

    @Test
    void providerErrorsAreReturnedAsFriendlyUnavailableErrors() {
        server.expect(requestTo(GROQ_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"rate limited\"}"));

        JobDescriptionExtractionException exception = assertThrows(
                JobDescriptionExtractionException.class,
                () -> service.extract("A description."));

        assertEquals("AI extraction is temporarily unavailable. Please try again later.",
                exception.getMessage());
        server.verify();
    }

    @Test
    void invalidStructuredContentReturnsFriendlyInvalidResponseError() {
        expectGroqResponse("this is not JSON");

        JobDescriptionExtractionException exception = assertThrows(
                JobDescriptionExtractionException.class,
                () -> service.extract("A description."));

        assertEquals("The extraction service returned an invalid response. Please try again.",
                exception.getMessage());
        server.verify();
    }

    private void configureService(String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        server = bindTo(builder).build();
        service = new JobDescriptionExtractionService(
                builder, JsonMapper.builder().build(), apiKey, "openai/gpt-oss-20b");
    }

    private void expectGroqResponse(String extractionJson) {
        server.expect(requestTo(GROQ_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "model": "openai/gpt-oss-20b",
                          "response_format": {
                            "type": "json_schema",
                            "json_schema": {
                              "name": "job_application_extraction",
                              "strict": true,
                              "schema": {
                                "required": ["company", "role", "location", "requiredSkills", "jobUrl"],
                                "additionalProperties": false
                              }
                            }
                          }
                        }
                        """, false))
                .andRespond(withSuccess(groqEnvelope(extractionJson), MediaType.APPLICATION_JSON));
    }

    private String groqEnvelope(String extractionJson) {
        String escaped = extractionJson.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
        return "{\"choices\":[{\"message\":{\"content\":\"" + escaped + "\"}}]}";
    }
}
