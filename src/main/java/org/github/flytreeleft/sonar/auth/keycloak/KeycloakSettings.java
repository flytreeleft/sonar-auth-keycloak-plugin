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

import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ServerSide;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.sonar.api.PropertyType.BOOLEAN;
import static org.sonar.api.PropertyType.SINGLE_SELECT_LIST;
import static org.sonar.api.PropertyType.TEXT;

@ServerSide
public class KeycloakSettings {
    public static final String LOGIN_STRATEGY_UNIQUE = "Unique";
    public static final String LOGIN_STRATEGY_PROVIDER_ID = "Same as Keycloak login";

    private static final String KEYCLOAK_JSON = "sonar.auth.keycloak.config";
    private static final String ENABLED = "sonar.auth.keycloak.enabled";
    private static final String ALLOW_USERS_TO_SIGN_UP = "sonar.auth.keycloak.allowUsersToSignUp";

    private static final String GROUPS_SYNC = "sonar.auth.keycloak.groupsSync";
    private static final String LOGIN_STRATEGY = "sonar.auth.keycloak.loginStrategy";
    private static final String LOGIN_STRATEGY_DEFAULT_VALUE = LOGIN_STRATEGY_PROVIDER_ID;

    private static final String CATEGORY = "keycloak";
    private static final String SUBCATEGORY = "authentication";

    private final Settings settings;

    public KeycloakSettings(Settings settings) {
        this.settings = settings;
    }

    public boolean isEnabled() {
        return this.settings.getBoolean(ENABLED) && !keycloakJson().isEmpty();
    }

    public String keycloakJson() {
        return this.settings.getString(KEYCLOAK_JSON);
    }

    public String loginStrategy() {
        return emptyIfNull(this.settings.getString(LOGIN_STRATEGY));
    }

    public boolean allowUsersToSignUp() {
        return this.settings.getBoolean(ALLOW_USERS_TO_SIGN_UP);
    }

    public boolean syncGroups() {
        return this.settings.getBoolean(GROUPS_SYNC);
    }

    private static String emptyIfNull(@Nullable String s) {
        return s == null ? "" : s;
    }

    public static List<PropertyDefinition> definitions() {
        int index = 1;
        return Arrays.asList(PropertyDefinition.builder(ENABLED)
                                               .name("Enabled")
                                               .description(
                                                       "Enable Keycloak users to login. Value is ignored if 'Keycloak JSON' are not defined.")
                                               .category(CATEGORY)
                                               .subCategory(SUBCATEGORY)
                                               .type(BOOLEAN)
                                               .defaultValue(valueOf(false))
                                               .index(index++)
                                               .build(),
                             PropertyDefinition.builder(KEYCLOAK_JSON)
                                               .name("Keycloak JSON")
                                               .description(
                                                       "Keycloak json configuration content. You can copy it from '[Your realm] -> Clients -> [The client for sonar] -> Installation -> [Choose 'Keycloak OIDC JSON' option]'")
                                               .category(CATEGORY)
                                               .subCategory(SUBCATEGORY)
                                               .type(TEXT)
                                               .index(index++)
                                               .build(),
                             PropertyDefinition.builder(ALLOW_USERS_TO_SIGN_UP)
                                               .name("Allow users to sign-up")
                                               .description(
                                                       "Allow new users to authenticate. When set to 'false', only existing users will be able to authenticate to the server.")
                                               .category(CATEGORY)
                                               .subCategory(SUBCATEGORY)
                                               .type(BOOLEAN)
                                               .defaultValue(valueOf(true))
                                               .index(index++)
                                               .build(),
                             PropertyDefinition.builder(LOGIN_STRATEGY)
                                               .name("Login generation strategy")
                                               .description(format(
                                                       "When the login strategy is set to '%s', the user's login will be auto-generated the first time so that it is unique. "
                                                       + "When the login strategy is set to '%s', the user's login will be the Keycloak login.",
                                                       LOGIN_STRATEGY_UNIQUE,
                                                       LOGIN_STRATEGY_PROVIDER_ID))
                                               .category(CATEGORY)
                                               .subCategory(SUBCATEGORY)
                                               .type(SINGLE_SELECT_LIST)
                                               .defaultValue(LOGIN_STRATEGY_DEFAULT_VALUE)
                                               .options(LOGIN_STRATEGY_UNIQUE, LOGIN_STRATEGY_PROVIDER_ID)
                                               .index(index++)
                                               .build(),
                             PropertyDefinition.builder(GROUPS_SYNC)
                                               .name("Synchronize user client roles")
                                               .description(
                                                       "The user will be associated to a group which has the same name with the client role in SonarQube.")
                                               .category(CATEGORY)
                                               .subCategory(SUBCATEGORY)
                                               .type(BOOLEAN)
                                               .defaultValue(valueOf(false))
                                               .index(index++)
                                               .build());
    }
}
