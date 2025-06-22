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

/**
 *     @Bean
 *     public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
 *         http
 *                 .authorizeExchange(exchanges -> exchanges
 *                         .anyExchange().authenticated()
 *                 )
 *                 .oauth2ResourceServer(oauth2 -> oauth2
 *                         .jwt(jwt -> jwt
 *                                 .jwtDecoder(reactiveJwtDecoder())
 *                         )
 *                 );
 *         return http.build();
 *     }
 *
 *     @Bean
 *     ReactiveJwtDecoder reactiveJwtDecoder() {
 *         NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder) ReactiveJwtDecoders.fromOidcIssuerLocation(issuer);
 *
 *         OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
 *         OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, scopeValidator);
 *
 *         jwtDecoder.setJwtValidator(withAudience);
 *
 *         return jwtDecoder;
 *     }
 */

/**
 *     @Bean
 *     public OAuth2AuthorizedClientProvider tokenExchange() {
 *
 *         // 1. Create the RestClientTokenExchangeTokenResponseClient
 *         // This client internally uses DefaultOAuth2TokenRequestParametersConverter
 *         // to handle standard token exchange parameters.
 *         RestClientTokenExchangeTokenResponseClient tokenResponseClient =
 *                 new RestClientTokenExchangeTokenResponseClient();
 *
 *         // 2. Customize the tokenResponseClient to add the 'audience' parameter
 *         // The parametersCustomizer is called AFTER the default parameters are added.
 *         tokenResponseClient.setParametersCustomizer(parameters -> {
 *             parameters.add(OAuth2ParameterNames.AUDIENCE, ezcartMcpServerAudience);
 *             if(parameters.containsKey(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE)) {
 *                 parameters.remove(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE);
 *             }
 *             parameters.add(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE, "urn:ietf:params:oauth:token-type:access_token");
 *         });
 *
 *         // 3. Create the TokenExchangeOAuth2AuthorizedClientProvider
 *         // This provider uses the tokenResponseClient to get the access token.
 *         TokenExchangeOAuth2AuthorizedClientProvider provider =
 *                 new TokenExchangeOAuth2AuthorizedClientProvider();
 *         provider.setAccessTokenResponseClient(tokenResponseClient);
 *
 *         return provider;
 *     }
 *
 *     @Bean
 *     public OAuth2AuthorizedClientManager authorizedClientManager(
 *             ClientRegistrationRepository clientRegistrationRepository,
 *             OAuth2AuthorizedClientRepository authorizedClientRepository) {
 *
 *         var authorizedClientProvider =
 *                 OAuth2AuthorizedClientProviderBuilder.builder()
 *                         .clientCredentials()
 *                         .authorizationCode()
 *                         .refreshToken()
 *                         .provider(tokenExchange())
 *                         .build();
 *
 *         var authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
 *                 clientRegistrationRepository, authorizedClientRepository);
 *         authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
 *
 *         return authorizedClientManager;
 *     }
 */