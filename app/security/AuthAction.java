package security;

// ==============================================================================
// Importations
// ==============================================================================

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import models.User;
import play.mvc.*;
import play.mvc.Http.Cookie;

import javax.crypto.SecretKey;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.typesafe.config.Config;

// ==============================================================================
// Donnees
// ==============================================================================

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

/**
 *	Gestion de l'authentification JWT via cookie httpOnly.
 *	Fournit des methodes statiques pour generer, verifier
 *	les tokens et hasher les mots de passe.
 */
public class AuthAction {

	private static String jwtSecret;

	// ------------------------------------------------------------------------------
	// Configuration
	// ------------------------------------------------------------------------------

	/**
	 *	Initialise le secret JWT depuis la configuration Play.
	 *
	 *	@param config Configuration Play injectee
	 */
	@Inject
	public AuthAction(Config config) {
		jwtSecret = config.getString("play.http.secret.key");
	}

	/**
	 *	Retourne la cle secrete HMAC pour signer les JWT.
	 *
	 *	@return SecretKey Cle HMAC
	 */
	private static SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	// ------------------------------------------------------------------------------
	// JWT
	// ------------------------------------------------------------------------------

	/**
	 *	Genere un token JWT contenant l'id et le role admin.
	 *
	 *	@param userId Identifiant utilisateur
	 *	@param isAdmin Role administrateur
	 *	@return Token JWT signe
	 */
	public static String generateToken(Long userId, boolean isAdmin) {
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim("admin", isAdmin)
			.issuedAt(Date.from(Instant.now()))
			.expiration(Date.from(Instant.now().plus(Duration.ofHours(24))))
			.signWith(getSigningKey())
			.compact();
	}

	/**
	 *	Verifie et decode un token JWT.
	 *
	 *	@param token Token JWT a verifier
	 *	@return Claims du token ou null si invalide
	 */
	public static Claims parseToken(String token) {
		try {
			return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (Exception parseError) {
			return null;
		}
	}

	// ------------------------------------------------------------------------------
	// Cookie
	// ------------------------------------------------------------------------------

	/**
	 *	Cree le cookie httpOnly contenant le token JWT.
	 *
	 *	@param token Token JWT
	 *	@return Cookie httpOnly
	 */
	public static Cookie buildCookie(String token) {
		return Cookie.builder("ps_play_token", token)
			.withPath("/")
			.withHttpOnly(true)
			.withSameSite(Cookie.SameSite.LAX)
			.build();
	}

	/**
	 *	Cree un cookie vide pour deconnecter l'utilisateur.
	 *
	 *	@return Cookie vide avec maxAge 0
	 */
	public static Cookie clearCookie() {
		return Cookie.builder("ps_play_token", "")
			.withPath("/")
			.withHttpOnly(true)
			.withMaxAge(Duration.ZERO)
			.build();
	}

	// ------------------------------------------------------------------------------
	// Extraction utilisateur
	// ------------------------------------------------------------------------------

	/**
	 *	Extrait l'utilisateur connecte depuis le cookie JWT.
	 *
	 *	@param request Requete HTTP
	 *	@return Optional contenant le User ou vide
	 */
	public static Optional<User> getUserFromRequest(Http.Request request) {
		Optional<Cookie> cookie = request.cookie("ps_play_token");
		if (cookie.isEmpty() || cookie.get().value().isEmpty()) {
			return Optional.empty();
		}
		Claims claims = parseToken(cookie.get().value());
		if (claims == null) return Optional.empty();
		Long userId = Long.valueOf(claims.getSubject());
		return Optional.ofNullable(User.find.byId(userId));
	}

	// ------------------------------------------------------------------------------
	// Mot de passe
	// ------------------------------------------------------------------------------

	/**
	 *	Hash un mot de passe avec bcrypt.
	 *
	 *	@param plainPassword Mot de passe en clair
	 *	@return Hash bcrypt
	 */
	public static String hashPassword(String plainPassword) {
		return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
	}

	/**
	 *	Verifie un mot de passe contre un hash bcrypt.
	 *
	 *	@param plainPassword Mot de passe en clair
	 *	@param hashedPassword Hash bcrypt
	 *	@return True si le mot de passe correspond
	 */
	public static boolean checkPassword(String plainPassword, String hashedPassword) {
		BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
		return result.verified;
	}
}