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

import org.keycloak.representations.IDToken;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.github.flytreeleft.sonar.auth.keycloak.KeycloakSettings.LOGIN_STRATEGY_PROVIDER_ID;
import static org.github.flytreeleft.sonar.auth.keycloak.KeycloakSettings.LOGIN_STRATEGY_UNIQUE;

@ServerSide
public class UserIdentityFactory {
    private static final Logger LOGGER = Loggers.get(UserIdentityFactory.class);

    private final KeycloakSettings settings;

    public UserIdentityFactory(KeycloakSettings settings) {
        this.settings = settings;
    }

    public UserIdentity create(IDToken idToken) {
        String login = getLogin(idToken);
        UserIdentity.Builder builder = UserIdentity.builder()
                                                   .setProviderLogin(login)
                                                   .setLogin(generateLogin(idToken))
                                                   .setName(generateName(idToken))
                                                   .setEmail(idToken.getEmail());
        if (this.settings.syncGroups()) {
            builder.setGroups(getGroups(idToken));
        }
        return builder.build();
    }

    private String getLogin(IDToken idToken) {
        String login = idToken.getPreferredUsername();

        if (login == null || login.isEmpty()) {
            Object claim = idToken.getOtherClaims().get("username");
            login = claim != null ? claim.toString() : null;
        }
        return login;
    }

    private Set<String> getGroups(IDToken idToken) {
        Set<String> groups = new HashSet<>();
        Map<String, Object> claims = idToken.getOtherClaims();

        Object claimGroups = claims.get("groups") != null ? claims.get("groups") : claims.get("roles");
        LOGGER.info("Keycloak client roles/groups: " + claimGroups + " (" + claimGroups.getClass().getName() + ")");

        if (claimGroups instanceof String) {
            groups = Arrays.stream(claimGroups.toString().split("\\s*,\\s*")).collect(Collectors.toSet());
        } else if (claimGroups instanceof Collection) {
            groups = ((Collection<String>) claimGroups).stream().collect(Collectors.toSet());
        }

        return groups;
    }

    private String generateLogin(IDToken idToken) {
        String login = getLogin(idToken);

        switch (this.settings.loginStrategy()) {
            case LOGIN_STRATEGY_PROVIDER_ID:
                return login;
            case LOGIN_STRATEGY_UNIQUE:
                return generateUniqueLogin(login);
            default:
                throw new IllegalStateException(format("Login strategy not supported : %s",
                                                       this.settings.loginStrategy()));
        }
    }

    private String generateName(IDToken idToken) {
        String name = idToken.getName();
        if (name == null || name.isEmpty()) {
            String givenName = idToken.getGivenName();
            String familyName = idToken.getFamilyName();
            name = (givenName != null ? givenName : "") + " " + (familyName != null ? familyName : "");
        }

        if (name.trim().isEmpty()) {
            name = getLogin(idToken);
        }
        return name;
    }

    private String generateUniqueLogin(String login) {
        return format("%s@%s", login, KeycloakIdentityProvider.KEY);
    }
}
