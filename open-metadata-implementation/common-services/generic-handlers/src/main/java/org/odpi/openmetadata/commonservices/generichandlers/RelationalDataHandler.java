/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.commonservices.generichandlers;

import org.odpi.openmetadata.frameworks.openmetadata.enums.KeyPattern;
import org.odpi.openmetadata.frameworks.openmetadata.enums.ServerAssetUseType;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataProperty;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryErrorHandler;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.metadatasecurity.server.OpenMetadataServerSecurityVerifier;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.ClassificationErrorException;
import org.odpi.openmetadata.repositoryservices.ffdc.exception.TypeErrorException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RelationalDataHandler manages the assets and schemas for relational data.  It is build on the AssetHandler
 * and the SchemaAttributeHandler.  Its role is to maintain a simple interface that covers databases,
 * database schemas, database tables and columns.  It automatically creates and links in the schema type
 * information in the structure and keeps it linked together.
 */
public class RelationalDataHandler<DATABASE,
                                   DATABASE_SCHEMA,
                                   DATABASE_TABLE,
                                   DATABASE_VIEW,
                                   DATABASE_COLUMN,
                                   SCHEMA_TYPE>

{
    private final String                                               serviceName;
    private final String                                               serverName;
    private final OMRSRepositoryHelper                                 repositoryHelper;
    private final InvalidParameterHandler                              invalidParameterHandler;
    private final AssetHandler<DATABASE>                               databaseHandler;
    private final AssetHandler<DATABASE_SCHEMA>                        databaseSchemaHandler;
    private final SchemaAttributeHandler<DATABASE_TABLE, SCHEMA_TYPE>  databaseTableHandler;
    private final SchemaAttributeHandler<DATABASE_VIEW, SCHEMA_TYPE>   databaseViewHandler;
    private final SchemaAttributeHandler<DATABASE_COLUMN, SCHEMA_TYPE> databaseColumnHandler;

    protected RepositoryErrorHandler errorHandler;


    /**
     * Construct the relational data handler with information needed to work with assets and schemas.
     *
     * @param databaseConverter specific converter for the DATABASE bean class
     * @param databaseClass name of bean class that is represented by the generic class DATABASE
     * @param databaseSchemaConverter specific converter for the DATABASE_SCHEMA bean class
     * @param databaseSchemaClass name of bean class that is represented by the generic class DATABASE_SCHEMA
     * @param databaseTableConverter specific converter for the DATABASE_TABLE bean class
     * @param databaseTableClass name of bean class that is represented by the generic class DATABASE_TABLE
     * @param databaseViewConverter specific converter for the DATABASE_VIEW bean class
     * @param databaseViewClass name of bean class that is represented by the generic class DATABASE_VIEW
     * @param databaseColumnConverter specific converter for the DATABASE_COLUMN bean class
     * @param databaseColumnClass name of bean class that is represented by the generic class DATABASE_COLUMN
     * @param schemaTypeConverter specific converter for the SCHEMA_TYPE bean class
     * @param schemaTypeClass name of bean class that is represented by the generic class SCHEMA_TYPE
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
    public RelationalDataHandler(OpenMetadataAPIGenericConverter<DATABASE> databaseConverter,
                                 Class<DATABASE>                                                 databaseClass,
                                 OpenMetadataAPIGenericConverter<DATABASE_SCHEMA> databaseSchemaConverter,
                                 Class<DATABASE_SCHEMA>                                          databaseSchemaClass,
                                 OpenMetadataAPIGenericConverter<DATABASE_TABLE> databaseTableConverter,
                                 Class<DATABASE_TABLE>                                           databaseTableClass,
                                 OpenMetadataAPIGenericConverter<DATABASE_VIEW> databaseViewConverter,
                                 Class<DATABASE_VIEW>                                            databaseViewClass,
                                 OpenMetadataAPIGenericConverter<DATABASE_COLUMN> databaseColumnConverter,
                                 Class<DATABASE_COLUMN>                                          databaseColumnClass,
                                 OpenMetadataAPIGenericConverter<SCHEMA_TYPE> schemaTypeConverter,
                                 Class<SCHEMA_TYPE>                                              schemaTypeClass,
                                 String                                                          serviceName,
                                 String                                                          serverName,
                                 InvalidParameterHandler invalidParameterHandler,
                                 RepositoryHandler repositoryHandler,
                                 OMRSRepositoryHelper repositoryHelper,
                                 String                                                          localServerUserId,
                                 OpenMetadataServerSecurityVerifier securityVerifier,
                                 List<String>                                                    supportedZones,
                                 List<String>                                                    defaultZones,
                                 List<String>                                                    publishZones,
                                 AuditLog auditLog)
    {
        this.serviceName                     = serviceName;
        this.serverName                      = serverName;
        this.invalidParameterHandler         = invalidParameterHandler;
        this.repositoryHelper                = repositoryHelper;

        this.databaseHandler = new AssetHandler<>(databaseConverter,
                                                  databaseClass,
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

        this.databaseSchemaHandler = new AssetHandler<>(databaseSchemaConverter,
                                                        databaseSchemaClass,
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

        this.databaseTableHandler = new SchemaAttributeHandler<>(databaseTableConverter,
                                                                 databaseTableClass,
                                                                 schemaTypeConverter,
                                                                 schemaTypeClass,
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

        this.databaseViewHandler = new SchemaAttributeHandler<>(databaseViewConverter,
                                                                databaseViewClass,
                                                                schemaTypeConverter,
                                                                schemaTypeClass,
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

        this.databaseColumnHandler = new SchemaAttributeHandler<>(databaseColumnConverter,
                                                                  databaseColumnClass,
                                                                  schemaTypeConverter,
                                                                  schemaTypeClass,
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

        this.errorHandler = new RepositoryErrorHandler(repositoryHelper, serviceName, serverName, auditLog);
    }


    /* ========================================================
     * The database is the top level asset on a database server
     */


    /**
     * Create a new metadata element to represent a database that is owned by an external element.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param qualifiedName unique name for this database
     * @param name the short name property for the database
     * @param resourceName the long name of the database
     * @param versionIdentifier the stored version property for the database
     * @param description the stored description property associated with the database
     * @param pathName the fully qualified physical location of the database
     * @param createTime the time that the database was created
     * @param modifiedTime the last known time the data store was modified
     * @param encodingType the name of the encoding style used in the database
     * @param encodingLanguage the name of the natural language used for text strings within the database
     * @param encodingDescription the description of the encoding used in the database
     * @param encodingProperties properties used to control encoding
     * @param deployedImplementationType a description of the database type
     * @param databaseVersion the version of the database - often this is related to the version of its schemas.
     * @param databaseInstance the name of this database instance - useful if the same schemas are deployed to multiple database instances
     * @param databaseImportedFrom the source (typically connection name) of the database information
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of Database - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabase(String               userId,
                                 String               databaseManagerGUID,
                                 String               databaseManagerName,
                                 String               qualifiedName,
                                 String               name,
                                 String               resourceName,
                                 String               versionIdentifier,
                                 String               description,
                                 String               pathName,
                                 Date                 createTime,
                                 Date                 modifiedTime,
                                 String               encodingType,
                                 String               encodingLanguage,
                                 String               encodingDescription,
                                 Map<String, String>  encodingProperties,
                                 String               deployedImplementationType,
                                 String               databaseVersion,
                                 String               databaseInstance,
                                 String               databaseImportedFrom,
                                 Map<String, String>  additionalProperties,
                                 String               typeName,
                                 Map<String, Object>  extendedProperties,
                                 Map<String, String>  vendorProperties,
                                 Date                 effectiveFrom,
                                 Date                 effectiveTo,
                                 boolean              forLineage,
                                 boolean              forDuplicateProcessing,
                                 Date                 effectiveTime,
                                 String               methodName) throws InvalidParameterException,
                                                                         UserNotAuthorizedException,
                                                                         PropertyServerException
    {
        final String databaseManagerGUIDParameterName  = "databaseManagerGUID";
        final String databaseGUIDParameterName         = "databaseGUID";
        final String qualifiedNameParameterName        = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        databaseHandler.verifyExternalSourceIdentity(userId,
                                                     databaseManagerGUID,
                                                     databaseManagerName,
                                                     forLineage,
                                                     forDuplicateProcessing,
                                                     effectiveTime,
                                                     methodName);

        String assetTypeName = OpenMetadataType.DATABASE.typeName;

        if (typeName != null)
        {
            assetTypeName = typeName;
        }


        Map<String, Object> assetExtendedProperties = new HashMap<>();
        if (extendedProperties != null)
        {
            assetExtendedProperties.putAll(extendedProperties);
        }

        assetExtendedProperties.put(OpenMetadataProperty.PATH_NAME.name, pathName);
        assetExtendedProperties.put(OpenMetadataProperty.STORE_CREATE_TIME.name, createTime);
        assetExtendedProperties.put(OpenMetadataProperty.STORE_UPDATE_TIME.name, modifiedTime);
        assetExtendedProperties.put(OpenMetadataProperty.DATABASE_VERSION.name, databaseVersion);
        assetExtendedProperties.put(OpenMetadataProperty.INSTANCE.name, databaseInstance);
        assetExtendedProperties.put(OpenMetadataProperty.IMPORTED_FROM.name, databaseImportedFrom);

        /*
         * This call will set up the default zones and give ownership of the asset to the calling user.
         */
        String databaseGUID = databaseHandler.createAssetInRepository(userId,
                                                                      databaseManagerGUID,
                                                                      databaseManagerName,
                                                                      qualifiedName,
                                                                      name,
                                                                      resourceName,
                                                                      versionIdentifier,
                                                                      description,
                                                                      deployedImplementationType,
                                                                      additionalProperties,
                                                                      assetTypeName,
                                                                      assetExtendedProperties,
                                                                      InstanceStatus.ACTIVE,
                                                                      effectiveFrom,
                                                                      effectiveTo,
                                                                      effectiveTime,
                                                                      methodName);

        if (databaseGUID != null)
        {
            if ((encodingType != null) || (encodingLanguage != null) || (encodingDescription != null))
            {
                InstanceProperties classificationProperties = this.getEncodingProperties(encodingType,
                                                                                         encodingLanguage,
                                                                                         encodingDescription,
                                                                                         encodingProperties,
                                                                                         methodName);


                databaseHandler.setClassificationInRepository(userId,
                                                              databaseManagerGUID,
                                                              databaseManagerName,
                                                              databaseGUID,
                                                              databaseGUIDParameterName,
                                                              OpenMetadataType.DATABASE.typeName,
                                                              OpenMetadataType.DATA_ASSET_ENCODING_CLASSIFICATION.typeGUID,
                                                              OpenMetadataType.DATA_ASSET_ENCODING_CLASSIFICATION.typeName,
                                                              classificationProperties,
                                                              true,
                                                              forLineage,
                                                              forDuplicateProcessing,
                                                              effectiveTime,
                                                              methodName);
            }

            databaseHandler.setVendorProperties(userId, databaseGUID, vendorProperties, forLineage, forDuplicateProcessing, effectiveTime, methodName);

            try
            {
                InstanceProperties relationshipProperties = repositoryHelper.addEnumPropertyToInstance(serviceName,
                                                                                                       null,
                                                                                                       OpenMetadataProperty.USE_TYPE.name,
                                                                                                       ServerAssetUseType.getOpenTypeGUID(),
                                                                                                       ServerAssetUseType.getOpenTypeName(),
                                                                                                       ServerAssetUseType.OWNS.getOrdinal(),
                                                                                                       methodName);

                databaseHandler.linkElementToElement(userId,
                                                     databaseManagerGUID,
                                                     databaseManagerName,
                                                     databaseManagerGUID,
                                                     databaseManagerGUIDParameterName,
                                                     OpenMetadataType.SOFTWARE_CAPABILITY.typeName,
                                                     databaseGUID,
                                                     databaseGUIDParameterName,
                                                     OpenMetadataType.DATABASE.typeName,
                                                     forLineage,
                                                     forDuplicateProcessing,
                                                     OpenMetadataType.SERVER_ASSET_USE_RELATIONSHIP.typeGUID,
                                                     OpenMetadataType.SERVER_ASSET_USE_RELATIONSHIP.typeName,
                                                     relationshipProperties,
                                                     effectiveFrom,
                                                     effectiveTo,
                                                     effectiveTime,
                                                     methodName);
            }
            catch (TypeErrorException error)
            {
                throw new InvalidParameterException(error, OpenMetadataProperty.USE_TYPE.name);
            }
        }

        return databaseGUID;
    }


    /**
     * Create a new metadata element to represent a database using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param templateGUID unique identifier of the metadata element to copy
     * @param qualifiedName unique name for this database - must not be null
     * @param technicalName the stored name property for the database - if null, the value from the template is used
     * @param versionIdentifier version identifier property for the database - if null, the value from the template is used
     * @param description the stored description property associated with the database - if null, the value from the template is used
     * @param pathName the fully qualified physical location of the data store
     * @param networkAddress physical location of the database - used to connect to it
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseFromTemplate(String               userId,
                                             String               databaseManagerGUID,
                                             String               databaseManagerName,
                                             String               templateGUID,
                                             String               qualifiedName,
                                             String               technicalName,
                                             String               versionIdentifier,
                                             String               description,
                                             String               pathName,
                                             String               networkAddress,
                                             boolean              forLineage,
                                             boolean              forDuplicateProcessing,
                                             Date                 effectiveTime,
                                             String               methodName) throws InvalidParameterException,
                                                                                     UserNotAuthorizedException,
                                                                                     PropertyServerException
    {
        final String templateGUIDParameterName   = "templateGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(templateGUID, templateGUIDParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        return databaseHandler.addAssetFromTemplate(userId,
                                                    databaseManagerGUID,
                                                    databaseManagerName,
                                                    templateGUID,
                                                    templateGUIDParameterName,
                                                    OpenMetadataType.DATABASE.typeGUID,
                                                    OpenMetadataType.DATABASE.typeName,
                                                    qualifiedName,
                                                    qualifiedNameParameterName,
                                                    technicalName,
                                                    versionIdentifier,
                                                    description,
                                                    null,
                                                    pathName,
                                                    networkAddress,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);
    }


    /**
     * Update the metadata element representing a database.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseGUID unique identifier of the metadata element to update
     * @param qualifiedName unique name for this database
     * @param name the short name property for the database
     * @param resourceName the long name of the database
     * @param versionIdentifier the stored version identifier for  the database
     * @param description the stored description property associated with the database
     * @param pathName the fully qualified physical location of the database
     * @param createTime the time that the database was created
     * @param modifiedTime the last known time the data store was modified
     * @param encodingType the name of the encoding style used in the database
     * @param encodingLanguage the name of the natural language used for text strings within the database
     * @param encodingDescription the description of the encoding used in the database
     * @param encodingProperties properties used to control encoding
     * @param deployedImplementationType a description of the database type
     * @param databaseVersion the version of the database - often this is related to the version of its schemas.
     * @param databaseInstance the name of this database instance - useful if the same schemas are deployed to multiple database instances
     * @param databaseImportedFrom the source (typically connection name) of the database information
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of Database - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate should the new properties be merged with existing properties (true) or completely replace them (false)?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateDatabase(String               userId,
                               String               databaseManagerGUID,
                               String               databaseManagerName,
                               String               databaseGUID,
                               String               qualifiedName,
                               String               name,
                               String               resourceName,
                               String               versionIdentifier,
                               String               description,
                               String               pathName,
                               Date                 createTime,
                               Date                 modifiedTime,
                               String               encodingType,
                               String               encodingLanguage,
                               String               encodingDescription,
                               Map<String, String>  encodingProperties,
                               String               deployedImplementationType,
                               String               databaseVersion,
                               String               databaseInstance,
                               String               databaseImportedFrom,
                               Map<String, String>  additionalProperties,
                               String               typeName,
                               Map<String, Object>  extendedProperties,
                               Map<String, String>  vendorProperties,
                               Date                 effectiveFrom,
                               Date                 effectiveTo,
                               boolean              isMergeUpdate,
                               boolean              forLineage,
                               boolean              forDuplicateProcessing,
                               Date                 effectiveTime,
                               String               methodName) throws InvalidParameterException,
                                                                       UserNotAuthorizedException,
                                                                       PropertyServerException
    {
        final String elementGUIDParameterName    = "databaseGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseGUID, elementGUIDParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        String assetTypeName = OpenMetadataType.DATABASE.typeName;

        if (typeName != null)
        {
            assetTypeName = typeName;
        }

        Map<String, Object> assetExtendedProperties = new HashMap<>();
        if (extendedProperties != null)
        {
            assetExtendedProperties.putAll(extendedProperties);
        }

        assetExtendedProperties.put(OpenMetadataProperty.PATH_NAME.name, pathName);
        assetExtendedProperties.put(OpenMetadataProperty.STORE_CREATE_TIME.name, createTime);
        assetExtendedProperties.put(OpenMetadataProperty.STORE_UPDATE_TIME.name, modifiedTime);

        assetExtendedProperties.put(OpenMetadataProperty.DATABASE_VERSION.name, databaseVersion);
        assetExtendedProperties.put(OpenMetadataProperty.INSTANCE.name, databaseInstance);
        assetExtendedProperties.put(OpenMetadataProperty.IMPORTED_FROM.name, databaseImportedFrom);

        databaseHandler.updateAsset(userId,
                                    databaseManagerGUID,
                                    databaseManagerName,
                                    databaseGUID,
                                    elementGUIDParameterName,
                                    qualifiedName,
                                    name,
                                    resourceName,
                                    versionIdentifier,
                                    description,
                                    deployedImplementationType,
                                    additionalProperties,
                                    assetTypeName,
                                    assetExtendedProperties,
                                    effectiveFrom,
                                    effectiveTo,
                                    isMergeUpdate,
                                    forLineage,
                                    forDuplicateProcessing,
                                    effectiveTime,
                                    methodName);

        if ((encodingType != null) || (encodingLanguage != null) || (encodingDescription != null))
        {
            InstanceProperties classificationProperties = this.getEncodingProperties(encodingType,
                                                                                     encodingLanguage,
                                                                                     encodingDescription,
                                                                                     encodingProperties,
                                                                                     methodName);


            databaseHandler.setClassificationInRepository(userId,
                                                          databaseManagerGUID,
                                                          databaseManagerName,
                                                          databaseGUID,
                                                          elementGUIDParameterName,
                                                          OpenMetadataType.DATABASE.typeName,
                                                          OpenMetadataType.DATA_ASSET_ENCODING_CLASSIFICATION.typeGUID,
                                                          OpenMetadataType.DATA_ASSET_ENCODING_CLASSIFICATION.typeName,
                                                          classificationProperties,
                                                          isMergeUpdate,
                                                          forLineage,
                                                          forDuplicateProcessing,
                                                          effectiveTime,
                                                          methodName);
        }

        if (vendorProperties != null)
        {
            databaseHandler.setVendorProperties(userId,
                                                databaseGUID,
                                                vendorProperties,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
        }
    }


    /**
     * Add the encoding parameters to an instance properties object.
     *
     * @param encodingType the name of the encoding style used in the database
     * @param encodingLanguage the name of the natural language used for text strings within the database
     * @param encodingDescription the description of the encoding used in the database
     * @param encodingProperties properties used to control encoding
     * @param methodName calling method
     * @return packaged properties
     */
    private InstanceProperties getEncodingProperties(String               encodingType,
                                                     String               encodingLanguage,
                                                     String               encodingDescription,
                                                     Map<String, String>  encodingProperties,
                                                     String               methodName)
    {
        InstanceProperties classificationProperties;

        classificationProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                null,
                                                                                OpenMetadataProperty.ENCODING.name,
                                                                                encodingType,
                                                                                methodName);
        classificationProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                classificationProperties,
                                                                                OpenMetadataProperty.ENCODING_LANGUAGE.name,
                                                                                encodingLanguage,
                                                                                methodName);
        classificationProperties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                classificationProperties,
                                                                                OpenMetadataProperty.ENCODING_DESCRIPTION.name,
                                                                                encodingDescription,
                                                                                methodName);
        classificationProperties = repositoryHelper.addStringMapPropertyToInstance(serviceName,
                                                                                   classificationProperties,
                                                                                   OpenMetadataProperty.ENCODING_DESCRIPTION.name,
                                                                                   encodingProperties,
                                                                                   methodName);

        return classificationProperties;
    }


    /**
     * Update the zones for the database asset so that it becomes visible to consumers.
     * (The zones are set to the list of zones in the publishedZones option configured for each
     * instance of the Data Platform OMAS).
     *
     * @param userId calling user
     * @param databaseGUID unique identifier of the metadata element to publish
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void publishDatabase(String  userId,
                                String  databaseGUID,
                                boolean forLineage,
                                boolean forDuplicateProcessing,
                                Date    effectiveTime,
                                String  methodName) throws InvalidParameterException,
                                                          UserNotAuthorizedException,
                                                          PropertyServerException
    {
        final String elementGUIDParameterName = "databaseGUID";

        databaseHandler.publishAsset(userId,
                                     databaseGUID,
                                     elementGUIDParameterName,
                                     forLineage,
                                     forDuplicateProcessing,
                                     effectiveTime,
                                     methodName);
    }


    /**
     * Update the zones for the database asset so that it is no longer visible to consumers.
     * (The zones are set to the list of zones in the defaultZones option configured for each
     * instance of the Data Platform OMAS.  This is the setting when the database is first created).
     *
     * @param userId calling user
     * @param databaseGUID unique identifier of the metadata element to withdraw
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void withdrawDatabase(String  userId,
                                 String  databaseGUID,
                                 boolean forLineage,
                                 boolean forDuplicateProcessing,
                                 Date    effectiveTime,
                                 String  methodName) throws InvalidParameterException,
                                                            UserNotAuthorizedException,
                                                            PropertyServerException
    {
        final String elementGUIDParameterName = "databaseGUID";

        databaseHandler.withdrawAsset(userId,
                                      databaseGUID,
                                      elementGUIDParameterName,
                                      forLineage,
                                      forDuplicateProcessing,
                                      effectiveTime,
                                      methodName);
    }


    /**
     * Remove the metadata element representing a database.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseGUID unique identifier of the metadata element to remove
     * @param qualifiedName optional unique name of the metadata element to remove
     * @param cascadedDelete     boolean indicating whether the delete request can cascade to dependent elements
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeDatabase(String  userId,
                               String  databaseManagerGUID,
                               String  databaseManagerName,
                               String  databaseGUID,
                               String  qualifiedName,
                               boolean cascadedDelete,
                               boolean forLineage,
                               boolean forDuplicateProcessing,
                               Date    effectiveTime,
                               String  methodName) throws InvalidParameterException,
                                                          UserNotAuthorizedException,
                                                          PropertyServerException
    {
        final String elementGUIDParameterName    = "databaseGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseGUID, elementGUIDParameterName, methodName);

        if (qualifiedName != null)
        {
            databaseHandler.deleteBeanInRepository(userId,
                                                   databaseManagerGUID,
                                                   databaseManagerName,
                                                   databaseGUID,
                                                   elementGUIDParameterName,
                                                   OpenMetadataType.DATABASE.typeGUID,
                                                   OpenMetadataType.DATABASE.typeName,
                                                   cascadedDelete,
                                                   OpenMetadataProperty.QUALIFIED_NAME.name,
                                                   qualifiedName,
                                                   forLineage,
                                                   forDuplicateProcessing,
                                                   effectiveTime,
                                                   methodName);
        }
        else
        {
            databaseHandler.deleteBeanInRepository(userId,
                                                   databaseManagerGUID,
                                                   databaseManagerName,
                                                   databaseGUID,
                                                   elementGUIDParameterName,
                                                   OpenMetadataType.DATABASE.typeGUID,
                                                   OpenMetadataType.DATABASE.typeName,
                                                   cascadedDelete,
                                                   null,
                                                   null,
                                                   forLineage,
                                                   forDuplicateProcessing,
                                                   effectiveTime,
                                                   methodName);
        }
    }


    /**
     * Retrieve the list of database metadata elements that contain the search string.
     * The search string is treated as a regular expression.
     *
     * @param userId calling user
     * @param searchString string to find in the properties
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE> findDatabases(String  userId,
                                        String  searchString,
                                        int     startFrom,
                                        int     pageSize,
                                        boolean forLineage,
                                        boolean forDuplicateProcessing,
                                        Date    effectiveTime,
                                        String  methodName) throws InvalidParameterException,
                                                                   UserNotAuthorizedException,
                                                                   PropertyServerException
    {
        final String searchStringParameterName = "searchString";

        return databaseHandler.findAssets(userId,
                                          OpenMetadataType.DATABASE.typeGUID,
                                          OpenMetadataType.DATABASE.typeName,
                                          searchString,
                                          searchStringParameterName,
                                          startFrom,
                                          pageSize,
                                          forLineage,
                                          forDuplicateProcessing,
                                          effectiveTime,
                                          methodName);
    }


    /**
     * Retrieve the list of database metadata elements with a matching qualified or display name.
     * There are no wildcards supported on this request.
     *
     * @param userId calling user
     * @param name name to search for
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE>   getDatabasesByName(String  userId,
                                               String  name,
                                               int     startFrom,
                                               int     pageSize,
                                               boolean forLineage,
                                               boolean forDuplicateProcessing,
                                               Date    effectiveTime,
                                               String  methodName) throws InvalidParameterException,
                                                                          UserNotAuthorizedException,
                                                                          PropertyServerException
    {
        final String nameParameterName = "name";

        return databaseHandler.getAssetsByName(userId,
                                               OpenMetadataType.DATABASE.typeGUID,
                                               OpenMetadataType.DATABASE.typeName,
                                               name,
                                               nameParameterName,
                                               startFrom,
                                               pageSize,
                                               forLineage,
                                               forDuplicateProcessing,
                                               effectiveTime,
                                               methodName);
    }


    /**
     * Retrieve the list of databases created for the requested database manager.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    @SuppressWarnings(value = "unused")
    public List<DATABASE>   getDatabasesForDatabaseManager(String  userId,
                                                           String  databaseManagerGUID,
                                                           String  databaseManagerName,
                                                           int     startFrom,
                                                           int     pageSize,
                                                           boolean forLineage,
                                                           boolean forDuplicateProcessing,
                                                           Date    effectiveTime,
                                                           String  methodName) throws InvalidParameterException,
                                                                                      UserNotAuthorizedException,
                                                                                      PropertyServerException
    {
        final String databaseManagerGUIDParameterName = "databaseManagerGUID";

        return databaseHandler.getAttachedElements(userId,
                                                   databaseManagerGUID,
                                                   databaseManagerGUIDParameterName,
                                                   OpenMetadataType.DATABASE_MANAGER.typeName,
                                                   OpenMetadataType.SERVER_ASSET_USE_RELATIONSHIP.typeGUID,
                                                   OpenMetadataType.SERVER_ASSET_USE_RELATIONSHIP.typeName,
                                                   OpenMetadataType.DATABASE.typeName,
                                                   null,
                                                   null,
                                                   0,
                                                   null,
                                                   null,
                                                   SequencingOrder.CREATION_DATE_RECENT,
                                                   null,
                                                   forLineage,
                                                   forDuplicateProcessing,
                                                   startFrom,
                                                   pageSize,
                                                   effectiveTime,
                                                   methodName);
    }


    /**
     * Retrieve the database metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param guid unique identifier of the requested metadata element
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return matching metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public DATABASE getDatabaseByGUID(String  userId,
                                      String  guid,
                                      boolean forLineage,
                                      boolean forDuplicateProcessing,
                                      Date    effectiveTime,
                                      String  methodName) throws InvalidParameterException,
                                                                 UserNotAuthorizedException,
                                                                 PropertyServerException
    {
        final String guidParameterName = "guid";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(guid, guidParameterName, methodName);

        /*
         * This call checks type of entity, zones and security.
         */
        return databaseHandler.getBeanFromRepository(userId,
                                                     guid,
                                                     guidParameterName,
                                                     OpenMetadataType.DATABASE.typeName,
                                                     forLineage,
                                                     forDuplicateProcessing,
                                                     effectiveTime,
                                                     methodName);
    }


    /* ============================================================================
     * A database may host one or more database schemas depending on its capability
     */

    /**
     * Create a new metadata element to represent a database schema.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseGUID unique identifier of the database where the schema is located
     * @param qualifiedName unique name for this database schema
     * @param name the short name property for the database schema
     * @param resourceName the long name of the database schema
     * @param versionIdentifier versionIdentifier property
     * @param technicalDescription the stored description property associated with the database schema
     * @param deployedImplementationType technology type
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new database schema
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseSchema(String               userId,
                                       String               databaseManagerGUID,
                                       String               databaseManagerName,
                                       String               databaseGUID,
                                       String               qualifiedName,
                                       String               name,
                                       String               resourceName,
                                       String               versionIdentifier,
                                       String               technicalDescription,
                                       String               deployedImplementationType,
                                       Map<String, String>  additionalProperties,
                                       String               typeName,
                                       Map<String, Object>  extendedProperties,
                                       Map<String, String>  vendorProperties,
                                       Date                 effectiveFrom,
                                       Date                 effectiveTo,
                                       boolean              forLineage,
                                       boolean              forDuplicateProcessing,
                                       Date                 effectiveTime,
                                       String               methodName) throws InvalidParameterException,
                                                                               UserNotAuthorizedException,
                                                                               PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseGUID";
        final String createdElementGUIDParameterName = "databaseSchemaGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        databaseSchemaHandler.verifyExternalSourceIdentity(userId,
                                                           databaseManagerGUID,
                                                           databaseManagerName,
                                                           forLineage,
                                                           forDuplicateProcessing,
                                                           effectiveTime,
                                                           methodName);

        String assetTypeName = OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName;

        if (typeName != null)
        {
            assetTypeName = typeName;
        }

        invalidParameterHandler.validateTypeName(assetTypeName,
                                                 OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                 serviceName,
                                                 methodName,
                                                 repositoryHelper);

        /*
         * This call will set up the default zones and give ownership of the asset to the calling user.
         */
        String databaseSchemaGUID = databaseSchemaHandler.createAssetInRepository(userId,
                                                                                  databaseManagerGUID,
                                                                                  databaseManagerName,
                                                                                  qualifiedName,
                                                                                  name,
                                                                                  resourceName,
                                                                                  versionIdentifier,
                                                                                  technicalDescription,
                                                                                  deployedImplementationType,
                                                                                  additionalProperties,
                                                                                  assetTypeName,
                                                                                  extendedProperties,
                                                                                  InstanceStatus.ACTIVE,
                                                                                  effectiveFrom,
                                                                                  effectiveTo,
                                                                                  effectiveTime,
                                                                                  methodName);

        if (databaseGUID != null)
        {
            /*
             * This relationship links the database to the database schema.
             */
            databaseSchemaHandler.linkElementToElement(userId,
                                                       databaseManagerGUID,
                                                       databaseManagerName,
                                                       databaseSchemaGUID,
                                                       createdElementGUIDParameterName,
                                                       OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                       databaseGUID,
                                                       parentElementGUIDParameterName,
                                                       OpenMetadataType.DATABASE.typeName,
                                                       forLineage,
                                                       forDuplicateProcessing,
                                                       OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeGUID,
                                                       OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeName,
                                                       null,
                                                       effectiveFrom,
                                                       effectiveTo,
                                                       effectiveTime,
                                                       methodName);

            if (vendorProperties != null)
            {
                databaseHandler.setVendorProperties(userId,
                                                    databaseSchemaGUID,
                                                    vendorProperties,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);
            }

        }
        return databaseSchemaGUID;
    }


    /**
     * Create a new metadata element to represent a database schema using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param templateGUID unique identifier of the metadata element to copy
     * @param databaseGUID unique identifier of the database where the schema is located
     * @param qualifiedName unique name for the database schema
     * @param technicalName the stored name property for the database schema
     * @param versionIdentifier the stored versionIdentifier property for the database schema
     * @param technicalDescription the stored description property associated with the database schema
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new database schema
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseSchemaFromTemplate(String  userId,
                                                   String  databaseManagerGUID,
                                                   String  databaseManagerName,
                                                   String  templateGUID,
                                                   String  databaseGUID,
                                                   String  qualifiedName,
                                                   String  technicalName,
                                                   String  versionIdentifier,
                                                   String  technicalDescription,
                                                   Date    effectiveFrom,
                                                   Date    effectiveTo,
                                                   boolean forLineage,
                                                   boolean forDuplicateProcessing,
                                                   Date    effectiveTime,
                                                   String  methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        final String templateGUIDParameterName       = "templateGUID";
        final String parentElementGUIDParameterName  = "databaseGUID";
        final String createdElementGUIDParameterName = "databaseSchemaGUID";
        final String qualifiedNameParameterName      = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(templateGUID, templateGUIDParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        String databaseSchemaGUID = databaseSchemaHandler.addAssetFromTemplate(userId,
                                                                               databaseManagerGUID,
                                                                               databaseManagerName,
                                                                               templateGUID,
                                                                               templateGUIDParameterName,
                                                                               OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeGUID,
                                                                               OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                                               qualifiedName,
                                                                               qualifiedNameParameterName,
                                                                               technicalName,
                                                                               versionIdentifier,
                                                                               technicalDescription,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               forLineage,
                                                                               forDuplicateProcessing,
                                                                               effectiveTime,
                                                                               methodName);

        if (databaseGUID != null)
        {
            /*
             * This relationship links the database to the database schema.
             */
            databaseSchemaHandler.linkElementToElement(userId,
                                                       databaseManagerGUID,
                                                       databaseManagerName,
                                                       databaseSchemaGUID,
                                                       createdElementGUIDParameterName,
                                                       OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                       databaseGUID,
                                                       parentElementGUIDParameterName,
                                                       OpenMetadataType.DATABASE.typeName,
                                                       forLineage,
                                                       forDuplicateProcessing,
                                                       OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeGUID,
                                                       OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeName,
                                                       null,
                                                       effectiveFrom,
                                                       effectiveTo,
                                                       effectiveTime,
                                                       methodName);
        }

        return databaseSchemaGUID;
    }


    /**
     * Update the metadata element representing a database schema.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseSchemaGUID unique identifier of the metadata element to update
     * @param qualifiedName unique name for the database schema
     * @param name the short name property for the database schema
     * @param resourceName the long name of the database schema
     * @param versionIdentifier the stored versionIdentifier property for the database schema
     * @param description the stored description property associated with the database schema
     * @param deployedImplementationType technology type
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate should the new properties be merged with existing properties (true) or completely replace them (false)?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateDatabaseSchema(String              userId,
                                     String              databaseManagerGUID,
                                     String              databaseManagerName,
                                     String              databaseSchemaGUID,
                                     String              qualifiedName,
                                     String              name,
                                     String              resourceName,
                                     String              versionIdentifier,
                                     String              description,
                                     String              deployedImplementationType,
                                     Map<String, String> additionalProperties,
                                     String              typeName,
                                     Map<String, Object> extendedProperties,
                                     Map<String, String> vendorProperties,
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
        final String elementGUIDParameterName = "databaseSchemaGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseSchemaGUID, elementGUIDParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        String assetTypeName = OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName;

        if (typeName != null)
        {
            assetTypeName = typeName;
        }

        databaseSchemaHandler.updateAsset(userId,
                                          databaseManagerGUID,
                                          databaseManagerName,
                                          databaseSchemaGUID,
                                          elementGUIDParameterName,
                                          qualifiedName,
                                          name,
                                          resourceName,
                                          versionIdentifier,
                                          description,
                                          deployedImplementationType,
                                          additionalProperties,
                                          assetTypeName,
                                          extendedProperties,
                                          effectiveFrom,
                                          effectiveTo,
                                          isMergeUpdate,
                                          forLineage,
                                          forDuplicateProcessing,
                                          effectiveTime,
                                          methodName);

        if (vendorProperties != null)
        {
            databaseHandler.setVendorProperties(userId,
                                                databaseSchemaGUID,
                                                vendorProperties,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
        }
    }


    /**
     * Update the zones for the database asset so that it becomes visible to consumers.
     * (The zones are set to the list of zones in the publishedZones option configured for each
     * instance of the Data Platform OMAS).
     *
     * @param userId calling user
     * @param databaseSchemaGUID unique identifier of the metadata element to publish
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void publishDatabaseSchema(String  userId,
                                      String  databaseSchemaGUID,
                                      boolean forLineage,
                                      boolean forDuplicateProcessing,
                                      Date    effectiveTime,
                                      String  methodName) throws InvalidParameterException,
                                                                 UserNotAuthorizedException,
                                                                 PropertyServerException
    {
        final String elementGUIDParameterName = "databaseSchemaGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseSchemaGUID, elementGUIDParameterName, methodName);

        databaseSchemaHandler.publishAsset(userId,
                                           databaseSchemaGUID,
                                           elementGUIDParameterName,
                                           forLineage,
                                           forDuplicateProcessing,
                                           effectiveTime,
                                           methodName);
    }


    /**
     * Update the zones for the database asset so that it is no longer visible to consumers.
     * (The zones are set to the list of zones in the defaultZones option configured for each
     * instance of the Data Platform OMAS.  This is the setting when the database is first created).
     *
     * @param userId calling user
     * @param databaseSchemaGUID unique identifier of the metadata element to withdraw
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void withdrawDatabaseSchema(String  userId,
                                       String  databaseSchemaGUID,
                                       boolean forLineage,
                                       boolean forDuplicateProcessing,
                                       Date    effectiveTime,
                                       String  methodName) throws InvalidParameterException,
                                                                  UserNotAuthorizedException,
                                                                  PropertyServerException
    {
        final String elementGUIDParameterName = "databaseSchemaGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseSchemaGUID, elementGUIDParameterName, methodName);

        databaseSchemaHandler.withdrawAsset(userId,
                                            databaseSchemaGUID,
                                            elementGUIDParameterName,
                                            forLineage,
                                            forDuplicateProcessing,
                                            effectiveTime, methodName);
    }


    /**
     * Remove the metadata element representing a database schema.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseSchemaGUID unique identifier of the metadata element to remove
     * @param qualifiedName optional unique name of the metadata element to remove
     * @param cascadedDelete     boolean indicating whether the delete request can cascade to dependent elements
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeDatabaseSchema(String  userId,
                                     String  databaseManagerGUID,
                                     String  databaseManagerName,
                                     String  databaseSchemaGUID,
                                     String  qualifiedName,
                                     boolean cascadedDelete,
                                     boolean forLineage,
                                     boolean forDuplicateProcessing,
                                     Date    effectiveTime,
                                     String  methodName) throws InvalidParameterException,
                                                                UserNotAuthorizedException,
                                                                PropertyServerException
    {
        final String elementGUIDParameterName    = "databaseSchemaGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseSchemaGUID, elementGUIDParameterName, methodName);

        if (qualifiedName != null)
        {
            databaseSchemaHandler.deleteBeanInRepository(userId,
                                                         databaseManagerGUID,
                                                         databaseManagerName,
                                                         databaseSchemaGUID,
                                                         elementGUIDParameterName,
                                                         OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeGUID,
                                                         OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                         cascadedDelete,
                                                         OpenMetadataProperty.QUALIFIED_NAME.name,
                                                         qualifiedName,
                                                         forLineage,
                                                         forDuplicateProcessing,
                                                         effectiveTime,
                                                         methodName);
        }
        else
        {
            databaseSchemaHandler.deleteBeanInRepository(userId,
                                                         databaseManagerGUID,
                                                         databaseManagerName,
                                                         databaseSchemaGUID,
                                                         elementGUIDParameterName,
                                                         OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeGUID,
                                                         OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                         cascadedDelete,
                                                         null,
                                                         null,
                                                         forLineage,
                                                         forDuplicateProcessing,
                                                         effectiveTime,
                                                         methodName);
        }
    }


    /**
     * Retrieve the list of database schema metadata elements that contain the search string.
     * The search string is treated as a regular expression.
     *
     * @param userId calling user
     * @param searchString string to find in the properties
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_SCHEMA>   findDatabaseSchemas(String  userId,
                                                       String  searchString,
                                                       int     startFrom,
                                                       int     pageSize,
                                                       boolean forLineage,
                                                       boolean forDuplicateProcessing,
                                                       Date    effectiveTime,
                                                       String  methodName) throws InvalidParameterException,
                                                                                  UserNotAuthorizedException,
                                                                                  PropertyServerException
    {
        final String searchStringParameterName = "searchString";

        return databaseSchemaHandler.findAssets(userId,
                                                OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeGUID,
                                                OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                searchString,
                                                searchStringParameterName,
                                                startFrom,
                                                pageSize,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Return the list of (deployed database) schemas associated with a database.
     *
     * @param userId calling user
     * @param databaseGUID unique identifier of the database to query
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of metadata elements describing the schemas associated with the requested database
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_SCHEMA>   getSchemasForDatabase(String  userId,
                                                         String  databaseGUID,
                                                         int     startFrom,
                                                         int     pageSize,
                                                         boolean forLineage,
                                                         boolean forDuplicateProcessing,
                                                         Date    effectiveTime,
                                                         String  methodName) throws InvalidParameterException,
                                                                                    UserNotAuthorizedException,
                                                                                    PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseGUID, parentElementGUIDParameterName, methodName);

        return databaseSchemaHandler.getAttachedElements(userId,
                                                         databaseGUID,
                                                         parentElementGUIDParameterName,
                                                         OpenMetadataType.DATABASE.typeName,
                                                         OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeGUID,
                                                         OpenMetadataType.DATA_SET_CONTENT_RELATIONSHIP.typeName,
                                                         OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                         null,
                                                         null,
                                                         1,
                                                         null,
                                                         null,
                                                         SequencingOrder.CREATION_DATE_RECENT,
                                                         null,
                                                         forLineage,
                                                         forDuplicateProcessing,
                                                         startFrom,
                                                         pageSize,
                                                         effectiveTime,
                                                         methodName);
    }


    /**
     * Retrieve the list of database schema metadata elements with a matching qualified or display name.
     * There are no wildcards supported on this request.
     *
     * @param userId calling user
     * @param name name to search for
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_SCHEMA>   getDatabaseSchemasByName(String  userId,
                                                            String  name,
                                                            int     startFrom,
                                                            int     pageSize,
                                                            boolean forLineage,
                                                            boolean forDuplicateProcessing,
                                                            Date    effectiveTime,
                                                            String  methodName) throws InvalidParameterException,
                                                                                       UserNotAuthorizedException,
                                                                                       PropertyServerException
    {
        final String nameParameterName = "name";

        return databaseSchemaHandler.getAssetsByName(userId,
                                                     OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeGUID,
                                                     OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                     name,
                                                     nameParameterName,
                                                     startFrom,
                                                     pageSize,
                                                     forLineage,
                                                     forDuplicateProcessing,
                                                     effectiveTime,
                                                     methodName);
    }


    /**
     * Retrieve the database schema metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param guid unique identifier of the requested metadata element
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return requested metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public DATABASE_SCHEMA getDatabaseSchemaByGUID(String  userId,
                                                   String  guid,
                                                   boolean forLineage,
                                                   boolean forDuplicateProcessing,
                                                   Date    effectiveTime,
                                                   String  methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        final String guidParameterName = "guid";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(guid, guidParameterName, methodName);

        /*
         * This call checks type of entity, zones and security.
         */
        return databaseSchemaHandler.getBeanFromRepository(userId,
                                                           guid,
                                                           guidParameterName,
                                                           OpenMetadataType.DEPLOYED_DATABASE_SCHEMA.typeName,
                                                           forLineage,
                                                           forDuplicateProcessing,
                                                           effectiveTime,
                                                           methodName);
    }


    /* ==========================================================================
     * A database or database schema may contain multiple database tables and database views.
     * These are linked to the database asset using a RelationalDBSchemaType. It is possible
     * to create the schema type, attach the tables and views to it and then attach the schema type to
     * the asset.  Alternatively it is possible to attach the tables/views directly to the database asset
     * and let this handler manage the creation of the RelationalDBSchemaType.  The first approach is
     * recommended for large databases because it reduces the number of LatestChange classification that
     * are generated for the asset.
     */


    /**
     * Create a database top-level schema type used to attach tables and views to the database/database schema.
     *
     * @param userId calling user
     * @param databaseManagerGUID guid of the software capability entity that represented the external source - null for local
     * @param databaseManagerName name of the software capability entity that represented the external source - null for local
     * @param qualifiedName qualified name ofr the schema type - suggest "SchemaOf:" + asset's qualified name
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     * @return unique identifier of the database schema type
     * @throws InvalidParameterException the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException problem accessing the property server
     */
    public String createDatabaseSchemaType(String               userId,
                                           String               databaseManagerGUID,
                                           String               databaseManagerName,
                                           String               qualifiedName,
                                           Date                 effectiveFrom,
                                           Date                 effectiveTo,
                                           Date                 effectiveTime,
                                           String               methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        SchemaTypeBuilder builder = new SchemaTypeBuilder(qualifiedName,
                                                          OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                          OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                          repositoryHelper,
                                                          serviceName,
                                                          serverName);

        builder.setEffectivityDates(effectiveFrom, effectiveTo);

        return databaseTableHandler.createBeanInRepository(userId,
                                                           databaseManagerGUID,
                                                           databaseManagerName,
                                                           OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                           OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                           builder,
                                                           effectiveTime,
                                                           methodName);
    }



    /**
     * Link the schema type and asset.  This is called from outside the AssetHandler.  The databaseAssetGUID is checked to ensure the
     * asset exists and updates are allowed.  If there is already a schema attached, it is deleted.
     *
     * @param userId calling user
     * @param databaseManagerGUID guid of the software capability entity that represented the external source - null for local
     * @param databaseManagerName name of the software capability entity that represented the external source - null for local
     * @param databaseAssetGUID unique identifier of the asset to connect the schema to
     * @param schemaTypeGUID identifier for schema Type object
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     * @throws InvalidParameterException the bean properties are invalid
     * @throws UserNotAuthorizedException user not authorized to issue this request
     * @throws PropertyServerException problem accessing the property server
     */
    public  void attachSchemaTypeToDatabaseAsset(String  userId,
                                                 String  databaseManagerGUID,
                                                 String  databaseManagerName,
                                                 String  databaseAssetGUID,
                                                 String  schemaTypeGUID,
                                                 Date    effectiveFrom,
                                                 Date    effectiveTo,
                                                 boolean forLineage,
                                                 boolean forDuplicateProcessing,
                                                 Date    effectiveTime,
                                                 String  methodName) throws InvalidParameterException,
                                                                            PropertyServerException,
                                                                            UserNotAuthorizedException
    {
        final String databaseAssetGUIDParameterName = "databaseAssetGUID";
        final String schemaTypeGUIDParameterName = "schemaTypeGUID";

        databaseHandler.attachSchemaTypeToAsset(userId,
                                                databaseManagerGUID,
                                                databaseManagerName,
                                                databaseAssetGUID,
                                                databaseAssetGUIDParameterName,
                                                schemaTypeGUID,
                                                schemaTypeGUIDParameterName,
                                                effectiveFrom,
                                                effectiveTo,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }



    /**
     * Create a new metadata element to represent a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseAssetGUID unique identifier of the database or database schema where the database table is located
     * @param qualifiedName unique name for the database table
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of RelationalTable - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database table
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    @Deprecated
    public String createDatabaseTable(String               userId,
                                      String               databaseManagerGUID,
                                      String               databaseManagerName,
                                      String               databaseAssetGUID,
                                      String               qualifiedName,
                                      String               displayName,
                                      String               description,
                                      boolean              isDeprecated,
                                      List<String>         aliases,
                                      Map<String, String>  additionalProperties,
                                      String               typeName,
                                      Map<String, Object>  extendedProperties,
                                      Map<String, String>  vendorProperties,
                                      String               methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        return this.createDatabaseTable(userId,
                                        databaseManagerGUID,
                                        databaseManagerName,
                                        databaseAssetGUID,
                                        qualifiedName,
                                        displayName,
                                        description,
                                        isDeprecated,
                                        aliases,
                                        additionalProperties,
                                        typeName,
                                        extendedProperties,
                                        vendorProperties,
                                        null,
                                        null,
                                        false,
                                        false,
                                        new Date(),
                                        methodName);
    }


    /**
     * Create a new metadata element to represent a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseAssetGUID unique identifier of the database or database schema where the database table is located
     * @param qualifiedName unique name for the database table
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of RelationalTable - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database table
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseTable(String               userId,
                                      String               databaseManagerGUID,
                                      String               databaseManagerName,
                                      String               databaseAssetGUID,
                                      String               qualifiedName,
                                      String               displayName,
                                      String               description,
                                      boolean              isDeprecated,
                                      List<String>         aliases,
                                      Map<String, String>  additionalProperties,
                                      String               typeName,
                                      Map<String, Object>  extendedProperties,
                                      Map<String, String>  vendorProperties,
                                      Date                 effectiveFrom,
                                      Date                 effectiveTo,
                                      boolean              forLineage,
                                      boolean              forDuplicateProcessing,
                                      Date                 effectiveTime,
                                      String               methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseAssetGUID";

        String databaseSchemaTypeGUID = databaseTableHandler.getAssetSchemaTypeGUID(userId,
                                                                                    databaseManagerGUID,
                                                                                    databaseManagerName,
                                                                                    databaseAssetGUID,
                                                                                    parentElementGUIDParameterName,
                                                                                    OpenMetadataType.ASSET.typeName,
                                                                                    databaseAssetGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                    effectiveFrom,
                                                                                    effectiveTo,
                                                                                    forLineage,
                                                                                    forDuplicateProcessing,
                                                                                    effectiveTime,
                                                                                    methodName);

        return createDatabaseTableForSchemaType(userId,
                                                databaseManagerGUID,
                                                databaseManagerName,
                                                databaseSchemaTypeGUID,
                                                qualifiedName,
                                                displayName,
                                                description,
                                                isDeprecated,
                                                aliases,
                                                additionalProperties,
                                                typeName,
                                                extendedProperties,
                                                vendorProperties,
                                                effectiveFrom,
                                                effectiveTo,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
    }


    /**
     * Create a new metadata element to represent a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseSchemaTypeGUID unique identifier of the database or database schema where the database table is located
     * @param qualifiedName unique name for the database table
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of RelationalTable - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database table
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseTableForSchemaType(String               userId,
                                                   String               databaseManagerGUID,
                                                   String               databaseManagerName,
                                                   String               databaseSchemaTypeGUID,
                                                   String               qualifiedName,
                                                   String               displayName,
                                                   String               description,
                                                   boolean              isDeprecated,
                                                   List<String>         aliases,
                                                   Map<String, String>  additionalProperties,
                                                   String               typeName,
                                                   Map<String, Object>  extendedProperties,
                                                   Map<String, String>  vendorProperties,
                                                   Date                 effectiveFrom,
                                                   Date                 effectiveTo,
                                                   boolean              forLineage,
                                                   boolean              forDuplicateProcessing,
                                                   Date                 effectiveTime,
                                                   String               methodName) throws InvalidParameterException,
                                                                                           UserNotAuthorizedException,
                                                                                           PropertyServerException
    {
        final String schemaTypeGUIDParameterName    = "databaseSchemaTypeGUID";
        final String qualifiedNameParameterName     = "qualifiedName";

        invalidParameterHandler.validateGUID(databaseSchemaTypeGUID, schemaTypeGUIDParameterName, methodName);

        /*
         * A database table is represented as a schemaAttribute of type RelationalTable (or a subtype).
         * Check that the type name requested is valid.
         */
        String attributeTypeName = OpenMetadataType.RELATIONAL_TABLE.typeName;
        String attributeTypeId   = OpenMetadataType.RELATIONAL_TABLE.typeGUID;

        if (typeName != null)
        {
            attributeTypeName = typeName;
            attributeTypeId   = invalidParameterHandler.validateTypeName(typeName,
                                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                         serviceName,
                                                                         methodName,
                                                                         repositoryHelper);
        }

        /*
         * Load up the builder objects for processing by the databaseTableHandler.  The builders manage the properties
         * of the metadata elements that make up the database table, and the schemaTypeHandler manages the elements themselves.
         */
        SchemaAttributeBuilder schemaAttributeBuilder = new SchemaAttributeBuilder(qualifiedName,
                                                                                   displayName,
                                                                                   description,
                                                                                   0,
                                                                                   1,
                                                                                   1,
                                                                                   isDeprecated,
                                                                                   null,
                                                                                   true,
                                                                                   false,
                                                                                   0,
                                                                                   0,
                                                                                   0,
                                                                                   0,
                                                                                   false,
                                                                                   null,
                                                                                   aliases,
                                                                                   additionalProperties,
                                                                                   attributeTypeId,
                                                                                   attributeTypeName,
                                                                                   extendedProperties,
                                                                                   repositoryHelper,
                                                                                   serviceName,
                                                                                   serverName);


        SchemaTypeBuilder schemaTypeBuilder = new SchemaTypeBuilder(qualifiedName + ":tableType",
                                                                    OpenMetadataType.RELATIONAL_TABLE_TYPE.typeGUID,
                                                                    OpenMetadataType.RELATIONAL_TABLE_TYPE.typeName,
                                                                    repositoryHelper,
                                                                    serviceName,
                                                                    serverName);

        schemaAttributeBuilder.setSchemaType(userId, schemaTypeBuilder, methodName);

        databaseTableHandler.setUpAnchorsClassificationFromParent(userId,
                                                                  databaseSchemaTypeGUID,
                                                                  schemaTypeGUIDParameterName,
                                                                  schemaAttributeBuilder,
                                                                  false,
                                                                  false,
                                                                  effectiveTime,
                                                                  methodName);

        schemaAttributeBuilder.setEffectivityDates(effectiveFrom, effectiveTo);

        String databaseTableGUID = databaseTableHandler.createNestedSchemaAttribute(userId,
                                                                                    databaseManagerGUID,
                                                                                    databaseManagerName,
                                                                                    databaseSchemaTypeGUID,
                                                                                    schemaTypeGUIDParameterName,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                    OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeGUID,
                                                                                    OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeName,
                                                                                    qualifiedName,
                                                                                    qualifiedNameParameterName,
                                                                                    schemaAttributeBuilder,
                                                                                    effectiveFrom,
                                                                                    effectiveTo,
                                                                                    forLineage,
                                                                                    forDuplicateProcessing,
                                                                                    effectiveTime,
                                                                                    methodName);

        if (databaseTableGUID != null)
        {
            databaseHandler.setVendorProperties(userId,
                                                databaseTableGUID,
                                                vendorProperties,
                                                forLineage,
                                                forDuplicateProcessing,
                                                effectiveTime,
                                                methodName);
        }

        return databaseTableGUID;
    }


    /**
     * Create a new metadata element to represent a database table using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS - if null a local element is created
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param templateGUID unique identifier of the metadata element to copy
     * @param databaseAssetGUID unique identifier of the database or database schema where the database table is located.
     * @param qualifiedName unique name for the database schema
     * @param technicalName the stored name property for the database table
     * @param description the stored description property associated with the database table
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new database schema
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseTableFromTemplate(String  userId,
                                                  String  databaseManagerGUID,
                                                  String  databaseManagerName,
                                                  String  templateGUID,
                                                  String  databaseAssetGUID,
                                                  String  qualifiedName,
                                                  String  technicalName,
                                                  String  description,
                                                  Date    effectiveFrom,
                                                  Date    effectiveTo,
                                                  boolean forLineage,
                                                  boolean forDuplicateProcessing,
                                                  Date    effectiveTime,
                                                  String  methodName) throws InvalidParameterException,
                                                                             UserNotAuthorizedException,
                                                                             PropertyServerException
    {
        final String guidParameterName = "databaseAssetGUID";
        final String parentElementGUIDParameterName = "databaseAssetGUID";
        final String templateParameterName = "templateGUID";
        final String qualifiedNameParameterName = "qualifiedName";
        final String schemaTypeGUIDParameterName    = "databaseSchemaTypeGUID";

        invalidParameterHandler.validateGUID(databaseAssetGUID, guidParameterName, methodName);
        invalidParameterHandler.validateGUID(templateGUID, templateParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        /*
         * If the deployed database schema (which is an asset) is new, it will not have a schema type attached.
         * However, if there are other tables already attached, the schema type will be there too.
         */
        String databaseSchemaTypeGUID = databaseTableHandler.getAssetSchemaTypeGUID(userId,
                                                                                    databaseManagerGUID,
                                                                                    databaseManagerName,
                                                                                    databaseAssetGUID,
                                                                                    parentElementGUIDParameterName,
                                                                                    OpenMetadataType.ASSET.typeName,
                                                                                    databaseAssetGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                    effectiveFrom,
                                                                                    effectiveTo,
                                                                                    forLineage,
                                                                                    forDuplicateProcessing,
                                                                                    effectiveTime,
                                                                                    methodName);

        if (databaseSchemaTypeGUID != null)
        {
            SchemaAttributeBuilder builder = new SchemaAttributeBuilder(qualifiedName,
                                                                        technicalName,
                                                                        description,
                                                                        repositoryHelper,
                                                                        serviceName,
                                                                        serverName);

            databaseTableHandler.setUpAnchorsClassificationFromParent(userId,
                                                                      databaseSchemaTypeGUID,
                                                                      schemaTypeGUIDParameterName,
                                                                      builder,
                                                                      false,
                                                                      false,
                                                                      effectiveTime,
                                                                      methodName);

            String databaseTableGUID = databaseTableHandler.createBeanFromTemplate(userId,
                                                                                   databaseManagerGUID,
                                                                                   databaseManagerName,
                                                                                   templateGUID,
                                                                                   templateParameterName,
                                                                                   OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                                                   OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                                   qualifiedName,
                                                                                   qualifiedNameParameterName,
                                                                                   builder,
                                                                                   databaseTableHandler.getSupportedZones(),
                                                                                   true,
                                                                                   false,
                                                                                   null,
                                                                                   methodName);

            if (databaseTableGUID != null)
            {
                final String databaseTableGUIDParameterName = "databaseTableGUID";

                databaseTableHandler.linkElementToElement(userId,
                                                          databaseManagerGUID,
                                                          databaseManagerName,
                                                          databaseSchemaTypeGUID,
                                                          schemaTypeGUIDParameterName,
                                                          OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                          databaseTableGUID,
                                                          databaseTableGUIDParameterName,
                                                          OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                          forLineage,
                                                          forDuplicateProcessing,
                                                          OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeGUID,
                                                          OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeName,
                                                          null,
                                                          effectiveFrom,
                                                          effectiveTo,
                                                          effectiveTime,
                                                          methodName);
                return databaseTableGUID;
            }
        }

        return null;
    }


    /**
     * Update the metadata element representing a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseTableGUID unique identifier of the database table to update
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate should the new properties be merged with existing properties (true) or completely replace them (false)?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateDatabaseTable(String               userId,
                                    String               databaseManagerGUID,
                                    String               databaseManagerName,
                                    String               databaseTableGUID,
                                    String               qualifiedName,
                                    String               displayName,
                                    String               description,
                                    boolean              isDeprecated,
                                    List<String>         aliases,
                                    Map<String, String>  additionalProperties,
                                    String               typeName,
                                    Map<String, Object>  extendedProperties,
                                    Map<String, String>  vendorProperties,
                                    Date                 effectiveFrom,
                                    Date                 effectiveTo,
                                    boolean              isMergeUpdate,
                                    boolean              forLineage,
                                    boolean              forDuplicateProcessing,
                                    Date                 effectiveTime,
                                    String               methodName) throws InvalidParameterException,
                                                                            UserNotAuthorizedException,
                                                                            PropertyServerException
    {
        final String elementGUIDParameterName   = "databaseTableGUID";
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseTableGUID, elementGUIDParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        /*
         * A database table is represented as a schemaAttribute of type RelationalTable (or a subtype).
         * Check that the type name requested is valid.
         */
        String attributeTypeName = OpenMetadataType.RELATIONAL_TABLE.typeName;
        String attributeTypeId   = OpenMetadataType.RELATIONAL_TABLE.typeGUID;

        if (typeName != null)
        {
            attributeTypeName = typeName;
            attributeTypeId   = invalidParameterHandler.validateTypeName(typeName,
                                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                         serviceName,
                                                                         methodName,
                                                                         repositoryHelper);
        }

        InstanceProperties properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                     null,
                                                                                     OpenMetadataProperty.QUALIFIED_NAME.name,
                                                                                     qualifiedName,
                                                                                     methodName);

        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.DISPLAY_NAME.name,
                                                                  displayName,
                                                                  methodName);

        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.DESCRIPTION.name,
                                                                  description,
                                                                  methodName);

        properties = repositoryHelper.addBooleanPropertyToInstance(serviceName,
                                                                   properties,
                                                                   OpenMetadataProperty.IS_DEPRECATED.name,
                                                                   isDeprecated,
                                                                   methodName);

        properties = repositoryHelper.addStringArrayPropertyToInstance(serviceName,
                                                                       properties,
                                                                       OpenMetadataProperty.ALIASES.name,
                                                                       aliases,
                                                                       methodName);


        properties = repositoryHelper.addStringMapPropertyToInstance(serviceName,
                                                                     properties,
                                                                     OpenMetadataProperty.ADDITIONAL_PROPERTIES.name,
                                                                     additionalProperties,
                                                                     methodName);

        if (extendedProperties != null)
        {
            try
            {
                properties = repositoryHelper.addPropertyMapToInstance(serviceName,
                                                                       null,
                                                                       extendedProperties,
                                                                       methodName);
            }
            catch (org.odpi.openmetadata.repositoryservices.ffdc.exception.InvalidParameterException error)
            {
                final String  propertyName = "extendedProperties";

                errorHandler.handleUnsupportedProperty(error, methodName, propertyName);
            }
        }

        databaseTableHandler.updateBeanInRepository(userId,
                                                    databaseManagerGUID,
                                                    databaseManagerName,
                                                    databaseTableGUID,
                                                    elementGUIDParameterName,
                                                    attributeTypeId,
                                                    attributeTypeName,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    databaseTableHandler.setUpEffectiveDates(properties, effectiveFrom, effectiveTo),
                                                    isMergeUpdate,
                                                    effectiveTime,
                                                    methodName);

        databaseHandler.setVendorProperties(userId,
                                            databaseTableGUID,
                                            vendorProperties,
                                            forLineage,
                                            forDuplicateProcessing,
                                            effectiveTime,
                                            methodName);
    }


    /**
     * Remove the metadata element representing a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseTableGUID unique identifier of the metadata element to remove
     * @param databaseTableGUIDParameterName name of parameter supplying databaseTableGUID
     * @param qualifiedName unique name of the metadata element to remove
     * @param cascadedDelete     boolean indicating whether the delete request can cascade to dependent elements
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeDatabaseTable(String  userId,
                                    String  databaseManagerGUID,
                                    String  databaseManagerName,
                                    String  databaseTableGUID,
                                    String  databaseTableGUIDParameterName,
                                    String  qualifiedName,
                                    boolean cascadedDelete,
                                    boolean forLineage,
                                    boolean forDuplicateProcessing,
                                    Date    effectiveTime,
                                    String  methodName) throws InvalidParameterException,
                                                               UserNotAuthorizedException,
                                                               PropertyServerException
    {
        if (qualifiedName != null)
        {
            databaseTableHandler.deleteBeanInRepository(userId,
                                                        databaseManagerGUID,
                                                        databaseManagerName,
                                                        databaseTableGUID,
                                                        databaseTableGUIDParameterName,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                        cascadedDelete,
                                                        OpenMetadataProperty.QUALIFIED_NAME.name,
                                                        qualifiedName,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        effectiveTime,
                                                        methodName);
        }
        else
        {
            databaseTableHandler.deleteBeanInRepository(userId,
                                                        databaseManagerGUID,
                                                        databaseManagerName,
                                                        databaseTableGUID,
                                                        databaseTableGUIDParameterName,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                        cascadedDelete,
                                                        null,
                                                        null,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        effectiveTime,
                                                        methodName);
        }
    }


    /**
     * Retrieve the list of database table metadata elements that contain the search string.
     * The search string is treated as a regular expression.
     *
     * @param userId calling user
     * @param searchString string to find in the properties
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_TABLE>   findDatabaseTables(String  userId,
                                                     String  searchString,
                                                     int     startFrom,
                                                     int     pageSize,
                                                     boolean forLineage,
                                                     boolean forDuplicateProcessing,
                                                     Date    effectiveTime,
                                                     String  methodName) throws InvalidParameterException,
                                                                                UserNotAuthorizedException,
                                                                                PropertyServerException
    {
        final String searchStringParameterName = "searchString";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateSearchString(searchString, searchStringParameterName, methodName);

        return databaseTableHandler.findSchemaAttributes(userId,
                                                         searchString,
                                                         searchStringParameterName,
                                                         OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                         null,
                                                         OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                         startFrom,
                                                         pageSize,
                                                         forLineage,
                                                         forDuplicateProcessing,
                                                         effectiveTime,
                                                         methodName);
    }


    /**
     * Retrieve the list of database tables associated with a database schema.
     *
     * @param userId calling user
     * @param databaseAssetGUID unique identifier of the database or database schema of interest
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of associated metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_TABLE> getTablesForDatabaseAsset(String  userId,
                                                          String  databaseAssetGUID,
                                                          int     startFrom,
                                                          int     pageSize,
                                                          Date    effectiveFrom,
                                                          Date    effectiveTo,
                                                          boolean forLineage,
                                                          boolean forDuplicateProcessing,
                                                          Date    effectiveTime,
                                                          String  methodName) throws InvalidParameterException,
                                                                                     UserNotAuthorizedException,
                                                                                     PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseAssetGUID";

        /*
         * If the deployed database schema (which is an asset) is new, it will not have a schema type attached.
         * However, if there are other tables already attached, the schema type will be there too.
         */
        String databaseSchemaTypeGUID = databaseTableHandler.getAssetSchemaTypeGUID(userId,
                                                                                    null,
                                                                                    null,
                                                                                    databaseAssetGUID,
                                                                                    parentElementGUIDParameterName,
                                                                                    OpenMetadataType.ASSET.typeName,
                                                                                    databaseAssetGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                                                    OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                    effectiveFrom,
                                                                                    effectiveTo,
                                                                                    forLineage,
                                                                                    forDuplicateProcessing,
                                                                                    effectiveTime,
                                                                                    methodName);

        if (databaseSchemaTypeGUID != null)
        {
            final String databaseSchemaTypeGUIDParameterName = "databaseSchemaTypeGUID";

            return databaseTableHandler.getSchemaAttributesForComplexSchemaType(userId,
                                                                                databaseSchemaTypeGUID,
                                                                                databaseSchemaTypeGUIDParameterName,
                                                                                null,
                                                                                OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                                                startFrom,
                                                                                pageSize,
                                                                                forLineage,
                                                                                forDuplicateProcessing,
                                                                                effectiveTime,
                                                                                methodName);
        }

        return null;
    }


    /**
     * Retrieve the list of database table metadata elements with a matching qualified or display name.
     * There are no wildcards supported on this request.
     *
     * @param userId calling user
     * @param name name to search for
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_TABLE> getDatabaseTablesByName(String  userId,
                                                        String  name,
                                                        int     startFrom,
                                                        int     pageSize,
                                                        boolean forLineage,
                                                        boolean forDuplicateProcessing,
                                                        Date    effectiveTime,
                                                        String  methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        return databaseTableHandler.getSchemaAttributesByName(userId,
                                                              OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                              OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                              name,
                                                              null,
                                                              OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                              startFrom,
                                                              pageSize,
                                                              forLineage,
                                                              forDuplicateProcessing,
                                                              effectiveTime,
                                                              methodName);
    }


    /**
     * Retrieve the database table metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param guid unique identifier of the requested metadata element
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return matching metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public DATABASE_TABLE getDatabaseTableByGUID(String  userId,
                                                 String  guid,
                                                 boolean forLineage,
                                                 boolean forDuplicateProcessing,
                                                 Date    effectiveTime,
                                                 String  methodName) throws InvalidParameterException,
                                                                            UserNotAuthorizedException,
                                                                            PropertyServerException
    {
        final String guidParameterName = "guid";

        return databaseTableHandler.getSchemaAttribute(userId,
                                                       guid,
                                                       guidParameterName,
                                                       OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                       null,
                                                       null,
                                                       forLineage,
                                                       forDuplicateProcessing,
                                                       effectiveTime,
                                                       methodName);
    }


    /**
     * Create a new metadata element to represent a database view.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseAssetGUID unique identifier of the database or database schema where the database view is located.
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param expression the code that generates the value for this view.
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database view
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseView(String               userId,
                                     String               databaseManagerGUID,
                                     String               databaseManagerName,
                                     String               databaseAssetGUID,
                                     String               qualifiedName,
                                     String               displayName,
                                     String               description,
                                     boolean              isDeprecated,
                                     List<String>         aliases,
                                     String               expression,
                                     Map<String, String>  additionalProperties,
                                     String               typeName,
                                     Map<String, Object>  extendedProperties,
                                     Map<String, String>  vendorProperties,
                                     Date                 effectiveFrom,
                                     Date                 effectiveTo,
                                     boolean              forLineage,
                                     boolean              forDuplicateProcessing,
                                     Date                 effectiveTime,
                                     String               methodName) throws InvalidParameterException,
                                                                             UserNotAuthorizedException,
                                                                             PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseAssetGUID";

        String databaseSchemaTypeGUID = databaseViewHandler.getAssetSchemaTypeGUID(userId,
                                                                                   databaseManagerGUID,
                                                                                   databaseManagerName,
                                                                                   databaseAssetGUID,
                                                                                   parentElementGUIDParameterName,
                                                                                   OpenMetadataType.ASSET.typeName,
                                                                                   databaseAssetGUID,
                                                                                   OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                                                   OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                   effectiveFrom,
                                                                                   effectiveTo,
                                                                                   forLineage,
                                                                                   forDuplicateProcessing,
                                                                                   effectiveTime,
                                                                                   methodName);

        return createDatabaseViewForSchemaType(userId,
                                               databaseManagerGUID,
                                               databaseManagerName,
                                               databaseSchemaTypeGUID,
                                               qualifiedName,
                                               displayName,
                                               description,
                                               isDeprecated,
                                               aliases,
                                               expression,
                                               additionalProperties,
                                               typeName,
                                               extendedProperties,
                                               vendorProperties,
                                               effectiveFrom,
                                               effectiveTo,
                                               forDuplicateProcessing,
                                               forLineage,
                                               effectiveTime,
                                               methodName);
    }


    /**
     * Create a new metadata element to represent a database view.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseSchemaTypeGUID unique identifier of the schema type where the database view is located.
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param expression the code that generates the value for this view.
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database view
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseViewForSchemaType(String               userId,
                                                  String               databaseManagerGUID,
                                                  String               databaseManagerName,
                                                  String               databaseSchemaTypeGUID,
                                                  String               qualifiedName,
                                                  String               displayName,
                                                  String               description,
                                                  boolean              isDeprecated,
                                                  List<String>         aliases,
                                                  String               expression,
                                                  Map<String, String>  additionalProperties,
                                                  String               typeName,
                                                  Map<String, Object>  extendedProperties,
                                                  Map<String, String>  vendorProperties,
                                                  Date                 effectiveFrom,
                                                  Date                 effectiveTo,
                                                  boolean              forLineage,
                                                  boolean              forDuplicateProcessing,
                                                  Date                 effectiveTime,
                                                  String               methodName) throws InvalidParameterException,
                                                                                          UserNotAuthorizedException,
                                                                                          PropertyServerException
    {
        final String schemaTypeGUIDParameterName = "databaseSchemaTypeGUID";
        final String qualifiedNameParameterName  = "qualifiedName";

        invalidParameterHandler.validateGUID(databaseSchemaTypeGUID, schemaTypeGUIDParameterName, methodName);

        /*
         * A database view is represented as a schemaAttribute of type RelationalTable (or a subtype).
         * Check that the type name requested is valid.
         */
        String attributeTypeName = OpenMetadataType.RELATIONAL_TABLE.typeName;
        String attributeTypeId   = OpenMetadataType.RELATIONAL_TABLE.typeGUID;

        if (typeName != null)
        {
            attributeTypeName = typeName;
            attributeTypeId   = invalidParameterHandler.validateTypeName(typeName,
                                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                         serviceName,
                                                                         methodName,
                                                                         repositoryHelper);
        }

        /*
         * The schema type that connects the database schema asset to the tables has been created/retrieved.
         * Now work out the position of the new table in the database schema type.  This is used to set the element position.
         * Since this value begins with 0 as the first element, the table count is this table's position.
         */
        int tableCount = databaseViewHandler.countSchemaAttributes(userId,
                                                                   databaseSchemaTypeGUID,
                                                                   schemaTypeGUIDParameterName,
                                                                   effectiveTime,
                                                                   methodName);

        /*
         * Load up the builder objects for processing by the databaseTableHandler.  The builders manage the properties
         * of the metadata elements that make up the database table, and the schemaTypeHandler manages the elements themselves.
         */
        SchemaAttributeBuilder schemaAttributeBuilder = new SchemaAttributeBuilder(qualifiedName,
                                                                                   displayName,
                                                                                   description,
                                                                                   tableCount,
                                                                                   1,
                                                                                   1,
                                                                                   isDeprecated,
                                                                                   null,
                                                                                   true,
                                                                                   false,
                                                                                   0,
                                                                                   0,
                                                                                   0,
                                                                                   0,
                                                                                   false,
                                                                                   null,
                                                                                   aliases,
                                                                                   additionalProperties,
                                                                                   attributeTypeId,
                                                                                   attributeTypeName,
                                                                                   extendedProperties,
                                                                                   repositoryHelper,
                                                                                   serviceName,
                                                                                   serverName);

        SchemaTypeBuilder schemaTypeBuilder = new SchemaTypeBuilder(qualifiedName + ":viewType",
                                                                    OpenMetadataType.RELATIONAL_TABLE_TYPE.typeGUID,
                                                                    OpenMetadataType.RELATIONAL_TABLE_TYPE.typeName,
                                                                    repositoryHelper,
                                                                    serviceName,
                                                                    serverName);

        schemaAttributeBuilder.setSchemaType(userId, schemaTypeBuilder, methodName);

        databaseTableHandler.setUpAnchorsClassificationFromParent(userId,
                                                                  databaseSchemaTypeGUID,
                                                                  schemaTypeGUIDParameterName,
                                                                  schemaAttributeBuilder,
                                                                  false,
                                                                  false,
                                                                  effectiveTime,
                                                                  methodName);

        schemaAttributeBuilder.setEffectivityDates(effectiveFrom, effectiveTo);
        schemaAttributeBuilder.setCalculatedValue(userId, databaseManagerGUID, databaseManagerName, expression, methodName);

        /*
         * Now create the table itself along with its schema type.  It also links the resulting table to the database schema type.
         * The returned value is the guid of the table.
         */
        String databaseViewGUID = databaseViewHandler.createNestedSchemaAttribute(userId,
                                                                                  databaseManagerGUID,
                                                                                  databaseManagerName,
                                                                                  databaseSchemaTypeGUID,
                                                                                  schemaTypeGUIDParameterName,
                                                                                  OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                  OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeGUID,
                                                                                  OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeName,
                                                                                  qualifiedName,
                                                                                  qualifiedNameParameterName,
                                                                                  schemaAttributeBuilder,
                                                                                  effectiveFrom,
                                                                                  effectiveTo,
                                                                                  forLineage,
                                                                                  forDuplicateProcessing,
                                                                                  effectiveTime,
                                                                                  methodName);

        if (databaseViewGUID != null)
        {
            databaseViewHandler.setVendorProperties(userId,
                                                    databaseViewGUID,
                                                    vendorProperties,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);

        }

        return databaseViewGUID;
    }


    /**
     * Create a new metadata element to represent a database view using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param templateGUID unique identifier of the metadata element to copy
     * @param databaseAssetGUID unique identifier of the database or database schema where the database view is located.
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database view
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseViewFromTemplate(String  userId,
                                                 String  databaseManagerGUID,
                                                 String  databaseManagerName,
                                                 String  templateGUID,
                                                 String  databaseAssetGUID,
                                                 String  qualifiedName,
                                                 String  displayName,
                                                 String  description,
                                                 Date    effectiveFrom,
                                                 Date    effectiveTo,
                                                 boolean forLineage,
                                                 boolean forDuplicateProcessing,
                                                 Date    effectiveTime,
                                                 String  methodName) throws InvalidParameterException,
                                                                            UserNotAuthorizedException,
                                                                            PropertyServerException
    {
        return this.createDatabaseTableFromTemplate(userId,
                                                    databaseManagerGUID,
                                                    databaseManagerName,
                                                    templateGUID,
                                                    databaseAssetGUID,
                                                    qualifiedName,
                                                    displayName,
                                                    description,
                                                    effectiveFrom,
                                                    effectiveTo,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);
    }


    /**
     * Update the metadata element representing a database table.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseViewGUID unique identifier of the database view to update
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param isDeprecated is this table deprecated?
     * @param aliases a list of alternative names for the attribute
     * @param expression the code that generates the value for this view.
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate should the new properties be merged with existing properties (true) or completely replace them (false)?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateDatabaseView(String               userId,
                                   String               databaseManagerGUID,
                                   String               databaseManagerName,
                                   String               databaseViewGUID,
                                   String               qualifiedName,
                                   String               displayName,
                                   String               description,
                                   boolean              isDeprecated,
                                   List<String>         aliases,
                                   String               expression,
                                   Map<String, String>  additionalProperties,
                                   String               typeName,
                                   Map<String, Object>  extendedProperties,
                                   Map<String, String>  vendorProperties,
                                   Date                 effectiveFrom,
                                   Date                 effectiveTo,
                                   boolean              isMergeUpdate,
                                   boolean              forLineage,
                                   boolean              forDuplicateProcessing,
                                   Date                 effectiveTime,
                                   String               methodName) throws InvalidParameterException,
                                                                           UserNotAuthorizedException,
                                                                           PropertyServerException
    {
        final String elementGUIDParameterName   = "databaseViewGUID";
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseViewGUID, elementGUIDParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        String  expectedTypeName = OpenMetadataType.RELATIONAL_TABLE.typeName;
        if (typeName != null)
        {
            expectedTypeName = typeName;
        }

        this.updateDatabaseTable(userId,
                                 databaseManagerGUID,
                                 databaseManagerName,
                                 databaseViewGUID,
                                 qualifiedName,
                                 displayName,
                                 description,
                                 isDeprecated,
                                 aliases,
                                 additionalProperties,
                                 typeName,
                                 extendedProperties,
                                 vendorProperties,
                                 effectiveFrom,
                                 effectiveTo,
                                 isMergeUpdate,
                                 forLineage,
                                 forDuplicateProcessing,
                                 effectiveTime,
                                 methodName);

        InstanceProperties properties = null;
        if (expression != null)
        {
            properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                      null,
                                                                      OpenMetadataProperty.FORMULA.name,
                                                                      expression,
                                                                      methodName);
        }

        databaseViewHandler.setClassificationInRepository(userId,
                                                          databaseManagerGUID,
                                                          databaseManagerName,
                                                          databaseViewGUID,
                                                          elementGUIDParameterName,
                                                          expectedTypeName,
                                                          OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeGUID,
                                                          OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                          properties,
                                                          isMergeUpdate,
                                                          forLineage,
                                                          forDuplicateProcessing,
                                                          effectiveTime,
                                                          methodName);
    }


    /**
     * Retrieve the list of database view metadata elements that contain the search string.
     * The search string is treated as a regular expression.
     *
     * @param userId calling user
     * @param searchString string to find in the properties
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_VIEW> findDatabaseViews(String  userId,
                                                 String  searchString,
                                                 int     startFrom,
                                                 int     pageSize,
                                                 boolean forLineage,
                                                 boolean forDuplicateProcessing,
                                                 Date    effectiveTime,
                                                 String  methodName) throws InvalidParameterException,
                                                                            UserNotAuthorizedException,
                                                                            PropertyServerException
    {
        final String searchStringParameterName = "searchString";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateSearchString(searchString, searchStringParameterName, methodName);

        return databaseViewHandler.findSchemaAttributes(userId,
                                                        searchString,
                                                        searchStringParameterName,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                        OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                        null,
                                                        startFrom,
                                                        pageSize,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        effectiveTime,
                                                        methodName);
    }


    /**
     * Retrieve the list of database views associated with a database or database schema.
     *
     * @param userId calling user
     * @param databaseAssetGUID unique identifier of the database or database schema of interest
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of associated metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_VIEW> getViewsForDatabaseAsset(String  userId,
                                                        String  databaseAssetGUID,
                                                        int     startFrom,
                                                        int     pageSize,
                                                        Date    effectiveFrom,
                                                        Date    effectiveTo,
                                                        boolean forLineage,
                                                        boolean forDuplicateProcessing,
                                                        Date    effectiveTime,
                                                        String  methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseAssetGUID";

        /*
         * If the deployed database/schema (which is an asset) is new, it will not have a schema type attached.
         * However, if there are other tables already attached, the schema type will be there too.
         */
        String databaseSchemaTypeGUID = databaseViewHandler.getAssetSchemaTypeGUID(userId,
                                                                                   null,
                                                                                   null,
                                                                                   databaseAssetGUID,
                                                                                   parentElementGUIDParameterName,
                                                                                   OpenMetadataType.ASSET.typeName,
                                                                                   databaseAssetGUID,
                                                                                   OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeGUID,
                                                                                   OpenMetadataType.RELATIONAL_DB_SCHEMA_TYPE.typeName,
                                                                                   effectiveFrom,
                                                                                   effectiveTo,
                                                                                   forLineage,
                                                                                   forDuplicateProcessing,
                                                                                   effectiveTime,
                                                                                   methodName);

        if (databaseSchemaTypeGUID != null)
        {
            final String databaseSchemaTypeGUIDParameterName = "databaseSchemaTypeGUID";

            return databaseViewHandler.getSchemaAttributesForComplexSchemaType(userId,
                                                                               databaseSchemaTypeGUID,
                                                                               databaseSchemaTypeGUIDParameterName,
                                                                               OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                                               null,
                                                                               startFrom,
                                                                               pageSize,
                                                                               forLineage,
                                                                               forDuplicateProcessing,
                                                                               effectiveTime,
                                                                               methodName);
        }

        return null;
    }


    /**
     * Retrieve the list of database view metadata elements with a matching qualified or display name.
     * There are no wildcards supported on this request.
     *
     * @param userId calling user
     * @param name name to search for
     * @param nameParameterName name of the search name parameter
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_VIEW>   getDatabaseViewsByName(String  userId,
                                                        String  name,
                                                        String  nameParameterName,
                                                        int     startFrom,
                                                        int     pageSize,
                                                        boolean forLineage,
                                                        boolean forDuplicateProcessing,
                                                        Date    effectiveTime,
                                                        String  methodName) throws InvalidParameterException,
                                                                                   UserNotAuthorizedException,
                                                                                   PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(name, nameParameterName, methodName);

        return databaseViewHandler.getSchemaAttributesByName(userId,
                                                             OpenMetadataType.RELATIONAL_TABLE.typeGUID,
                                                             OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                             name,
                                                             OpenMetadataType.CALCULATED_VALUE_CLASSIFICATION.typeName,
                                                             null,
                                                             startFrom,
                                                             pageSize,
                                                             forLineage,
                                                             forDuplicateProcessing,
                                                             effectiveTime,
                                                             methodName);
    }


    /**
     * Retrieve the database view metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param guid unique identifier of the requested metadata element
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return matching metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public DATABASE_VIEW getDatabaseViewByGUID(String  userId,
                                               String  guid,
                                               boolean forLineage,
                                               boolean forDuplicateProcessing,
                                               Date    effectiveTime,
                                               String  methodName) throws InvalidParameterException,
                                                                          UserNotAuthorizedException,
                                                                          PropertyServerException
    {
        final String guidParameterName = "guid";

        return databaseViewHandler.getSchemaAttribute(userId,
                                                      guid,
                                                      guidParameterName,
                                                      OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                      null,
                                                      null,
                                                      forLineage,
                                                      forDuplicateProcessing,
                                                      effectiveTime,
                                                      methodName);
    }


    /* ==============================================================================================
     * Database tables and views have columns.  They are either directly stored or derived from other
     * values.
     */

    /**
     * Create a new metadata element to represent a database column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseTableGUID unique identifier of the database table where this column is located
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param externalSchemaTypeGUID unique identifier of a schema Type that provides the type. If null, a private schema type is used
     * @param dataType data type name - for stored values
     * @param defaultValue string containing default value - for stored values
     * @param fixedValue string containing a fixed value - for a literal
     * @param validValuesSetGUID unique identifier of a valid value set that lists the valid values for this schema
     * @param formula String formula - for derived values
     * @param isDeprecated is this table deprecated?
     * @param elementPosition the position of this column in its parent table.
     * @param minCardinality minimum number of repeating instances allowed for this column - typically 1
     * @param maxCardinality the maximum number of repeating instances allowed for this column - typically 1
     * @param allowsDuplicateValues  whether the same value can be used by more than one instance of this attribute
     * @param orderedValues whether the attribute instances are arranged in an order
     * @param sortOrder the order that the attribute instances are arranged in - if any
     * @param minimumLength the minimum length of the data
     * @param length the length of the data field
     * @param significantDigits number of significant digits to the right of decimal point
     * @param isNullable whether the field is nullable or not
     * @param nativeJavaClass equivalent Java class implementation
     * @param defaultValueOverride default value for this column
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database column
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    @Deprecated
    public String createDatabaseColumn(String               userId,
                                       String               databaseManagerGUID,
                                       String               databaseManagerName,
                                       String               databaseTableGUID,
                                       String               qualifiedName,
                                       String               displayName,
                                       String               description,
                                       String               externalSchemaTypeGUID,
                                       String               dataType,
                                       String               defaultValue,
                                       String               fixedValue,
                                       String               validValuesSetGUID,
                                       String               formula,
                                       boolean              isDeprecated,
                                       int                  elementPosition,
                                       int                  minCardinality,
                                       int                  maxCardinality,
                                       boolean              allowsDuplicateValues,
                                       boolean              orderedValues,
                                       String               defaultValueOverride,
                                       int                  sortOrder,
                                       int                  minimumLength,
                                       int                  length,
                                       int                  significantDigits,
                                       boolean              isNullable,
                                       String               nativeJavaClass,
                                       List<String>         aliases,
                                       Map<String, String>  additionalProperties,
                                       String               typeName,
                                       Map<String, Object>  extendedProperties,
                                       Map<String, String>  vendorProperties,
                                       String               methodName) throws InvalidParameterException,
                                                                               UserNotAuthorizedException,
                                                                               PropertyServerException
    {
        return this.createDatabaseColumn(userId,
                                         databaseManagerGUID,
                                         databaseManagerName,
                                         databaseTableGUID,
                                         qualifiedName,
                                         displayName,
                                         description,
                                         externalSchemaTypeGUID,
                                         dataType,
                                         defaultValue,
                                         fixedValue,
                                         validValuesSetGUID,
                                         formula,
                                         isDeprecated,
                                         elementPosition,
                                         minCardinality,
                                         maxCardinality,
                                         allowsDuplicateValues,
                                         orderedValues,
                                         defaultValueOverride,
                                         sortOrder,
                                         minimumLength,
                                         length,
                                         significantDigits,
                                         isNullable,
                                         nativeJavaClass,
                                         aliases,
                                         additionalProperties,
                                         typeName,
                                         extendedProperties,
                                         vendorProperties,
                                         null,
                                         null,
                                         false,
                                         false,
                                         new Date(),
                                         methodName);
    }

    /**
     * Create a new metadata element to represent a database column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseTableGUID unique identifier of the database table where this column is located
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param externalSchemaTypeGUID unique identifier of a schema Type that provides the type. If null, a private schema type is used
     * @param dataType data type name - for stored values
     * @param defaultValue string containing default value - for stored values
     * @param fixedValue string containing a fixed value - for a literal
     * @param validValuesSetGUID unique identifier of a valid value set that lists the valid values for this schema
     * @param formula String formula - for derived values
     * @param isDeprecated is this table deprecated?
     * @param elementPosition the position of this column in its parent table.
     * @param minCardinality minimum number of repeating instances allowed for this column - typically 1
     * @param maxCardinality the maximum number of repeating instances allowed for this column - typically 1
     * @param allowsDuplicateValues  whether the same value can be used by more than one instance of this attribute
     * @param orderedValues whether the attribute instances are arranged in an order
     * @param sortOrder the order that the attribute instances are arranged in - if any
     * @param minimumLength the minimum length of the data
     * @param length the length of the data field
     * @param significantDigits number of significant digits to the right of decimal point
     * @param isNullable whether the field is nullable or not
     * @param nativeJavaClass equivalent Java class implementation
     * @param defaultValueOverride default value for this column
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database column
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseColumn(String               userId,
                                       String               databaseManagerGUID,
                                       String               databaseManagerName,
                                       String               databaseTableGUID,
                                       String               qualifiedName,
                                       String               displayName,
                                       String               description,
                                       String               externalSchemaTypeGUID,
                                       String               dataType,
                                       String               defaultValue,
                                       String               fixedValue,
                                       String               validValuesSetGUID,
                                       String               formula,
                                       boolean              isDeprecated,
                                       int                  elementPosition,
                                       int                  minCardinality,
                                       int                  maxCardinality,
                                       boolean              allowsDuplicateValues,
                                       boolean              orderedValues,
                                       String               defaultValueOverride,
                                       int                  sortOrder,
                                       int                  minimumLength,
                                       int                  length,
                                       int                  significantDigits,
                                       boolean              isNullable,
                                       String               nativeJavaClass,
                                       List<String>         aliases,
                                       Map<String, String>  additionalProperties,
                                       String               typeName,
                                       Map<String, Object>  extendedProperties,
                                       Map<String, String>  vendorProperties,
                                       Date                 effectiveFrom,
                                       Date                 effectiveTo,
                                       boolean              forLineage,
                                       boolean              forDuplicateProcessing,
                                       Date                 effectiveTime,
                                       String               methodName) throws InvalidParameterException,
                                                                               UserNotAuthorizedException,
                                                                               PropertyServerException
    {
        final String databaseTableGUIDParameterName = "databaseTableGUID";
        final String qualifiedNameParameterName     = "qualifiedName";
        final String dataTypeParameterName     = "dataType";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseTableGUID, databaseTableGUIDParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);

        /*
         * Data type needs to be set unless there is an external schema type in use.
         */
        if (externalSchemaTypeGUID == null)
        {
            invalidParameterHandler.validateName(dataType, dataTypeParameterName, methodName);
        }

        /*
         * Retrieve and validate the table that this column is for
         */
        EntityDetail databaseTableEntity = databaseColumnHandler.getEntityFromRepository(userId,
                                                                                         databaseTableGUID,
                                                                                         databaseTableGUIDParameterName,
                                                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                                         null,
                                                                                         null,
                                                                                         forLineage,
                                                                                         forDuplicateProcessing,
                                                                                         effectiveTime,
                                                                                         methodName);

        if (databaseTableEntity != null)
        {
            /*
             * A database column is represented as a schemaAttribute of type RelationalColumn (or a subtype).
             * Check that the type name requested is valid.
             */
            String attributeTypeName = OpenMetadataType.RELATIONAL_COLUMN.typeName;
            String attributeTypeId   = OpenMetadataType.RELATIONAL_COLUMN.typeGUID;

            if (typeName != null)
            {
                attributeTypeName = typeName;
                attributeTypeId   = invalidParameterHandler.validateTypeName(typeName,
                                                                             OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                                             serviceName,
                                                                             methodName,
                                                                             repositoryHelper);
            }

            /*
             * Load up the builder objects for processing by the generic handler.  The builders manage the properties
             * of the metadata elements that make up the database table, and the schemaTypeHandler manages the elements themselves.
             */
            SchemaAttributeBuilder schemaAttributeBuilder = new SchemaAttributeBuilder(qualifiedName,
                                                                                       displayName,
                                                                                       description,
                                                                                       elementPosition,
                                                                                       minCardinality,
                                                                                       maxCardinality,
                                                                                       isDeprecated,
                                                                                       defaultValueOverride,
                                                                                       allowsDuplicateValues,
                                                                                       orderedValues,
                                                                                       sortOrder,
                                                                                       minimumLength,
                                                                                       length,
                                                                                       significantDigits,
                                                                                       isNullable,
                                                                                       nativeJavaClass,
                                                                                       aliases,
                                                                                       additionalProperties,
                                                                                       attributeTypeId,
                                                                                       attributeTypeName,
                                                                                       extendedProperties,
                                                                                       repositoryHelper,
                                                                                       serviceName,
                                                                                       serverName);


            /*
             * If the database table is set up with an anchor then this is propagated to the column
             */
            OpenMetadataAPIGenericHandler.AnchorIdentifiers anchorIdentifiers =
                    databaseColumnHandler.getAnchorsFromAnchorsClassification(databaseTableEntity, methodName);
            if (anchorIdentifiers != null)
            {
                schemaAttributeBuilder.setAnchors(userId,
                                                  anchorIdentifiers.anchorGUID,
                                                  anchorIdentifiers.anchorTypeName,
                                                  anchorIdentifiers.anchorDomainName,
                                                  anchorIdentifiers.anchorScopeGUID,
                                                  methodName);
            }

            /*
             * Begin by looking to see if the type information for the attribute located in an attached schema type
             */
            String parentTypeName           = OpenMetadataType.RELATIONAL_TABLE_TYPE.typeName;
            String parentAttachmentTypeGUID = OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeGUID;
            String parentAttachmentTypeName = OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeName;

            String parentGUID = databaseColumnHandler.getAttachedElementGUID(userId,
                                                                             databaseTableGUID,
                                                                             databaseTableGUIDParameterName,
                                                                             OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                             OpenMetadataType.SCHEMA_ATTRIBUTE_TYPE_RELATIONSHIP.typeGUID,
                                                                             OpenMetadataType.SCHEMA_ATTRIBUTE_TYPE_RELATIONSHIP.typeName,
                                                                             OpenMetadataType.SCHEMA_TYPE.typeName,
                                                                             2,
                                                                             null,
                                                                             null,
                                                                             SequencingOrder.CREATION_DATE_RECENT,
                                                                             null,
                                                                             forLineage,
                                                                             forDuplicateProcessing,
                                                                             effectiveTime,
                                                                             methodName);

            if (parentGUID == null)
            {
                /*
                 * The table may have its type stored as a classification, or as a linked schema type.  The column is linked to
                 * the attribute in the first case, and the schema type in the second case.
                 */
                try
                {
                    Classification typeClassification = repositoryHelper.getClassificationFromEntity(serviceName,
                                                                                                     databaseTableEntity,
                                                                                                     OpenMetadataType.TYPE_EMBEDDED_ATTRIBUTE_CLASSIFICATION.typeName,
                                                                                                     methodName);

                    if (typeClassification != null)
                    {
                        parentGUID               = databaseTableGUID;
                        parentTypeName           = OpenMetadataType.RELATIONAL_TABLE.typeName;
                        parentAttachmentTypeGUID = OpenMetadataType.NESTED_SCHEMA_ATTRIBUTE_RELATIONSHIP.typeGUID;
                        parentAttachmentTypeName = OpenMetadataType.NESTED_SCHEMA_ATTRIBUTE_RELATIONSHIP.typeName;
                    }
                }
                catch (ClassificationErrorException classificationNotKnown)
                {
                    /*
                     * Type classification not supported.
                     */
                }
            }

            if (parentGUID == null)
            {
                invalidParameterHandler.throwUnknownElement(userId,
                                                            databaseTableGUID,
                                                            OpenMetadataType.RELATIONAL_TABLE_TYPE.typeName,
                                                            serviceName,
                                                            serverName,
                                                            methodName);
                return null;
            }

            SchemaTypeBuilder schemaTypeBuilder = databaseColumnHandler.getSchemaTypeBuilder(qualifiedName,
                                                                                             externalSchemaTypeGUID,
                                                                                             dataType,
                                                                                             defaultValue,
                                                                                             fixedValue,
                                                                                             validValuesSetGUID);

            schemaAttributeBuilder.setSchemaType(userId, schemaTypeBuilder, methodName);
            schemaAttributeBuilder.setEffectivityDates(effectiveFrom, effectiveTo);

            /*
             * The formula is set if the column is derived
             */
            if (formula != null)
            {
                schemaAttributeBuilder.setCalculatedValue(userId, databaseManagerGUID, databaseManagerName, formula, methodName);
            }

            /*
             * Now create the table itself along with its schema type.  It also links the resulting table to the database schema type.
             * The returned value is the guid of the table.
             */
            String databaseColumnGUID = databaseColumnHandler.createBeanInRepository(userId,
                                                                                     databaseManagerGUID,
                                                                                     databaseManagerName,
                                                                                     OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                                                     OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                                                     schemaAttributeBuilder,
                                                                                     effectiveTime,
                                                                                     methodName);

            if (databaseColumnGUID != null)
            {
                /*
                 * link the new database column to its table
                 */
                final String databaseColumnGUIDParameterName = "databaseColumnGUID";

                databaseColumnHandler.linkElementToElement(userId,
                                                           databaseManagerGUID,
                                                           databaseManagerName,
                                                           parentGUID,
                                                           databaseTableGUIDParameterName,
                                                           parentTypeName,
                                                           databaseColumnGUID,
                                                           databaseColumnGUIDParameterName,
                                                           OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                           forLineage,
                                                           forDuplicateProcessing,
                                                           parentAttachmentTypeGUID,
                                                           parentAttachmentTypeName,
                                                           schemaAttributeBuilder.getParentRelationshipProperties(methodName),
                                                           effectiveFrom,
                                                           effectiveTo,
                                                           effectiveTime,
                                                           methodName);

                databaseColumnHandler.setVendorProperties(userId,
                                                          databaseColumnGUID,
                                                          vendorProperties,
                                                          forLineage,
                                                          forDuplicateProcessing,
                                                          effectiveTime, methodName);
                return databaseColumnGUID;
            }

            return null;
        }
        else
        {
            invalidParameterHandler.throwUnknownElement(userId,
                                                        databaseTableGUID,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                        serviceName,
                                                        serverName,
                                                        methodName);
            return null;
        }
    }




    /**
     * Create a new query relationship for a derived database column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseColumnGUID unique identifier of the database column that this query supports
     * @param queryId identifier for the query - used as a placeholder in the formula (stored in the column's CalculatedValue classification)
     * @param query the query that is made on the targetGUID
     * @param queryTargetGUID the unique identifier of the target (this is a schema element - typically a column)
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void createDatabaseColumnQuery(String               userId,
                                          String               databaseManagerGUID,
                                          String               databaseManagerName,
                                          String               databaseColumnGUID,
                                          String               queryId,
                                          String               query,
                                          String               queryTargetGUID,
                                          Date                 effectiveFrom,
                                          Date                 effectiveTo,
                                          boolean              forLineage,
                                          boolean              forDuplicateProcessing,
                                          Date                 effectiveTime,
                                          String               methodName) throws InvalidParameterException,
                                                                                  UserNotAuthorizedException,
                                                                                  PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseColumnGUID";
        final String targetElementGUIDParameterName = "queryTargetGUID";
        final String queryParameterName = "query";

        invalidParameterHandler.validateObject(query, queryParameterName, methodName);

        InstanceProperties properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                     null,
                                                                                     OpenMetadataProperty.QUERY.name,
                                                                                     query,
                                                                                     methodName);

        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.QUERY_ID.name,
                                                                  queryId,
                                                                  methodName);

        databaseColumnHandler.linkElementToElement(userId,
                                                   databaseManagerGUID,
                                                   databaseManagerName,
                                                   databaseColumnGUID,
                                                   parentElementGUIDParameterName,
                                                   OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                   queryTargetGUID,
                                                   targetElementGUIDParameterName,
                                                   OpenMetadataType.SCHEMA_ELEMENT.typeName,
                                                   forLineage,
                                                   forDuplicateProcessing,
                                                   OpenMetadataType.DERIVED_SCHEMA_TYPE_QUERY_TARGET_RELATIONSHIP.typeGUID,
                                                   OpenMetadataType.DERIVED_SCHEMA_TYPE_QUERY_TARGET_RELATIONSHIP.typeName,
                                                   properties,
                                                   effectiveFrom,
                                                   effectiveTo,
                                                   effectiveTime,
                                                   methodName);
    }


    /**
     * Create a new metadata element to represent a database column using an existing metadata element as a template.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param templateGUID unique identifier of the metadata element to copy
     * @param databaseTableGUID unique identifier of the database table where this column is located
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element for the database column
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createDatabaseColumnFromTemplate(String  userId,
                                                   String  databaseManagerGUID,
                                                   String  databaseManagerName,
                                                   String  templateGUID,
                                                   String  databaseTableGUID,
                                                   String  qualifiedName,
                                                   String  displayName,
                                                   String  description,
                                                   Date    effectiveFrom,
                                                   Date    effectiveTo,
                                                   boolean forLineage,
                                                   boolean forDuplicateProcessing,
                                                   Date    effectiveTime,
                                                   String  methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        final String templateGUIDParameterName      = "templateGUID";
        final String databaseTableGUIDParameterName = "databaseTableGUID";
        final String qualifiedNameParameterName     = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(templateGUID, templateGUIDParameterName, methodName);
        invalidParameterHandler.validateGUID(databaseTableGUID, databaseTableGUIDParameterName, methodName);
        invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);


        OpenMetadataAPIGenericHandler.AnchorIdentifiers anchorIdentifiers  = null;
        String                                          parentTypeName = null;
        String                                          parentAttachmentTypeGUID = null;
        String                                          parentAttachmentTypeName = null;
        String                                          parentGUID = null;

        /*
         * Retrieve and validate the table that this column is for
         */
        EntityDetail databaseTableEntity = databaseColumnHandler.getEntityFromRepository(userId,
                                                                                         databaseTableGUID,
                                                                                         databaseTableGUIDParameterName,
                                                                                         OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                                         null,
                                                                                         null,
                                                                                         forLineage,
                                                                                         forDuplicateProcessing,
                                                                                         effectiveTime,
                                                                                         methodName);


        if (databaseTableEntity != null)
        {
            /*
             * If the database table is set up with an anchor then this is propagated to the column
             */
            anchorIdentifiers = databaseColumnHandler.getAnchorsFromAnchorsClassification(databaseTableEntity, methodName);

            /*
             * The table may have its type stored as a classification, or as a linked schema type.  The column is linked to
             * the attribute in the first case, and the schema type in the second case.
             */
            try
            {
                Classification typeClassification = repositoryHelper.getClassificationFromEntity(serviceName,
                                                                                                 databaseTableEntity,
                                                                                                 OpenMetadataType.TYPE_EMBEDDED_ATTRIBUTE_CLASSIFICATION.typeName,
                                                                                                 methodName);

                if (typeClassification != null)
                {
                    parentGUID = databaseTableGUID;
                    parentTypeName = OpenMetadataType.RELATIONAL_TABLE.typeName;
                    parentAttachmentTypeGUID = OpenMetadataType.NESTED_SCHEMA_ATTRIBUTE_RELATIONSHIP.typeGUID;
                    parentAttachmentTypeName = OpenMetadataType.NESTED_SCHEMA_ATTRIBUTE_RELATIONSHIP.typeName;
                }
            }
            catch (ClassificationErrorException classificationNotKnown)
            {
                /*
                 * Type classification not supported.
                 */
            }

            if (parentGUID == null)
            {
                parentTypeName = OpenMetadataType.RELATIONAL_TABLE_TYPE.typeName;
                parentAttachmentTypeGUID = OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeGUID;
                parentAttachmentTypeName = OpenMetadataType.ATTRIBUTE_FOR_SCHEMA_RELATIONSHIP.typeName;

                parentGUID = databaseColumnHandler.getAttachedElementGUID(userId,
                                                                          databaseTableGUID,
                                                                          databaseTableGUIDParameterName,
                                                                          OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                                          OpenMetadataType.SCHEMA_ATTRIBUTE_TYPE_RELATIONSHIP.typeGUID,
                                                                          OpenMetadataType.SCHEMA_ATTRIBUTE_TYPE_RELATIONSHIP.typeName,
                                                                          OpenMetadataType.SCHEMA_TYPE.typeName,
                                                                          2,
                                                                          null,
                                                                          null,
                                                                          SequencingOrder.CREATION_DATE_RECENT,
                                                                          null,
                                                                          forLineage,
                                                                          forDuplicateProcessing,
                                                                          effectiveTime,
                                                                          methodName);
            }
        }

        if (parentGUID == null)
        {
            invalidParameterHandler.throwUnknownElement(userId,
                                                        databaseTableGUID,
                                                        OpenMetadataType.RELATIONAL_TABLE.typeName,
                                                        serviceName,
                                                        serverName,
                                                        methodName);
            return null;
        }

        SchemaAttributeBuilder builder = new SchemaAttributeBuilder(qualifiedName,
                                                                    displayName,
                                                                    description,
                                                                    repositoryHelper,
                                                                    serviceName,
                                                                    serverName);

        if (anchorIdentifiers != null)
        {
            builder.setAnchors(userId,
                               anchorIdentifiers.anchorGUID,
                               anchorIdentifiers.anchorTypeName,
                               anchorIdentifiers.anchorDomainName,
                               anchorIdentifiers.anchorScopeGUID,
                               methodName);
        }

        /*
         * Now create the table itself along with its schema type.  It also links the resulting table to the database schema type.
         * The returned value is the guid of the table.
         */
        String databaseColumnGUID = databaseColumnHandler.createBeanFromTemplate(userId,
                                                                                 databaseManagerGUID,
                                                                                 databaseManagerName,
                                                                                 templateGUID,
                                                                                 templateGUIDParameterName,
                                                                                 OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                                                 OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                                                 qualifiedName,
                                                                                 qualifiedNameParameterName,
                                                                                 builder,
                                                                                 databaseColumnHandler.getSupportedZones(),
                                                                                 true,
                                                                                 false,
                                                                                 null,
                                                                                 methodName);

        if (databaseColumnGUID != null)
        {
            /*
             * link the new database column to its table
             */
            final String databaseColumnGUIDParameterName = "databaseColumnGUID";

            databaseColumnHandler.linkElementToElement(userId,
                                                       databaseManagerGUID,
                                                       databaseManagerName,
                                                       parentGUID,
                                                       databaseTableGUIDParameterName,
                                                       parentTypeName,
                                                       databaseColumnGUID,
                                                       databaseColumnGUIDParameterName,
                                                       OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                       forLineage,
                                                       forDuplicateProcessing,
                                                       parentAttachmentTypeGUID,
                                                       parentAttachmentTypeName,
                                                       builder.getParentRelationshipProperties(methodName),
                                                       effectiveFrom,
                                                       effectiveTo,
                                                       effectiveTime,
                                                       methodName);
        }

        return databaseColumnGUID;
    }


    /**
     * Update the metadata element representing a database column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseColumnGUID unique identifier of the metadata element to update
     * @param qualifiedName unique name for the database schema
     * @param displayName the stored display name property for the database table
     * @param description the stored description property associated with the database table
     * @param externalSchemaTypeGUID unique identifier of an external schema identifier
     * @param dataType data type name - for stored values
     * @param defaultValue string containing default value - for stored values
     * @param fixedValue string containing fixed value - for literals
     * @param validValuesSetGUID unique identifier for a valid values set to support
     * @param formula String formula - for derived values
     * @param isDeprecated is this table deprecated?
     * @param elementPosition the position of this column in its parent table.
     * @param minCardinality minimum number of repeating instances allowed for this column - typically 1
     * @param maxCardinality the maximum number of repeating instances allowed for this column - typically 1
     * @param allowsDuplicateValues  whether the same value can be used by more than one instance of this attribute
     * @param orderedValues whether the attribute instances are arranged in an order
     * @param sortOrder the order that the attribute instances are arranged in - if any
     * @param minimumLength the minimum length of the data
     * @param length the length of the data field
     * @param significantDigits number of significant digits to the right of decimal point
     * @param isNullable whether the field is nullable or not
     * @param nativeJavaClass equivalent Java class implementation
     * @param defaultValueOverride default value for this column
     * @param aliases a list of alternative names for the attribute
     * @param additionalProperties any arbitrary properties not part of the type system
     * @param typeName name of the type that is a subtype of DeployedDatabaseSchema - or null to create standard type
     * @param extendedProperties properties from any subtype
     * @param vendorProperties additional properties relating to the source of the database technology
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate  combine supplied properties with existing properties?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateDatabaseColumn(String               userId,
                                     String               databaseManagerGUID,
                                     String               databaseManagerName,
                                     String               databaseColumnGUID,
                                     String               qualifiedName,
                                     String               displayName,
                                     String               description,
                                     String               externalSchemaTypeGUID,
                                     String               dataType,
                                     String               defaultValue,
                                     String               fixedValue,
                                     String               validValuesSetGUID,
                                     String               formula,
                                     boolean              isDeprecated,
                                     int                  elementPosition,
                                     int                  minCardinality,
                                     int                  maxCardinality,
                                     boolean              allowsDuplicateValues,
                                     boolean              orderedValues,
                                     String               defaultValueOverride,
                                     int                  sortOrder,
                                     int                  minimumLength,
                                     int                  length,
                                     int                  significantDigits,
                                     boolean              isNullable,
                                     String               nativeJavaClass,
                                     List<String>         aliases,
                                     Map<String, String>  additionalProperties,
                                     String               typeName,
                                     Map<String, Object>  extendedProperties,
                                     Map<String, String>  vendorProperties,
                                     Date                 effectiveFrom,
                                     Date                 effectiveTo,
                                     boolean              isMergeUpdate,
                                     boolean              forLineage,
                                     boolean              forDuplicateProcessing,
                                     Date                 effectiveTime,
                                     String               methodName) throws InvalidParameterException,
                                                                             UserNotAuthorizedException,
                                                                             PropertyServerException
    {
        final String elementGUIDParameterName   = "databaseColumnGUID";
        final String qualifiedNameParameterName = "qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseColumnGUID, elementGUIDParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(qualifiedName, qualifiedNameParameterName, methodName);
        }

        /*
         * A database column is represented as a schemaAttribute of type RelationalColumn (or a subtype).
         * Check that the type name requested is valid.
         */
        String attributeTypeName = OpenMetadataType.RELATIONAL_COLUMN.typeName;

        if (typeName != null)
        {
            attributeTypeName = typeName;
            invalidParameterHandler.validateTypeName(typeName,
                                                     OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                     serviceName,
                                                     methodName,
                                                     repositoryHelper);
        }

        databaseColumnHandler.updateSchemaAttribute(userId,
                                                    databaseManagerGUID,
                                                    databaseManagerName,
                                                    databaseColumnGUID,
                                                    elementGUIDParameterName,
                                                    qualifiedName,
                                                    qualifiedNameParameterName,
                                                    displayName,
                                                    description,
                                                    externalSchemaTypeGUID,
                                                    dataType,
                                                    defaultValue,
                                                    fixedValue,
                                                    validValuesSetGUID,
                                                    formula,
                                                    isDeprecated,
                                                    elementPosition,
                                                    minCardinality,
                                                    maxCardinality,
                                                    allowsDuplicateValues,
                                                    orderedValues,
                                                    defaultValueOverride,
                                                    sortOrder,
                                                    minimumLength,
                                                    length,
                                                    significantDigits,
                                                    isNullable,
                                                    nativeJavaClass,
                                                    aliases,
                                                    additionalProperties,
                                                    attributeTypeName,
                                                    extendedProperties,
                                                    effectiveFrom,
                                                    effectiveTo,
                                                    isMergeUpdate,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);

        databaseColumnHandler.setVendorProperties(userId,
                                                  databaseColumnGUID,
                                                  vendorProperties,
                                                  forLineage,
                                                  forDuplicateProcessing,
                                                  effectiveTime,
                                                  methodName);
    }


    /**
     * Remove the metadata element representing a database column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseColumnGUID unique identifier of the metadata element to remove
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeDatabaseColumn(String  userId,
                                     String  databaseManagerGUID,
                                     String  databaseManagerName,
                                     String  databaseColumnGUID,
                                     boolean forLineage,
                                     boolean forDuplicateProcessing,
                                     Date    effectiveTime,
                                     String  methodName) throws InvalidParameterException,
                                                                UserNotAuthorizedException,
                                                                PropertyServerException
    {
        final String elementGUIDParameterName    = "databaseColumnGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseColumnGUID, elementGUIDParameterName, methodName);

        databaseColumnHandler.deleteBeanInRepository(userId,
                                                     databaseManagerGUID,
                                                     databaseManagerName,
                                                     databaseColumnGUID,
                                                     elementGUIDParameterName,
                                                     OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                     OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                     false,
                                                     null,
                                                     null,
                                                     forLineage,
                                                     forDuplicateProcessing,
                                                     effectiveTime,
                                                     methodName);
    }


    /**
     * Retrieve the list of database column metadata elements that contain the search string.
     * The search string is treated as a regular expression.
     *
     * @param userId calling user
     * @param searchString string to find in the properties
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_COLUMN>   findDatabaseColumns(String  userId,
                                                       String  searchString,
                                                       int     startFrom,
                                                       int     pageSize,
                                                       boolean forLineage,
                                                       boolean forDuplicateProcessing,
                                                       Date    effectiveTime,
                                                       String  methodName) throws InvalidParameterException,
                                                                                  UserNotAuthorizedException,
                                                                                  PropertyServerException
    {
        final String searchStringParameterName = "searchString";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateSearchString(searchString, searchStringParameterName, methodName);

        return databaseColumnHandler.findSchemaAttributes(userId,
                                                          searchString,
                                                          searchStringParameterName,
                                                          OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                          OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                          null,
                                                          null,
                                                          startFrom,
                                                          pageSize,
                                                          forLineage,
                                                          forDuplicateProcessing,
                                                          effectiveTime,
                                                          methodName);
    }


    /**
     * Retrieve the list of column for a database table (or view)
     *
     * @param userId calling user
     * @param databaseTableGUID unique identifier of the database table of interest
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_COLUMN> getColumnsForDatabaseTable(String  userId,
                                                            String  databaseTableGUID,
                                                            int     startFrom,
                                                            int     pageSize,
                                                            boolean forLineage,
                                                            boolean forDuplicateProcessing,
                                                            Date    effectiveTime,
                                                            String  methodName) throws InvalidParameterException,
                                                                                       UserNotAuthorizedException,
                                                                                       PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseTableGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseTableGUID, parentElementGUIDParameterName, methodName);

        return databaseColumnHandler.getNestedSchemaAttributes(userId,
                                                               databaseTableGUID,
                                                               parentElementGUIDParameterName,
                                                               startFrom,
                                                               pageSize,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               effectiveTime,
                                                               methodName);
    }


    /**
     * Retrieve the list of database column metadata elements with a matching qualified or display name.
     * There are no wildcards supported on this request.
     *
     * @param userId calling user
     * @param name name to search for
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<DATABASE_COLUMN>   getDatabaseColumnsByName(String  userId,
                                                            String  name,
                                                            int     startFrom,
                                                            int     pageSize,
                                                            boolean forLineage,
                                                            boolean forDuplicateProcessing,
                                                            Date    effectiveTime,
                                                            String methodName) throws InvalidParameterException,
                                                                                      UserNotAuthorizedException,
                                                                                      PropertyServerException
    {
        final String nameParameterName = "name";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateName(name, nameParameterName, methodName);

        return databaseColumnHandler.getSchemaAttributesByName(userId,
                                                               OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                               OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                               name,
                                                               null,
                                                               null,
                                                               startFrom,
                                                               pageSize,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               effectiveTime,
                                                               methodName);
    }


    /**
     * Retrieve the database column metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param guid unique identifier of the requested metadata element
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return matching metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public DATABASE_COLUMN getDatabaseColumnByGUID(String  userId,
                                                   String  guid,
                                                   boolean forLineage,
                                                   boolean forDuplicateProcessing,
                                                   Date    effectiveTime,
                                                   String  methodName) throws InvalidParameterException,
                                                                              UserNotAuthorizedException,
                                                                              PropertyServerException
    {
        final String guidParameterName = "guid";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(guid, guidParameterName, methodName);

        return databaseColumnHandler.getSchemaAttribute(userId,
                                                        guid,
                                                        guidParameterName,
                                                        OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                        null,
                                                        null,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        effectiveTime,
                                                        methodName);
    }


    /* ==================================================================================
     * Database columns can be decorated with additional information about their content.
     */

    /**
     * Classify a column in a database table as the primary key.  This means each row has a different value
     * in this column, and it can be used to uniquely identify the column.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseColumnGUID unique identifier if the primary key column
     * @param name name of primary key
     * @param keyPatternOrdinal type of lifecycle and scope
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param isMergeUpdate should the new properties be merged with existing properties (true) or completely replace them (false)?
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void setPrimaryKeyOnColumn(String     userId,
                                      String     databaseManagerGUID,
                                      String     databaseManagerName,
                                      String     databaseColumnGUID,
                                      String     name,
                                      int        keyPatternOrdinal,
                                      Date       effectiveFrom,
                                      Date       effectiveTo,
                                      boolean    isMergeUpdate,
                                      boolean    forLineage,
                                      boolean    forDuplicateProcessing,
                                      Date       effectiveTime,
                                      String     methodName) throws InvalidParameterException,
                                                                    UserNotAuthorizedException,
                                                                    PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseColumnGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseColumnGUID, parentElementGUIDParameterName, methodName);

        InstanceProperties properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                     null,
                                                                                     OpenMetadataProperty.NAME.name,
                                                                                     name,
                                                                                     methodName);

        try
        {
            properties = repositoryHelper.addEnumPropertyToInstance(serviceName,
                                                                    properties,
                                                                    OpenMetadataProperty.KEY_PATTERN.name,
                                                                    KeyPattern.getOpenTypeGUID(),
                                                                    KeyPattern.getOpenTypeName(),
                                                                    keyPatternOrdinal,
                                                                    methodName);
        }
        catch (TypeErrorException classificationNotSupported)
        {
            throw new InvalidParameterException(classificationNotSupported, OpenMetadataProperty.KEY_PATTERN.name);
        }

        databaseColumnHandler.setUpEffectiveDates(properties, effectiveFrom, effectiveTo);

        databaseColumnHandler.setClassificationInRepository(userId,
                                                            databaseManagerGUID,
                                                            databaseManagerName,
                                                            databaseColumnGUID,
                                                            parentElementGUIDParameterName,
                                                            OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                            OpenMetadataType.PRIMARY_KEY_CLASSIFICATION.typeGUID,
                                                            OpenMetadataType.PRIMARY_KEY_CLASSIFICATION.typeName,
                                                            properties,
                                                            isMergeUpdate,
                                                            forLineage,
                                                            forDuplicateProcessing,
                                                            effectiveTime,
                                                            methodName);
    }


    /**
     * Remove the classification that this column is a primary key.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param databaseColumnGUID unique identifier if the primary key column
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removePrimaryKeyFromColumn(String  userId,
                                           String  databaseManagerGUID,
                                           String  databaseManagerName,
                                           String  databaseColumnGUID,
                                           boolean forLineage,
                                           boolean forDuplicateProcessing,
                                           Date    effectiveTime,
                                           String  methodName) throws InvalidParameterException,
                                                                      UserNotAuthorizedException,
                                                                      PropertyServerException
    {
        final String parentElementGUIDParameterName = "databaseColumnGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(databaseColumnGUID, parentElementGUIDParameterName, methodName);

        databaseColumnHandler.removeClassificationFromRepository(userId,
                                                                 databaseManagerGUID,
                                                                 databaseManagerName,
                                                                 databaseColumnGUID,
                                                                 parentElementGUIDParameterName,
                                                                 OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                                 OpenMetadataType.PRIMARY_KEY_CLASSIFICATION.typeGUID,
                                                                 OpenMetadataType.PRIMARY_KEY_CLASSIFICATION.typeName,
                                                                 forLineage,
                                                                 forDuplicateProcessing,
                                                                 effectiveTime,
                                                                 methodName);
    }


    /**
     * Create a foreign relationship between two columns.  One of the columns holds the primary key of the other
     * to form a link.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param primaryKeyColumnGUID unique identifier of the column containing the primary key
     * @param foreignKeyColumnGUID unique identifier of the column containing the primary key from the other table
     * @param name the display name for UIs and reports
     * @param description description of the foreign key
     * @param confidence the level of confidence that the foreign key is correct.  This is a value between 0 and 100
     * @param steward the name of the steward who assigned the foreign key (or approved the discovered value)
     * @param source the id of the source of the knowledge of the foreign key
     * @param effectiveFrom      starting time for this relationship (null for all time)
     * @param effectiveTo        ending time for this relationship (null for all time)
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void addForeignKeyRelationship(String  userId,
                                          String  databaseManagerGUID,
                                          String  databaseManagerName,
                                          String  primaryKeyColumnGUID,
                                          String  foreignKeyColumnGUID,
                                          String  name,
                                          String  description,
                                          int     confidence,
                                          String  steward,
                                          String  source,
                                          Date    effectiveFrom,
                                          Date    effectiveTo,
                                          boolean forLineage,
                                          boolean forDuplicateProcessing,
                                          Date    effectiveTime,
                                          String  methodName) throws InvalidParameterException,
                                                                    UserNotAuthorizedException,
                                                                    PropertyServerException
    {
        final String primaryElementGUIDParameterName = "primaryKeyColumnGUID";
        final String foreignElementGUIDParameterName = "foreignKeyColumnGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(primaryKeyColumnGUID, primaryElementGUIDParameterName, methodName);
        invalidParameterHandler.validateGUID(foreignKeyColumnGUID, foreignElementGUIDParameterName, methodName);

        InstanceProperties properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                                     null,
                                                                                     OpenMetadataProperty.NAME.name,
                                                                                     name,
                                                                                     methodName);
        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.DESCRIPTION.name,
                                                                  description,
                                                                  methodName);
        properties = repositoryHelper.addIntPropertyToInstance(serviceName,
                                                               properties,
                                                               OpenMetadataProperty.CONFIDENCE.name,
                                                               confidence,
                                                               methodName);
        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.STEWARD.name,
                                                                  steward,
                                                                  methodName);
        properties = repositoryHelper.addStringPropertyToInstance(serviceName,
                                                                  properties,
                                                                  OpenMetadataProperty.SOURCE.name,
                                                                  source,
                                                                  methodName);

        databaseColumnHandler.linkElementToElement(userId,
                                                   databaseManagerGUID,
                                                   databaseManagerName,
                                                   primaryKeyColumnGUID,
                                                   primaryElementGUIDParameterName,
                                                   OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                   foreignKeyColumnGUID,
                                                   foreignElementGUIDParameterName,
                                                   OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                   forLineage,
                                                   forDuplicateProcessing,
                                                   OpenMetadataType.FOREIGN_KEY_RELATIONSHIP.typeGUID,
                                                   OpenMetadataType.FOREIGN_KEY_RELATIONSHIP.typeName,
                                                   databaseColumnHandler.setUpEffectiveDates(properties, effectiveFrom, effectiveTo),
                                                   effectiveFrom,
                                                   effectiveTo,
                                                   effectiveTime,
                                                   methodName);
    }


    /**
     * Remove the foreign key relationship for the requested columns.
     *
     * @param userId calling user
     * @param databaseManagerGUID unique identifier of software capability representing the DBMS
     * @param databaseManagerName unique name of software capability representing the DBMS
     * @param primaryKeyColumnGUID unique identifier of the column that is the linked primary key
     * @param foreignKeyColumnGUID unique identifier of the column the contains the primary key from another table
     * @param forLineage                the request is to support lineage retrieval this means entities with the Memento classification can be returned
     * @param forDuplicateProcessing    the request is for duplicate processing and so must not deduplicate
     * @param effectiveTime the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeForeignKeyRelationship(String  userId,
                                             String  databaseManagerGUID,
                                             String  databaseManagerName,
                                             String  primaryKeyColumnGUID,
                                             String  foreignKeyColumnGUID,
                                             boolean forLineage,
                                             boolean forDuplicateProcessing,
                                             Date    effectiveTime,
                                             String  methodName) throws InvalidParameterException,
                                                                        UserNotAuthorizedException,
                                                                        PropertyServerException
    {
        final String primaryElementGUIDParameterName = "primaryKeyColumnGUID";
        final String foreignElementGUIDParameterName = "foreignKeyColumnGUID";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(primaryKeyColumnGUID, primaryElementGUIDParameterName, methodName);
        invalidParameterHandler.validateGUID(foreignKeyColumnGUID, foreignElementGUIDParameterName, methodName);

        databaseColumnHandler.unlinkElementFromElement(userId,
                                                       false,
                                                       databaseManagerGUID,
                                                       databaseManagerName,
                                                       primaryKeyColumnGUID,
                                                       primaryElementGUIDParameterName,
                                                       OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                       foreignKeyColumnGUID,
                                                       foreignElementGUIDParameterName,
                                                       OpenMetadataType.RELATIONAL_COLUMN.typeGUID,
                                                       OpenMetadataType.RELATIONAL_COLUMN.typeName,
                                                       forLineage,
                                                       forDuplicateProcessing,
                                                       OpenMetadataType.FOREIGN_KEY_RELATIONSHIP.typeGUID,
                                                       OpenMetadataType.FOREIGN_KEY_RELATIONSHIP.typeName,
                                                       effectiveTime,
                                                       methodName);
    }
}
