/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.frameworkservices.omf.server;

import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.ffdc.RESTCallLogger;
import org.odpi.openmetadata.commonservices.ffdc.RESTCallToken;
import org.odpi.openmetadata.commonservices.ffdc.RESTExceptionHandler;
import org.odpi.openmetadata.commonservices.ffdc.rest.*;
import org.odpi.openmetadata.commonservices.generichandlers.*;
import org.odpi.openmetadata.commonservices.mermaid.OpenMetadataMermaidGraphBuilder;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.frameworks.openmetadata.enums.ElementStatus;
import org.odpi.openmetadata.frameworks.openmetadata.mapper.OpenMetadataValidValues;
import org.odpi.openmetadata.frameworks.openmetadata.properties.*;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataProperty;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.frameworkservices.omf.converters.OpenMetadataRelationshipConverter;
import org.odpi.openmetadata.frameworkservices.omf.ffdc.OpenMetadataStoreAuditCode;
import org.odpi.openmetadata.frameworkservices.omf.handlers.MetadataElementHandler;
import org.odpi.openmetadata.frameworkservices.omf.rest.ArchiveRequestBody;
import org.odpi.openmetadata.frameworkservices.omf.rest.MetadataSourceRequestBody;
import org.odpi.openmetadata.frameworkservices.omf.rest.TemplateRequestBody;
import org.odpi.openmetadata.frameworkservices.omf.rest.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.HistorySequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.MatchCriteria;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.instances.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.search.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.typedefs.*;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.odpi.openmetadata.frameworks.openmetadata.mapper.OpenMetadataValidValues.constructValidValueCategory;
import static org.odpi.openmetadata.frameworks.openmetadata.mapper.OpenMetadataValidValues.constructValidValueQualifiedName;


/**
 * The OpenMetadataStoreRESTServices provides the server-side implementation of the services used by the governance
 * engine as it is managing requests to execute open governance services in the governance server.
 * These services align with the interface definitions from the Governance Action Framework (omf).
 */
public class OpenMetadataStoreRESTServices
{
    private final static OMFMetadataManagementInstanceHandler instanceHandler = new OMFMetadataManagementInstanceHandler();

    private final        RESTExceptionHandler restExceptionHandler = new RESTExceptionHandler();
    private final static RESTCallLogger       restCallLogger       = new RESTCallLogger(LoggerFactory.getLogger(OpenMetadataStoreRESTServices.class),
                                                                                        instanceHandler.getServiceName());


    private final InvalidParameterHandler invalidParameterHandler = new InvalidParameterHandler();
    private final String propertyNameParameter   = "propertyName";
    private final String preferredValueParameter = "preferredValue";
    private final String actualValueParameter    = "actualValue";
    private final String validValueGUIDParameter = "element.getGUID";


    /**
     * Default constructor
     */
    public OpenMetadataStoreRESTServices()
    {
    }


