/*
 * Keycloak Authentication for SonarQube
 * Copyright (C) 2017 flytreeleft
 * mailto:flytreeleft@126.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.github.flytreeleft.sonar.auth.keycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

@ServerSide
public class KeycloakIdentityProvider implements OAuth2IdentityProvider {
    public static final String KEY = "keycloak";

    private final KeycloakSettings settings;
    private final UserIdentityFactory userIdentityFactory;
    private transient KeycloakDeployment keycloakDeployment;

    public KeycloakIdentityProvider(KeycloakSettings settings, UserIdentityFactory userIdentityFactory) {
        this.settings = settings;
        this.userIdentityFactory = userIdentityFactory;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getName() {
        return "Keycloak";
    }

    @Override
    public Display getDisplay() {
        return Display.builder()
                      // URL of src/main/resources/static/keycloak.svg at runtime
                      .setIconPath("/static/authkeycloak/keycloak.svg").setBackgroundColor("#444444").build();
    }

    @Override
    public boolean isEnabled() {
        return this.settings.isEnabled();
    }

    @Override
    public boolean allowsUsersToSignUp() {
        return this.settings.allowUsersToSignUp();
    }

    @Override
    public void init(InitContext context) {
        String redirect = context.getCallbackUrl();
        String state = UUID.randomUUID().toString();
        String authUrl = getKeycloakDeployment().getAuthUrl()
                                                .clone()
                                                .queryParam(OAuth2Constants.CLIENT_ID,
                                                            getKeycloakDeployment().getResourceName())
                                                .queryParam(OAuth2Constants.REDIRECT_URI, redirect)
                                                .queryParam(OAuth2Constants.STATE, state)
                                                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                                                .build()
                                                .toString();
        context.redirectTo(authUrl);
    }

    @Override
    public void callback(CallbackContext context) {
        try {
            onCallback(context);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void onCallback(CallbackContext context) throws Exception {
        //        context.verifyCsrfState();
        HttpServletRequest request = context.getRequest();
        String code = request.getParameter("code");
        String redirect = context.getCallbackUrl();
        AccessTokenResponse tokenResponse = ServerRequest.invokeAccessCodeToToken(getKeycloakDeployment(),
                                                                                  code,
                                                                                  redirect,
                                                                                  null);
        String idTokenString = tokenResponse.getIdToken();

        if (idTokenString != null) {
            JWSInput input = new JWSInput(idTokenString);
            IDToken idToken = input.readJsonContent(IDToken.class);

            UserIdentity userIdentity = this.userIdentityFactory.create(idToken);
            context.authenticate(userIdentity);
        }

        context.redirectToRequestedPage();
    }

    private synchronized KeycloakDeployment getKeycloakDeployment() {
        if (this.keycloakDeployment == null || this.keycloakDeployment.getClient() == null) {
            try {
                AdapterConfig adapterConfig = JsonSerialization.readValue(this.settings.keycloakJson(),
                                                                          AdapterConfig.class);
                this.keycloakDeployment = KeycloakDeploymentBuilder.build(adapterConfig);
            } catch (IOException e) {
                throw new IllegalStateException("Parse Keycloak json configuration failed", e);
            }
        }
        return this.keycloakDeployment;
    }
}
