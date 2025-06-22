package com.example.ezcart.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.TokenExchangeOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.RestClientTokenExchangeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${okta.ezcart-mcp-server.audience}")
    private String ezcartMcpServerAudience;

    private final ScopeValidator scopeValidator;
    private final AudienceValidator audienceValidator;

    public SecurityConfig(ScopeValidator scopeValidator, AudienceValidator audienceValidator) {
        this.scopeValidator = scopeValidator;
        this.audienceValidator = audienceValidator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                )
            );
        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientProvider tokenExchange() {

        // 1. Create the RestClientTokenExchangeTokenResponseClient
        // This client internally uses DefaultOAuth2TokenRequestParametersConverter
        // to handle standard token exchange parameters.
        RestClientTokenExchangeTokenResponseClient tokenResponseClient =
                new RestClientTokenExchangeTokenResponseClient();

        // 2. Customize the tokenResponseClient to add the 'audience' parameter
        // The parametersCustomizer is called AFTER the default parameters are added.
        tokenResponseClient.setParametersCustomizer(parameters -> {
            parameters.add(OAuth2ParameterNames.AUDIENCE, ezcartMcpServerAudience);
            if(parameters.containsKey(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE)) {
                parameters.remove(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE);
            }
            parameters.add(OAuth2ParameterNames.SUBJECT_TOKEN_TYPE, "urn:ietf:params:oauth:token-type:access_token");
        });

        // 3. Create the TokenExchangeOAuth2AuthorizedClientProvider
        // This provider uses the tokenResponseClient to get the access token.
        TokenExchangeOAuth2AuthorizedClientProvider provider =
                new TokenExchangeOAuth2AuthorizedClientProvider();
        provider.setAccessTokenResponseClient(tokenResponseClient);

        return provider;
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {

        var authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .authorizationCode()
                        .refreshToken()
                        .provider(tokenExchange())
                        .build();

        var authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, scopeValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }
}