    /**
     * Log an audit message about this asset.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId         userId of user making request.
     * @param assetGUID      unique identifier for asset.
     * @param governanceService name of governance service
     * @param message        message to log
     *
     * @return void or
     *  InvalidParameterException one of the parameters is null or invalid.
     *  UserNotAuthorizedException user not authorized to issue this request.
     *  PropertyServerException there was a problem that occurred within the property server.
     */
    public VoidResponse logAssetAuditMessage(String serverName,
                                             String serviceURLMarker,
                                             String userId,
                                             String assetGUID,
                                             String governanceService,
                                             String message)
    {
        final String   methodName = "logAssetAuditMessage";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            this.getMetadataElementByGUID(serverName, serviceURLMarker, userId, assetGUID, new AnyTimeRequestBody());

            auditLog.logMessage(methodName, OpenMetadataStoreAuditCode.ASSET_AUDIT_LOG.getMessageDefinition(assetGUID, governanceService, message));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Returns the list of different types of metadata organized into two groups.  The first are the
     * attribute type definitions (AttributeTypeDefs).  These provide types for properties in full
     * type definitions.  Full type definitions (TypeDefs) describe types for entities, relationships
     * and classifications.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @return TypeDefGalleryResponse:
     * List of different categories of type definitions or
     * RepositoryErrorException there is a problem communicating with the metadata repository or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @SuppressWarnings(value = "unused")
    public TypeDefGalleryResponse getAllTypes(String   serverName,
                                              String   serviceURLMarker,
                                              String   userId)
    {
        final String methodName = "getAllTypes";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefGalleryResponse response = new TypeDefGalleryResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            response.setTypeDefs(this.getTypeDefs(repositoryHelper.getKnownTypeDefs()));
            response.setAttributeTypeDefs(this.getAttributeTypeDefs(repositoryHelper.getKnownAttributeTypeDefs()));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Returns all the TypeDefs for a specific category.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param category find parameters used to limit the returned results.
     * @return TypeDefListResponse:
     * TypeDefs list or
     * InvalidParameterException the TypeDefCategory is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @SuppressWarnings(value = "unused")
    public TypeDefListResponse findTypeDefsByCategory(String                      serverName,
                                                      String                      serviceURLMarker,
                                                      String                      userId,
                                                      OpenMetadataTypeDefCategory category)
    {
        final String methodName = "findTypeDefsByCategory";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefListResponse response = new TypeDefListResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            List<TypeDef> allTypeDefs = repositoryHelper.getKnownTypeDefs();
            if (allTypeDefs != null)
            {
                List<OpenMetadataTypeDef> openMetadataTypeDefList = new ArrayList<>();

                for (TypeDef typeDef : allTypeDefs)
                {
                    if (((category == OpenMetadataTypeDefCategory.ENTITY_DEF) && (typeDef.getCategory() == TypeDefCategory.ENTITY_DEF)) ||
                                ((category == OpenMetadataTypeDefCategory.RELATIONSHIP_DEF) && (typeDef.getCategory() == TypeDefCategory.RELATIONSHIP_DEF)) ||
                                ((category == OpenMetadataTypeDefCategory.CLASSIFICATION_DEF) && (typeDef.getCategory() == TypeDefCategory.CLASSIFICATION_DEF)))
                    {
                        openMetadataTypeDefList.add(this.getTypeDef(typeDef));
                    }
                }

                if (! openMetadataTypeDefList.isEmpty())
                {
                    response.setTypeDefs(openMetadataTypeDefList);
                }
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Returns all the AttributeTypeDefs for a specific category.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param category find parameters used to limit the returned results.
     * @return AttributeTypeDefListResponse:
     * AttributeTypeDefs list or
     * InvalidParameterException the TypeDefCategory is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @SuppressWarnings(value = "unused")
    public AttributeTypeDefListResponse findAttributeTypeDefsByCategory(String                               serverName,
                                                                        String                               serviceURLMarker,
                                                                        String                               userId,
                                                                        OpenMetadataAttributeTypeDefCategory category)
    {
        final String methodName = "findAttributeTypeDefsByCategory";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        AttributeTypeDefListResponse response = new AttributeTypeDefListResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            List<AttributeTypeDef> allAttributeTypeDefs = repositoryHelper.getKnownAttributeTypeDefs();
            if (allAttributeTypeDefs != null)
            {
                List<OpenMetadataAttributeTypeDef> openMetadataAttributeTypeDefList = new ArrayList<>();

                for (AttributeTypeDef attributeTypeDef : allAttributeTypeDefs)
                {
                    if (((category == OpenMetadataAttributeTypeDefCategory.ENUM_DEF) && (attributeTypeDef.getCategory() == AttributeTypeDefCategory.ENUM_DEF)) ||
                                ((category == OpenMetadataAttributeTypeDefCategory.COLLECTION) && (attributeTypeDef.getCategory() == AttributeTypeDefCategory.COLLECTION)) ||
                                ((category == OpenMetadataAttributeTypeDefCategory.PRIMITIVE) && (attributeTypeDef.getCategory() == AttributeTypeDefCategory.PRIMITIVE)))
                    {
                        openMetadataAttributeTypeDefList.add(this.getAttributeTypeDef(attributeTypeDef));
                    }
                }

                if (! openMetadataAttributeTypeDefList.isEmpty())
                {
                    response.setAttributeTypeDefs(openMetadataAttributeTypeDefList);
                }
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return the types that are linked to the elements from the specified standard.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param standard name of the standard null means any.
     * @param organization name of the organization null means any.
     * @param identifier identifier of the element in the standard null means any.
     * @return TypeDefsGalleryResponse:
     * A list of types or
     * InvalidParameterException all attributes of the external id are null or
     * RepositoryErrorException there is a problem communicating with the metadata repository or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    @SuppressWarnings(value = "unused")
    public TypeDefListResponse findTypesByExternalId(String    serverName,
                                                     String    serviceURLMarker,
                                                     String    userId,
                                                     String    standard,
                                                     String    organization,
                                                     String    identifier)
    {
        final String methodName = "findTypesByExternalId";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefListResponse response = new TypeDefListResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            List<TypeDef> allTypeDefs = repositoryHelper.getKnownTypeDefs();
            if (allTypeDefs != null)
            {
                List<OpenMetadataTypeDef> openMetadataTypeDefList = new ArrayList<>();

                for (TypeDef typeDef : allTypeDefs)
                {
                    if (typeDef.getExternalStandardMappings() != null)
                    {
                        for (ExternalStandardMapping externalStandardMapping : typeDef.getExternalStandardMappings())
                        {
                            if (((standard == null) || (standard.equals(externalStandardMapping.getStandardName()))) &&
                                        ((organization == null) || (organization.equals(externalStandardMapping.getStandardOrganization()))) &&
                                        ((identifier == null) || (identifier.equals(externalStandardMapping.getStandardTypeName()))))
                            {
                                openMetadataTypeDefList.add(this.getTypeDef(typeDef));
                            }
                        }
                    }
                }

                if (! openMetadataTypeDefList.isEmpty())
                {
                    response.setTypeDefs(openMetadataTypeDefList);
                }
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Returns all the TypeDefs for a specific subtype.  If a null result is returned it means the
     * type has no subtypes.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param typeName name of type to retrieve against.
     * @return TypeDefListResponse:
     * TypeDefs list or
     * InvalidParameterException the typeName is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    public TypeDefListResponse getSubTypes(String serverName,
                                           String serviceURLMarker,
                                           String userId,
                                           String typeName)
    {
        final String methodName = "getSubTypes";
        final String parameterName = "typeName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefListResponse response = new TypeDefListResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);
            String               serviceName      = instanceHandler.getServiceName(serviceURLMarker);

            invalidParameterHandler.validateName(typeName, parameterName, methodName);

            List<String>  subTypeNames = repositoryHelper.getSubTypesOf(serviceName, typeName);

            if (subTypeNames != null)
            {
                List<OpenMetadataTypeDef> openMetadataTypeDefList = new ArrayList<>();

                for (String subTypeName : subTypeNames)
                {
                    if (subTypeName != null)
                    {
                        TypeDef subType = repositoryHelper.getTypeDefByName(serviceName, subTypeName);

                        openMetadataTypeDefList.add(this.getTypeDef(subType));
                    }
                }

                if (! openMetadataTypeDefList.isEmpty())
                {
                    response.setTypeDefs(openMetadataTypeDefList);
                }
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return the TypeDef identified by the GUID.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param guid String unique id of the TypeDef.
     * @return TypeDefResponse:
     * TypeDef structure describing its category and properties or
     * InvalidParameterException the guid is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored or
     * TypeDefNotKnownException The requested TypeDef is not known in the metadata collection or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    public TypeDefResponse getTypeDefByGUID(String    serverName,
                                            String    serviceURLMarker,
                                            String    userId,
                                            String    guid)
    {
        final String methodName = "getTypeDefByGUID";
        final String guidParameterName = "guid";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefResponse response = new TypeDefResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            TypeDef typeDef = repositoryHelper.getTypeDef(instanceHandler.getServiceName(serviceURLMarker),
                                                          guidParameterName,
                                                          guid,
                                                          methodName);
            response.setTypeDef(this.getTypeDef(typeDef));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return the AttributeTypeDef identified by the GUID.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param guid String unique id of the TypeDef
     * @return AttributeTypeDefResponse:
     * TypeDef structure describing its category and properties or
     * InvalidParameterException the guid is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored or
     * TypeDefNotKnownException The requested TypeDef is not known in the metadata collection or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    public AttributeTypeDefResponse getAttributeTypeDefByGUID(String    serverName,
                                                              String    serviceURLMarker,
                                                              String    userId,
                                                              String    guid)
    {
        final String methodName = "getAttributeTypeDefByGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        AttributeTypeDefResponse response = new AttributeTypeDefResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            AttributeTypeDef attributeTypeDef = repositoryHelper.getAttributeTypeDef(instanceHandler.getServiceName(serviceURLMarker),
                                                                                     guid,
                                                                                     methodName);
            response.setAttributeTypeDef(this.getAttributeTypeDef(attributeTypeDef));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return the TypeDef identified by the unique name.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param name String name of the TypeDef.
     * @return TypeDefResponse:
     * TypeDef structure describing its category and properties or
     * InvalidParameterException the name is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored or
     * TypeDefNotKnownException the requested TypeDef is not found in the metadata collection or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    public TypeDefResponse getTypeDefByName(String    serverName,
                                            String    serviceURLMarker,
                                            String    userId,
                                            String    name)
    {
        final String methodName = "getTypeDefByName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TypeDefResponse response = new TypeDefResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            TypeDef typeDef = repositoryHelper.getTypeDefByName(instanceHandler.getServiceName(serviceURLMarker), name);
            response.setTypeDef(this.getTypeDef(typeDef));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return the AttributeTypeDef identified by the unique name.
     *
     * @param serverName unique identifier for requested server.
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId unique identifier for requesting user.
     * @param name String name of the TypeDef.
     * @return AttributeTypeDefResponse:
     * AttributeTypeDef structure describing its category and properties or
     * InvalidParameterException the name is null or
     * RepositoryErrorException there is a problem communicating with the metadata repository where
     *                                  the metadata collection is stored or
     * TypeDefNotKnownException the requested TypeDef is not found in the metadata collection or
     * UserNotAuthorizedException the userId is not permitted to perform this operation.
     */
    public  AttributeTypeDefResponse getAttributeTypeDefByName(String    serverName,
                                                               String    serviceURLMarker,
                                                               String    userId,
                                                               String    name)
    {
        final String methodName = "getAttributeTypeDefByName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        AttributeTypeDefResponse response = new AttributeTypeDefResponse();

        try
        {
            auditLog                              = instanceHandler.getAuditLog(userId, serverName, methodName);
            OMRSRepositoryHelper repositoryHelper = instanceHandler.getRepositoryHelper(userId, serverName, methodName);

            AttributeTypeDef attributeTypeDef = repositoryHelper.getAttributeTypeDefByName(instanceHandler.getServiceName(serviceURLMarker), name);
            response.setAttributeTypeDef(this.getAttributeTypeDef(attributeTypeDef));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the metadata element using its unique identifier.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier for the metadata element
     * @param requestBody only return the element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     *
     * @return metadata element properties or
     *  InvalidParameterException the unique identifier is null or not known.
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataElementResponse getMetadataElementByGUID(String             serverName,
                                                                String             serviceURLMarker,
                                                                String             userId,
                                                                String             elementGUID,
                                                                AnyTimeRequestBody requestBody)
    {
        final String methodName = "getMetadataElementByGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        OpenMetadataElementResponse response = new OpenMetadataElementResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            if (requestBody != null)
            {
                response.setElement(handler.getMetadataElementByGUID(userId,
                                                                     elementGUID,
                                                                     requestBody.getForLineage(),
                                                                     requestBody.getForDuplicateProcessing(),
                                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                     requestBody.getAsOfTime(),
                                                                     requestBody.getEffectiveTime(),
                                                                     methodName));
            }
            else
            {
                response.setElement(handler.getMetadataElementByGUID(userId,
                                                                     elementGUID,
                                                                     false,
                                                                     false,
                                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                     null,
                                                                     new Date(),
                                                                     methodName));
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the metadata element using its unique name (typically the qualified name).
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved element is for duplicate processing so do not combine results from known duplicates.
     * @param requestBody unique name for the metadata element
     *
     * @return metadata element properties or
     *  InvalidParameterException the unique identifier is null or not known.
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataElementResponse getMetadataElementByUniqueName(String          serverName,
                                                                      String          serviceURLMarker,
                                                                      String          userId,
                                                                      boolean         forLineage,
                                                                      boolean         forDuplicateProcessing,
                                                                      NameRequestBody requestBody)
    {
        final String methodName = "getMetadataElementByUniqueName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        OpenMetadataElementResponse response = new OpenMetadataElementResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setElement(handler.getMetadataElementByUniqueName(userId,
                                                                           requestBody.getName(),
                                                                           requestBody.getNameParameterName(),
                                                                           requestBody.getNamePropertyName(),
                                                                           requestBody.getLimitResultsByStatus(),
                                                                           requestBody.getAsOfTime(),
                                                                           requestBody.getSequencingOrder(),
                                                                           requestBody.getSequencingProperty(),
                                                                           forLineage,
                                                                           forDuplicateProcessing,
                                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                           requestBody.getEffectiveTime(),
                                                                           methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the unique identifier of a metadata element using its unique name (typically the qualified name).
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved element is for duplicate processing so do not combine results from known duplicates.
     * @param requestBody unique name for the metadata element
     *
     * @return metadata element unique identifier (guid) or
     *  InvalidParameterException the unique identifier is null or not known or
     *  UserNotAuthorizedException the governance action service is not able to access the element or
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public GUIDResponse getMetadataElementGUIDByUniqueName(String          serverName,
                                                           String          serviceURLMarker,
                                                           String          userId,
                                                           boolean         forLineage,
                                                           boolean         forDuplicateProcessing,
                                                           NameRequestBody requestBody)
    {
        final String methodName = "getMetadataElementGUIDByUniqueName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        GUIDResponse response = new GUIDResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setGUID(handler.getMetadataElementGUIDByUniqueName(userId,
                                                                            requestBody.getName(),
                                                                            requestBody.getNameParameterName(),
                                                                            requestBody.getNamePropertyName(),
                                                                            requestBody.getLimitResultsByStatus(),
                                                                            requestBody.getAsOfTime(),
                                                                            requestBody.getSequencingOrder(),
                                                                            requestBody.getSequencingProperty(),
                                                                            forLineage,
                                                                            forDuplicateProcessing,
                                                                            instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                            requestBody.getEffectiveTime(),
                                                                            methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }



    /**
     * Retrieve all the versions of an element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier for the metadata element
     * @param forLineage the retrieved elements are for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param oldestFirst  defining how the results should be ordered.
     * @param requestBody the time window required
     *
     * @return list of matching metadata elements (or null if no elements match the name) or
     *  InvalidParameterException the qualified name is null
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataElementsResponse getMetadataElementHistory(String             serverName,
                                                                  String             serviceURLMarker,
                                                                  String             userId,
                                                                  String             elementGUID,
                                                                  boolean            forLineage,
                                                                  boolean            forDuplicateProcessing,
                                                                  int                startFrom,
                                                                  int                pageSize,
                                                                  boolean            oldestFirst,
                                                                  HistoryRequestBody requestBody)
    {
        final String methodName = "getMetadataElementHistory";
        final String guidParameterName  = "elementGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        OpenMetadataElementsResponse response = new OpenMetadataElementsResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            HistorySequencingOrder sequencingOrder = HistorySequencingOrder.BACKWARDS;

            if (oldestFirst)
            {
                sequencingOrder = HistorySequencingOrder.FORWARDS;
            }

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            if (requestBody != null)
            {
                response.setElementList(handler.getBeanHistory(userId,
                                                               elementGUID,
                                                               guidParameterName,
                                                               OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                               requestBody.getFromTime(),
                                                               requestBody.getToTime(),
                                                               startFrom,
                                                               pageSize,
                                                               sequencingOrder,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                               requestBody.getEffectiveTime(),
                                                               methodName));
            }
            else
            {
                response.setElementList(handler.getBeanHistory(userId,
                                                               elementGUID,
                                                               guidParameterName,
                                                               OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                               null,
                                                               null,
                                                               startFrom,
                                                               pageSize,
                                                               sequencingOrder,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                               null,
                                                               methodName));
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the metadata elements that contain the requested string.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param requestBody searchString  to retrieve
     *
     * @return list of matching metadata elements (or null if no elements match the name) or
     *  InvalidParameterException the qualified name is null
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataElementsResponse findMetadataElementsWithString(String                  serverName,
                                                                       String                  serviceURLMarker,
                                                                       String                  userId,
                                                                       int                     startFrom,
                                                                       int                     pageSize,
                                                                       SearchStringRequestBody requestBody)
    {
        final String methodName = "findMetadataElementsWithString";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        OpenMetadataElementsResponse response = new OpenMetadataElementsResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setElementList(handler.findMetadataElementsWithString(userId,
                                                                               requestBody.getSearchString(),
                                                                               requestBody.getTypeName(),
                                                                               requestBody.getLimitResultsByStatus(),
                                                                               requestBody.getAsOfTime(),
                                                                               requestBody.getSequencingProperty(),
                                                                               requestBody.getSequencingOrder(),
                                                                               requestBody.getForLineage(),
                                                                               requestBody.getForDuplicateProcessing(),
                                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                               requestBody.getEffectiveTime(),
                                                                               startFrom,
                                                                               pageSize,
                                                                               methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return a list of elements with the requested search string in their (display, resource)name, qualified name,
     * title, text, summary, identifier or description.  The search string is interpreted as a regular expression (RegEx).
     * The breadth of the search is determined by the supplied anchorGUID.
     *
     * @param serverName name of the server instances for this request
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId calling user
     * @param anchorGUID unique identifier of anchor
     * @param requestBody string to search for in text
     * @param startFrom starting element (used in paging through large result sets)
     * @param pageSize maximum number of results to return
     *
     * @return list of results for assets that match the search string or
     * InvalidParameterException the searchString is invalid or
     * PropertyServerException there is a problem access in the property server or
     * UserNotAuthorizedException the user does not have access to the properties
     */
    public AnchorSearchMatchesResponse findElementsForAnchor(String                  serverName,
                                                             String                  serviceURLMarker,
                                                             String                  userId,
                                                             String                  anchorGUID,
                                                             SearchStringRequestBody requestBody,
                                                             int                     startFrom,
                                                             int                     pageSize)
    {
        final String methodName = "findElementsForAnchor";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AnchorSearchMatchesResponse response = new AnchorSearchMatchesResponse();
        AuditLog                    auditLog = null;

        try
        {
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                SearchProperties searchProperties = new SearchProperties();

                searchProperties.setMatchCriteria(MatchCriteria.ANY);
                searchProperties.setConditions(getPropertyConditions(requestBody.getSearchString(),
                                                                     getAnchorSearchesPropertyList()));

                SearchClassifications searchClassifications = getAnchorSearchClassifications(anchorGUID,
                                                                                             OpenMetadataProperty.ANCHOR_GUID.name);

                List<EntityDetail> anchoredEntities = handler.findEntities(userId,
                                                                           requestBody.getTypeName(),
                                                                           null,
                                                                           searchProperties,
                                                                           null,
                                                                           searchClassifications,
                                                                           requestBody.getAsOfTime(),
                                                                           null,
                                                                           null,
                                                                           false,
                                                                           false,
                                                                           startFrom,
                                                                           pageSize,
                                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                           requestBody.getEffectiveTime(),
                                                                           methodName);

                response.setElement(this.assembleAnchorSearchMatches(userId, anchorGUID, anchoredEntities, handler));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return a list of elements with the requested search string in their (display, resource)name, qualified name,
     * title, text, summary, identifier or description.  The search string is interpreted as a regular expression (RegEx).
     * The breadth of the search is determined by the supplied domain name. The results are organized by anchor element.
     *
     * @param serverName name of the server instances for this request
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId calling user
     * @param anchorDomainName name of open metadata type for the domain
     * @param requestBody string to search for in text
     * @param startFrom starting element (used in paging through large result sets)
     * @param pageSize maximum number of results to return
     *
     * @return list of results for assets that match the search string or
     * InvalidParameterException the searchString is invalid or
     * PropertyServerException there is a problem access in the property server or
     * UserNotAuthorizedException the user does not have access to the properties
     */
    public AnchorSearchMatchesListResponse findElementsInAnchorDomain(String                  serverName,
                                                                      String                  serviceURLMarker,
                                                                      String                  userId,
                                                                      String                  anchorDomainName,
                                                                      SearchStringRequestBody requestBody,
                                                                      int                     startFrom,
                                                                      int                     pageSize)
    {
        final String methodName = "findElementsInAnchorDomain";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AnchorSearchMatchesListResponse response = new AnchorSearchMatchesListResponse();
        AuditLog                        auditLog = null;

        try
        {
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                SearchProperties searchProperties = new SearchProperties();

                searchProperties.setMatchCriteria(MatchCriteria.ANY);
                searchProperties.setConditions(getPropertyConditions(requestBody.getSearchString(),
                                                                     getAnchorSearchesPropertyList()));

                SearchClassifications searchClassifications = getAnchorSearchClassifications(anchorDomainName,
                                                                                             OpenMetadataProperty.ANCHOR_DOMAIN_NAME.name);

                List<EntityDetail> anchoredEntities = handler.findEntities(userId,
                                                                           requestBody.getTypeName(),
                                                                           null,
                                                                           searchProperties,
                                                                           null,
                                                                           searchClassifications,
                                                                           requestBody.getAsOfTime(),
                                                                           null,
                                                                           null,
                                                                           false,
                                                                           false,
                                                                           startFrom,
                                                                           pageSize,
                                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                           requestBody.getEffectiveTime(),
                                                                           methodName);

                response.setElements(this.organizeAnchorSearchResults(userId, anchoredEntities, handler));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return a list of elements with the requested search string in their (display, resource)name, qualified name,
     * title, text, summary, identifier or description.  The search string is interpreted as a regular expression (RegEx).
     * The breadth of the search is determined by the supplied scope guid. The results are organized by anchor element.
     *
     * @param serverName name of the server instances for this request
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId calling user
     * @param anchorScopeGUID unique identifier of the scope to use
     * @param requestBody string to search for in text
     * @param startFrom starting element (used in paging through large result sets)
     * @param pageSize maximum number of results to return
     *
     * @return list of results for assets that match the search string or
     * InvalidParameterException the searchString is invalid or
     * PropertyServerException there is a problem access in the property server or
     * UserNotAuthorizedException the user does not have access to the properties
     */
    public AnchorSearchMatchesListResponse findElementsInAnchorScope(String                  serverName,
                                                                     String                  serviceURLMarker,
                                                                     String                  userId,
                                                                     String                  anchorScopeGUID,
                                                                     SearchStringRequestBody requestBody,
                                                                     int                     startFrom,
                                                                     int                     pageSize)
    {
        final String methodName = "findElementsInAnchorScope";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AnchorSearchMatchesListResponse response = new AnchorSearchMatchesListResponse();
        AuditLog                        auditLog = null;

        try
        {
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                SearchProperties searchProperties = new SearchProperties();

                searchProperties.setMatchCriteria(MatchCriteria.ANY);
                searchProperties.setConditions(getPropertyConditions(requestBody.getSearchString(),
                                                                     getAnchorSearchesPropertyList()));

                SearchClassifications searchClassifications = getAnchorSearchClassifications(anchorScopeGUID,
                                                                                             OpenMetadataProperty.ANCHOR_SCOPE_GUID.name);

                List<EntityDetail> anchoredEntities = handler.findEntities(userId,
                                                                           requestBody.getTypeName(),
                                                                           null,
                                                                           searchProperties,
                                                                           null,
                                                                           searchClassifications,
                                                                           requestBody.getAsOfTime(),
                                                                           null,
                                                                           null,
                                                                           false,
                                                                           false,
                                                                           startFrom,
                                                                           pageSize,
                                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                           requestBody.getEffectiveTime(),
                                                                           methodName);

                response.setElements(this.organizeAnchorSearchResults(userId, anchoredEntities, handler));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Organize the results of an anchor search by anchor entity.
     *
     * @param userId calling user
     * @param anchoredEntities search results
     * @param handler handler for more info
     * @return list of organized search results
     * @throws PropertyServerException problem with a converter
     */
    private List<AnchorSearchMatches> organizeAnchorSearchResults(String                                      userId,
                                                                  List<EntityDetail>                          anchoredEntities,
                                                                  MetadataElementHandler<OpenMetadataElement> handler) throws PropertyServerException
    {
        final String methodName = "formatAnchorSearchResults";

        List<AnchorSearchMatches> anchorSearchMatchesList = null;

        if (anchoredEntities != null)
        {
            anchorSearchMatchesList = new ArrayList<>();

            Map<String, Map<String, EntityDetail>> organizedEntities = new HashMap<>();

            for (EntityDetail entityDetail : anchoredEntities)
            {
                if (entityDetail != null)
                {
                    OpenMetadataAPIGenericHandler.AnchorIdentifiers anchorIdentifiers = handler.getAnchorsFromAnchorsClassification(entityDetail, methodName);

                    String anchorGUID;
                    if ((anchorIdentifiers == null) || (anchorIdentifiers.anchorGUID == null))
                    {
                        anchorGUID = entityDetail.getGUID();
                    }
                    else
                    {
                        anchorGUID = anchorIdentifiers.anchorGUID;
                    }

                    Map<String, EntityDetail> assetEntityMap = organizedEntities.get(anchorGUID);

                    if (assetEntityMap == null)
                    {
                        assetEntityMap = new HashMap<>();
                    }

                    assetEntityMap.put(entityDetail.getGUID(), entityDetail);
                    organizedEntities.put(anchorGUID, assetEntityMap);
                }
            }

            for (String anchorGUID : organizedEntities.keySet())
            {
                AnchorSearchMatches  anchorSearchMatches = this.assembleAnchorSearchMatches(userId,
                                                                                            anchorGUID,
                                                                                            new ArrayList<>(organizedEntities.get(anchorGUID).values()),
                                                                                            handler);
                if (anchorSearchMatches != null)
                {
                    anchorSearchMatchesList.add(anchorSearchMatches);
                }
            }
        }

        return anchorSearchMatchesList;
    }


    /**
     * Format the search results for a single anchor entity.
     *
     * @param userId calling user
     * @param anchorGUID unique identifier of the anchor
     * @param anchoredEntities search results
     * @param handler handler for more info
     * @return list of organized search results
     * @throws PropertyServerException problem with a converter
     */
    private AnchorSearchMatches assembleAnchorSearchMatches(String                                      userId,
                                                            String                                      anchorGUID,
                                                            List<EntityDetail>                          anchoredEntities,
                                                            MetadataElementHandler<OpenMetadataElement> handler) throws PropertyServerException
    {
        final String  methodName = "assembleAnchorSearchMatches";
        final String  parameterName = "anchorGUID";

        OpenMetadataAPIGenericConverter<OpenMetadataElement> converter = handler.getConverter();

        OpenMetadataElement anchorElement;

        try
        {
            anchorElement = handler.getBeanFromRepository(userId,
                                                          anchorGUID,
                                                          parameterName,
                                                          OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                          methodName);
        }
        catch (InvalidParameterException | UserNotAuthorizedException | PropertyServerException notVisible)
        {
            anchorElement = null;
        }

        if (anchorElement != null)
        {
            AnchorSearchMatches       anchorSearchMatches = new AnchorSearchMatches(anchorElement);
            List<OpenMetadataElement> anchoredElements    = new ArrayList<>();

            if (anchoredEntities != null)
            {
                for (EntityDetail anchoredEntity : anchoredEntities)
                {
                    OpenMetadataElement matchedElementElement = converter.getNewBean(OpenMetadataElement.class,
                                                                                     anchoredEntity,
                                                                                     methodName);

                    anchoredElements.add(matchedElementElement);
                }
            }

            anchorSearchMatches.setMatchingElements(anchoredElements);

            return anchorSearchMatches;
        }

        return null;
    }


    /**
     * Set up values for the anchors classification that controls the context of the search.
     *
     * @param contextValue value of the context
     * @param anchorPropertyName the property to match it against in the Anchors classification
     * @return search classification structure
     */
    private SearchClassifications getAnchorSearchClassifications(String contextValue,
                                                                 String anchorPropertyName)
    {
        SearchClassifications         searchClassifications            = new SearchClassifications();
        List<ClassificationCondition> classificationConditions         = new ArrayList<>();
        ClassificationCondition       classificationCondition          = new ClassificationCondition();
        SearchProperties              classificationSearchProperties   = new SearchProperties();
        List<PropertyCondition>       classificationPropertyConditions = new ArrayList<>();
        PropertyCondition             classificationPropertyCondition  = new PropertyCondition();
        PrimitivePropertyValue        classificationPropertyValue      = new PrimitivePropertyValue();

        classificationPropertyValue.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
        classificationPropertyValue.setPrimitiveValue(contextValue);
        classificationPropertyValue.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
        classificationPropertyValue.setTypeGUID(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getGUID());

        classificationPropertyCondition.setProperty(anchorPropertyName);
        classificationPropertyCondition.setOperator(PropertyComparisonOperator.EQ);
        classificationPropertyCondition.setValue(classificationPropertyValue);
        classificationPropertyConditions.add(classificationPropertyCondition);
        classificationSearchProperties.setMatchCriteria(MatchCriteria.ALL);
        classificationSearchProperties.setConditions(classificationPropertyConditions);

        classificationCondition.setName(OpenMetadataType.ANCHORS_CLASSIFICATION.typeName);
        classificationCondition.setMatchProperties(classificationSearchProperties);
        classificationConditions.add(classificationCondition);
        searchClassifications.setMatchCriteria(MatchCriteria.ALL);
        searchClassifications.setConditions(classificationConditions);

        return searchClassifications;
    }


    /**
     * Set up the search string in a property value structure.
     *
     * @param searchString requested search string
     * @return instance property value
     */
    private InstancePropertyValue getSearchPropertyValue(String searchString)
    {
        PrimitivePropertyValue  primitivePropertyValue        = new PrimitivePropertyValue();

        primitivePropertyValue.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
        primitivePropertyValue.setPrimitiveValue(searchString);
        primitivePropertyValue.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
        primitivePropertyValue.setTypeGUID(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getGUID());

        return primitivePropertyValue;
    }


    /**
     * Return a property condition used to control the search.
     *
     * @param searchString value to search for
     * @param propertyName property to look in
     * @return property condition
     */
    private PropertyCondition getSearchPropertyCondition(String searchString,
                                                         String propertyName)
    {
        PropertyCondition propertyCondition = new PropertyCondition();

        propertyCondition.setProperty(propertyName);
        propertyCondition.setOperator(PropertyComparisonOperator.LIKE);
        propertyCondition.setValue(getSearchPropertyValue(searchString));

        return propertyCondition;
    }


    /**
     * Build a set of property conditions for a search on a list of properties.
     *
     * @param searchString value to search for
     * @param propertyNames places to look
     * @return list of property conditions
     */
    private List<PropertyCondition> getPropertyConditions(String       searchString,
                                                          List<String> propertyNames)
    {
        List<PropertyCondition> propertyConditions = null;

        if (propertyNames != null)
        {
            propertyConditions = new ArrayList<>();

            for (String propertyName : propertyNames)
            {
                if (propertyName != null)
                {
                    propertyConditions.add(getSearchPropertyCondition(searchString, propertyName));
                }
            }
        }

        return propertyConditions;
    }


    /**
     * Defines the list of properties that an anchor search uses.
     *
     * @return list of property names
     */
    private List<String> getAnchorSearchesPropertyList()
    {
        List<String> propertyList = new ArrayList<>();

        propertyList.add(OpenMetadataProperty.QUALIFIED_NAME.name);
        propertyList.add(OpenMetadataProperty.NAME.name);
        propertyList.add(OpenMetadataProperty.DISPLAY_NAME.name);
        propertyList.add(OpenMetadataProperty.DEPLOYED_IMPLEMENTATION_TYPE.name);
        propertyList.add(OpenMetadataProperty.RESOURCE_NAME.name);
        propertyList.add(OpenMetadataProperty.PATH_NAME.name);
        propertyList.add(OpenMetadataProperty.IDENTIFIER.name);
        propertyList.add(OpenMetadataProperty.TITLE.name);
        propertyList.add(OpenMetadataProperty.SUMMARY.name);
        propertyList.add(OpenMetadataProperty.TEXT.name);
        propertyList.add(OpenMetadataProperty.DESCRIPTION.name);

        return propertyList;
    }

    /**
     * Retrieve the metadata elements connected to the supplied element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier for the starting metadata element
     * @param relationshipTypeName type name of relationships to follow (or null for all)
     * @param startingAtEnd indicates which end to retrieve from (0 is "either end"; 1 is end1; 2 is end 2)
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param effectiveTime only return an element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return list of related elements
     *  InvalidParameterException the unique identifier is null or not known; the relationship type is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public RelatedMetadataElementListResponse getRelatedMetadataElements(String  serverName,
                                                                         String  serviceURLMarker,
                                                                         String  userId,
                                                                         String  elementGUID,
                                                                         String  relationshipTypeName,
                                                                         boolean forLineage,
                                                                         boolean forDuplicateProcessing,
                                                                         long    effectiveTime,
                                                                         int     startingAtEnd,
                                                                         int     startFrom,
                                                                         int     pageSize)
    {
        if (effectiveTime != 0)
        {
            ResultsRequestBody requestBody = new ResultsRequestBody();

            requestBody.setEffectiveTime(new Date(effectiveTime));
            return getRelatedMetadataElements(serverName,
                                              serviceURLMarker,
                                              userId,
                                              elementGUID,
                                              relationshipTypeName,
                                              forLineage,
                                              forDuplicateProcessing,
                                              startingAtEnd,
                                              startFrom,
                                              pageSize,
                                              requestBody);
        }
        else
        {
            return getRelatedMetadataElements(serverName,
                                              serviceURLMarker,
                                              userId,
                                              elementGUID,
                                              relationshipTypeName,
                                              forLineage,
                                              forDuplicateProcessing,
                                              startingAtEnd,
                                              startFrom,
                                              pageSize,
                                              null);
        }
    }


    /**
     * Retrieve the metadata elements connected to the supplied element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier for the starting metadata element
     * @param relationshipTypeName type name of relationships to follow (or null for all)
     * @param startingAtEnd indicates which end to retrieve from (0 is "either end"; 1 is end1; 2 is end 2)
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param requestBody only return an element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     *
     * @return list of related elements
     *  InvalidParameterException the unique identifier is null or not known; the relationship type is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public RelatedMetadataElementListResponse getRelatedMetadataElements(String             serverName,
                                                                         String             serviceURLMarker,
                                                                         String             userId,
                                                                         String             elementGUID,
                                                                         String             relationshipTypeName,
                                                                         boolean            forLineage,
                                                                         boolean            forDuplicateProcessing,
                                                                         int                startingAtEnd,
                                                                         int                startFrom,
                                                                         int                pageSize,
                                                                         ResultsRequestBody requestBody)
    {
        final String methodName = "getRelatedMetadataElements";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                           auditLog = null;
        RelatedMetadataElementListResponse response = new RelatedMetadataElementListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            RelatedMetadataElementList relatedElementList = new RelatedMetadataElementList();

            if (requestBody != null)
            {
                relatedElementList.setElementList(handler.getRelatedMetadataElements(userId,
                                                                                  elementGUID,
                                                                                  startingAtEnd,
                                                                                  relationshipTypeName,
                                                                                  requestBody.getLimitResultsByStatus(),
                                                                                  requestBody.getAsOfTime(),
                                                                                  requestBody.getSequencingProperty(),
                                                                                  requestBody.getSequencingOrder(),
                                                                                  forLineage,
                                                                                  forDuplicateProcessing,
                                                                                  instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                  requestBody.getEffectiveTime(),
                                                                                  startFrom,
                                                                                  pageSize,
                                                                                  methodName));
            }
            else
            {
                relatedElementList.setElementList(handler.getRelatedMetadataElements(userId,
                                                                                  elementGUID,
                                                                                  startingAtEnd,
                                                                                  relationshipTypeName,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  null,
                                                                                  forLineage,
                                                                                  forDuplicateProcessing,
                                                                                  instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                  null,
                                                                                  startFrom,
                                                                                  pageSize,
                                                                                  methodName));
            }

            if (relatedElementList.getElementList() != null)
            {
                OpenMetadataMermaidGraphBuilder graphBuilder = new OpenMetadataMermaidGraphBuilder(elementGUID, relatedElementList.getElementList());
                relatedElementList.setMermaidGraph(graphBuilder.getMermaidGraph());
            }

            response.setRelatedElementList(relatedElementList);
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the relationships linking to the supplied elements.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementAtEnd1GUID unique identifier of the metadata element at end 1 of the relationship
     * @param metadataElementAtEnd2GUID unique identifier of the metadata element at end 2 of the relationship
     * @param relationshipTypeName type name of relationships to follow (or null for all)
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param effectiveTime only return an element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return list of related elements
     *  InvalidParameterException the unique identifier is null or not known; the relationship type is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataRelationshipListResponse getMetadataElementRelationships(String  serverName,
                                                                                String  serviceURLMarker,
                                                                                String  userId,
                                                                                String  metadataElementAtEnd1GUID,
                                                                                String  relationshipTypeName,
                                                                                String  metadataElementAtEnd2GUID,
                                                                                boolean forLineage,
                                                                                boolean forDuplicateProcessing,
                                                                                long    effectiveTime,
                                                                                int     startFrom,
                                                                                int     pageSize)
    {
        if (effectiveTime != 0)
        {
            ResultsRequestBody requestBody = new ResultsRequestBody();
            requestBody.setEffectiveTime(new Date(effectiveTime));

            return this.getMetadataElementRelationships(serverName,
                                                        serviceURLMarker,
                                                        userId,
                                                        metadataElementAtEnd1GUID,
                                                        relationshipTypeName,
                                                        metadataElementAtEnd2GUID,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        startFrom,
                                                        pageSize,
                                                        requestBody);
        }
        else
        {
            return this.getMetadataElementRelationships(serverName,
                                                        serviceURLMarker,
                                                        userId,
                                                        metadataElementAtEnd1GUID,
                                                        relationshipTypeName,
                                                        metadataElementAtEnd2GUID,
                                                        forLineage,
                                                        forDuplicateProcessing,
                                                        startFrom,
                                                        pageSize,
                                                        null);
        }
    }


    /**
     * Retrieve the relationships linking to the supplied elements.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementAtEnd1GUID unique identifier of the metadata element at end 1 of the relationship
     * @param metadataElementAtEnd2GUID unique identifier of the metadata element at end 2 of the relationship
     * @param relationshipTypeName type name of relationships to follow (or null for all)
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param requestBody only return an element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return list of related elements
     *  InvalidParameterException the unique identifier is null or not known; the relationship type is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataRelationshipListResponse getMetadataElementRelationships(String             serverName,
                                                                                String             serviceURLMarker,
                                                                                String             userId,
                                                                                String             metadataElementAtEnd1GUID,
                                                                                String             relationshipTypeName,
                                                                                String             metadataElementAtEnd2GUID,
                                                                                boolean            forLineage,
                                                                                boolean            forDuplicateProcessing,
                                                                                int                startFrom,
                                                                                int                pageSize,
                                                                                ResultsRequestBody requestBody)
    {
        final String methodName = "getMetadataElementRelationships";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                             auditLog = null;
        OpenMetadataRelationshipListResponse response = new OpenMetadataRelationshipListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            OpenMetadataRelationshipList relationshipList = new OpenMetadataRelationshipList();
            if (requestBody != null)
            {
                relationshipList.setElementList(handler.getMetadataElementRelationships(userId,
                                                                                     metadataElementAtEnd1GUID,
                                                                                     relationshipTypeName,
                                                                                     metadataElementAtEnd2GUID,
                                                                                     requestBody.getLimitResultsByStatus(),
                                                                                     requestBody.getAsOfTime(),
                                                                                     requestBody.getSequencingProperty(),
                                                                                     requestBody.getSequencingOrder(),
                                                                                     forLineage,
                                                                                     forDuplicateProcessing,
                                                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                     requestBody.getEffectiveTime(),
                                                                                     startFrom,
                                                                                     pageSize,
                                                                                     methodName));
            }
            else
            {
                relationshipList.setElementList(handler.getMetadataElementRelationships(userId,
                                                                                     metadataElementAtEnd1GUID,
                                                                                     relationshipTypeName,
                                                                                     metadataElementAtEnd2GUID,
                                                                                     null,
                                                                                     null,
                                                                                     null,
                                                                                     null,
                                                                                     forLineage,
                                                                                     forDuplicateProcessing,
                                                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                     null,
                                                                                     startFrom,
                                                                                     pageSize,
                                                                                     methodName));
            }

            if (response.getRelationshipList() != null)
            {
                OpenMetadataMermaidGraphBuilder graphBuilder = new OpenMetadataMermaidGraphBuilder(metadataElementAtEnd1GUID, metadataElementAtEnd2GUID, relationshipList.getElementList());
                relationshipList.setMermaidGraph(graphBuilder.getMermaidGraph());
            }

            response.setRelationshipList(relationshipList);
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return a list of metadata elements that match the supplied criteria.  The results can be returned over many pages.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param requestBody properties defining the search criteria
     *
     * @return a list of elements matching the supplied criteria; null means no matching elements in the metadata store.
     *  InvalidParameterException one of the search parameters are is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataElementsResponse findMetadataElements(String          serverName,
                                                             String          serviceURLMarker,
                                                             String          userId,
                                                             boolean         forLineage,
                                                             boolean         forDuplicateProcessing,
                                                             int             startFrom,
                                                             int             pageSize,
                                                             FindRequestBody requestBody)
    {
        final String methodName = "findMetadataElements";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        OpenMetadataElementsResponse response = new OpenMetadataElementsResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                if ((requestBody.getSearchProperties() != null) || (requestBody.getMatchClassifications() != null))
                {
                    response.setElementList(handler.findMetadataElements(userId,
                                                                         requestBody.getMetadataElementTypeName(),
                                                                         requestBody.getMetadataElementSubtypeNames(),
                                                                         requestBody.getSearchProperties(),
                                                                         requestBody.getLimitResultsByStatus(),
                                                                         requestBody.getMatchClassifications(),
                                                                         requestBody.getAsOfTime(),
                                                                         requestBody.getSequencingProperty(),
                                                                         requestBody.getSequencingOrder(),
                                                                         forLineage,
                                                                         forDuplicateProcessing,
                                                                         instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                         requestBody.getEffectiveTime(),
                                                                         startFrom,
                                                                         pageSize,
                                                                         methodName));
                }
                else
                {
                    response.setElementList(handler.getMetadataElementsByType(userId,
                                                                              requestBody.getMetadataElementTypeName(),
                                                                              forLineage,
                                                                              forDuplicateProcessing,
                                                                              requestBody.getLimitResultsByStatus(),
                                                                              requestBody.getAsOfTime(),
                                                                              requestBody.getSequencingProperty(),
                                                                              requestBody.getSequencingOrder(),
                                                                              instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                              requestBody.getEffectiveTime(),
                                                                              startFrom,
                                                                              pageSize,
                                                                              methodName));
                }
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return all the elements that are anchored to an asset plus relationships between these elements and to other elements.
     *
     * @param serverName name of the server instances for this request
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId the userId of the requesting user
     * @param elementGUID  unique identifier for the element
     * @param startFrom starting element (used in paging through large result sets)
     * @param pageSize maximum number of results to return
     * @param requestBody effective time and asOfTime
     *
     * @return graph of elements or
     * InvalidParameterException - one of the parameters is null or invalid or
     * PropertyServerException - there is a problem retrieving the connected asset properties from the property server or
     * UserNotAuthorizedException - the requesting user is not authorized to issue this request.
     */
    public OpenMetadataGraphResponse getAnchoredElementsGraph(String             serverName,
                                                              String             serviceURLMarker,
                                                              String             userId,
                                                              String             elementGUID,
                                                              int                startFrom,
                                                              int                pageSize,
                                                              AnyTimeRequestBody requestBody)
    {
        final String parameterName = "elementGUID";
        final String methodName    = "getAnchoredElementsGraph";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        OpenMetadataGraphResponse response = new OpenMetadataGraphResponse();
        AuditLog                  auditLog = null;

        try
        {
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            OpenMetadataElement anchorElement;
            Date                effectiveTime = new Date();
            Date                asOfTime = null;
            boolean             forLineage = false;
            boolean             forDuplicateProcessing = false;

            if (requestBody != null)
            {
                effectiveTime = requestBody.getEffectiveTime();
                asOfTime      = requestBody.getAsOfTime();
                forLineage    = requestBody.getForLineage();
                forDuplicateProcessing = requestBody.getForDuplicateProcessing();

                anchorElement = handler.getBeanFromRepository(userId,
                                                              elementGUID,
                                                              parameterName,
                                                              OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                              forLineage,
                                                              forDuplicateProcessing,
                                                              instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                              asOfTime,
                                                              effectiveTime,
                                                              methodName);
            }
            else
            {
                anchorElement = handler.getBeanFromRepository(userId,
                                                              elementGUID,
                                                              parameterName,
                                                              OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                              forLineage,
                                                              forDuplicateProcessing,
                                                              instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                              asOfTime,
                                                              effectiveTime,
                                                              methodName);
            }

            if (anchorElement != null)
            {
                OpenMetadataElementGraph elementGraph = new OpenMetadataElementGraph(anchorElement);

                Map<String, Relationship> receivedRelationships = new HashMap<>();

                List<Relationship>  relationships = handler.getAllAttachmentLinks(userId,
                                                                                  anchorElement.getElementGUID(),
                                                                                  parameterName,
                                                                                  anchorElement.getType().getTypeName(),
                                                                                  null,
                                                                                  asOfTime,
                                                                                  SequencingOrder.CREATION_DATE_RECENT,
                                                                                  null,
                                                                                  forLineage,
                                                                                  forDuplicateProcessing,
                                                                                  effectiveTime,
                                                                                  methodName);
                if (relationships != null)
                {
                    for (Relationship relationship : relationships)
                    {
                        if (relationship != null)
                        {
                            receivedRelationships.put(relationship.getGUID(), relationship);
                        }
                    }
                }

                SearchClassifications         searchClassifications    = new SearchClassifications();
                List<ClassificationCondition> classificationConditions = new ArrayList<>();
                ClassificationCondition       classificationCondition  = new ClassificationCondition();
                SearchProperties              searchProperties         = new SearchProperties();
                List<PropertyCondition>       propertyConditions       = new ArrayList<>();
                PropertyCondition             propertyCondition        = new PropertyCondition();
                PrimitivePropertyValue        primitivePropertyValue   = new PrimitivePropertyValue();

                primitivePropertyValue.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
                primitivePropertyValue.setPrimitiveValue(anchorElement.getElementGUID());
                primitivePropertyValue.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
                primitivePropertyValue.setTypeGUID(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getGUID());

                propertyCondition.setProperty(OpenMetadataProperty.ANCHOR_GUID.name);
                propertyCondition.setOperator(PropertyComparisonOperator.EQ);
                propertyCondition.setValue(primitivePropertyValue);
                propertyConditions.add(propertyCondition);
                searchProperties.setMatchCriteria(MatchCriteria.ALL);
                searchProperties.setConditions(propertyConditions);

                classificationCondition.setName(OpenMetadataType.ANCHORS_CLASSIFICATION.typeName);
                classificationCondition.setMatchProperties(searchProperties);
                classificationConditions.add(classificationCondition);
                searchClassifications.setMatchCriteria(MatchCriteria.ALL);
                searchClassifications.setConditions(classificationConditions);

                List<OpenMetadataElement> anchoredElements = handler.findBeans(userId,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               null,
                                                                               searchClassifications,
                                                                               asOfTime,
                                                                               null,
                                                                               SequencingOrder.CREATION_DATE_RECENT,
                                                                               forLineage,
                                                                               forDuplicateProcessing,
                                                                               startFrom,
                                                                               pageSize,
                                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                               effectiveTime,
                                                                               methodName);

                elementGraph.setAnchoredElements(anchoredElements);

                if (anchoredElements != null)
                {
                    final String anchoredElementParameterName = "anchoredElement.getGUID";

                    for (OpenMetadataElement metadataElement : anchoredElements)
                    {
                        if (metadataElement != null)
                        {
                            relationships = handler.getAllAttachmentLinks(userId,
                                                                          metadataElement.getElementGUID(),
                                                                          anchoredElementParameterName,
                                                                          metadataElement.getType().getTypeName(),
                                                                          null,
                                                                          asOfTime,
                                                                          SequencingOrder.CREATION_DATE_RECENT,
                                                                          null,
                                                                          forLineage,
                                                                          forDuplicateProcessing,
                                                                          effectiveTime,
                                                                          methodName);
                            if (relationships != null)
                            {
                                for (Relationship relationship : relationships)
                                {
                                    if (relationship != null)
                                    {
                                        receivedRelationships.put(relationship.getGUID(), relationship);
                                    }
                                }
                            }
                        }
                    }
                }

                if (! receivedRelationships.isEmpty())
                {
                    OpenMetadataRelationshipConverter<OpenMetadataRelationship> converter = new OpenMetadataRelationshipConverter<>(handler.getRepositoryHelper(),
                                                                                                                                    handler.getServiceName(),
                                                                                                                                    serverName);

                    List<OpenMetadataRelationship> metadataRelationships = new ArrayList<>();

                    for (Relationship relationship: receivedRelationships.values())
                    {
                        if (relationship != null)
                        {
                            OpenMetadataRelationship metadataRelationship = converter.getNewRelationshipBean(OpenMetadataRelationship.class,
                                                                                                             relationship,
                                                                                                             methodName);

                            metadataRelationships.add(metadataRelationship);
                        }
                    }

                    elementGraph.setRelationships(metadataRelationships);
                }

                OpenMetadataMermaidGraphBuilder graphBuilder = new OpenMetadataMermaidGraphBuilder(elementGraph);
                elementGraph.setMermaidGraph(graphBuilder.getMermaidGraph());

                response.setElementGraph(elementGraph);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return a list of relationships that match the requested conditions.  The results can be received as a series of pages.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param forLineage the retrieved element is for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param requestBody properties defining the search criteria
     *
     * @return a list of relationships - null means no matching relationships - or
     *  InvalidParameterException one of the search parameters are is invalid
     *  UserNotAuthorizedException the governance action service is not able to access the elements
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataRelationshipListResponse findRelationshipsBetweenMetadataElements(String                      serverName,
                                                                                         String                      serviceURLMarker,
                                                                                         String                      userId,
                                                                                         boolean                     forLineage,
                                                                                         boolean                     forDuplicateProcessing,
                                                                                         int                         startFrom,
                                                                                         int                         pageSize,
                                                                                         FindRelationshipRequestBody requestBody)
    {
        final String methodName = "findRelationshipsBetweenMetadataElements";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                             auditLog = null;
        OpenMetadataRelationshipListResponse response = new OpenMetadataRelationshipListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                OpenMetadataRelationshipList relationshipList = new OpenMetadataRelationshipList();

                if (requestBody.getSearchProperties() != null)
                {
                    relationshipList.setElementList(handler.findRelationshipsBetweenMetadataElements(userId,
                                                                                                  requestBody.getRelationshipTypeName(),
                                                                                                  requestBody.getSearchProperties(),
                                                                                                  requestBody.getLimitResultsByStatus(),
                                                                                                  requestBody.getAsOfTime(),
                                                                                                  requestBody.getSequencingProperty(),
                                                                                                  requestBody.getSequencingOrder(),
                                                                                                  forLineage,
                                                                                                  forDuplicateProcessing,
                                                                                                  instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                                  requestBody.getEffectiveTime(),
                                                                                                  startFrom,
                                                                                                  pageSize,
                                                                                                  methodName));
                }
                else
                {
                    relationshipList.setElementList(handler.getRelationshipsByType(userId,
                                                                                requestBody.getRelationshipTypeName(),
                                                                                forLineage,
                                                                                forDuplicateProcessing,
                                                                                requestBody.getLimitResultsByStatus(),
                                                                                requestBody.getAsOfTime(),
                                                                                requestBody.getSequencingProperty(),
                                                                                requestBody.getSequencingOrder(),
                                                                                instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                requestBody.getEffectiveTime(),
                                                                                startFrom,
                                                                                pageSize,
                                                                                methodName));
                }

                if (relationshipList.getElementList() != null)
                {
                    OpenMetadataMermaidGraphBuilder graphBuilder = new OpenMetadataMermaidGraphBuilder(relationshipList.getElementList());
                    relationshipList.setMermaidGraph(graphBuilder.getMermaidGraph());
                }

                response.setRelationshipList(relationshipList);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }



    /**
     * Retrieve the relationship using its unique identifier.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param relationshipGUID unique identifier for the relationship
     * @param requestBody only return the element if it is effective at this time. Null means anytime. Use "new Date()" for now.
     *
     * @return metadata element properties or
     *  InvalidParameterException the unique identifier is null or not known.
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public OpenMetadataRelationshipResponse getRelationshipByGUID(String             serverName,
                                                                  String             serviceURLMarker,
                                                                  String             userId,
                                                                  String             relationshipGUID,
                                                                  AnyTimeRequestBody requestBody)
    {
        final String methodName = "getRelationshipByGUID";
        final String guidParameterName = "relationshipGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                         auditLog = null;
        OpenMetadataRelationshipResponse response = new OpenMetadataRelationshipResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            Relationship relationship;
            if (requestBody != null)
            {
                relationship = handler.getAttachmentLink(userId,
                                                         relationshipGUID,
                                                         guidParameterName,
                                                         null,
                                                         requestBody.getAsOfTime(),
                                                         requestBody.getForLineage(),
                                                         requestBody.getForDuplicateProcessing(),
                                                         handler.getSupportedZones(),
                                                         requestBody.getEffectiveTime(),
                                                         methodName);
            }
            else
            {
                relationship = handler.getAttachmentLink(userId,
                                                         relationshipGUID,
                                                         guidParameterName,
                                                         null,
                                                         null,
                                                         false,
                                                         false,
                                                         handler.getSupportedZones(),
                                                         new Date(),
                                                         methodName);
            }

            if (relationship != null)
            {
                OpenMetadataRelationshipConverter<OpenMetadataRelationship> converter = new OpenMetadataRelationshipConverter<>(handler.getRepositoryHelper(),
                                                                                                                                handler.getServiceName(),
                                                                                                                                serverName);
                response.setElement(converter.getNewRelationshipBean(OpenMetadataRelationship.class,
                                                                     relationship,
                                                                     methodName));
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }



    /**
     * Retrieve all the versions of an element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param relationshipGUID unique identifier for the metadata element
     * @param forLineage the retrieved elements are for lineage processing so include archived elements
     * @param forDuplicateProcessing the retrieved elements are for duplicate processing so do not combine results from known duplicates.
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param oldestFirst  defining how the results should be ordered.
     * @param requestBody the time window required
     *
     * @return list of matching metadata elements (or null if no elements match the name) or
     *  InvalidParameterException the qualified name is null
     *  UserNotAuthorizedException the governance action service is not able to access the element
     *  PropertyServerException there is a problem accessing the metadata store
     */
    public OpenMetadataRelationshipListResponse getRelationshipHistory(String             serverName,
                                                                       String             serviceURLMarker,
                                                                       String             userId,
                                                                       String             relationshipGUID,
                                                                       boolean            forLineage,
                                                                       boolean            forDuplicateProcessing,
                                                                       int                startFrom,
                                                                       int                pageSize,
                                                                       boolean            oldestFirst,
                                                                       HistoryRequestBody requestBody)
    {
        final String methodName = "getRelationshipHistory";
        final String guidParameterName  = "relationshipGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                             auditLog = null;
        OpenMetadataRelationshipListResponse response = new OpenMetadataRelationshipListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            HistorySequencingOrder sequencingOrder = HistorySequencingOrder.BACKWARDS;

            if (oldestFirst)
            {
                sequencingOrder = HistorySequencingOrder.FORWARDS;
            }

            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            List<Relationship> relationships;
            if (requestBody != null)
            {
                relationships = handler.getRelationshipHistory(userId,
                                                               relationshipGUID,
                                                               guidParameterName,
                                                               requestBody.getFromTime(),
                                                               requestBody.getToTime(),
                                                               startFrom,
                                                               pageSize,
                                                               sequencingOrder,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                               requestBody.getEffectiveTime(),
                                                               methodName);
            }
            else
            {
                 relationships = handler.getRelationshipHistory(userId,
                                                               relationshipGUID,
                                                               guidParameterName,
                                                               null,
                                                               null,
                                                               startFrom,
                                                               pageSize,
                                                               sequencingOrder,
                                                               forLineage,
                                                               forDuplicateProcessing,
                                                               instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                               null,
                                                               methodName);
            }

            OpenMetadataRelationshipList openMetadataRelationshipList = new OpenMetadataRelationshipList();
            openMetadataRelationshipList.setElementList(handler.convertOpenMetadataRelationships(relationships, methodName));

            response.setRelationshipList(openMetadataRelationshipList);
            // to do add mermaid graph
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create a new metadata element in the metadata store.  The type name comes from the open metadata types.
     * The selected type also controls the names and types of the properties that are allowed.
     * This version of the method allows access to advanced features such as multiple states and
     * effectivity dates.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param requestBody properties for the new element
     *
     * @return unique identifier of the new metadata element
     *  InvalidParameterException the type name, status or one of the properties is invalid
     *  UserNotAuthorizedException the governance action service is not authorized to create this type of element
     *  PropertyServerException there is a problem with the metadata store
     */
    public GUIDResponse createMetadataElementInStore(String                            serverName,
                                                     String                            serviceURLMarker,
                                                     String                            userId,
                                                     NewOpenMetadataElementRequestBody requestBody)
    {
        final String methodName = "createMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        GUIDResponse response = new GUIDResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setGUID(handler.createMetadataElementInStore(userId,
                                                                      requestBody.getExternalSourceGUID(),
                                                                      requestBody.getExternalSourceName(),
                                                                      requestBody.getTypeName(),
                                                                      requestBody.getInitialStatus(),
                                                                      requestBody.getInitialClassifications(),
                                                                      requestBody.getAnchorGUID(),
                                                                      requestBody.getIsOwnAnchor(),
                                                                      requestBody.getAnchorScopeGUID(),
                                                                      requestBody.getEffectiveFrom(),
                                                                      requestBody.getEffectiveTo(),
                                                                      requestBody.getProperties(),
                                                                      requestBody.getParentGUID(),
                                                                      requestBody.getParentRelationshipTypeName(),
                                                                      requestBody.getParentRelationshipProperties(),
                                                                      requestBody.getParentAtEnd1(),
                                                                      instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                      requestBody.getForLineage(),
                                                                      requestBody.getForDuplicateProcessing(),
                                                                      requestBody.getEffectiveTime(),
                                                                      methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create a new metadata element in the metadata store using a template.  The type name comes from the open metadata types.
     * The selected type also controls the names and types of the properties that are allowed.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param allowRetrieve can an existing element be returned if it exists
     * @param requestBody properties for the new element
     *
     * @return unique identifier of the new metadata element
     *  InvalidParameterException the type name, status or one of the properties is invalid
     *  UserNotAuthorizedException the governance action service is not authorized to create this type of element
     *  PropertyServerException there is a problem with the metadata store
     */
    public GUIDResponse createMetadataElementFromTemplate(String              serverName,
                                                          String              serviceURLMarker,
                                                          String              userId,
                                                          boolean             allowRetrieve,
                                                          TemplateRequestBody requestBody)
    {
        final String methodName = "createMetadataElementFromTemplate";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        GUIDResponse response = new GUIDResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setGUID(handler.createMetadataElementFromTemplate(userId,
                                                                           requestBody.getExternalSourceGUID(),
                                                                           requestBody.getExternalSourceName(),
                                                                           requestBody.getTypeName(),
                                                                           requestBody.getAnchorGUID(),
                                                                           requestBody.getIsOwnAnchor(),
                                                                           requestBody.getAnchorScopeGUID(),
                                                                           allowRetrieve,
                                                                           requestBody.getEffectiveFrom(),
                                                                           requestBody.getEffectiveTo(),
                                                                           requestBody.getTemplateGUID(),
                                                                           requestBody.getReplacementProperties(),
                                                                           requestBody.getPlaceholderPropertyValues(),
                                                                           requestBody.getParentGUID(),
                                                                           requestBody.getParentRelationshipTypeName(),
                                                                           requestBody.getParentRelationshipProperties(),
                                                                           requestBody.getParentAtEnd1(),
                                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                           requestBody.getForLineage(),
                                                                           requestBody.getForDuplicateProcessing(),
                                                                           requestBody.getEffectiveTime(),
                                                                           methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the properties of a specific metadata element.  The properties must match the type definition associated with the
     * metadata element when it was created.  However, it is possible to update a few properties, or replace all them by
     * the value used in the replaceProperties flag.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param requestBody new properties
     *
     * @return void or
     *  InvalidParameterException either the unique identifier or the properties are invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateMetadataElementInStore(String                      serverName,
                                                     String                      serviceURLMarker,
                                                     String                      userId,
                                                     String                      metadataElementGUID,
                                                     UpdatePropertiesRequestBody requestBody)
    {
        final String methodName = "updateMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateMetadataElementInStore(userId,
                                                     requestBody.getExternalSourceGUID(),
                                                     requestBody.getExternalSourceName(),
                                                     metadataElementGUID,
                                                     requestBody.getReplaceProperties(),
                                                     requestBody.getForLineage(),
                                                     requestBody.getForDuplicateProcessing(),
                                                     requestBody.getProperties(),
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     requestBody.getEffectiveTime(),
                                                     methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the status of specific metadata element. The new status must match a status value that is defined for the element's type
     * assigned when it was created.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param requestBody new status values - use null to leave as is
     *
     * @return void or
     *  InvalidParameterException either the unique identifier or the status are invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateMetadataElementStatusInStore(String                  serverName,
                                                           String                  serviceURLMarker,
                                                           String                  userId,
                                                           String                  metadataElementGUID,
                                                           UpdateStatusRequestBody requestBody)
    {
        final String methodName = "updateMetadataElementStatusInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateMetadataElementStatusInStore(userId,
                                                           requestBody.getExternalSourceGUID(),
                                                           requestBody.getExternalSourceName(),
                                                           metadataElementGUID,
                                                           requestBody.getNewStatus(),
                                                           requestBody.getForLineage(),
                                                           requestBody.getForDuplicateProcessing(),
                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                           requestBody.getEffectiveTime(),
                                                           methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the effectivity dates control the visibility of the element through specific APIs.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param requestBody new status values - use null to leave as is
     *
     * @return void or
     *  InvalidParameterException either the unique identifier or the status are invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateMetadataElementEffectivityInStore(String                            serverName,
                                                                String                            serviceURLMarker,
                                                                String                            userId,
                                                                String                            metadataElementGUID,
                                                                UpdateEffectivityDatesRequestBody requestBody)
    {
        final String methodName = "updateMetadataElementEffectivityInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateMetadataElementEffectivityInStore(userId,
                                                                requestBody.getExternalSourceGUID(),
                                                                requestBody.getExternalSourceName(),
                                                                metadataElementGUID,
                                                                requestBody.getForLineage(),
                                                                requestBody.getForDuplicateProcessing(),
                                                                requestBody.getEffectiveFrom(),
                                                                requestBody.getEffectiveTo(),
                                                                instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                requestBody.getEffectiveTime(),
                                                                methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Delete a specific metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param cascadedDelete     boolean indicating whether the delete request can cascade to dependent elements
     * @param requestBody null request body
     *
     * @return void or
     *  InvalidParameterException the unique identifier is null or invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to delete this element
     *  PropertyServerException there is a problem with the metadata store
     */
    @SuppressWarnings(value = "unused")
    public  VoidResponse deleteMetadataElementInStore(String                    serverName,
                                                      String                    serviceURLMarker,
                                                      String                    userId,
                                                      String                    metadataElementGUID,
                                                      boolean                   cascadedDelete,
                                                      MetadataSourceRequestBody requestBody)
    {
        final String methodName = "deleteMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            if (requestBody != null)
            {
                handler.deleteMetadataElementInStore(userId,
                                                     requestBody.getExternalSourceGUID(),
                                                     requestBody.getExternalSourceName(),
                                                     metadataElementGUID,
                                                     cascadedDelete,
                                                     requestBody.getForLineage(),
                                                     requestBody.getForDuplicateProcessing(),
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     requestBody.getEffectiveTime(),
                                                     methodName);
            }
            else
            {
                handler.deleteMetadataElementInStore(userId,
                                                     null,
                                                     null,
                                                     metadataElementGUID,
                                                     cascadedDelete,
                                                     false,
                                                     false,
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     new Date(),
                                                     methodName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Archive a specific metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param requestBody null request body
     *
     * @return void or
     *  InvalidParameterException the unique identifier is null or invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to archive this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public  VoidResponse archiveMetadataElementInStore(String             serverName,
                                                       String             serviceURLMarker,
                                                       String             userId,
                                                       String             metadataElementGUID,
                                                       ArchiveRequestBody requestBody)
    {
        final String methodName = "archiveMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

            if (requestBody != null)
            {
                handler.archiveMetadataElementInStore(userId,
                                                     requestBody.getExternalSourceGUID(),
                                                     requestBody.getExternalSourceName(),
                                                     metadataElementGUID,
                                                     requestBody.getArchiveProperties(),
                                                     requestBody.getForLineage(),
                                                     requestBody.getForDuplicateProcessing(),
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     requestBody.getEffectiveTime(),
                                                     methodName);
            }
            else
            {
                handler.archiveMetadataElementInStore(userId,
                                                      null,
                                                      null,
                                                      metadataElementGUID,
                                                      null,
                                                      false,
                                                      false,
                                                      instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                      new Date(),
                                                      methodName);            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Add a new classification to the metadata element.  Note that only one classification with the same name can be attached to
     * a metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param classificationName name of the classification to add (if the classification is already present then use reclassify)
     * @param requestBody properties to store in the new classification.  These must conform to the valid properties associated with the
     *                   classification name
     *
     * @return void or
     *  InvalidParameterException the unique identifier or classification name is null or invalid in some way; properties do not match the
     *                                   valid properties associated with the classification's type definition
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse classifyMetadataElementInStore(String                       serverName,
                                                       String                       serviceURLMarker,
                                                       String                       userId,
                                                       String                       metadataElementGUID,
                                                       String                       classificationName,
                                                       NewClassificationRequestBody requestBody)
    {
        final String methodName = "classifyMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.classifyMetadataElementInStore(userId,
                                                       requestBody.getExternalSourceGUID(),
                                                       requestBody.getExternalSourceName(),
                                                       metadataElementGUID,
                                                       classificationName,
                                                       requestBody.getForLineage(),
                                                       requestBody.getForDuplicateProcessing(),
                                                       requestBody.getEffectiveFrom(),
                                                       requestBody.getEffectiveTo(),
                                                       requestBody.getProperties(),
                                                       instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                       requestBody.getEffectiveTime(),
                                                       methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the properties of a classification that is currently attached to a specific metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param classificationName unique name of the classification to update
     * @param requestBody new properties for the classification
     *
     * @return void or
     *  InvalidParameterException the unique identifier or classification name is null or invalid in some way; properties do not match the
     *                                   valid properties associated with the classification's type definition
     *  UserNotAuthorizedException the governance action service is not authorized to update this element/classification
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse reclassifyMetadataElementInStore(String                      serverName,
                                                         String                      serviceURLMarker,
                                                         String                      userId,
                                                         String                      metadataElementGUID,
                                                         String                      classificationName,
                                                         UpdatePropertiesRequestBody requestBody)
    {
        final String methodName = "reclassifyMetadataElementInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.reclassifyMetadataElementInStore(userId,
                                                         requestBody.getExternalSourceGUID(),
                                                         requestBody.getExternalSourceName(),
                                                         metadataElementGUID,
                                                         classificationName,
                                                         requestBody.getReplaceProperties(),
                                                         requestBody.getForLineage(),
                                                         requestBody.getForDuplicateProcessing(),
                                                         requestBody.getProperties(),
                                                         instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                         requestBody.getEffectiveTime(),
                                                         methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the effectivity dates of a specific classification attached to a metadata element.
     * The effectivity dates control the visibility of the classification through specific APIs.
     *
     * @param serverName name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param classificationName unique name of the classification to update
     * @param requestBody the dates when this element is active / inactive - null for no restriction
     *
     * @return void or
     *  InvalidParameterException either the unique identifier or the status are invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateClassificationEffectivityInStore(String                            serverName,
                                                               String                            serviceURLMarker,
                                                               String                            userId,
                                                               String                            metadataElementGUID,
                                                               String                            classificationName,
                                                               UpdateEffectivityDatesRequestBody requestBody)
    {
        final String methodName = "updateClassificationStatusInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateClassificationStatusInStore(userId,
                                                          requestBody.getExternalSourceGUID(),
                                                          requestBody.getExternalSourceName(),
                                                          metadataElementGUID,
                                                          classificationName,
                                                          requestBody.getForLineage(),
                                                          requestBody.getForDuplicateProcessing(),
                                                          requestBody.getEffectiveFrom(),
                                                          requestBody.getEffectiveTo(),
                                                          instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                          requestBody.getEffectiveTime(),
                                                          methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Remove the named classification from a specific metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param metadataElementGUID unique identifier of the metadata element to update
     * @param classificationName unique name of the classification to remove
     * @param requestBody null request body
     *
     * @return void or
     *  InvalidParameterException the unique identifier or classification name is null or invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to remove this classification
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse declassifyMetadataElementInStore(String                    serverName,
                                                         String                    serviceURLMarker,
                                                         String                    userId,
                                                         String                    metadataElementGUID,
                                                         String                    classificationName,
                                                         MetadataSourceRequestBody requestBody)
    {
        final String methodName = "declassifyMetadataElementInStore";
        final String metadataElementGUIDParameterName = "metadataElementGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.declassifyMetadataElementInStore(userId,
                                                         requestBody.getExternalSourceGUID(),
                                                         requestBody.getExternalSourceName(),
                                                         metadataElementGUID,
                                                         metadataElementGUIDParameterName,
                                                         OpenMetadataType.OPEN_METADATA_ROOT.typeName,
                                                         classificationName,
                                                         requestBody.getForLineage(),
                                                         requestBody.getForDuplicateProcessing(),
                                                         instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                         requestBody.getEffectiveTime(),
                                                         methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create a relationship between two metadata elements.  It is important to put the right element at each end of the relationship
     * according to the type definition since this will affect how the relationship is interpreted.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param requestBody the properties of the relationship
     *
     * @return unique identifier of the new relationship or
     *  InvalidParameterException the unique identifier's of the metadata elements are null or invalid in some way; the properties are
     *                                    not valid for this type of relationship
     *  UserNotAuthorizedException the governance action service is not authorized to create this type of relationship
     *  PropertyServerException there is a problem with the metadata store
     */
    public GUIDResponse createRelatedElementsInStore(String                        serverName,
                                                     String                        serviceURLMarker,
                                                     String                        userId,
                                                     NewRelatedElementsRequestBody requestBody)
    {
        final String methodName = "createRelatedElementsInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        GUIDResponse response = new GUIDResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                response.setGUID(handler.createRelatedElementsInStore(userId,
                                                                      requestBody.getExternalSourceGUID(),
                                                                      requestBody.getExternalSourceName(),
                                                                      requestBody.getTypeName(),
                                                                      requestBody.getMetadataElement1GUID(),
                                                                      requestBody.getMetadataElement2GUID(),
                                                                      requestBody.getForLineage(),
                                                                      requestBody.getForDuplicateProcessing(),
                                                                      requestBody.getEffectiveFrom(),
                                                                      requestBody.getEffectiveTo(),
                                                                      requestBody.getProperties(),
                                                                      instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                      requestBody.getEffectiveTime(),
                                                                      methodName));
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the properties associated with a relationship.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param relationshipGUID unique identifier of the relationship to update
     * @param requestBody new properties for the relationship
     *
     * @return void or
     *  InvalidParameterException the unique identifier of the relationship is null or invalid in some way; the properties are
     *                                    not valid for this type of relationship
     *  UserNotAuthorizedException the governance action service is not authorized to update this relationship
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateRelatedElementsInStore(String                      serverName,
                                                     String                      serviceURLMarker,
                                                     String                      userId,
                                                     String                      relationshipGUID,
                                                     UpdatePropertiesRequestBody requestBody)
    {
        final String methodName = "updateRelatedElementsInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateRelatedElementsInStore(userId,
                                                     requestBody.getExternalSourceGUID(),
                                                     requestBody.getExternalSourceName(),
                                                     relationshipGUID,
                                                     requestBody.getReplaceProperties(),
                                                     requestBody.getForLineage(),
                                                     requestBody.getForDuplicateProcessing(),
                                                     requestBody.getProperties(),
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     requestBody.getEffectiveTime(),
                                                     methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Update the effectivity dates of a specific relationship between metadata elements.
     * The effectivity dates control the visibility of the classification through specific APIs.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param relationshipGUID unique identifier of the relationship to update
     * @param requestBody the dates when this element is active / inactive - null for no restriction
     *
     * @return void or
     *  InvalidParameterException either the unique identifier or the status are invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to update this element
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse updateRelatedElementsEffectivityInStore(String                            serverName,
                                                                String                            serviceURLMarker,
                                                                String                            userId,
                                                                String                            relationshipGUID,
                                                                UpdateEffectivityDatesRequestBody requestBody)
    {
        final String methodName = "updateRelatedElementsStatusInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.updateRelatedElementsStatusInStore(userId,
                                                           requestBody.getExternalSourceGUID(),
                                                           requestBody.getExternalSourceName(),
                                                           relationshipGUID,
                                                           requestBody.getEffectiveFrom(),
                                                           requestBody.getEffectiveTo(),
                                                           requestBody.getForLineage(),
                                                           requestBody.getForDuplicateProcessing(),
                                                           instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                           requestBody.getEffectiveTime(),
                                                           methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Delete a relationship between two metadata elements.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker      the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param relationshipGUID unique identifier of the relationship to delete
     * @param requestBody null request body
     *
     * @return void or
     *  InvalidParameterException the unique identifier of the relationship is null or invalid in some way
     *  UserNotAuthorizedException the governance action service is not authorized to delete this relationship
     *  PropertyServerException there is a problem with the metadata store
     */
    public VoidResponse deleteRelatedElementsInStore(String                    serverName,
                                                     String                    serviceURLMarker,
                                                     String                    userId,
                                                     String                    relationshipGUID,
                                                     MetadataSourceRequestBody requestBody)
    {
        final String methodName = "deleteRelatedElementsInStore";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                MetadataElementHandler<OpenMetadataElement> handler = instanceHandler.getMetadataElementHandler(userId, serverName, methodName);

                handler.deleteRelatedElementsInStore(userId,
                                                     requestBody.getExternalSourceGUID(),
                                                     requestBody.getExternalSourceName(),
                                                     relationshipGUID,
                                                     requestBody.getForLineage(),
                                                     requestBody.getForDuplicateProcessing(),
                                                     instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                     requestBody.getEffectiveTime(),
                                                     methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create or update the translation for a particular language/locale for a metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier of the element that this translation is related to
     * @param translationDetail properties of the translation
     *
     * @return void or
     * InvalidParameterException  the unique identifier is null or not known.
     * UserNotAuthorizedException the service is not able to access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public VoidResponse setTranslation(String            serverName,
                                       String            serviceURLMarker,
                                       String            userId,
                                       String            elementGUID,
                                       TranslationDetail translationDetail)
    {
        final String methodName = "setTranslation";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        // todo

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Remove the translation for a particular language/locale for a metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier of the element that this translation is related to
     * @param language language requested
     * @param locale optional locale to qualify which translation if there are multiple translations for the language.
     * @param requestBody null request body
     *
     * @return void or
     * InvalidParameterException  the language is null or not known or not unique (add locale)
     * UserNotAuthorizedException the service is not able to access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public VoidResponse clearTranslation(String          serverName,
                                         String          serviceURLMarker,
                                         String          userId,
                                         String          elementGUID,
                                         String          language,
                                         String          locale,
                                         NullRequestBody requestBody)
    {
        final String methodName = "clearTranslation";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        // todo

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the translation for the matching language/locale.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier of the element that this translation is related to
     * @param language language requested
     * @param locale optional locale to qualify which translation if there are multiple translations for the language.
     *
     * @return the properties of the translation or null if there is none or
     * InvalidParameterException  the unique identifier is null or not known.
     * UserNotAuthorizedException the service is not able to access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public TranslationDetailResponse getTranslation(String serverName,
                                                    String serviceURLMarker,
                                                    String userId,
                                                    String elementGUID,
                                                    String language,
                                                    String locale)
    {
        final String methodName = "getTranslation";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TranslationDetailResponse response = new TranslationDetailResponse();

        // todo

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve all translations associated with a metadata element.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param elementGUID unique identifier of the element that this translation is related to
     * @param startFrom  index of the list to start from (0 for start)
     * @param pageSize   maximum number of elements to return
     *
     * @return list of translation properties or null if there are none or
     * InvalidParameterException  the unique identifier is null or not known.
     * UserNotAuthorizedException the service is not able to access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public TranslationListResponse getTranslations(String serverName,
                                                   String serviceURLMarker,
                                                   String userId,
                                                   String elementGUID,
                                                   int    startFrom,
                                                   int    pageSize)
    {
        final String methodName = "getTranslations";
        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        TranslationListResponse response = new TranslationListResponse();

        // todo

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Find or create the parent set for a valid value.
     *
     * @param userId calling user
     * @param typeName name of the type (can be null)
     * @param propertyName name of the property (can be null)
     * @param mapName name of the mapName (can be null)
     * @param handler handler
     * @param methodName calling method
     * @return unique identifier (guid) of the parent set
     * @throws InvalidParameterException the userId is null or invalid.
     * @throws PropertyServerException there is a problem retrieving information from the repositories.
     * @throws UserNotAuthorizedException the requesting user is not authorized to issue this request.
     */
    private String getParentSet(String                                 userId,
                                String                                 typeName,
                                String                                 propertyName,
                                String                                 mapName,
                                ValidValuesHandler<ValidMetadataValue> handler,
                                String                                 methodName) throws InvalidParameterException,
                                                                                          PropertyServerException,
                                                                                          UserNotAuthorizedException
    {
        final String parentDescription = "Organizing set for valid metadata values";

        List<String> specificMatchPropertyNames = new ArrayList<>();
        specificMatchPropertyNames.add(OpenMetadataProperty.QUALIFIED_NAME.name);

        String parentQualifiedName = constructValidValueQualifiedName(typeName, propertyName, mapName, null);

        EntityDetail parentSet = handler.getEntityByValue(userId,
                                                          parentQualifiedName,
                                                          OpenMetadataProperty.QUALIFIED_NAME.name,
                                                          OpenMetadataType.VALID_VALUE_DEFINITION.typeGUID,
                                                          OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                                          specificMatchPropertyNames,
                                                          null,
                                                          null,
                                                          SequencingOrder.CREATION_DATE_RECENT,
                                                          null,
                                                          true,
                                                          false,
                                                          null,
                                                          methodName);

        if (parentSet == null)
        {
            String grandParentSetGUID = null;
            String parentDisplayName = parentQualifiedName.substring(26);

            if (mapName != null)
            {
                grandParentSetGUID = getParentSet(userId, typeName, propertyName, null, handler, methodName);
            }
            else if (propertyName != null)
            {
                grandParentSetGUID = getParentSet(userId, typeName, null, null, handler, methodName);
            }
            else if (typeName != null)
            {
                grandParentSetGUID = getParentSet(userId, null, null, null, handler, methodName);
            }

            return handler.createValidValue(userId,
                                            null,
                                            null,
                                            grandParentSetGUID,
                                            grandParentSetGUID,
                                            OpenMetadataType.VALID_VALUE_SET.typeName,
                                            false,
                                            parentQualifiedName,
                                            parentDisplayName,
                                            parentDescription,
                                            constructValidValueCategory(typeName, propertyName, mapName),
                                            OpenMetadataValidValues.VALID_METADATA_VALUES_USAGE,
                                            OpenMetadataValidValues.OPEN_METADATA_ECOSYSTEM_SCOPE,
                                            null,
                                            null,
                                            false,
                                            false,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            false,
                                            false,
                                            null,
                                            methodName);
        }
        else
        {
            return parentSet.getGUID();
        }
    }


    /**
     * Retrieve a valid metadata value entity from the open metadata ecosystem.
     *
     * @param userId calling user
     * @param typeName optional open metadata type name
     * @param propertyName property name
     * @param mapName optional map name
     * @param preferredValue valid value
     * @param handler valid values handler
     * @param methodName calling method
     * @return valid metadata value entity
     * @throws InvalidParameterException the userId is null or invalid.
     * @throws PropertyServerException there is a problem with the repositories
     * @throws UserNotAuthorizedException the user is not allowed to perform this action
     */
    private EntityDetail getValidMetadataElement(String                                 userId,
                                                 String                                 typeName,
                                                 String                                 propertyName,
                                                 String                                 mapName,
                                                 String                                 preferredValue,
                                                 ValidValuesHandler<ValidMetadataValue> handler,
                                                 String                                 methodName) throws PropertyServerException,
                                                                                                           UserNotAuthorizedException,
                                                                                                           InvalidParameterException
    {
        List<String> specificMatchPropertyNames = new ArrayList<>();
        specificMatchPropertyNames.add(OpenMetadataProperty.QUALIFIED_NAME.name);

        String qualifiedName = constructValidValueQualifiedName(typeName, propertyName, mapName, preferredValue);

        EntityDetail element = handler.getEntityByValue(userId,
                                                        qualifiedName,
                                                        OpenMetadataProperty.QUALIFIED_NAME.name,
                                                        OpenMetadataType.VALID_VALUE_DEFINITION.typeGUID,
                                                        OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                                        specificMatchPropertyNames,
                                                        null,
                                                        null,
                                                        SequencingOrder.CREATION_DATE_RECENT,
                                                        null,
                                                        true,
                                                        false,
                                                        null,
                                                        methodName);

        if (element == null)
        {
            qualifiedName = constructValidValueQualifiedName(null, propertyName, mapName, preferredValue);

            element = handler.getEntityByValue(userId,
                                               qualifiedName,
                                               OpenMetadataProperty.QUALIFIED_NAME.name,
                                               OpenMetadataType.VALID_VALUE_DEFINITION.typeGUID,
                                               OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                               specificMatchPropertyNames,
                                               null,
                                               null,
                                               SequencingOrder.CREATION_DATE_RECENT,
                                               null,
                                               true,
                                               false,
                                               null,
                                               methodName);
        }

        return element;
    }


    /**
     * Retrieve a valid metadata value bean from the open metadata ecosystem.
     *
     * @param userId calling user
     * @param typeName optional open metadata type name
     * @param propertyName property name
     * @param mapName optional map name
     * @param preferredValue valid value
     * @param handler valid values handler
     * @param methodName calling method
     * @return valid metadata value bean
     * @throws InvalidParameterException one of the properties passed to the handler is invalid
     * @throws PropertyServerException there is a problem with the repositories
     * @throws UserNotAuthorizedException the user is not allowed to perform this action
     */
    private ValidMetadataValue getValidMetadataBean(String                                 userId,
                                                    String                                 typeName,
                                                    String                                 propertyName,
                                                    String                                 mapName,
                                                    String                                 preferredValue,
                                                    ValidValuesHandler<ValidMetadataValue> handler,
                                                    String                                 methodName) throws InvalidParameterException,
                                                                                                              PropertyServerException,
                                                                                                              UserNotAuthorizedException
    {
        EntityDetail element = getValidMetadataElement(userId,
                                                       typeName,
                                                       propertyName,
                                                       mapName,
                                                       preferredValue,
                                                       handler,
                                                       methodName);;

        if (element != null)
        {
            OpenMetadataAPIGenericConverter<ValidMetadataValue> converter = handler.getConverter();

            return converter.getNewBean(ValidMetadataValue.class, element, methodName);
        }

        return null;
    }


    /**
     * Create or update the valid value for a particular open metadata property name.  If the typeName is null, this valid value
     * applies to properties of this name from all types.  The valid value is stored in the preferredValue property.  If a valid value is
     * already set up for this property (with overlapping effective dates) then the valid value is updated.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param requestBody preferred value to use in the open metadata types plus additional descriptive values.
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public VoidResponse setUpValidMetadataValue(String             serverName,
                                                String             serviceURLMarker,
                                                String             userId,
                                                String             typeName,
                                                String             propertyName,
                                                ValidMetadataValue requestBody)
    {
        final String methodName = "setUpValidMetadataValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(requestBody.getPreferredValue(), preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                String setGUID = this.getParentSet(userId, typeName, propertyName, null, handler, methodName);

                handler.createValidValue(userId,
                                         null,
                                         null,
                                         setGUID,
                                         setGUID,
                                         OpenMetadataType.VALID_VALUE_SET.typeName,
                                         false,
                                         constructValidValueQualifiedName(typeName, propertyName, null, requestBody.getPreferredValue()),
                                         requestBody.getDisplayName(),
                                         requestBody.getDescription(),
                                         constructValidValueCategory(typeName, propertyName, null),
                                         OpenMetadataValidValues.VALID_METADATA_VALUES_USAGE,
                                         OpenMetadataValidValues.OPEN_METADATA_ECOSYSTEM_SCOPE,
                                         requestBody.getPreferredValue(),
                                         requestBody.getDataType(),
                                         requestBody.getIsDeprecated(),
                                         requestBody.getIsCaseSensitive(),
                                         requestBody.getAdditionalProperties(),
                                         null,
                                         null,
                                         requestBody.getEffectiveFrom(),
                                         requestBody.getEffectiveTo(),
                                         false,
                                         false,
                                         null,
                                         methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create or update the valid value for a name that can be stored in a particular open metadata property name.
     * This property is of type map from name to string.
     * The valid value is stored in the preferredValue property of validMetadataValue.
     * If the typeName is null, this valid value applies to properties of this name from any open metadata type.
     * If a valid value is already set up for this property (with overlapping effective dates) then the valid value is updated.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param requestBody preferred value to use in the open metadata types plus additional descriptive values.
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public VoidResponse setUpValidMetadataMapName(String             serverName,
                                                  String             serviceURLMarker,
                                                  String             userId,
                                                  String             typeName,
                                                  String             propertyName,
                                                  ValidMetadataValue requestBody)
    {
        final String methodName = "setUpValidMetadataMapName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(requestBody.getPreferredValue(), preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                String setGUID = this.getParentSet(userId, typeName, propertyName, null, handler, methodName);

                handler.createValidValue(userId,
                                         null,
                                         null,
                                         setGUID,
                                         setGUID,
                                         OpenMetadataType.VALID_VALUE_SET.typeName,
                                         false,
                                         constructValidValueQualifiedName(typeName, propertyName, requestBody.getPreferredValue(), null),
                                         requestBody.getDisplayName(),
                                         requestBody.getDescription(),
                                         constructValidValueCategory(typeName, propertyName, null),
                                         OpenMetadataValidValues.VALID_METADATA_VALUES_USAGE,
                                         OpenMetadataValidValues.OPEN_METADATA_ECOSYSTEM_SCOPE,
                                         requestBody.getPreferredValue(),
                                         "string",
                                         requestBody.getIsDeprecated(),
                                         requestBody.getIsCaseSensitive(),
                                         requestBody.getAdditionalProperties(),
                                         null,
                                         null,
                                         requestBody.getEffectiveFrom(),
                                         requestBody.getEffectiveTo(),
                                         false,
                                         false,
                                         null,
                                         methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Create or update the valid value for a name that can be stored in a particular open metadata property name.
     * This property is of type map from name to string.
     * The valid value is stored in the preferredValue property of validMetadataValue.
     * <p>
     * If the typeName is null, this valid value applies to properties of this name from any open metadata type.
     * If a valid value is already set up for this property (with overlapping effective dates) then the valid value is updated.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param mapName name in the map that this valid value applies.  If null then the value can be used for any name in the map.
     * @param requestBody preferred value to use in the open metadata types plus additional descriptive values.
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public VoidResponse setUpValidMetadataMapValue(String             serverName,
                                                   String             serviceURLMarker,
                                                   String             userId,
                                                   String             typeName,
                                                   String             propertyName,
                                                   String             mapName,
                                                   ValidMetadataValue requestBody)
    {
        final String methodName = "setUpValidMetadataMapValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(requestBody.getPreferredValue(), preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                String setGUID = this.getParentSet(userId, typeName, propertyName, mapName, handler, methodName);

                handler.createValidValue(userId,
                                         null,
                                         null,
                                         setGUID,
                                         setGUID,
                                         OpenMetadataType.VALID_VALUE_SET.typeName,
                                         false,
                                         constructValidValueQualifiedName(typeName, propertyName, mapName, requestBody.getPreferredValue()),
                                         requestBody.getDisplayName(),
                                         requestBody.getDescription(),
                                         constructValidValueCategory(typeName, propertyName, mapName),
                                         OpenMetadataValidValues.VALID_METADATA_VALUES_USAGE,
                                         OpenMetadataValidValues.OPEN_METADATA_ECOSYSTEM_SCOPE,
                                         requestBody.getPreferredValue(),
                                         null,
                                         requestBody.getIsDeprecated(),
                                         requestBody.getIsCaseSensitive(),
                                         requestBody.getAdditionalProperties(),
                                         null,
                                         null,
                                         requestBody.getEffectiveFrom(),
                                         requestBody.getEffectiveTo(),
                                         false,
                                         false,
                                         null,
                                         methodName);
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Remove a valid value for a property.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param preferredValue specific valid value to remove
     * @param requestBody null request body
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public VoidResponse clearValidMetadataValue(String          serverName,
                                                String          serviceURLMarker,
                                                String          userId,
                                                String          typeName,
                                                String          propertyName,
                                                String          preferredValue,
                                                NullRequestBody requestBody)
    {
        final String methodName = "clearValidMetadataValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                EntityDetail element = this.getValidMetadataElement(userId, typeName, propertyName, null, preferredValue, handler, methodName);

                if (element != null)
                {
                    handler.deleteValidValue(userId,
                                             null,
                                             null,
                                             element.getGUID(),
                                             null,
                                             false,
                                             false,
                                             null,
                                             methodName);
                }
                else
                {
                    invalidParameterHandler.throwUnknownElementQualifiedName(userId,
                                                                             constructValidValueQualifiedName(typeName, propertyName, null, preferredValue),
                                                                             typeName,
                                                                             instanceHandler.getServiceName(serviceURLMarker),
                                                                             serverName,
                                                                             methodName);
                }
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Remove a valid map name value for a property.  The match is done on preferred name.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param preferredValue specific valid value to remove
     * @param requestBody null request body
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public VoidResponse clearValidMetadataMapName(String          serverName,
                                                  String          serviceURLMarker,
                                                  String          userId,
                                                  String          typeName,
                                                  String          propertyName,
                                                  String          preferredValue,
                                                  NullRequestBody requestBody)
    {
        final String methodName = "clearValidMetadataMapName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                EntityDetail element = this.getValidMetadataElement(userId,
                                                                    typeName,
                                                                    propertyName,
                                                                    null,
                                                                    preferredValue,
                                                                    handler,
                                                                    methodName);

                if (element != null)
                {
                    handler.deleteValidValue(userId,
                                             null,
                                             null,
                                             element.getGUID(),
                                             null,
                                             false,
                                             false,
                                             null,
                                             methodName);
                }
                else
                {
                    invalidParameterHandler.throwUnknownElementQualifiedName(userId,
                                                                             constructValidValueQualifiedName(typeName,
                                                                                                              propertyName,
                                                                                                              null,
                                                                                                              preferredValue),
                                                                             typeName,
                                                                             instanceHandler.getServiceName(serviceURLMarker),
                                                                             serverName,
                                                                             methodName);
                }
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Remove a valid map name value for a property.  The match is done on preferred name.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param mapName name in the map that this valid value applies.  If null then the value can be used for any name in the map.
     * @param preferredValue specific valid value to remove
     * @param requestBody null request body
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public VoidResponse clearValidMetadataMapValue(String          serverName,
                                                   String          serviceURLMarker,
                                                   String          userId,
                                                   String          typeName,
                                                   String          propertyName,
                                                   String          mapName,
                                                   String          preferredValue,
                                                   NullRequestBody requestBody)
    {
        final String methodName = "clearValidMetadataMapValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            if (requestBody != null)
            {
                invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
                invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

                ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

                String qualifiedName = constructValidValueQualifiedName(typeName, propertyName, mapName, preferredValue);

                EntityDetail element = this.getValidMetadataElement(userId, typeName, propertyName, mapName, preferredValue, handler, methodName);

                if (element != null)
                {
                    handler.deleteValidValue(userId,
                                             null,
                                             null,
                                             element.getGUID(),
                                             null,
                                             false,
                                             false,
                                             null,
                                             methodName);
                }
                else
                {
                    invalidParameterHandler.throwUnknownElementQualifiedName(userId, qualifiedName, typeName, instanceHandler.getServiceName(serviceURLMarker), serverName, methodName);
                }
            }
            else
            {
                restExceptionHandler.handleNoRequestBody(userId, methodName, serverName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Validate whether the value found in an open metadata property is valid.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param actualValue value stored in the property - if this is null, true is only returned if null is set up as a valid value.
     *
     * @return boolean flag - true if the value is one of the defined valid values or there are no valid values set up for the property (and so any value is value) or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public BooleanResponse validateMetadataValue(String serverName,
                                                 String serviceURLMarker,
                                                 String userId,
                                                 String typeName,
                                                 String propertyName,
                                                 String actualValue)
    {
        final String methodName = "validateMetadataValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        BooleanResponse response = new BooleanResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(actualValue, actualValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element = this.getValidMetadataElement(userId,
                                                                typeName,
                                                                propertyName,
                                                                null,
                                                                actualValue,
                                                                handler,
                                                                methodName);

            response.setFlag(false);

            if (element != null)
            {
                response.setFlag(true);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Validate whether the name found in an open metadata map property is valid.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param actualValue value stored in the property - if this is null, true is only returned if null is set up as a valid value.
     *
     * @return boolean flag - true if the value is one of the defined valid values or there are no valid values set up for the property (and so any value is value) or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public BooleanResponse validateMetadataMapName(String serverName,
                                                   String serviceURLMarker,
                                                   String userId,
                                                   String typeName,
                                                   String propertyName,
                                                   String actualValue)
    {
        final String methodName = "validateMetadataMapName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        BooleanResponse response = new BooleanResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(actualValue, actualValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element = this.getValidMetadataElement(userId,
                                                                typeName,
                                                                propertyName,
                                                                actualValue,
                                                                null,
                                                                handler,
                                                                methodName);

            response.setFlag(false);

            if (element != null)
            {
                response.setFlag(true);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Validate whether the name found in an open metadata map property is valid.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param mapName name in the map that this valid value applies.  If null then the value can be used for any name in the map.
     * @param actualValue value stored in the property - if this is null, true is only returned if null is set up as a valid value.
     *
     * @return boolean flag - true if the value is one of the defined valid values or there are no valid values set up for the property (and so any value is value) or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public BooleanResponse validateMetadataMapValue(String serverName,
                                                    String serviceURLMarker,
                                                    String userId,
                                                    String typeName,
                                                    String propertyName,
                                                    String mapName,
                                                    String actualValue)
    {
        final String methodName = "validateMetadataMapValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        BooleanResponse response = new BooleanResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(actualValue, actualValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element = this.getValidMetadataElement(userId,
                                                                typeName,
                                                                propertyName,
                                                                mapName,
                                                                actualValue,
                                                                handler,
                                                                methodName);

            response.setFlag(false);

            if (element != null)
            {
                response.setFlag(true);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve details of a specific valid value for a property.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param preferredValue valid value to match
     *
     * @return specific valid value definition or none if there is no definition stored or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public ValidMetadataValueResponse getValidMetadataValue(String serverName,
                                                            String serviceURLMarker,
                                                            String userId,
                                                            String typeName,
                                                            String propertyName,
                                                            String preferredValue)
    {
        final String methodName = "getValidMetadataValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        ValidMetadataValueResponse response = new ValidMetadataValueResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            String qualifiedName = constructValidValueQualifiedName(typeName, propertyName, null, preferredValue);

            ValidMetadataValue element = this.getValidMetadataBean(userId,
                                                                   typeName,
                                                                   propertyName,
                                                                   null,
                                                                   preferredValue,
                                                                   handler,
                                                                   methodName);

            if (element != null)
            {
                response.setElement(element);
            }
            else
            {
                invalidParameterHandler.throwUnknownElementQualifiedName(userId,
                                                                         qualifiedName,
                                                                         OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                                                         instanceHandler.getServiceName(serviceURLMarker),
                                                                         serverName,
                                                                         methodName);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve details of a specific valid name for a map property.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param preferredValue valid value to match
     *
     * @return specific valid value definition or none if there is no definition stored or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public ValidMetadataValueResponse getValidMetadataMapName(String serverName,
                                                              String serviceURLMarker,
                                                              String userId,
                                                              String typeName,
                                                              String propertyName,
                                                              String preferredValue)
    {
        final String methodName = "getValidMetadataMapName";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        ValidMetadataValueResponse response = new ValidMetadataValueResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            response.setElement(this.getValidMetadataBean(userId,
                                                          typeName,
                                                          propertyName,
                                                          preferredValue,
                                                          null,
                                                          handler,
                                                          methodName));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve details of a specific valid value for a map name.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param mapName name in the map that this valid value applies.  If null then the value can be used for any name in the map.
     * @param preferredValue valid value to match
     *
     * @return specific valid value definition or none if there is no definition stored or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public ValidMetadataValueResponse getValidMetadataMapValue(String serverName,
                                                               String serviceURLMarker,
                                                               String userId,
                                                               String typeName,
                                                               String propertyName,
                                                               String mapName,
                                                               String preferredValue)
    {
        final String methodName = "getValidMetadataMapValue";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog auditLog = null;
        ValidMetadataValueResponse response = new ValidMetadataValueResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);
            invalidParameterHandler.validateName(preferredValue, preferredValueParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            response.setElement(this.getValidMetadataBean(userId,
                                                          typeName,
                                                          propertyName,
                                                          mapName,
                                                          preferredValue,
                                                          handler,
                                                          methodName));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve all the valid values for the requested property.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return list of valid values defined for the property or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public ValidMetadataValueDetailListResponse getValidMetadataValues(String serverName,
                                                                       String serviceURLMarker,
                                                                       String userId,
                                                                       String typeName,
                                                                       String propertyName,
                                                                       int    startFrom,
                                                                       int    pageSize)
    {
        final String methodName = "getValidMetadataValues";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                             auditLog = null;
        ValidMetadataValueDetailListResponse response = new ValidMetadataValueDetailListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element = this.getValidMetadataElement(userId,
                                                                typeName,
                                                                propertyName,
                                                                null,
                                                                null,
                                                                handler,
                                                                methodName);

            if (element != null)
            {
                ValidValuesHandler<ValidMetadataValueDetail> detailsHandler = instanceHandler.getValidMetadataValuesDetailHandler(userId, serverName, methodName);

                List<ValidMetadataValueDetail> details = detailsHandler.getValidValueSetMembers(userId,
                                                                                                element.getGUID(),
                                                                                                validValueGUIDParameter,
                                                                                                startFrom,
                                                                                                pageSize,
                                                                                                false,
                                                                                                false,
                                                                                                instanceHandler.getSupportedZones(userId, serverName, serviceURLMarker, methodName),
                                                                                                null,
                                                                                                methodName);

                if (details != null)
                {
                    for (ValidMetadataValueDetail detail : details)
                    {
                        EntityDetail detailElement = this.getValidMetadataElement(userId,
                                                                                  typeName,
                                                                                  propertyName,
                                                                                  null,
                                                                                  detail.getPreferredValue(),
                                                                                  handler,
                                                                                  methodName);

                        if (detailElement == null)
                        {
                            detailElement = this.getValidMetadataElement(userId,
                                                                         typeName,
                                                                         propertyName,
                                                                         detail.getPreferredValue(),
                                                                         null,
                                                                         handler,
                                                                         methodName);
                        }

                        if (detailElement != null)
                        {
                            List<ValidMetadataValue> mapDetails = handler.getValidValueSetMembers(userId,
                                                                                                  detailElement.getGUID(),
                                                                                                  validValueGUIDParameter,
                                                                                                  0,
                                                                                                  0,
                                                                                                  false,
                                                                                                  false,
                                                                                                  null,
                                                                                                  null,
                                                                                                  methodName);

                            detail.setValidMapNameValues(mapDetails);
                        }
                    }
                }

                response.setElementList(details);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Retrieve the valid value entities that have the right type name.
     *
     * @param retrievedValues entities from repository
     * @param repositoryHelper entity helper
     * @param serviceName this service
     * @return list of filtered entities
     */
    private List<EntityDetail> filterByType(List<EntityDetail>   retrievedValues,
                                            OMRSRepositoryHelper repositoryHelper,
                                            String               serviceName)
    {
        final String methodName = "filterByType";

        if (retrievedValues != null)
        {
            List<EntityDetail> filteredValues = new ArrayList<>();

            for (EntityDetail retrievedEntity : retrievedValues)
            {
                if (retrievedEntity != null)
                {
                    Map<String, String> additionalProperties = repositoryHelper.getStringMapFromProperty(serviceName,
                                                                                                         OpenMetadataProperty.ADDITIONAL_PROPERTIES.name,
                                                                                                         retrievedEntity.getProperties(),
                                                                                                         methodName);
                    String typeName = null;

                    if (additionalProperties != null)
                    {
                        typeName = additionalProperties.get(OpenMetadataProperty.OPEN_METADATA_TYPE_NAME.name);
                    }

                    if ((typeName == null) || (repositoryHelper.isTypeOf(serviceName,
                                                                         retrievedEntity.getType().getTypeDefName(),
                                                                         typeName)))
                    {
                        filteredValues.add(retrievedEntity);
                    }
                }
            }

            return filteredValues;
        }

        return null;
    }

    /**
     * Build out a set of search conditions for finding one or more valid values.
     *
     * @param propertyValue value to search for
     * @param propertyName property name
     * @param operator comparison operator
     * @return search properties
     */
    private SearchProperties getValidValueConditions(String                     propertyValue,
                                                     String                     propertyName,
                                                     PropertyComparisonOperator operator)
    {
        SearchProperties        searchProperties   = new SearchProperties();
        List<PropertyCondition> propertyConditions = new ArrayList<>();

        if (propertyValue != null)
        {
            PropertyCondition preferredValueCondition = new PropertyCondition();
            PrimitivePropertyValue  preferredValue    = new PrimitivePropertyValue();
            preferredValueCondition.setProperty(OpenMetadataProperty.PREFERRED_VALUE.name);
            preferredValueCondition.setOperator(operator);
            preferredValue.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
            preferredValue.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
            preferredValue.setPrimitiveValue(propertyValue);
            preferredValueCondition.setValue(preferredValue);
            propertyConditions.add(preferredValueCondition);
        }

        PropertyCondition scopeCondition = new PropertyCondition();
        PrimitivePropertyValue  scope          = new PrimitivePropertyValue();
        scopeCondition.setProperty(OpenMetadataProperty.SCOPE.name);
        scopeCondition.setOperator(PropertyComparisonOperator.EQ);
        scope.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
        scope.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
        scope.setPrimitiveValue(OpenMetadataValidValues.OPEN_METADATA_ECOSYSTEM_SCOPE);
        scopeCondition.setValue(scope);
        propertyConditions.add(scopeCondition);

        PropertyCondition categoryCondition = new PropertyCondition();
        PrimitivePropertyValue   category          = new PrimitivePropertyValue();
        categoryCondition.setProperty(OpenMetadataProperty.CATEGORY.name);
        categoryCondition.setOperator(PropertyComparisonOperator.LIKE);
        category.setPrimitiveDefCategory(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING);
        category.setTypeName(PrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING.getName());
        category.setPrimitiveValue(".*" + propertyName);
        categoryCondition.setValue(category);
        propertyConditions.add(categoryCondition);

        searchProperties.setConditions(propertyConditions);
        searchProperties.setMatchCriteria(MatchCriteria.ALL);

        return searchProperties;
    }



    /**
     * Retrieve all the consistent valid values for the requested property.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName name of property that this valid value applies
     * @param mapName optional name of map key that this valid value applies
     * @param preferredValue the value to match against
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return list of valid values defined for the property or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    public ValidMetadataValueListResponse getConsistentMetadataValues(String serverName,
                                                                      String serviceURLMarker,
                                                                      String userId,
                                                                      String typeName,
                                                                      String propertyName,
                                                                      String mapName,
                                                                      String preferredValue,
                                                                      int    startFrom,
                                                                      int    pageSize)
    {
        final String methodName = "getConsistentMetadataValues";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog                       auditLog = null;
        ValidMetadataValueListResponse response = new ValidMetadataValueListResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName, propertyNameParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element = this.getValidMetadataElement(userId,
                                                                typeName,
                                                                propertyName,
                                                                mapName,
                                                                preferredValue,
                                                                handler,
                                                                methodName);

            if (element != null)
            {
                List<ValidMetadataValue> details = handler.getConsistentValidValues(userId,
                                                                                    element.getGUID(),
                                                                                    validValueGUIDParameter,
                                                                                    startFrom,
                                                                                    pageSize,
                                                                                    false,
                                                                                    false,
                                                                                    null,
                                                                                    null,
                                                                                    methodName);

                response.setElementList(details);
            }
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Set up consistent metadata values relationship between the two property values.
     *
     * @param serverName     name of server instance to route request to
     * @param serviceURLMarker the identifier of the access service (for example asset-owner for the Asset Owner OMAS)
     * @param userId caller's userId
     * @param typeName1 type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName1 name of property that this valid value applies
     * @param mapName1 optional name of map key that this valid value applies
     * @param preferredValue1 the value to match against
     * @param typeName2 type name if this is valid value is specific for a type, or null if this valid value if for the property name for all types
     * @param propertyName2 name of property that this valid value applies
     * @param mapName2 optional name of map key that this valid value applies
     * @param preferredValue2 the value to match against
     * @param requestBody null request body
     *
     * @return void or
     * InvalidParameterException  the property name is null or not known.
     * UserNotAuthorizedException the service is not able to create/access the element
     * PropertyServerException    there is a problem accessing the metadata store
     */
    @SuppressWarnings(value = "unused")
    public VoidResponse setConsistentMetadataValues(String          serverName,
                                                    String          serviceURLMarker,
                                                    String          userId,
                                                    String          typeName1,
                                                    String          propertyName1,
                                                    String          mapName1,
                                                    String          preferredValue1,
                                                    String          typeName2,
                                                    String          propertyName2,
                                                    String          mapName2,
                                                    String          preferredValue2,
                                                    NullRequestBody requestBody)
    {
        final String methodName = "setConsistentMetadataValues";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        AuditLog     auditLog = null;
        VoidResponse response = new VoidResponse();

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            invalidParameterHandler.validateName(propertyName1, propertyNameParameter, methodName);

            ValidValuesHandler<ValidMetadataValue> handler = instanceHandler.getValidMetadataValuesHandler(userId, serverName, methodName);

            EntityDetail element1 = this.getValidMetadataElement(userId, typeName1, propertyName1, mapName1, preferredValue1, handler, methodName);

            if (element1 == null)
            {
                invalidParameterHandler.throwUnknownElementQualifiedName(userId,
                                                                         constructValidValueQualifiedName(typeName1, propertyName1, mapName1, preferredValue1),
                                                                         OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                                                         instanceHandler.getServiceName(serviceURLMarker),
                                                                         serverName,
                                                                         methodName);
            }

            EntityDetail element2 = this.getValidMetadataElement(userId, typeName2, propertyName2, mapName2, preferredValue2, handler, methodName);

            if (element2 == null)
            {
                invalidParameterHandler.throwUnknownElementQualifiedName(userId,
                                                                         constructValidValueQualifiedName( typeName2, propertyName2, mapName2, preferredValue2),
                                                                         OpenMetadataType.VALID_VALUE_DEFINITION.typeName,
                                                                         instanceHandler.getServiceName(serviceURLMarker),
                                                                         serverName,
                                                                         methodName);
            }

            handler.attachConsistentValidValues(userId,
                                                null,
                                                null,
                                                element1.getGUID(),
                                                element2.getGUID(),
                                                null,
                                                null,
                                                null,
                                                false,
                                                false,
                                                null,
                                                methodName);
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param typeDef omrs type
     * @return open metadata type
     */
    private OpenMetadataTypeDef getTypeDef(TypeDef typeDef)
    {
        if (typeDef != null)
        {
            OpenMetadataTypeDef openMetadataTypeDef;

            if (typeDef instanceof EntityDef)
            {
                openMetadataTypeDef = new OpenMetadataEntityDef();;
                openMetadataTypeDef.setCategory(OpenMetadataTypeDefCategory.ENTITY_DEF);
            }
            else if (typeDef instanceof RelationshipDef relationshipDef)
            {
                OpenMetadataRelationshipDef openMetadataRelationshipDef = new OpenMetadataRelationshipDef();

                openMetadataRelationshipDef.setPropagationRule(this.getPropagationRule(relationshipDef.getPropagationRule()));
                openMetadataRelationshipDef.setEndDef1(this.getRelationshipEndDef(relationshipDef.getEndDef1()));
                openMetadataRelationshipDef.setEndDef2(this.getRelationshipEndDef(relationshipDef.getEndDef2()));
                openMetadataRelationshipDef.setMultiLink(relationshipDef.getMultiLink());

                openMetadataTypeDef = openMetadataRelationshipDef;
                openMetadataTypeDef.setCategory(OpenMetadataTypeDefCategory.RELATIONSHIP_DEF);
            }
            else if (typeDef instanceof ClassificationDef classificationDef)
            {
                OpenMetadataClassificationDef openMetadataClassificationDef = new OpenMetadataClassificationDef();

                openMetadataClassificationDef.setValidEntityDefs(this.getTypeDefLinks(classificationDef.getValidEntityDefs()));
                openMetadataClassificationDef.setPropagatable(classificationDef.isPropagatable());

                openMetadataTypeDef = openMetadataClassificationDef;
                openMetadataTypeDef.setCategory(OpenMetadataTypeDefCategory.CLASSIFICATION_DEF);
            }
            else
            {
                return null;
            }

            openMetadataTypeDef.setGUID(typeDef.getGUID());
            openMetadataTypeDef.setName(typeDef.getName());
            openMetadataTypeDef.setStatus(this.getTypeDefStatus(typeDef.getStatus()));
            openMetadataTypeDef.setReplacedByTypeGUID(typeDef.getReplacedByTypeGUID());
            openMetadataTypeDef.setReplacedByTypeName(typeDef.getReplacedByTypeName());
            openMetadataTypeDef.setVersionName(typeDef.getVersionName());
            openMetadataTypeDef.setVersion(typeDef.getVersion());
            openMetadataTypeDef.setSuperType(this.getTypeDefLink(typeDef.getSuperType()));
            openMetadataTypeDef.setDescription(typeDef.getDescription());
            openMetadataTypeDef.setDescriptionGUID(typeDef.getDescriptionGUID());
            openMetadataTypeDef.setDescriptionWiki(typeDef.getDescriptionWiki());
            openMetadataTypeDef.setOrigin(typeDef.getOrigin());
            openMetadataTypeDef.setCreatedBy(typeDef.getCreatedBy());
            openMetadataTypeDef.setUpdatedBy(typeDef.getUpdatedBy());
            openMetadataTypeDef.setCreateTime(typeDef.getCreateTime());
            openMetadataTypeDef.setUpdateTime(typeDef.getUpdateTime());
            openMetadataTypeDef.setOptions(typeDef.getOptions());
            openMetadataTypeDef.setExternalStandardMappings(this.getExternalStandardMappings(typeDef.getExternalStandardMappings()));
            openMetadataTypeDef.setValidElementStatusList(this.getElementStatuses(typeDef.getValidInstanceStatusList()));
            openMetadataTypeDef.setAttributeDefinitions(this.getTypeDefAttributes(typeDef.getPropertiesDefinition()));
            openMetadataTypeDef.setInitialStatus(this.getElementStatus(typeDef.getInitialStatus()));

            return openMetadataTypeDef;
        }

        return null;
    }


    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param typeDefAttributes list of OMRS types
     * @return list of open metadata types
     */
    private List<OpenMetadataTypeDefAttribute> getTypeDefAttributes(List<TypeDefAttribute> typeDefAttributes)
    {
        if (typeDefAttributes != null)
        {
            List<OpenMetadataTypeDefAttribute> externalStandardTypeMappings = new ArrayList<>();

            for (TypeDefAttribute typeDefAttribute : typeDefAttributes)
            {
                externalStandardTypeMappings.add(this.getTypeDefAttribute(typeDefAttribute));
            }

            if (! externalStandardTypeMappings.isEmpty())
            {
                return externalStandardTypeMappings;
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param typeDefAttribute omrs type
     * @return open metadata type
     */
    private OpenMetadataTypeDefAttribute getTypeDefAttribute(TypeDefAttribute typeDefAttribute)
    {
        if (typeDefAttribute != null)
        {
            OpenMetadataTypeDefAttribute openMetadataTypeDefAttribute = new OpenMetadataTypeDefAttribute();

            openMetadataTypeDefAttribute.setAttributeName(typeDefAttribute.getAttributeName());
            openMetadataTypeDefAttribute.setAttributeType(this.getAttributeTypeDef(typeDefAttribute.getAttributeType()));
            openMetadataTypeDefAttribute.setAttributeStatus(this.getAttributeStatus(typeDefAttribute.getAttributeStatus()));
            openMetadataTypeDefAttribute.setReplacedByAttribute(typeDefAttribute.getReplacedByAttribute());
            openMetadataTypeDefAttribute.setAttributeDescription(typeDefAttribute.getAttributeDescription());
            openMetadataTypeDefAttribute.setAttributeDescriptionGUID(typeDefAttribute.getAttributeDescriptionGUID());
            openMetadataTypeDefAttribute.setAttributeCardinality(this.getAttributeCardinality(typeDefAttribute.getAttributeCardinality()));
            openMetadataTypeDefAttribute.setValuesMinCount(typeDefAttribute.getValuesMinCount());
            openMetadataTypeDefAttribute.setValuesMaxCount(typeDefAttribute.getValuesMaxCount());
            openMetadataTypeDefAttribute.setUnique(typeDefAttribute.isUnique());
            openMetadataTypeDefAttribute.setIndexable(typeDefAttribute.isIndexable());
            openMetadataTypeDefAttribute.setDefaultValue(typeDefAttribute.getDefaultValue());
            openMetadataTypeDefAttribute.setExternalStandardMappings(this.getExternalStandardMappings(typeDefAttribute.getExternalStandardMappings()));

            return openMetadataTypeDefAttribute;
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param attributeCardinality omrs type
     * @return open metadata type
     */
    private OpenMetadataAttributeCardinality getAttributeCardinality(AttributeCardinality attributeCardinality)
    {
        if (attributeCardinality != null)
        {
            switch (attributeCardinality)
            {
                case UNKNOWN ->
                {
                    return OpenMetadataAttributeCardinality.UNKNOWN;
                }
                case AT_MOST_ONE ->
                {
                    return OpenMetadataAttributeCardinality.AT_MOST_ONE;
                }
                case ONE_ONLY ->
                {
                    return OpenMetadataAttributeCardinality.ONE_ONLY;
                }
                case AT_LEAST_ONE_ORDERED ->
                {
                    return OpenMetadataAttributeCardinality.AT_LEAST_ONE_ORDERED;
                }
                case AT_LEAST_ONE_UNORDERED ->
                {
                    return OpenMetadataAttributeCardinality.AT_LEAST_ONE_UNORDERED;
                }
                case ANY_NUMBER_ORDERED ->
                {
                    return OpenMetadataAttributeCardinality.ANY_NUMBER_ORDERED;
                }
                case ANY_NUMBER_UNORDERED ->
                {
                    return OpenMetadataAttributeCardinality.ANY_NUMBER_UNORDERED;
                }
            }
        }

        return OpenMetadataAttributeCardinality.UNKNOWN;

    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param typeDefAttributeStatus omrs type
     * @return open metadata type
     */
    private OpenMetadataTypeDefAttributeStatus getAttributeStatus(TypeDefAttributeStatus typeDefAttributeStatus)
    {
        if (typeDefAttributeStatus != null)
        {
            switch (typeDefAttributeStatus)
            {
                case ACTIVE_ATTRIBUTE ->
                {
                    return OpenMetadataTypeDefAttributeStatus.ACTIVE_ATTRIBUTE;
                }
                case RENAMED_ATTRIBUTE ->
                {
                    return OpenMetadataTypeDefAttributeStatus.RENAMED_ATTRIBUTE;
                }
                case DEPRECATED_ATTRIBUTE ->
                {
                    return OpenMetadataTypeDefAttributeStatus.DEPRECATED_ATTRIBUTE;
                }
            }
        }

        return null;
    }


    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param externalStandardMappings list of OMRS types
     * @return list of open metadata types
     */
    private List<ExternalStandardTypeMapping> getExternalStandardMappings(List<ExternalStandardMapping> externalStandardMappings)
    {
        if (externalStandardMappings != null)
        {
            List<ExternalStandardTypeMapping> externalStandardTypeMappings = new ArrayList<>();

            for (ExternalStandardMapping externalStandardMapping : externalStandardMappings)
            {
                externalStandardTypeMappings.add(this.getExternalStandardMapping(externalStandardMapping));
            }

            if (! externalStandardTypeMappings.isEmpty())
            {
                return externalStandardTypeMappings;
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param externalStandardMapping omrs type
     * @return open metadata type
     */
    private ExternalStandardTypeMapping getExternalStandardMapping(ExternalStandardMapping externalStandardMapping)
    {
        if (externalStandardMapping != null)
        {
            ExternalStandardTypeMapping externalStandardTypeMapping = new ExternalStandardTypeMapping();

            externalStandardTypeMapping.setStandardName(externalStandardMapping.getStandardName());
            externalStandardTypeMapping.setStandardOrganization(externalStandardMapping.getStandardOrganization());
            externalStandardTypeMapping.setStandardTypeName(externalStandardMapping.getStandardTypeName());

            return externalStandardTypeMapping;
        }

        return null;
    }


    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param instanceStatusList list of OMRS types
     * @return list of open metadata types
     */
    private List<ElementStatus> getElementStatuses(List<InstanceStatus> instanceStatusList)
    {
        if (instanceStatusList != null)
        {
            List<ElementStatus> elementStatusList = new ArrayList<>();

            for (InstanceStatus instanceStatus : instanceStatusList)
            {
                elementStatusList.add(this.getElementStatus(instanceStatus));
            }

            if (! elementStatusList.isEmpty())
            {
                return elementStatusList;
            }
        }

        return null;
    }


    /**
     * Translate the repository services' InstanceStatus to an ElementStatus.
     *
     * @param instanceStatus value from the repository services
     * @return ElementStatus enum
     */
    protected ElementStatus getElementStatus(InstanceStatus instanceStatus)
    {
        if (instanceStatus != null)
        {
            switch (instanceStatus)
            {
                case UNKNOWN ->
                {
                    return ElementStatus.UNKNOWN;
                }
                case DRAFT ->
                {
                    return ElementStatus.DRAFT;
                }
                case PREPARED ->
                {
                    return ElementStatus.PREPARED;
                }
                case PROPOSED ->
                {
                    return ElementStatus.PROPOSED;
                }
                case APPROVED ->
                {
                    return ElementStatus.APPROVED;
                }
                case REJECTED ->
                {
                    return ElementStatus.REJECTED;
                }
                case APPROVED_CONCEPT ->
                {
                    return ElementStatus.APPROVED_CONCEPT;
                }
                case UNDER_DEVELOPMENT ->
                {
                    return ElementStatus.UNDER_DEVELOPMENT;
                }
                case DEVELOPMENT_COMPLETE ->
                {
                    return ElementStatus.DEVELOPMENT_COMPLETE;
                }
                case APPROVED_FOR_DEPLOYMENT ->
                {
                    return ElementStatus.APPROVED_FOR_DEPLOYMENT;
                }
                case STANDBY ->
                {
                    return ElementStatus.STANDBY;
                }
                case ACTIVE ->
                {
                    return ElementStatus.ACTIVE;
                }
                case FAILED ->
                {
                    return ElementStatus.FAILED;
                }
                case DISABLED ->
                {
                    return ElementStatus.DISABLED;
                }
                case COMPLETE ->
                {
                    return ElementStatus.COMPLETE;
                }
                case DEPRECATED ->
                {
                    return ElementStatus.DEPRECATED;
                }
                case DELETED ->
                {
                    return ElementStatus.DELETED;
                }
                case OTHER ->
                {
                    return ElementStatus.OTHER;
                }
            }
        }

        return ElementStatus.UNKNOWN;
    }


    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param typeDefs list of OMRS types
     * @return list of open metadata types
     */
    private List<OpenMetadataTypeDef> getTypeDefs(List<TypeDef> typeDefs)
    {
        if (typeDefs != null)
        {
            List<OpenMetadataTypeDef> openMetadataTypeDefs = new ArrayList<>();
            for (TypeDef typeDef : typeDefs)
            {
                openMetadataTypeDefs.add(this.getTypeDef(typeDef));
            }

            if (! openMetadataTypeDefs.isEmpty())
            {
                return openMetadataTypeDefs;
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param propagationRule omrs type
     * @return open metadata type
     */
    private OpenMetadataClassificationPropagationRule getPropagationRule(ClassificationPropagationRule propagationRule)
    {
        if (propagationRule != null)
        {
            switch (propagationRule)
            {
                case NONE ->
                {
                    return OpenMetadataClassificationPropagationRule.NONE;
                }
                case ONE_TO_TWO ->
                {
                    return OpenMetadataClassificationPropagationRule.ONE_TO_TWO;
                }
                case TWO_TO_ONE ->
                {
                    return OpenMetadataClassificationPropagationRule.TWO_TO_ONE;
                }
                case BOTH ->
                {
                    return OpenMetadataClassificationPropagationRule.BOTH;
                }
            }
        }

        return OpenMetadataClassificationPropagationRule.NONE;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param relationshipEndDef omrs type
     * @return open metadata type
     */
    private OpenMetadataRelationshipEndDef getRelationshipEndDef(RelationshipEndDef relationshipEndDef)
    {
        if (relationshipEndDef != null)
        {
            OpenMetadataRelationshipEndDef openMetadataRelationshipEndDef = new OpenMetadataRelationshipEndDef();

            openMetadataRelationshipEndDef.setEntityType(this.getTypeDefLink(relationshipEndDef.getEntityType()));
            openMetadataRelationshipEndDef.setAttributeName(relationshipEndDef.getAttributeName());
            openMetadataRelationshipEndDef.setAttributeCardinality(this.getRelationshipEndCardinality(relationshipEndDef.getAttributeCardinality()));
            openMetadataRelationshipEndDef.setAttributeDescription(relationshipEndDef.getAttributeDescription());
            openMetadataRelationshipEndDef.setAttributeDescriptionGUID(relationshipEndDef.getAttributeDescriptionGUID());

            return openMetadataRelationshipEndDef;
        }

        return null;
    }


    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param typeDefLinks list of OMRS types
     * @return list of open metadata types
     */
    private List<OpenMetadataTypeDefLink> getTypeDefLinks(List<TypeDefLink> typeDefLinks)
    {
        if (typeDefLinks != null)
        {
            List<OpenMetadataTypeDefLink> openMetadataTypeDefs = new ArrayList<>();

            for (TypeDefLink typeDefLink : typeDefLinks)
            {
                openMetadataTypeDefs.add(this.getTypeDefLink(typeDefLink));
            }

            if (! openMetadataTypeDefs.isEmpty())
            {
                return openMetadataTypeDefs;
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param typeDefLink omrs type
     * @return open metadata type
     */
    private OpenMetadataTypeDefLink getTypeDefLink(TypeDefLink typeDefLink)
    {
        if (typeDefLink != null)
        {
            OpenMetadataTypeDefLink openMetadataTypeDefLink = new OpenMetadataTypeDefLink();

            openMetadataTypeDefLink.setGUID(typeDefLink.getGUID());
            openMetadataTypeDefLink.setName(typeDefLink.getName());
            openMetadataTypeDefLink.setStatus(getTypeDefStatus(typeDefLink.getStatus()));
            openMetadataTypeDefLink.setReplacedByTypeGUID(typeDefLink.getReplacedByTypeGUID());
            openMetadataTypeDefLink.setReplacedByTypeName(typeDefLink.getReplacedByTypeName());

            return openMetadataTypeDefLink;
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param typeDefStatus omrs type
     * @return open metadata type
     */
    private OpenMetadataTypeDefStatus getTypeDefStatus(TypeDefStatus typeDefStatus)
    {
        if (typeDefStatus != null)
        {
            switch (typeDefStatus)
            {
                case ACTIVE_TYPEDEF ->
                {
                    return OpenMetadataTypeDefStatus.ACTIVE_TYPEDEF;
                }
                case RENAMED_TYPEDEF ->
                {
                    return OpenMetadataTypeDefStatus.RENAMED_TYPEDEF;
                }
                case DEPRECATED_TYPEDEF ->
                {
                    return OpenMetadataTypeDefStatus.DEPRECATED_TYPEDEF;
                }
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param relationshipEndCardinality omrs type
     * @return open metadata type
     */
    private OpenMetadataRelationshipEndCardinality getRelationshipEndCardinality(RelationshipEndCardinality relationshipEndCardinality)
    {
        if (relationshipEndCardinality != null)
        {
            switch (relationshipEndCardinality)
            {
                case UNKNOWN ->
                {
                    return OpenMetadataRelationshipEndCardinality.UNKNOWN;
                }
                case ANY_NUMBER ->
                {
                    return OpenMetadataRelationshipEndCardinality.ANY_NUMBER;
                }
                case AT_MOST_ONE ->
                {
                    return OpenMetadataRelationshipEndCardinality.AT_MOST_ONE;
                }
            }
        }

        return OpenMetadataRelationshipEndCardinality.UNKNOWN;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param attributeTypeDef omrs type
     * @return open metadata type
     */
    private OpenMetadataAttributeTypeDef getAttributeTypeDef(AttributeTypeDef attributeTypeDef)
    {
        if (attributeTypeDef != null)
        {
            OpenMetadataAttributeTypeDef openMetadataAttributeTypeDef;

            if (attributeTypeDef instanceof PrimitiveDef primitiveDef)
            {
                OpenMetadataPrimitiveDef openMetadataPrimitiveDef = new OpenMetadataPrimitiveDef();

                openMetadataPrimitiveDef.setPrimitiveDefCategory(this.getPrimitiveDefCategory(primitiveDef.getPrimitiveDefCategory()));

                openMetadataAttributeTypeDef = openMetadataPrimitiveDef;
                openMetadataAttributeTypeDef.setCategory(OpenMetadataAttributeTypeDefCategory.PRIMITIVE);
            }
            else if (attributeTypeDef instanceof CollectionDef collectionDef)
            {
                OpenMetadataCollectionDef openMetadataCollectionDef = new OpenMetadataCollectionDef();

                openMetadataCollectionDef.setCollectionDefCategory(this.getCollectionDefCategory(collectionDef.getCollectionDefCategory()));
                openMetadataCollectionDef.setArgumentCount(collectionDef.getArgumentCount());

                if (collectionDef.getArgumentTypes() != null)
                {
                    List<OpenMetadataPrimitiveDefCategory> argumentTypes = new ArrayList<>();

                    for (PrimitiveDefCategory argumentType : collectionDef.getArgumentTypes())
                    {
                        argumentTypes.add(this.getPrimitiveDefCategory(argumentType));
                    }

                    if (! argumentTypes.isEmpty())
                    {
                        openMetadataCollectionDef.setArgumentTypes(argumentTypes);
                    }
                }

                openMetadataAttributeTypeDef = openMetadataCollectionDef;
                openMetadataAttributeTypeDef.setCategory(OpenMetadataAttributeTypeDefCategory.COLLECTION);
            }
            else if (attributeTypeDef instanceof EnumDef enumDef)
            {
                OpenMetadataEnumDef openMetadataEnumDef = new OpenMetadataEnumDef();

                openMetadataEnumDef.setElementDefs(getEnumElementDefs(enumDef.getElementDefs()));
                openMetadataEnumDef.setDefaultValue(getEnumElementDef(enumDef.getDefaultValue()));

                openMetadataAttributeTypeDef = openMetadataEnumDef;
                openMetadataAttributeTypeDef.setCategory(OpenMetadataAttributeTypeDefCategory.ENUM_DEF);
            }
            else
            {
                return null;
            }

            openMetadataAttributeTypeDef.setGUID(attributeTypeDef.getGUID());
            openMetadataAttributeTypeDef.setName(attributeTypeDef.getName());
            openMetadataAttributeTypeDef.setDescription(attributeTypeDef.getDescription());
            openMetadataAttributeTypeDef.setDescriptionGUID(attributeTypeDef.getDescriptionGUID());
            openMetadataAttributeTypeDef.setVersion(attributeTypeDef.getVersion());
            openMetadataAttributeTypeDef.setVersionName(attributeTypeDef.getVersionName());

            return openMetadataAttributeTypeDef;
        }

        return null;
    }


    /**
     * Return a list of open metadata type equivalents to the OMRS types supplied in the parameter.
     *
     * @param enumElementDefs omrs types
     * @return open metadata type
     */
    private List<OpenMetadataEnumElementDef> getEnumElementDefs(List<EnumElementDef> enumElementDefs)
    {
        if (enumElementDefs != null)
        {
            List<OpenMetadataEnumElementDef> openMetadataEnumElementDefs = new ArrayList<>();

            for (EnumElementDef enumElementDef : enumElementDefs)
            {
                if (enumElementDef != null)
                {
                    openMetadataEnumElementDefs.add(getEnumElementDef(enumElementDef));
                }
            }

            if (! openMetadataEnumElementDefs.isEmpty())
            {
                return openMetadataEnumElementDefs;
            }
        }

        return null;
    }


    /**
     * Return an open metadata type equivalent to the OMRS type supplied in the parameter.
     *
     * @param enumElementDef omrs type
     * @return open metadata type
     */
    private OpenMetadataEnumElementDef getEnumElementDef(EnumElementDef enumElementDef)
    {
        if (enumElementDef != null)
        {
            OpenMetadataEnumElementDef openMetadataEnumElementDef = new OpenMetadataEnumElementDef();

            openMetadataEnumElementDef.setOrdinal(enumElementDef.getOrdinal());
            openMetadataEnumElementDef.setValue(enumElementDef.getValue());
            openMetadataEnumElementDef.setDescription(enumElementDef.getDescription());
            openMetadataEnumElementDef.setDescriptionGUID(enumElementDef.getDescriptionGUID());

            return openMetadataEnumElementDef;
        }

        return null;
    }


    /**
     * Convert the OMRS category to the open metadata version of the category.
     *
     * @param primitiveDefCategory omrs category
     * @return open metadata category
     */
    private OpenMetadataPrimitiveDefCategory getPrimitiveDefCategory(PrimitiveDefCategory primitiveDefCategory)
    {
        if (primitiveDefCategory != null)
        {
            switch (primitiveDefCategory)
            {
                case OM_PRIMITIVE_TYPE_UNKNOWN ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_UNKNOWN;
                }
                case OM_PRIMITIVE_TYPE_BOOLEAN ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_BOOLEAN;
                }
                case OM_PRIMITIVE_TYPE_BYTE ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_BYTE;
                }
                case OM_PRIMITIVE_TYPE_CHAR ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_CHAR;
                }
                case OM_PRIMITIVE_TYPE_SHORT ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_SHORT;
                }
                case OM_PRIMITIVE_TYPE_INT ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_INT;
                }
                case OM_PRIMITIVE_TYPE_LONG ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_LONG;
                }
                case OM_PRIMITIVE_TYPE_FLOAT ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_FLOAT;
                }
                case OM_PRIMITIVE_TYPE_DOUBLE ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_DOUBLE;
                }
                case OM_PRIMITIVE_TYPE_BIGINTEGER ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_BIGINTEGER;
                }
                case OM_PRIMITIVE_TYPE_BIGDECIMAL ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_BIGDECIMAL;
                }
                case OM_PRIMITIVE_TYPE_STRING ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_STRING;
                }
                case OM_PRIMITIVE_TYPE_DATE ->
                {
                    return OpenMetadataPrimitiveDefCategory.OM_PRIMITIVE_TYPE_DATE;
                }
            }
        }

        return null;
    }


    /**
     * Convert the OMRS category to the open metadata version of the category.
     *
     * @param collectionDefCategory omrs category
     * @return open metadata category
     */
    private OpenMetadataCollectionDefCategory getCollectionDefCategory(CollectionDefCategory collectionDefCategory)
    {
        if (collectionDefCategory != null)
        {
            switch(collectionDefCategory)
            {
                case OM_COLLECTION_UNKNOWN ->
                {
                    return OpenMetadataCollectionDefCategory.OM_COLLECTION_UNKNOWN;
                }
                case OM_COLLECTION_MAP ->
                {
                    return OpenMetadataCollectionDefCategory.OM_COLLECTION_MAP;
                }
                case OM_COLLECTION_ARRAY ->
                {
                    return OpenMetadataCollectionDefCategory.OM_COLLECTION_ARRAY;
                }
                case OM_COLLECTION_STRUCT ->
                {
                    return OpenMetadataCollectionDefCategory.OM_COLLECTION_STRUCT;
                }
            }
        }

        return null;
    }

    /**
     * Return a list of open metadata types equivalent to the OMRS types supplied in the parameter.
     *
     * @param attributeTypeDefs list of OMRS types
     * @return list of open metadata types
     */
    private List<OpenMetadataAttributeTypeDef> getAttributeTypeDefs(List<AttributeTypeDef> attributeTypeDefs)
    {
        if (attributeTypeDefs != null)
        {
            List<OpenMetadataAttributeTypeDef> openMetadataAttributeTypeDefs = new ArrayList<>();
            for (AttributeTypeDef attributeTypeDef : attributeTypeDefs)
            {
                openMetadataAttributeTypeDefs.add(this.getAttributeTypeDef(attributeTypeDef));
            }

            if (! openMetadataAttributeTypeDefs.isEmpty())
            {
                return openMetadataAttributeTypeDefs;
            }
        }

        return null;
    }
}
