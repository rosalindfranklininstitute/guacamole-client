/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.saml;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.saml.acs.AssertionConsumerServiceResource;
import org.apache.guacamole.auth.saml.acs.AuthenticationSessionManager;
import org.apache.guacamole.auth.saml.user.SAMLAuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.TokenInjectingUserContext;
import org.apache.guacamole.net.auth.UserContext;

/**
 * AuthenticationProvider implementation that authenticates Guacamole users
 * against a SAML SSO Identity Provider (IdP). This module does not provide any
 * storage for connection information, and must be layered with other modules
 * for authenticated users to have access to Guacamole connections.
 */
public class SAMLAuthenticationProvider extends AbstractAuthenticationProvider {

    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;

    /**
     * Creates a new SAMLAuthenticationProvider that authenticates users
     * against a SAML IdP.
     */
    public SAMLAuthenticationProvider() {

        // Set up Guice injector.
        injector = Guice.createInjector(
            new SAMLAuthenticationProviderModule(this)
        );

    }

    @Override
    public String getIdentifier() {
        return "saml";
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return injector.getInstance(AssertionConsumerServiceResource.class);
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        // Attempt to authenticate user with given credentials
        AuthenticationProviderService authProviderService =
                injector.getInstance(AuthenticationProviderService.class);
        return authProviderService.authenticateUser(credentials);

    }

    @Override
    public UserContext decorate(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        // Only decorate if the user authenticated with SAML
        if (!(authenticatedUser instanceof SAMLAuthenticatedUser))
            return context;

        // Apply SAML-specific tokens to all connections / connection groups
        return new TokenInjectingUserContext(context,
                ((SAMLAuthenticatedUser) authenticatedUser).getTokens());

    }

    @Override
    public void shutdown() {
        injector.getInstance(AuthenticationSessionManager.class).shutdown();
    }

}
