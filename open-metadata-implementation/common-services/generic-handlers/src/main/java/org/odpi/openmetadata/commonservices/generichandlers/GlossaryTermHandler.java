/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.commonservices.generichandlers;

import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataProperty;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.metadatasecurity.server.OpenMetadataServerSecurityVerifier;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.MatchCriteria;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.RelationshipDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDef;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.TypeDefStatus;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.util.*;

/**
 * GlossaryTermHandler retrieves Glossary Term objects from the property server.  It runs server-side
 * and retrieves Glossary Term entities through the OMRSRepositoryConnector.
 *
 * @param <B> class for the glossary term bean
 */
public class GlossaryTermHandler<B> extends ReferenceableHandler<B>
{
    /**
     * Construct the glossary term handler caching the objects needed to operate within a single server instance.
     *
     * @param converter specific converter for this bean class
     * @param beanClass name of bean class that is represented by the generic class B
     * @param serviceName      name of this service
     * @param serverName       name of the local server
     * @param invalidParameterHandler handler for managing parameter errors
     * @param repositoryHandler     manages calls to the repository services
     * @param repositoryHelper provides utilities for manipulating the repository services objects
     * @param localServerUserId userId for this server
     * @param securityVerifier open metadata security services verifier
     * @param supportedZones list of zones that the access service is allowed to serve Asset instances from.
     * @param defaultZones list of zones that the access service should set in all new Asset instances.
     * @param publishZones list of zones that the access service sets up in published Asset instances.
     * @param auditLog destination for audit log events.
     */
    public GlossaryTermHandler(OpenMetadataAPIGenericConverter<B> converter,
                               Class<B>                           beanClass,
                               String                             serviceName,
                               String                             serverName,
                               InvalidParameterHandler            invalidParameterHandler,
                               RepositoryHandler                  repositoryHandler,
                               OMRSRepositoryHelper               repositoryHelper,
                               String                             localServerUserId,
                               OpenMetadataServerSecurityVerifier securityVerifier,
                               List<String>                       supportedZones,
                               List<String>                       defaultZones,
                               List<String>                       publishZones,
                               AuditLog                           auditLog)
    {
        super(converter,
              beanClass,
              serviceName,
              serverName,
              invalidParameterHandler,
              repositoryHandler,
              repositoryHelper,
              localServerUserId,
              securityVerifier,
              supportedZones,
              defaultZones,
              publishZones,
              auditLog);
    }


    /**
     * Return the list of term-to-term relationship names.
     *
     * @return list of type names that are subtypes of asset
     */
    public List<String> getTermRelationshipTypeNames()
    {
        List<TypeDef>  knownTypeDefs = repositoryHelper.getKnownTypeDefs();
        List<String>   relationshipNames = new ArrayList<>();
        if (knownTypeDefs != null)
        {
            for (TypeDef typeDef : knownTypeDefs)
            {
                if ((typeDef != null) && (typeDef.getStatus() == TypeDefStatus.ACTIVE_TYPEDEF) && (typeDef instanceof RelationshipDef relationshipDef))
                {
                    if ((relationshipDef.getEndDef1().getEntityType().getName().equals(OpenMetadataType.GLOSSARY_TERM.typeName)) &&
                        (relationshipDef.getEndDef2().getEntityType().getName().equals(OpenMetadataType.GLOSSARY_TERM.typeName)))
                    {
                        relationshipNames.add(relationshipDef.getName());
                    }
                }
            }
        }

       return relationshipNames;
    }


    /**
     * Create a new metadata element to represent a glossary term (or a subtype).
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryGUID unique identifier of the owning glossary
     * @param glossaryGUIDParameterName parameter supplying glossaryGUID
     * @param qualifiedName unique name for the category - used in other configuration
     * @param displayName  display name for the term
     * @param aliases alternative names for the glossary term
     * @param summary short description
     * @param description description of the term
     * @param examples examples of this term
     * @param abbreviation abbreviation used for the term
     * @param usage illustrations of how the term is used
     * @param publishVersionIdentifier user control version identifier
     * @param additionalProperties additional properties for a term
     * @param suppliedTypeName type name from the caller (enables creation of subtypes)
     * @param extendedProperties  properties for a term subtype
     * @param initialStatus glossary term status to use when the object is created
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the glossary term
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createGlossaryTerm(String              userId,
                                     String              externalSourceGUID,
                                     String              externalSourceName,
                                     String              glossaryGUID,
                                     String              glossaryGUIDParameterName,
                                     String              qualifiedName,
                                     String              displayName,
                                     List<String>        aliases,
                                     String              summary,
                                     String              description,
                                     String              examples,
                                     String              abbreviation,
                                     String              usage,
                                     String              publishVersionIdentifier,
                                     Map<String, String> additionalProperties,
                                     String              suppliedTypeName,
                                     Map<String, Object> extendedProperties,
                                     InstanceStatus      initialStatus,
                                     Date                effectiveFrom,
                                     Date                effectiveTo,
                                     Date                effectiveTime,
                                     String              methodName) throws InvalidParameterException,
                                                                            UserNotAuthorizedException,
                                                                            PropertyServerException
    {
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        String typeName;

        if (suppliedTypeName != null)
        {
            typeName = suppliedTypeName;
        }
        else if ((initialStatus == null) || (initialStatus == InstanceStatus.ACTIVE))
        {
            typeName = OpenMetadataType.GLOSSARY_TERM.typeName;
        }
        else
        {
            typeName = OpenMetadataType.CONTROLLED_GLOSSARY_TERM.typeName;
        }

        TypeDef glossaryTypeDef = invalidParameterHandler.validateTypeDefName(typeName,
                                                                              OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                              serviceName,
                                                                              methodName,
                                                                              repositoryHelper);

        InstanceStatus instanceStatus;

        if (initialStatus != null)
        {
            instanceStatus = initialStatus;
        }
        else
        {
            instanceStatus = glossaryTypeDef.getInitialStatus();
        }

        GlossaryTermBuilder builder = new GlossaryTermBuilder(qualifiedName,
                                                              displayName,
                                                              aliases,
                                                              summary,
                                                              description,
                                                              examples,
                                                              abbreviation,
                                                              usage,
                                                              publishVersionIdentifier,
                                                              additionalProperties,
                                                              extendedProperties,
                                                              instanceStatus,
                                                              repositoryHelper,
                                                              serviceName,
                                                              serverName);

        builder.setAnchors(userId,
                           null,
                           typeName,
                           OpenMetadataType.GLOSSARY_TERM.typeName,
                           glossaryGUID,
                           methodName);

        builder.setEffectivityDates(effectiveFrom, effectiveTo);

        String glossaryTermGUID = this.createBeanInRepository(userId,
                                                              externalSourceGUID,
                                                              externalSourceName,
                                                              glossaryTypeDef.getGUID(),
                                                              glossaryTypeDef.getName(),
                                                              builder,
                                                              effectiveTime,
                                                              methodName);

        if (glossaryTermGUID != null)
        {
            /*
             * Link the term to its glossary.  This relationship is always effective.
             */
            final String glossaryTermGUIDParameterName = "glossaryTermGUID";

