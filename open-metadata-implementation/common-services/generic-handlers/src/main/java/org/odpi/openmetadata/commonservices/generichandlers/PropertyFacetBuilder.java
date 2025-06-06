/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.commonservices.generichandlers;

import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataProperty;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.InvalidParameterException;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.util.Map;


/**
 * PropertyFacetBuilder is able to build the properties for a SearchKeyword entity.
 */
public class PropertyFacetBuilder extends ReferenceableBuilder
{
    private final String              schemaVersion;
    private final String              description;
    private final Map<String, String> properties;


    /**
     * Constructor used for constructing search arguments.
     *
     * @param schemaVersion name of the schemaVersion
     * @param description description of the meaning of the schemaVersion
     * @param repositoryHelper helper methods
     * @param serviceName name of this OMAS
     * @param serverName name of local server
     */
    PropertyFacetBuilder(String               qualifiedName,
                         String               schemaVersion,
                         String               description,
                         Map<String, String>  properties,
                         OMRSRepositoryHelper repositoryHelper,
                         String               serviceName,
                         String               serverName)
    {
        super(qualifiedName,
              OpenMetadataType.PROPERTY_FACET.typeGUID,
              OpenMetadataType.PROPERTY_FACET.typeName,
              repositoryHelper,
              serviceName,
              serverName);

        this.schemaVersion = schemaVersion;
        this.description = description;
        this.properties = properties;
    }


    /**
     * Return the supplied bean properties in an InstanceProperties object.
     *
     * @param methodName name of the calling method
     * @return InstanceProperties object
     * @throws InvalidParameterException there is a problem with the properties
     */
    @Override
    public InstanceProperties getInstanceProperties(String  methodName) throws InvalidParameterException
    {
        InstanceProperties instanceProperties = super.getInstanceProperties(methodName);

        instanceProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                          instanceProperties,
                                                                          OpenMetadataProperty.SCHEMA_VERSION.name,
                                                                          schemaVersion,
                                                                          methodName);

        instanceProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                          instanceProperties,
                                                                          OpenMetadataProperty.DESCRIPTION.name,
                                                                          description,
                                                                          methodName);

        instanceProperties = repositoryHelper.addStringMapPropertyToInstance(serviceName,
                                                                             instanceProperties,
                                                                             OpenMetadataProperty.PROPERTIES.name,
                                                                             properties,
                                                                             methodName);
        return instanceProperties;
    }
}
