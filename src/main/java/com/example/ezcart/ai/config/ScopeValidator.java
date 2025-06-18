package com.example.ezcart.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScopeValidator implements OAuth2TokenValidator<Jwt> {

    private final String requiredScope;

    public ScopeValidator(@Value("${security.oauth2.resourceserver.jwt.scope}") String requiredScope) {
        this.requiredScope = requiredScope;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> scopes = jwt.getClaimAsStringList("scp");
        if (scopes != null && scopes.contains(requiredScope)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error("invalid_token", "The required scope is missing", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
