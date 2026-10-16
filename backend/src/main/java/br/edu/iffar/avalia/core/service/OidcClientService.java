package br.edu.iffar.avalia.core.service;

import br.edu.iffar.avalia.core.dto.OidcCallbackDTO;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OidcClientService {
    public record IdentidadeOidc(String cpf, String sub, List<String> grupos, Instant expiraEm) {}
    public record TokensOidc(IdentidadeOidc identidade, String accessToken) {}

    private final String providerUrl = config("AVALIA_OIDC_PROVIDER_URL", "http://localhost:9036/web/auth/realms/feliva");
    private final String expectedIssuer = config("AVALIA_OIDC_ISSUER", providerUrl);
    private final String clientId = config("AVALIA_OIDC_CLIENT_ID", "avalia");
    private final String clientSecret = System.getenv("AVALIA_OIDC_CLIENT_SECRET");
    private final String redirectUri = config("AVALIA_OIDC_REDIRECT_URI", "http://localhost:5173");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private static String config(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String frontendOrigin() {
        URI uri = URI.create(redirectUri);
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    public boolean cookieSecure() {
        return "https".equalsIgnoreCase(URI.create(redirectUri).getScheme());
    }

    public Map<String, String> configuracaoPublica() throws Exception {
        exigirClientSecret();
        JsonObject metadata = descobrir();
        return Map.of(
                "authorizationEndpoint", endpointLocal(metadata, "authorization_endpoint"),
                "endSessionEndpoint", endpointLocal(metadata, "end_session_endpoint"),
                "clientId", clientId,
                "redirectUri", redirectUri
        );
    }

    public TokensOidc trocarCodigoPorTokens(OidcCallbackDTO callback) throws Exception {
        exigirClientSecret();
        if (callback == null || callback.code == null || callback.code.isBlank() || callback.code.length() > 1024
                || callback.codeVerifier == null || !callback.codeVerifier.matches("[A-Za-z0-9._~-]{43,128}")
                || callback.nonce == null || !callback.nonce.matches("[A-Za-z0-9_-]{43,128}")
                || !redirectUri.equals(callback.redirectUri)) {
            throw new SecurityException("Resposta OIDC inválida.");
        }

        JsonObject metadata = descobrir();
        String form = campo("grant_type", "authorization_code")
                + "&" + campo("client_id", clientId)
                + "&" + campo("code", callback.code)
                + "&" + campo("code_verifier", callback.codeVerifier)
                + "&" + campo("redirect_uri", redirectUri);
        form += "&" + campo("client_secret", clientSecret);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointLocal(metadata, "token_endpoint")))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new SecurityException("Código OIDC recusado pelo provedor.");
        JsonObject tokens = json(response.body());
        String idToken = tokens.getString("id_token", null);
        if (idToken == null) throw new SecurityException("Provedor não retornou id_token.");
        if (!"Bearer".equalsIgnoreCase(tokens.getString("token_type", "")))
            throw new SecurityException("Tipo de access token inválido.");
        String accessToken = tokens.getString("access_token", null);
        if (accessToken == null || accessToken.isBlank())
            throw new SecurityException("Provedor não retornou access_token.");
        String issuer = metadata.getString("issuer");
        String jwksUri = endpointLocal(metadata, "jwks_uri");
        IdentidadeOidc identidade = verificarIdentidade(idToken, callback.nonce, issuer, jwksUri);
        IdentidadeOidc acesso = verificarAccessToken(accessToken, identidade, issuer, jwksUri);
        return new TokensOidc(acesso, accessToken);
    }

    private IdentidadeOidc verificarIdentidade(String idToken, String nonce, String issuer, String jwksUri) throws Exception {
        JWTClaimsSet claims = verificarAssinatura(idToken, jwksUri);
        Instant now = Instant.now();
        Date expiry = claims.getExpirationTime();
        Date notBefore = claims.getNotBeforeTime();
        Date issuedAt = claims.getIssueTime();
        String authorizedParty = claims.getStringClaim("azp");
        if (!issuer.equals(claims.getIssuer()) || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getAudience() == null || claims.getAudience().size() != 1
                || !claims.getAudience().contains(clientId)
                || (authorizedParty != null && !clientId.equals(authorizedParty))
                || expiry == null || !expiry.toInstant().isAfter(now)
                || issuedAt == null || issuedAt.toInstant().isAfter(now.plusSeconds(60))
                || !expiry.toInstant().isAfter(issuedAt.toInstant())
                || (notBefore != null && notBefore.toInstant().isAfter(now.plusSeconds(60)))
                || !nonce.equals(claims.getStringClaim("nonce"))) {
            throw new SecurityException("Claims OIDC inválidas.");
        }
        String cpf = claims.getStringClaim("preferred_username");
        if (cpf == null || !cpf.matches("\\d{11}"))
            throw new SecurityException("O provedor não retornou um CPF válido.");
        return new IdentidadeOidc(cpf, claims.getSubject(), List.of(), expiry.toInstant());
    }

    private IdentidadeOidc verificarAccessToken(String accessToken, IdentidadeOidc identidade,
                                                String issuer, String jwksUri) throws Exception {
        JWTClaimsSet claims = verificarAssinatura(accessToken, jwksUri);
        Instant now = Instant.now();
        Date expiry = claims.getExpirationTime();
        Date notBefore = claims.getNotBeforeTime();
        Date issuedAt = claims.getIssueTime();
        if (!issuer.equals(claims.getIssuer()) || !identidade.sub().equals(claims.getSubject())
                || claims.getAudience() == null || claims.getAudience().size() != 1
                || !claims.getAudience().contains(clientId)
                || expiry == null || !expiry.toInstant().isAfter(now)
                || issuedAt == null || issuedAt.toInstant().isAfter(now.plusSeconds(60))
                || !expiry.toInstant().isAfter(issuedAt.toInstant())
                || (notBefore != null && notBefore.toInstant().isAfter(now.plusSeconds(60)))
                || !identidade.cpf().equals(claims.getStringClaim("preferred_username")))
            throw new SecurityException("Claims do access token inválidas.");
        List<String> grupos = claims.getStringListClaim("groups");
        if (grupos == null || grupos.isEmpty())
            throw new SecurityException("O provedor não retornou as permissões do usuário.");
        return new IdentidadeOidc(identidade.cpf(), identidade.sub(), List.copyOf(grupos), expiry.toInstant());
    }

    private JWTClaimsSet verificarAssinatura(String token, String jwksUri) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm()))
            throw new SecurityException("Algoritmo OIDC inválido.");
        String kid = jwt.getHeader().getKeyID();
        if (kid == null || kid.isBlank()) throw new SecurityException("Chave OIDC não identificada.");

        HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUri))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new SecurityException("Chaves OIDC indisponíveis.");
        JWK key = JWKSet.parse(response.body()).getKeyByKeyId(kid);
        if (!(key instanceof RSAKey rsa) || !jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey())))
            throw new SecurityException("Assinatura OIDC inválida.");
        return jwt.getJWTClaimsSet();
    }

    private JsonObject descobrir() throws Exception {
        String url = providerUrl + "/.well-known/openid-configuration";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IllegalStateException("Descoberta OIDC indisponível.");
        JsonObject metadata = json(response.body());
        String discoveredIssuer = metadata.getString("issuer", null);
        URI provider = URI.create(providerUrl);
        boolean loopback = "localhost".equalsIgnoreCase(provider.getHost())
                || "127.0.0.1".equals(provider.getHost()) || "::1".equals(provider.getHost());
        if (discoveredIssuer == null || !expectedIssuer.equals(discoveredIssuer)
                || (!loopback && (!"https".equalsIgnoreCase(provider.getScheme())
                || !providerUrl.equals(discoveredIssuer))))
            throw new SecurityException("Issuer OIDC inesperado.");
        return metadata;
    }

    private String endpointLocal(JsonObject metadata, String name) {
        String issuer = metadata.getString("issuer");
        String published = metadata.getString(name);
        if (!published.startsWith(issuer + "/"))
            throw new SecurityException("Endpoint OIDC inesperado: " + name);
        return providerUrl + published.substring(issuer.length());
    }

    private static JsonObject json(String body) {
        try (var reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        }
    }

    private void exigirClientSecret() {
        if (clientSecret == null || clientSecret.isBlank())
            throw new IllegalStateException("AVALIA_OIDC_CLIENT_SECRET não configurada.");
    }

    private static String campo(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
