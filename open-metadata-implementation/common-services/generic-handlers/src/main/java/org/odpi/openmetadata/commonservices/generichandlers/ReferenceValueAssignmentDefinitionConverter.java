/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.commonservices.generichandlers;


import org.odpi.openmetadata.frameworks.openmetadata.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.ReferenceValueAssignmentDefinitionElement;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.ValidValueElement;
import org.odpi.openmetadata.frameworks.openmetadata.properties.validvalues.ReferenceValueAssignmentProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.EntityDetail;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.InstanceProperties;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.Relationship;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefCategory;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.lang.reflect.InvocationTargetException;


/**
 * ReferenceValueAssignmentDefinitionConverter provides common methods for transferring relevant properties from an Open Metadata Repository Services
 * (OMRS) Relationship and linked EntityDetail object into a bean that inherits from ReferenceValueAssignmentDefinitionElement.
 */
public class ReferenceValueAssignmentDefinitionConverter<B> extends ValidValueConverter<B>
{
    /**
     * Constructor
     *
     * @param repositoryHelper helper object to parse entity
     * @param serviceName name of this component
     * @param serverName local server name
     */
    public ReferenceValueAssignmentDefinitionConverter(OMRSRepositoryHelper repositoryHelper,
                                                       String serviceName,
                                                       String serverName)
    {
        super(repositoryHelper, serviceName, serverName);
    }


    /**
     * Using the supplied instances, return a new instance of the bean. This is used for beans that
     * contain a combination of the properties from an entity and that of a connected relationship.
     *
     * @param beanClass name of the class to create
     * @param entity entity containing the properties
     * @param relationship relationship containing the properties
     * @param methodName calling method
     * @return bean populated with properties from the instances supplied
     * @throws PropertyServerException there is a problem instantiating the bean
     */
    @Override
    public B getNewBean(Class<B>     beanClass,
                        EntityDetail entity,
                        Relationship relationship,
                        String       methodName) throws PropertyServerException
    {
        try
        {
            /*
             * This is initial confirmation that the generic converter has been initialized with an appropriate bean class.
             */
            B returnBean = beanClass.getDeclaredConstructor().newInstance();

            if (returnBean instanceof ReferenceValueAssignmentDefinitionElement bean)
            {
                ValidValueElement                         definition = new ValidValueElement();

                super.updateSimpleMetadataElement(beanClass, definition, entity, methodName);

                bean.setValidValueElement(definition);

                if (relationship != null)
                {
                    InstanceProperties instanceProperties = relationship.getProperties();

                    ReferenceValueAssignmentProperties referenceValueAssignmentProperties = new ReferenceValueAssignmentProperties();

                    referenceValueAssignmentProperties.setAttributeName(this.getAttributeName(instanceProperties));
                    referenceValueAssignmentProperties.setConfidence(this.getConfidence(instanceProperties));
                    referenceValueAssignmentProperties.setSteward(this.getSteward(instanceProperties));
                    referenceValueAssignmentProperties.setStewardTypeName(this.getStewardTypeName(instanceProperties));
                    referenceValueAssignmentProperties.setStewardPropertyName(this.getStewardPropertyName(instanceProperties));
                    referenceValueAssignmentProperties.setNotes(this.getNotes(instanceProperties));

                    bean.setRelationshipProperties(referenceValueAssignmentProperties);
                }
                else
                {
                    handleMissingMetadataInstance(beanClass.getName(), TypeDefCategory.RELATIONSHIP_DEF, methodName);
                }
            }

            return returnBean;
        }
        catch (IllegalAccessException | InstantiationException | ClassCastException | NoSuchMethodException | InvocationTargetException error)
        {
            super.handleInvalidBeanClass(beanClass.getName(), error, methodName);
        }

        return null;
    }
}