            this.uncheckedLinkElementToElement(userId,
                                               externalSourceGUID,
                                               externalSourceName,
                                               glossaryGUID,
                                               glossaryGUIDParameterName,
                                               glossaryTermGUID,
                                               glossaryTermGUIDParameterName,
                                               OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeGUID,
                                               null,
                                               methodName);
        }

        return glossaryTermGUID;
    }


    /**
     * Create a new metadata element to represent a glossary term using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryGUID unique identifier of the owning glossary
     * @param glossaryGUIDParameterName parameter supplying glossaryGUID
     * @param templateGUID unique identifier of the metadata element to copy
     * @param qualifiedName unique name for the term - used in other configuration
     * @param displayName short display name for the term
     * @param aliases alternative names for the glossary term
     * @param description description of the  term
     * @param publishVersionIdentifier author controlled version identifier
     * @param initialStatus glossary term status to use when the object is created
     * @param deepCopy should the template creation extend to the anchored elements or just the direct entity?
     * @param templateSubstitute is this element a template substitute (used as the "other end" of a new/updated relationship)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the glossary term
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createGlossaryTermFromTemplate(String         userId,
                                                 String         externalSourceGUID,
                                                 String         externalSourceName,
                                                 String         glossaryGUID,
                                                 String         glossaryGUIDParameterName,
                                                 String         templateGUID,
                                                 String         qualifiedName,
                                                 String         displayName,
                                                 List<String>   aliases,
                                                 String         description,
                                                 String         publishVersionIdentifier,
                                                 InstanceStatus initialStatus,
                                                 boolean        deepCopy,
                                                 boolean        templateSubstitute,
                                                 String         methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        final String templateGUIDParameterName   = "templateGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(templateGUID, templateGUIDParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        GlossaryTermBuilder builder = new GlossaryTermBuilder(qualifiedName,
                                                              displayName,
                                                              aliases,
                                                              description,
                                                              publishVersionIdentifier,
                                                              repositoryHelper,
                                                              serviceName,
                                                              serverName);

        builder.setAnchors(userId,
                           null,
                           OpenMetadataType.GLOSSARY_TERM.typeName,
                           OpenMetadataType.GLOSSARY_TERM.typeName,
                           glossaryGUID,
                           methodName);

        String glossaryTermGUID = this.createBeanFromTemplate(userId,
                                                              externalSourceGUID,
                                                              externalSourceName,
                                                              templateGUID,
                                                              templateGUIDParameterName,
                                                              OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                                              OpenMetadataType.GLOSSARY_TERM.typeName,
                                                              qualifiedName,
                                                              OpenMetadataProperty.QUALIFIED_NAME.name,
                                                              builder,
                                                              supportedZones,
                                                              deepCopy,
                                                              templateSubstitute,
                                                              null,
                                                              methodName);

        if (glossaryTermGUID != null)
        {
            final String glossaryTermGUIDParameterName = "glossaryTermGUID";

            if (initialStatus != null)
            {
                this.updateBeanStatusInRepository(userId,
                                                  externalSourceGUID,
                                                  externalSourceName,
                                                  glossaryTermGUID,
                                                  glossaryTermGUIDParameterName,
                                                  OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                                  OpenMetadataType.GLOSSARY_TERM.typeName,
                                                  false,
                                                  false,
                                                  initialStatus,
                                                  "initialStatus",
                                                  null,
                                                  methodName);
            }

            /*
             * Link the term to its glossary.  This relationship is always effective.
             */
            this.uncheckedLinkElementToElement(userId,
                                               externalSourceGUID,
                                               externalSourceName,
                                               glossaryGUID,
                                               glossaryGUIDParameterName,
                                               glossaryTermGUID,
                                               glossaryTermGUIDParameterName,
                                               OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeGUID,
                                               null,
                                               methodName);
        }

        return glossaryTermGUID;
    }


    /**
     * Update the properties of the metadata element representing a glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the glossary term to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryGUID
     * @param qualifiedName unique name for the category - used in other configuration
     * @param displayName short display name for the term
     * @param aliases alternative names for the glossary term
     * @param summary string text
     * @param description description of the  term
     * @param examples string text
     * @param abbreviation string text
     * @param usage string text
     * @param publishVersionIdentifier user-controlled version identifier
     * @param additionalProperties additional properties for a term
     * @param typeName type name from the caller (enables creation of subtypes)
     * @param extendedProperties  properties for a term subtype
     * @param effectiveFrom  the time that the relationship element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the relationship must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateGlossaryTerm(String              userId,
                                   String              externalSourceGUID,
                                   String              externalSourceName,
                                   String              glossaryTermGUID,
                                   String              glossaryTermGUIDParameterName,
                                   String              qualifiedName,
                                   String              displayName,
                                   List<String>        aliases,
                                   String              summary,
                                   String              description,
                                   String              examples,
                                   String              abbreviation,
                                   String              usage,
                                   String              publishVersionIdentifier,
                                   Map<String, String> additionalProperties,
                                   String              typeName,
                                   Map<String, Object> extendedProperties,
                                   Date                effectiveFrom,
                                   Date                effectiveTo,
                                   boolean             isMergeUpdate,
                                   boolean             forLineage,
                                   boolean             forDuplicateProcessing,
                                   Date                effectiveTime,
                                   String              methodName) throws InvalidParameterException,
                                                                          UserNotAuthorizedException,
                                                                          PropertyServerException
    {
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);
        if (!isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        String typeGUID = invalidParameterHandler.validateTypeName(typeName,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   serviceName,
                                                                   methodName,
                                                                   repositoryHelper);

        GlossaryTermBuilder builder = new GlossaryTermBuilder(qualifiedName,
                                                              displayName,
                                                              aliases,
                                                              summary,
                                                              description,
                                                              examples,
                                                              abbreviation,
                                                              usage,
                                                              publishVersionIdentifier,
                                                              additionalProperties,
                                                              extendedProperties,
                                                              InstanceStatus.ACTIVE,
                                                              repositoryHelper,
                                                              serviceName,
                                                              serverName);

        builder.setEffectivityDates(effectiveFrom, effectiveTo);

        this.updateBeanInRepository(userId,
                                    externalSourceGUID,
                                    externalSourceName,
                                    glossaryTermGUID,
                                    glossaryTermGUIDParameterName,
                                    typeGUID,
                                    typeName,
                                    forLineage,
                                    forDuplicateProcessing,
                                    supportedZones,
                                    builder.getInstanceProperties(methodName),
                                    isMergeUpdate,
                                    effectiveTime,
                                    methodName);
    }


    /**
     * Update the status of the metadata element representing a glossary term.  This is only valid on
     * a controlled glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the glossary term to update
     * @param glossaryTermGUIDParameterName parameter name for glossaryTermGUID
     * @param glossaryTermStatus new status value for the glossary term
     * @param glossaryTermStatusParameterName parameter name for the status value
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateGlossaryTermStatus(String         userId,
                                         String         externalSourceGUID,
                                         String         externalSourceName,
                                         String         glossaryTermGUID,
                                         String         glossaryTermGUIDParameterName,
                                         InstanceStatus glossaryTermStatus,
                                         String         glossaryTermStatusParameterName,
                                         Date           effectiveTime,
                                         boolean        forLineage,
                                         boolean        forDuplicateProcessing,
                                         String         methodName) throws InvalidParameterException,
                                                                           UserNotAuthorizedException,
                                                                           PropertyServerException
    {
        this.updateBeanStatusInRepository(userId,
                                          externalSourceGUID,
                                          externalSourceName,
                                          glossaryTermGUID,
                                          glossaryTermGUIDParameterName,
                                          OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                          OpenMetadataType.GLOSSARY_TERM.typeName,
                                          forLineage,
                                          forDuplicateProcessing,
                                          supportedZones,
                                          glossaryTermStatus,
                                          glossaryTermStatusParameterName,
                                          effectiveTime,
                                          methodName);
    }


    /**
     * Update the glossary term using the properties and classifications from the parentGUID stored in the request body.
     * This may be turned into a method for generic types in the future.  At the moment it does not traverse the associated anchored elements
     * and so only works for glossary workflow cases.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the glossary term to update
     * @param glossaryTermGUIDParameterName parameter name for glossaryTermGUID
     * @param templateGUID identifier for the new glossary
     * @param templateGUIDParameterName parameter name for the templateGUID value
     * @param isMergeClassifications should the classification be merged or replace the target entity?
     * @param isMergeProperties should the properties be merged with the existing ones or replace them
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param methodName calling method
     * @return term entity
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public EntityDetail updateGlossaryTermFromTemplate(String         userId,
                                                       String         externalSourceGUID,
                                                       String         externalSourceName,
                                                       String         glossaryTermGUID,
                                                       String         glossaryTermGUIDParameterName,
                                                       String         templateGUID,
                                                       String         templateGUIDParameterName,
                                                       boolean        isMergeClassifications,
                                                       boolean        isMergeProperties,
                                                       Date           effectiveTime,
                                                       boolean        forLineage,
                                                       boolean        forDuplicateProcessing,
                                                       String         methodName) throws InvalidParameterException,
                                                                                         UserNotAuthorizedException,
                                                                                         PropertyServerException
    {
        EntityDetail templateEntity = repositoryHandler.getEntityByGUID(userId,
                                                                        templateGUID,
                                                                        templateGUIDParameterName,
                                                                        OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                        forLineage,
                                                                        forDuplicateProcessing,
                                                                        effectiveTime,
                                                                        methodName);

        EntityDetail termEntity = repositoryHandler.getEntityByGUID(userId,
                                                                    glossaryTermGUID,
                                                                    glossaryTermGUIDParameterName,
                                                                    OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                    forLineage,
                                                                    forDuplicateProcessing,
                                                                    effectiveTime,
                                                                    methodName);

        if (templateEntity != null)
        {
            InstanceProperties templateProperties = null;
            if (templateEntity.getProperties() != null)
            {
                templateProperties = new InstanceProperties();

                templateProperties.setEffectiveFromTime(templateEntity.getProperties().getEffectiveFromTime());
                templateProperties.setEffectiveToTime(templateEntity.getProperties().getEffectiveToTime());

                if (templateEntity.getProperties().getPropertyCount() > 0)
                {
                    Iterator<String> propertyNames = templateEntity.getProperties().getPropertyNames();

                    while (propertyNames.hasNext())
                    {
                        String propertyName = propertyNames.next();

                        /*
                         * Ignore qualified name.
                         */
                        if (! OpenMetadataProperty.QUALIFIED_NAME.name.equals(propertyName))
                        {
                            templateProperties.setProperty(propertyName, templateEntity.getProperties().getPropertyValue(propertyName));
                        }
                    }
                }
            }

            this.updateBeanInRepository(userId,
                                        externalSourceGUID,
                                        externalSourceName,
                                        termEntity,
                                        glossaryTermGUIDParameterName,
                                        OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                        OpenMetadataType.GLOSSARY_TERM.typeName,
                                        forLineage,
                                        forDuplicateProcessing,
                                        supportedZones,
                                        templateProperties,
                                        isMergeProperties,
                                        effectiveTime,
                                        methodName);

            List<String> updatedClassifications = new ArrayList<>();

            if (templateEntity.getClassifications() != null)
            {
                for (Classification classification : templateEntity.getClassifications())
                {
                    if (! OpenMetadataType.ANCHORS_CLASSIFICATION.typeName.equals(classification.getName()))
                    {
                        this.setClassificationInRepository(userId,
                                                           externalSourceGUID,
                                                           externalSourceName,
                                                           termEntity,
                                                           glossaryTermGUIDParameterName,
                                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                                           classification.getType().getTypeDefGUID(),
                                                           classification.getType().getTypeDefName(),
                                                           classification.getProperties(),
                                                           isMergeProperties,
                                                           forLineage,
                                                           forDuplicateProcessing,
                                                           supportedZones,
                                                           effectiveTime,
                                                           methodName);

                        updatedClassifications.add(classification.getName());
                    }
                }
            }

            if ((! isMergeClassifications) && (termEntity.getClassifications() != null))
            {
                for (Classification classification : termEntity.getClassifications())
                {
                    if ((! OpenMetadataType.ANCHORS_CLASSIFICATION.typeName.equals(classification.getName())) &&
                                (! updatedClassifications.contains(classification.getName())))
                    {
                        this.removeClassificationFromRepository(userId,
                                                                externalSourceGUID,
                                                                externalSourceName,
                                                                termEntity.getGUID(),
                                                                glossaryTermGUIDParameterName,
                                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                classification.getType().getTypeDefGUID(),
                                                                classification.getType().getTypeDefName(),
                                                                forLineage,
                                                                forDuplicateProcessing,
                                                                supportedZones,
                                                                effectiveTime,
                                                                methodName);
                    }
                }
            }
        }

        return termEntity;
    }


    /**
     * Move a glossary term from one glossary to another.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the glossary term to update
     * @param glossaryTermGUIDParameterName parameter name for glossaryTermGUID
     * @param newGlossaryGUID identifier for the new glossary
     * @param newGlossaryGUIDParameterName parameter name for the newGlossaryGUID value
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void moveGlossaryTerm(String         userId,
                                 String         externalSourceGUID,
                                 String         externalSourceName,
                                 String         glossaryTermGUID,
                                 String         glossaryTermGUIDParameterName,
                                 String         newGlossaryGUID,
                                 String         newGlossaryGUIDParameterName,
                                 Date           effectiveTime,
                                 boolean        forLineage,
                                 boolean        forDuplicateProcessing,
                                 String         methodName) throws InvalidParameterException,
                                                                   UserNotAuthorizedException,
                                                                   PropertyServerException
    {
        EntityDetail termEntity = repositoryHandler.getEntityByGUID(userId,
                                                                    glossaryTermGUID,
                                                                    glossaryTermGUIDParameterName,
                                                                    OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                    forLineage,
                                                                    forDuplicateProcessing,
                                                                    effectiveTime,
                                                                    methodName);

        List<Relationship> termAnchors = this.getAttachmentLinks(userId,
                                                                 termEntity.getGUID(),
                                                                 glossaryTermGUIDParameterName,
                                                                 OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                 OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeGUID,
                                                                 OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeName,
                                                                 null,
                                                                 OpenMetadataType.GLOSSARY.typeName,
                                                                 1,
                                                                 null,
                                                                 null,
                                                                 SequencingOrder.CREATION_DATE_RECENT,
                                                                 null,
                                                                 forLineage,
                                                                 forDuplicateProcessing,
                                                                 0,
                                                                 0,
                                                                 effectiveTime,
                                                                 methodName);

        /*
         * Remove the current term anchor (should only be one but this cleans up any extra relationships).
         */
        if (termAnchors != null)
        {
            for (Relationship termAnchor : termAnchors)
            {
                if (termAnchor != null)
                {
                    repositoryHandler.removeRelationship(userId, externalSourceGUID, externalSourceName, termAnchor, methodName);
                }
            }
        }

        /*
         * Remove the original anchor.
         */
        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                termEntity.getGUID(),
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.ANCHORS_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.ANCHORS_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);

        // todo check that the anchor classification is correctly reestablished by linkElementToElement.
        // in particular, the anchorScopeGUID must be set up to the new glossary.

        /*
         * This will set up the correct anchor
         */
        this.linkElementToElement(userId,
                                  externalSourceGUID,
                                  externalSourceName,
                                  newGlossaryGUID,
                                  newGlossaryGUIDParameterName,
                                  OpenMetadataType.GLOSSARY.typeName,
                                  termEntity.getGUID(),
                                  glossaryTermGUIDParameterName,
                                  OpenMetadataType.GLOSSARY_TERM.typeName,
                                  forLineage,
                                  forDuplicateProcessing,
                                  supportedZones,
                                  OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeGUID,
                                  OpenMetadataType.TERM_ANCHOR_RELATIONSHIP.typeName,
                                  null,
                                  null,
                                  null,
                                  effectiveTime,
                                  methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes an abstract concept.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsAbstractConcept(String  userId,
                                         String  externalSourceGUID,
                                         String  externalSourceName,
                                         String  glossaryTermGUID,
                                         String  glossaryTermGUIDParameterName,
                                         Date    effectiveFrom,
                                         Date    effectiveTo,
                                         boolean isMergeUpdate,
                                         boolean forLineage,
                                         boolean forDuplicateProcessing,
                                         Date    effectiveTime,
                                         String  methodName) throws InvalidParameterException,
                                                                    UserNotAuthorizedException,
                                                                    PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.ABSTRACT_CONCEPT_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.ABSTRACT_CONCEPT_CLASSIFICATION.typeName,
                                           this.setUpEffectiveDates(null, effectiveFrom, effectiveTo),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the abstract concept designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsAbstractConcept(String  userId,
                                           String  externalSourceGUID,
                                           String  externalSourceName,
                                           String  glossaryTermGUID,
                                           String  glossaryTermGUIDParameterName,
                                           boolean forLineage,
                                           boolean forDuplicateProcessing,
                                           Date    effectiveTime,
                                           String  methodName) throws InvalidParameterException,
                                                                      UserNotAuthorizedException,
                                                                      PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.ABSTRACT_CONCEPT_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.ABSTRACT_CONCEPT_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes a data value.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsDataValue(String  userId,
                                   String  externalSourceGUID,
                                   String  externalSourceName,
                                   String  glossaryTermGUID,
                                   String  glossaryTermGUIDParameterName,
                                   Date    effectiveFrom,
                                   Date    effectiveTo,
                                   boolean isMergeUpdate,
                                   boolean forLineage,
                                   boolean forDuplicateProcessing,
                                   Date    effectiveTime,
                                   String  methodName) throws InvalidParameterException,
                                                              UserNotAuthorizedException,
                                                              PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.DATA_VALUE_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.DATA_VALUE_CLASSIFICATION.typeName,
                                           this.setUpEffectiveDates(null, effectiveFrom, effectiveTo),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the data value designation from the glossary term.
     *
     * @param userId calling user
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsDataValue(String  userId,
                                     String  externalSourceGUID,
                                     String  externalSourceName,
                                     String  glossaryTermGUID,
                                     String  glossaryTermGUIDParameterName,
                                     boolean forLineage,
                                     boolean forDuplicateProcessing,
                                     Date    effectiveTime,
                                     String  methodName) throws InvalidParameterException,
                                                                UserNotAuthorizedException,
                                                                PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.DATA_VALUE_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.DATA_VALUE_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes a data value.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param activityType ordinal for type of activity
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsActivity(String  userId,
                                  String  externalSourceGUID,
                                  String  externalSourceName,
                                  String  glossaryTermGUID,
                                  String  glossaryTermGUIDParameterName,
                                  int     activityType,
                                  Date    effectiveFrom,
                                  Date    effectiveTo,
                                  boolean isMergeUpdate,
                                  boolean forLineage,
                                  boolean forDuplicateProcessing,
                                  Date    effectiveTime,
                                  String  methodName) throws InvalidParameterException,
                                                             UserNotAuthorizedException,
                                                             PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        GlossaryTermBuilder builder = new GlossaryTermBuilder(repositoryHelper, serviceName, serverName);

        builder.setEffectivityDates(effectiveFrom, effectiveTo);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.ACTIVITY_DESCRIPTION_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.ACTIVITY_DESCRIPTION_CLASSIFICATION.typeName,
                                           builder.getActivityTypeProperties(activityType, methodName),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the activity designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsActivity(String  userId,
                                    String  externalSourceGUID,
                                    String  externalSourceName,
                                    String  glossaryTermGUID,
                                    String  glossaryTermGUIDParameterName,
                                    boolean forLineage,
                                    boolean forDuplicateProcessing,
                                    Date    effectiveTime,
                                    String  methodName) throws InvalidParameterException,
                                                               UserNotAuthorizedException,
                                                               PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.ACTIVITY_DESCRIPTION_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.ACTIVITY_DESCRIPTION_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes a context.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param description description of the context
     * @param scope the scope of where the context applies
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsContext(String  userId,
                                 String  externalSourceGUID,
                                 String  externalSourceName,
                                 String  glossaryTermGUID,
                                 String  glossaryTermGUIDParameterName,
                                 String  description,
                                 String  scope,
                                 Date    effectiveFrom,
                                 Date    effectiveTo,
                                 boolean isMergeUpdate,
                                 boolean forLineage,
                                 boolean forDuplicateProcessing,
                                 Date    effectiveTime,
                                 String  methodName) throws InvalidParameterException,
                                                            UserNotAuthorizedException,
                                                            PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        GlossaryTermBuilder builder = new GlossaryTermBuilder(repositoryHelper, serviceName, serverName);

        builder.setEffectivityDates(effectiveFrom, effectiveTo);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.CONTEXT_DEFINITION_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.CONTEXT_DEFINITION_CLASSIFICATION.typeName,
                                           builder.getContextDescriptionProperties(description, scope, methodName),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the context definition designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsContext(String  userId,
                                   String  externalSourceGUID,
                                   String  externalSourceName,
                                   String  glossaryTermGUID,
                                   String  glossaryTermGUIDParameterName,
                                   boolean forLineage,
                                   boolean forDuplicateProcessing,
                                   Date    effectiveTime,
                                   String  methodName) throws InvalidParameterException,
                                                              UserNotAuthorizedException,
                                                              PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.CONTEXT_DEFINITION_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.CONTEXT_DEFINITION_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes a spine object.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsSpineObject(String  userId,
                                     String  externalSourceGUID,
                                     String  externalSourceName,
                                     String  glossaryTermGUID,
                                     String  glossaryTermGUIDParameterName,
                                     Date    effectiveFrom,
                                     Date    effectiveTo,
                                     boolean isMergeUpdate,
                                     boolean forLineage,
                                     boolean forDuplicateProcessing,
                                     Date    effectiveTime,
                                     String  methodName) throws InvalidParameterException,
                                                                UserNotAuthorizedException,
                                                                PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.SPINE_OBJECT_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.SPINE_OBJECT_CLASSIFICATION.typeName,
                                           this.setUpEffectiveDates(null, effectiveFrom, effectiveTo),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the spine object designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsSpineObject(String  userId,
                                       String  externalSourceGUID,
                                       String  externalSourceName,
                                       String  glossaryTermGUID,
                                       String  glossaryTermGUIDParameterName,
                                       boolean forLineage,
                                       boolean forDuplicateProcessing,
                                       Date    effectiveTime,
                                       String  methodName) throws InvalidParameterException,
                                                                  UserNotAuthorizedException,
                                                                  PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.SPINE_OBJECT_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.SPINE_OBJECT_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes a spine attribute.
     *
     * @param userId calling user
     * @param externalSourceGUID guid of the software capability entity that represented the external source - null for local
     * @param externalSourceName name of the software capability entity that represented the external source
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsSpineAttribute(String  userId,
                                        String  externalSourceGUID,
                                        String  externalSourceName,
                                        String  glossaryTermGUID,
                                        String  glossaryTermGUIDParameterName,
                                        Date    effectiveFrom,
                                        Date    effectiveTo,
                                        boolean isMergeUpdate,
                                        boolean forLineage,
                                        boolean forDuplicateProcessing,
                                        Date    effectiveTime,
                                        String  methodName) throws InvalidParameterException,
                                                                   UserNotAuthorizedException,
                                                                   PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.SPINE_ATTRIBUTE_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.SPINE_ATTRIBUTE_CLASSIFICATION.typeName,
                                           this.setUpEffectiveDates(null, effectiveFrom, effectiveTo),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the spine attribute designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsSpineAttribute(String  userId,
                                          String  externalSourceGUID,
                                          String  externalSourceName,
                                          String  glossaryTermGUID,
                                          String  glossaryTermGUIDParameterName,
                                          boolean forLineage,
                                          boolean forDuplicateProcessing,
                                          Date    effectiveTime,
                                          String  methodName) throws InvalidParameterException,
                                                                     UserNotAuthorizedException,
                                                                     PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.SPINE_ATTRIBUTE_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.SPINE_ATTRIBUTE_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term to indicate that it describes an object identifier.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param effectiveFrom  the time that the element must be effective from (null for any time, new Date() for now)
     * @param effectiveTo  the time that the must be effective to (null for any time, new Date() for now)
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setTermAsObjectIdentifier(String  userId,
                                          String  externalSourceGUID,
                                          String  externalSourceName,
                                          String  glossaryTermGUID,
                                          String  glossaryTermGUIDParameterName,
                                          Date    effectiveFrom,
                                          Date    effectiveTo,
                                          boolean isMergeUpdate,
                                          boolean forLineage,
                                          boolean forDuplicateProcessing,
                                          Date    effectiveTime,
                                          String  methodName) throws InvalidParameterException,
                                                                     UserNotAuthorizedException,
                                                                     PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.setClassificationInRepository(userId,
                                           externalSourceGUID,
                                           externalSourceName,
                                           glossaryTermGUID,
                                           glossaryTermGUIDParameterName,
                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                           OpenMetadataType.OBJECT_IDENTIFIER_CLASSIFICATION.typeGUID,
                                           OpenMetadataType.OBJECT_IDENTIFIER_CLASSIFICATION.typeName,
                                           this.setUpEffectiveDates(null, effectiveFrom, effectiveTo),
                                           isMergeUpdate,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Remove the object identifier designation from the glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void clearTermAsObjectIdentifier(String  userId,
                                            String  externalSourceGUID,
                                            String  externalSourceName,
                                            String  glossaryTermGUID,
                                            String  glossaryTermGUIDParameterName,
                                            boolean forLineage,
                                            boolean forDuplicateProcessing,
                                            Date    effectiveTime,
                                            String  methodName) throws InvalidParameterException,
                                                                       UserNotAuthorizedException,
                                                                       PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(glossaryTermGUID, glossaryTermGUIDParameterName, methodName);

        this.removeClassificationFromRepository(userId,
                                                externalSourceGUID,
                                                externalSourceName,
                                                glossaryTermGUID,
                                                glossaryTermGUIDParameterName,
                                                OpenMetadataType.GLOSSARY_TERM.typeName,
                                                OpenMetadataType.OBJECT_IDENTIFIER_CLASSIFICATION.typeGUID,
                                                OpenMetadataType.OBJECT_IDENTIFIER_CLASSIFICATION.typeName,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Classify the glossary term in the repository to show that it has been archived and is only needed for lineage.
     *
     * @param userId calling user
     * @param assetManagerGUID unique identifier of software server capability representing the caller
     * @param assetManagerName unique name of software server capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param archiveDate date that the file was archived or discovered to have been archived.  Null means now.
     * @param archiveProcess name of archiving process
     * @param archiveProperties properties to help locate the archive copy
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException one of the parameters is null or invalid.
     * @throws PropertyServerException there is a problem removing the properties from the repositories.
     * @throws UserNotAuthorizedException the requesting user is not authorized to issue this request.
     */
    public void archiveGlossaryTerm(String              userId,
                                    String              assetManagerGUID,
                                    String              assetManagerName,
                                    String              glossaryTermGUID,
                                    String              glossaryTermGUIDParameterName,
                                    Date                archiveDate,
                                    String              archiveProcess,
                                    Map<String, String> archiveProperties,
                                    boolean             forDuplicateProcessing,
                                    Date                effectiveTime,
                                    String              methodName) throws InvalidParameterException,
                                                                           PropertyServerException,
                                                                           UserNotAuthorizedException
    {
        EntityDetail entity = this.getEntityFromRepository(userId,
                                                           glossaryTermGUID,
                                                           glossaryTermGUIDParameterName,
                                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                                           null,
                                                           null,
                                                           true,
                                                           forDuplicateProcessing,
                                                           effectiveTime,
                                                           methodName);

        ReferenceableBuilder builder = new ReferenceableBuilder(repositoryHelper, serviceName, serverName);

        repositoryHandler.classifyEntity(userId,
                                         assetManagerGUID,
                                         assetManagerName,
                                         entity.getGUID(),
                                         entity,
                                         glossaryTermGUIDParameterName,
                                         OpenMetadataType.GLOSSARY_TERM.typeName,
                                         OpenMetadataType.MEMENTO_CLASSIFICATION.typeGUID,
                                         OpenMetadataType.MEMENTO_CLASSIFICATION.typeName,
                                         ClassificationOrigin.ASSIGNED,
                                         entity.getGUID(),
                                         builder.getMementoProperties(archiveDate,
                                                                      userId,
                                                                      archiveProcess,
                                                                      archiveProperties,
                                                                      methodName),
                                         true,
                                         forDuplicateProcessing,
                                         effectiveTime,
                                         methodName);
    }


    /**
     * Remove the metadata element representing a glossary term.
     *
     * @param userId calling user
     * @param externalSourceGUID     unique identifier of software capability representing the caller
     * @param externalSourceName     unique name of software capability representing the caller
     * @param glossaryTermGUID unique identifier of the metadata element to update
     * @param glossaryTermGUIDParameterName parameter supplying glossaryTermGUID
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeGlossaryTerm(String  userId,
                                   String  externalSourceGUID,
                                   String  externalSourceName,
                                   String  glossaryTermGUID,
                                   String  glossaryTermGUIDParameterName,
                                   boolean forLineage,
                                   boolean forDuplicateProcessing,
                                   Date    effectiveTime,
                                   String  methodName) throws InvalidParameterException,
                                                              UserNotAuthorizedException,
                                                              PropertyServerException
    {
        this.deleteBeanInRepository(userId,
                                    externalSourceGUID,
                                    externalSourceName,
                                    glossaryTermGUID,
                                    glossaryTermGUIDParameterName,
                                    OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                    OpenMetadataType.GLOSSARY_TERM.typeName,
                                    false,
                                    null,
                                    null,
                                    forLineage,
                                    forDuplicateProcessing,
                                    effectiveTime,
                                    methodName);
    }


    /**
     * Returns the glossary term object corresponding to the supplied term name.
     *
     * @param userId  String - userId of user making request.
     * @param glossaryGUID unique identifier of the glossary to query
     * @param name  this may be the qualifiedName or displayName of the term.
     * @param limitResultsByStatus By default, terms in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all status values.
     * @param startFrom  index of the list to start from (0 for start)
     * @param pageSize   maximum number of elements to return.
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return List of glossary terms retrieved from property server
     * @throws InvalidParameterException one of the parameters is null or invalid.
     * @throws PropertyServerException there is a problem retrieving information from the property (metadata) server.
     * @throws UserNotAuthorizedException the requesting user is not authorized to issue this request.
     */
    public List<B> getTermsByName(String               userId,
                                  String               glossaryGUID,
                                  String               name,
                                  List<InstanceStatus> limitResultsByStatus,
                                  int                  startFrom,
                                  int                  pageSize,
                                  boolean              forLineage,
                                  boolean              forDuplicateProcessing,
                                  Date                 effectiveTime,
                                  String               methodName) throws InvalidParameterException,
                                                                          PropertyServerException,
                                                                          UserNotAuthorizedException
    {
        invalidParameterHandler.validateUserId(userId, methodName);

        int queryPageSize = invalidParameterHandler.validatePaging(startFrom, pageSize, methodName);

        InstanceProperties matchProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                          null,
                                                                                          OpenMetadataProperty.QUALIFIED_NAME.name,
                                                                                          name,
                                                                                          methodName);
        matchProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                       matchProperties,
                                                                       OpenMetadataProperty.DISPLAY_NAME.name,
                                                                       name,
                                                                       methodName);

        matchProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                       matchProperties,
                                                                       OpenMetadataProperty.ALIASES.name,
                                                                       name,
                                                                       methodName);

        List<EntityDetail> termEntities = repositoryHandler.getEntitiesByName(userId,
                                                                              matchProperties,
                                                                              OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                                                              limitResultsByStatus,
                                                                              null,
                                                                              null,
                                                                              SequencingOrder.CREATION_DATE_RECENT,
                                                                              null,
                                                                              forLineage,
                                                                              forDuplicateProcessing,
                                                                              startFrom,
                                                                              queryPageSize,
                                                                              effectiveTime,
                                                                              methodName);

        return getValidTerms(userId,
                             glossaryGUID,
                             effectiveTime,
                             forLineage,
                             forDuplicateProcessing,
                             methodName,
                             termEntities);
    }


    /**
     * Return the valid category beans retrieved from the repository.
     *
     * @param userId calling user
     * @param glossaryGUID unique identifier of the glossary to query
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param methodName calling method
     * @param termEntities entities retrieved from the repository
     * @return list of beans
     */
    private List<B> getValidTerms(String             userId,
                                  String             glossaryGUID,
                                  Date               effectiveTime,
                                  boolean            forLineage,
                                  boolean            forDuplicateProcessing,
                                  String             methodName,
                                  List<EntityDetail> termEntities)
    {
        if (termEntities != null)
        {
            List<EntityDetail> validatedTerms = super.validateEntitiesAndAnchorsForRead(userId,
                                                                                        termEntities,
                                                                                        forLineage,
                                                                                        forDuplicateProcessing,
                                                                                        supportedZones,
                                                                                        effectiveTime,
                                                                                        methodName);
            List<B> results = new ArrayList<>();

            for (EntityDetail entity : validatedTerms)
            {
                if (entity != null)
                {
                    try
                    {
                        if (glossaryGUID == null)
                        {
                            results.add(this.getGlossaryTermBean(userId, entity, null, forLineage, forDuplicateProcessing, effectiveTime));
                        }
                        else
                        {
                            AnchorIdentifiers anchorIdentifiers = this.getAnchorsFromAnchorsClassification(entity, methodName);

                            if (glossaryGUID.equals(anchorIdentifiers.anchorScopeGUID))
                            {
                                results.add(this.getGlossaryTermBean(userId, entity, null, forLineage, forDuplicateProcessing, effectiveTime));
                            }
                        }
                    }
                    catch (Exception notVisible)
                    {
                        // ignore entity
                    }
                }
            }

            if (!results.isEmpty())
            {
                return results;
            }
        }

        return null;
    }


    /**
     * Returns the glossary term object containing the supplied term name.  This may include wildcard characters
     *
     * @param userId  String - userId of user making request.
     * @param glossaryGUID unique identifier of the glossary to query
     * @param searchString string to find in the properties
     * @param limitResultsByStatus By default, terms in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all status values.
     * @param startFrom  index of the list to start from (0 for start)
     * @param pageSize   maximum number of elements to return.
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return List of glossary terms retrieved from property server
     * @throws InvalidParameterException one of the parameters is null or invalid.
     * @throws PropertyServerException there is a problem retrieving information from the property (metadata) server.
     * @throws UserNotAuthorizedException the requesting user is not authorized to issue this request.
     */
    public List<B> findTerms(String               userId,
                             String               glossaryGUID,
                             String               searchString,
                             List<InstanceStatus> limitResultsByStatus,
                             int                  startFrom,
                             int                  pageSize,
                             boolean              forLineage,
                             boolean              forDuplicateProcessing,
                             Date                 effectiveTime,
                             String               methodName) throws InvalidParameterException,
                                                                     PropertyServerException,
                                                                     UserNotAuthorizedException
    {
        int queryPageSize = invalidParameterHandler.validatePaging(startFrom, pageSize, methodName);

        List<EntityDetail> termEntities = repositoryHandler.getEntitiesByValue(userId,
                                                                               searchString,
                                                                               OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                                                               limitResultsByStatus,
                                                                               null,
                                                                               null,
                                                                               SequencingOrder.CREATION_DATE_RECENT,
                                                                               null,
                                                                               forLineage,
                                                                               forDuplicateProcessing,
                                                                               startFrom,
                                                                               queryPageSize,
                                                                               effectiveTime,
                                                                               methodName);

        return getValidTerms(userId,
                             glossaryGUID,
                             effectiveTime,
                             forLineage,
                             forDuplicateProcessing,
                             methodName,
                             termEntities);
    }


    /**
     * Returns the glossary term object corresponding to the supplied glossary term GUID.
     *
     * @param userId  String - userId of user making request
     * @param guid  the unique id for the glossary term within the property server
     * @param guidParameter name of parameter supplying the guid
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return Glossary Term retrieved from the property server
     * @throws InvalidParameterException one of the parameters is null or invalid.
     * @throws PropertyServerException there is a problem retrieving information from the property (metadata) server.
     * @throws UserNotAuthorizedException the requesting user is not authorized to issue this request.
     */
    public B getTerm(String    userId,
                     String    guid,
                     String    guidParameter,
                     boolean   forLineage,
                     boolean   forDuplicateProcessing,
                     Date      effectiveTime,
                     String    methodName) throws InvalidParameterException,
                                                  PropertyServerException,
                                                  UserNotAuthorizedException
    {
        EntityDetail entity = this.getEntityFromRepository(userId,
                                                           guid,
                                                           guidParameter,
                                                           OpenMetadataType.GLOSSARY_TERM.typeName,
                                                           null,
                                                           null,
                                                           forLineage,
                                                           forDuplicateProcessing,
                                                           effectiveTime,
                                                           methodName);

        return this.getGlossaryTermBean(userId, entity, null, forLineage, forDuplicateProcessing, effectiveTime);
    }


    /**
     * Retrieve the list of glossary terms associated with a glossary.
     *
     * @param userId calling user
     * @param glossaryGUID unique identifier of the glossary of interest
     * @param glossaryGUIDParameterName property supplying the glossaryGUID
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of associated metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<B>  getTermsForGlossary(String               userId,
                                        String               glossaryGUID,
                                        String               glossaryGUIDParameterName,
                                        int                  startFrom,
                                        int                  pageSize,
                                        boolean              forLineage,
                                        boolean              forDuplicateProcessing,
                                        Date                 effectiveTime,
                                        String               methodName) throws InvalidParameterException,
                                                                                UserNotAuthorizedException,
                                                                                PropertyServerException
    {
        invalidParameterHandler.validateGUID(glossaryGUID, glossaryGUIDParameterName, methodName);
        int queryPageSize = invalidParameterHandler.validatePaging(startFrom, pageSize, methodName);

        EntityDetail glossaryEntity = repositoryHandler.getEntityByGUID(userId,
                                                                        glossaryGUID,
                                                                        glossaryGUIDParameterName,
                                                                        OpenMetadataType.GLOSSARY.typeName,
                                                                        forLineage,
                                                                        forDuplicateProcessing,
                                                                        effectiveTime,
                                                                        methodName);

        securityVerifier.validateUserForElementRead(userId,
                                                    glossaryEntity,
                                                    repositoryHelper,
                                                    serviceName,
                                                    methodName);

        InstanceProperties matchProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                          null,
                                                                                          OpenMetadataProperty.ANCHOR_SCOPE_GUID.name,
                                                                                          glossaryGUID,
                                                                                          methodName);

        /*
         * Notice that we use a search rather than a navigation via the TermAnchor relationship for speed.
         */
        List<EntityDetail> termEntities = repositoryHandler.getEntitiesForClassificationType(userId,
                                                                                             OpenMetadataType.GLOSSARY_TERM.typeGUID,
                                                                                             OpenMetadataType.ANCHORS_CLASSIFICATION.typeName,
                                                                                             matchProperties,
                                                                                             MatchCriteria.ALL,
                                                                                             null,
                                                                                             null,
                                                                                             SequencingOrder.CREATION_DATE_RECENT,
                                                                                             null,
                                                                                             forLineage,
                                                                                             forDuplicateProcessing,
                                                                                             startFrom,
                                                                                             queryPageSize,
                                                                                             effectiveTime,
                                                                                             methodName);

        return this.getGlossaryTermBeans(userId, termEntities, forLineage, forDuplicateProcessing, effectiveTime);
    }


    /**
     * Convert the returned glossary term entities into a list of glossary term elements.
     *
     * @param userId calling user
     * @param termEntities list of entities returned
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @return list of glossary term elements
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    private List<B> getGlossaryTermBeans(String             userId,
                                         List<EntityDetail> termEntities,
                                         boolean            forLineage,
                                         boolean            forDuplicateProcessing,
                                         Date               effectiveTime) throws InvalidParameterException,
                                                                                  UserNotAuthorizedException,
                                                                                  PropertyServerException
    {
        if (termEntities != null)
        {
            List<B> results = new ArrayList<>();

            for (EntityDetail termEntity : termEntities)
            {
                results.add(getGlossaryTermBean(userId,
                                                    termEntity,
                                                    null,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime));
            }

            return results;
        }

        return null;
    }


    /**
     * Convert the returned glossary term entities into a list of glossary term elements.
     *
     * @param userId calling user
     * @param relatedTerms list of entities returned
     * @param limitResultsByStatus By default, term relationships in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all status values.
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @return list of glossary term elements
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    private List<B> getGlossaryTermBeans(String              userId,
                                         List<RelatedEntity> relatedTerms,
                                         List<Integer>       limitResultsByStatus,
                                         boolean             forLineage,
                                         boolean             forDuplicateProcessing,
                                         Date                effectiveTime) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        final String methodName = "getGlossaryTermBeans";

        if (relatedTerms != null)
        {
            List<B> results = new ArrayList<>();

            for (RelatedEntity relatedTerm : relatedTerms)
            {
                if ((relatedTerm != null) && (relatedTerm.relationship() != null) && (relatedTerm.entityDetail() != null))
                {
                    if ((limitResultsByStatus != null) && (!limitResultsByStatus.isEmpty()))
                    {
                        Integer relationshipOrdinal = repositoryHelper.getEnumPropertyOrdinal(serviceName,
                                                                                              OpenMetadataProperty.TERM_RELATIONSHIP_STATUS.name,
                                                                                              relatedTerm.relationship().getProperties(),
                                                                                              methodName);

                        if (limitResultsByStatus.contains(relationshipOrdinal))
                        {
                            results.add(this.getGlossaryTermBean(userId, relatedTerm.entityDetail(), relatedTerm.relationship(), forLineage, forDuplicateProcessing, effectiveTime));
                        }
                    }
                    else
                    {
                        results.add(this.getGlossaryTermBean(userId, relatedTerm.entityDetail(), relatedTerm.relationship(), forLineage, forDuplicateProcessing, effectiveTime));
                    }
                }
            }

            return results;
        }

        return null;
    }


    /**
     * Convert the returned glossary term entity into an element.
     *
     * @param userId calling user
     * @param termEntity entity returned
     * @param relatedByRelationship optional relationship used to find entity
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @return list of glossary term elements
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    private B getGlossaryTermBean(String       userId,
                                  EntityDetail termEntity,
                                  Relationship relatedByRelationship,
                                  boolean      forLineage,
                                  boolean      forDuplicateProcessing,
                                  Date         effectiveTime) throws InvalidParameterException,
                                                                     UserNotAuthorizedException,
                                                                     PropertyServerException
    {
        final String methodName = "getGlossaryTermBeans";
        final String startingGUIDParameterName = "termEntity.guid";

        if (termEntity != null)
        {
            List<RelatedEntity> relatedEntities = this.getAllRelatedEntities(userId,
                                                                             termEntity,
                                                                             startingGUIDParameterName,
                                                                             OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                             null,
                                                                             null,
                                                                             SequencingOrder.CREATION_DATE_RECENT,
                                                                             null,
                                                                             forLineage,
                                                                             forDuplicateProcessing,
                                                                             effectiveTime,
                                                                             methodName);

            return converter.getNewComplexBean(beanClass, termEntity, relatedByRelationship, relatedEntities, methodName);
        }

        return null;
    }


    /**
     * Retrieve the list of glossary terms associated with a glossary category.
     *
     * @param userId calling user
     * @param glossaryCategoryGUID unique identifier of the glossary category of interest
     * @param glossaryCategoryGUIDParameterName property supplying the glossaryCategoryGUID
     * @param limitResultsByStatus By default, term relationships in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all status values.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of associated metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<B>    getTermsForGlossaryCategory(String        userId,
                                                  String        glossaryCategoryGUID,
                                                  String        glossaryCategoryGUIDParameterName,
                                                  List<Integer> limitResultsByStatus,
                                                  int           startFrom,
                                                  int           pageSize,
                                                  boolean       forLineage,
                                                  boolean       forDuplicateProcessing,
                                                  Date          effectiveTime,
                                                  String        methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        EntityDetail categoryEntity = this.getEntityFromRepository(userId,
                                                                   glossaryCategoryGUID,
                                                                   glossaryCategoryGUIDParameterName,
                                                                   OpenMetadataType.GLOSSARY_CATEGORY.typeName,
                                                                   null,
                                                                   null,
                                                                   forLineage,
                                                                   forDuplicateProcessing,
                                                                   effectiveTime,
                                                                   methodName);

        List<RelatedEntity> relatedTerms = this.getRelatedEntities(userId,
                                                                   categoryEntity,
                                                                   glossaryCategoryGUIDParameterName,
                                                                   OpenMetadataType.GLOSSARY_CATEGORY.typeName,
                                                                   OpenMetadataType.TERM_CATEGORIZATION.typeGUID,
                                                                   OpenMetadataType.TERM_CATEGORIZATION.typeName,
                                                                   null,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   2,
                                                                   null,
                                                                   null,
                                                                   SequencingOrder.CREATION_DATE_RECENT,
                                                                   null,
                                                                   forLineage,
                                                                   forDuplicateProcessing,
                                                                   supportedZones,
                                                                   startFrom,
                                                                   pageSize,
                                                                   effectiveTime,
                                                                   methodName);

        return this.getGlossaryTermBeans(userId, relatedTerms, limitResultsByStatus, forLineage, forDuplicateProcessing, effectiveTime);
    }


    /**
     * Retrieve the list of glossary terms associated with a glossary term.
     *
     * @param userId calling user
     * @param glossaryTermGUID unique identifier of the glossary of interest
     * @param glossaryTermGUIDParameterName property supplying the glossaryTermGUID
     * @param relationshipTypeName optional name of relationship
     * @param limitResultsByStatus By default, term relationships in all statuses are returned.  However, it is possible
     *                             to specify a list of statuses (eg ACTIVE) to restrict the results to.  Null means all status values.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of associated metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<B>  getRelatedTerms(String        userId,
                                    String        glossaryTermGUID,
                                    String        glossaryTermGUIDParameterName,
                                    String        relationshipTypeName,
                                    List<Integer> limitResultsByStatus,
                                    int           startFrom,
                                    int           pageSize,
                                    boolean       forLineage,
                                    boolean       forDuplicateProcessing,
                                    Date          effectiveTime,
                                    String        methodName) throws InvalidParameterException,
                                                                     UserNotAuthorizedException,
                                                                     PropertyServerException
    {
        String relationshipTypeGUID = null;

        if (relationshipTypeName != null)
        {
            relationshipTypeGUID = invalidParameterHandler.validateTypeName(relationshipTypeName,
                                                                            relationshipTypeName,
                                                                            serviceName,
                                                                            methodName,
                                                                            repositoryHelper);
        }

        EntityDetail startingTermEntity = this.getEntityFromRepository(userId,
                                                                       glossaryTermGUID,
                                                                       glossaryTermGUIDParameterName,
                                                                       OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                       null,
                                                                       null,
                                                                       forLineage,
                                                                       forDuplicateProcessing,
                                                                       effectiveTime,
                                                                       methodName);

        List<RelatedEntity> relatedTerms = this.getRelatedEntities(userId,
                                                                   startingTermEntity,
                                                                   glossaryTermGUIDParameterName,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   relationshipTypeGUID,
                                                                   relationshipTypeName,
                                                                   null,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   0,
                                                                   null,
                                                                   null,
                                                                   SequencingOrder.CREATION_DATE_RECENT,
                                                                   null,
                                                                   forLineage,
                                                                   forDuplicateProcessing,
                                                                   supportedZones,
                                                                   startFrom,
                                                                   pageSize,
                                                                   effectiveTime,
                                                                   methodName);

        return this.getGlossaryTermBeans(userId, relatedTerms, limitResultsByStatus, forLineage, forDuplicateProcessing, effectiveTime);
    }


    /**
     * Return the glossary terms attached to a supplied entity through the semantic assignment.
     *
     * @param userId     calling user
     * @param elementGUID identifier for the entity that the feedback is attached to
     * @param elementGUIDParameterName name of parameter supplying the GUID
     * @param elementTypeName name of the type of object being attached to
     * @param serviceSupportedZones supported zones for calling service
     * @param startingFrom where to start from in the list
     * @param pageSize maximum number of results that can be returned
     * @param forLineage the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     * @return list of objects or null if none found
     * @throws InvalidParameterException  the input properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException    problem accessing the property server
     */
    public List<B>  getAttachedMeanings(String       userId,
                                        String       elementGUID,
                                        String       elementGUIDParameterName,
                                        String       elementTypeName,
                                        List<String> serviceSupportedZones,
                                        int          startingFrom,
                                        int          pageSize,
                                        boolean      forLineage,
                                        boolean      forDuplicateProcessing,
                                        Date         effectiveTime,
                                        String       methodName) throws InvalidParameterException,
                                                                        PropertyServerException,
                                                                        UserNotAuthorizedException
    {
        EntityDetail startingEntity = this.getEntityFromRepository(userId,
                                                                       elementGUID,
                                                                       elementGUIDParameterName,
                                                                       elementTypeName,
                                                                       null,
                                                                       null,
                                                                       forLineage,
                                                                       forDuplicateProcessing,
                                                                       effectiveTime,
                                                                       methodName);

        List<RelatedEntity> relatedTerms = this.getRelatedEntities(userId,
                                                                   startingEntity,
                                                                   elementGUIDParameterName,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   OpenMetadataType.SEMANTIC_ASSIGNMENT_RELATIONSHIP.typeGUID,
                                                                   OpenMetadataType.SEMANTIC_ASSIGNMENT_RELATIONSHIP.typeName,
                                                                   null,
                                                                   OpenMetadataType.GLOSSARY_TERM.typeName,
                                                                   0,
                                                                   null,
                                                                   null,
                                                                   SequencingOrder.CREATION_DATE_RECENT,
                                                                   null,
                                                                   forLineage,
                                                                   forDuplicateProcessing,
                                                                   serviceSupportedZones,
                                                                   startingFrom,
                                                                   pageSize,
                                                                   effectiveTime,
                                                                   methodName);

        return this.getGlossaryTermBeans(userId, relatedTerms, null, forLineage, forDuplicateProcessing, effectiveTime);
    }
}
