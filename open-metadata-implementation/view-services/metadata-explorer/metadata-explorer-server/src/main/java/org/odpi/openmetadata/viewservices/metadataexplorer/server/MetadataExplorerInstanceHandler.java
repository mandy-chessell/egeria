/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.viewservices.metadataexplorer.server;


import org.odpi.openmetadata.adminservices.configuration.registration.ViewServiceDescription;
import org.odpi.openmetadata.commonservices.multitenant.OMVSServiceInstanceHandler;



/**
 * MetadataExplorerInstanceHandler retrieves information from the instance map for the
 * view service instances.  The instance map is thread-safe.  Instances are added
 * and removed by the MetadataExplorerAdmin class.
 */
public class MetadataExplorerInstanceHandler extends OMVSServiceInstanceHandler
{
    /**
     * Default constructor registers the view service
     */
    public MetadataExplorerInstanceHandler()
    {
        super(ViewServiceDescription.METADATA_EXPLORER.getViewServiceName());

        MetadataExplorerRegistration.registerViewService();
    }


}