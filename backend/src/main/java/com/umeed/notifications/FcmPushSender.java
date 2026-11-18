package com.umeed.notifications;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Sends notifications through Firebase Cloud Messaging (HTTP v1 API), signed with the service-account key.
 * The key file stays outside the project; its path comes from the UMEED_FIREBASE_CREDENTIALS variable.
 */
class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final GoogleCredentials credentials;
    private final String projectId;
    private final RestClient restClient;

    FcmPushSender(Path keyFile) throws IOException {
        try (InputStream content = Files.newInputStream(keyFile)) {
            ServiceAccountCredentials account = ServiceAccountCredentials.fromStream(content);
            this.projectId = account.getProjectId();
            this.credentials = account.createScoped(List.of(SCOPE));
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IOException("The Firebase key file has no project_id");
        }
        this.restClient = RestClient.builder().baseUrl("https://fcm.googleapis.com").build();
    }

    @Override
    public Result send(String token, Notice notice) {
        try {
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();
            Map<String, Object> message = Map.of(
                    "token", token,
                    "notification", Map.of("title", notice.title(), "body", notice.body()),
                    "data", notice.data(),
                    "android", Map.of("priority", "HIGH"));
            restClient.post()
                    .uri("/v1/projects/{project}/messages:send", projectId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message))
                    .retrieve()
                    .toBodilessEntity();
            return Result.SENT;
        } catch (HttpClientErrorException e) {
            // 404 UNREGISTERED: the app was removed or the token replaced. The token is never logged.
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 404 || body.contains("UNREGISTERED")) {
                return Result.INVALID_TOKEN;
            }
            log.warn("Firebase refused a notification: {} {}", e.getStatusCode().value(), body);
            return Result.FAILED;
        } catch (IOException | RestClientException e) {
            log.warn("Could not send a notification", e);
            return Result.FAILED;
        }
    }
}
