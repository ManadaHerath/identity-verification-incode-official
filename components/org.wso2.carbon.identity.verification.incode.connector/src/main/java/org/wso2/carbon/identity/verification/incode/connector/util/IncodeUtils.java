/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.verification.incode.connector.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.application.common.model.FederatedAuthenticatorConfig;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.User;
import org.wso2.carbon.identity.user.profile.mgt.association.federation.exception.FederatedAssociationManagerClientException;
import org.wso2.carbon.identity.user.profile.mgt.association.federation.exception.FederatedAssociationManagerException;
import org.wso2.carbon.identity.user.profile.mgt.association.federation.model.FederatedAssociation;
import org.wso2.carbon.identity.verification.incode.connector.constants.Constants;
import org.wso2.carbon.identity.verification.incode.connector.constants.ErrorConstants.ErrorMessage;
import org.wso2.carbon.identity.verification.incode.connector.internal.IncodeConnectorDataHolder;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;

import java.util.ArrayList;
import java.util.List;

/**
 * Link lookups and logging shared by the Incode authenticator and executor.
 */
public final class IncodeUtils {

    private static final Log LOG = LogFactory.getLog(IncodeUtils.class);

    private IncodeUtils() {

    }

    /**
     * The user's link to an identity made by an Incode connection. Sign-up links the user through the onboarding
     * connection, so the link is found by the connection's authenticator, not by the name of the connection in use.
     * A user that does not exist has no link, so callers do not reveal whether a username exists.
     */
    public static String getLinkedIdentity(User user)
            throws FederatedAssociationManagerException, IdentityProviderManagementException {

        FederatedAssociation[] associations;
        try {
            associations = IncodeConnectorDataHolder.getFederatedAssociationManager()
                    .getFederatedAssociationsOfUser(user);
        } catch (FederatedAssociationManagerClientException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No Incode link: the user could not be resolved. " + e.getMessage());
            }
            return null;
        }
        for (FederatedAssociation association : associations) {
            if (isIncodeConnection(association.getIdp().getName(), user.getTenantDomain())) {
                return association.getFederatedUserId();
            }
        }
        return null;
    }

    /**
     * Whether the Incode identity is linked to an account through the given connection or any other Incode
     * connection of the organization, the same connections whose links login and password recovery accept.
     */
    public static boolean isIdentityLinked(String identityId, String connectionName, String tenantDomain)
            throws FederatedAssociationManagerException, IdentityProviderManagementException {

        List<String> connectionNames = new ArrayList<>();
        connectionNames.add(connectionName);
        List<IdentityProvider> idps = IncodeConnectorDataHolder.getIdpManager().getIdPs(tenantDomain);
        if (idps != null) {
            for (IdentityProvider idp : idps) {
                String name = idp.getIdentityProviderName();
                if (!connectionName.equals(name) && isIncodeConnection(name, tenantDomain)) {
                    connectionNames.add(name);
                }
            }
        }
        for (String name : connectionNames) {
            if (IncodeConnectorDataHolder.getFederatedAssociationManager().getUserForFederatedAssociation(
                    tenantDomain, name, identityId) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs a client error at info level and a server error at error level.
     */
    public static void log(Log log, ErrorMessage error) {

        if (error.isClientError()) {
            log.info(error);
        } else {
            log.error(error);
        }
    }

    private static boolean isIncodeConnection(String connectionName, String tenantDomain)
            throws IdentityProviderManagementException {

        IdentityProvider idp = IncodeConnectorDataHolder.getIdpManager().getIdPByName(connectionName, tenantDomain);
        FederatedAuthenticatorConfig authenticator = idp == null ? null : idp.getDefaultAuthenticatorConfig();
        return authenticator != null && Constants.AUTHENTICATOR_NAME.equals(authenticator.getName());
    }
}
