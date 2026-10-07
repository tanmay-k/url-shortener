package com.practice.url_shortner.utility;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

@Component
public class JwtUtils {
	@Value("${app.security.jwt-secret}")
	private String jwtSecret;

	@Value("${app.security.jwt-issuer}")
	private String issuer;

	@Value("${app.security.jwt-audience}")
	private String audience;

	@Value("${app.security.jwt-validity}")
	private long tokenValidity;

	public String createToken(String userName){
		return createToken(userName, Instant.now().plusSeconds(tokenValidity));
	}

	/**
	 * Creates a token that expires at the given instant instead of the configured
	 * validity. An instant in the past yields an already-expired token.
	 */
	public String createToken(String userName, Instant expiresAt){
		Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
		return JWT.create()
				.withSubject(userName)
				.withIssuedAt(Instant.now())
				.withExpiresAt(expiresAt)
				.withIssuer(issuer)
				.withAudience(audience)
				.sign(algorithm);
	}

	public DecodedJWT verifyJwt(String token){
		Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
		JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer(issuer)
				.withAudience(audience)
				.acceptExpiresAt(0)
				.build();
		return verifier.verify(token);
	}
}