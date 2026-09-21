package mexa.club.authservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import mexa.club.authservice.exception.InvalidGoogleTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.google.client-id}") String googleClientId) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("app.google.client-id must be configured");
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singleton(googleClientId))
                .setAcceptableTimeSkewSeconds(300L)
                .build();
    }

    public GooglePrincipal verify(String idToken) {
        try {
            GoogleIdToken parsed = verifier.verify(idToken);
            if (parsed == null) {
                throw new InvalidGoogleTokenException("Google token is invalid");
            }
            GoogleIdToken.Payload payload = parsed.getPayload();
            String email = payload.getEmail();
            String sub = payload.getSubject();
            Object nameObj = payload.get("name");
            Object pictureObj = payload.get("picture");
            if (email == null || email.isBlank() || sub == null || sub.isBlank()) {
                throw new InvalidGoogleTokenException("Google token missing required claims");
            }
            String name = nameObj != null ? String.valueOf(nameObj) : null;
            String picture = pictureObj != null ? String.valueOf(pictureObj) : null;
            return new GooglePrincipal(sub, email.trim().toLowerCase(), name, picture);
        } catch (GeneralSecurityException | IOException ex) {
            throw new InvalidGoogleTokenException("Google token verification failed", ex);
        }
    }

    public record GooglePrincipal(String googleId, String email, String name, String avatarUrl) {}
}
