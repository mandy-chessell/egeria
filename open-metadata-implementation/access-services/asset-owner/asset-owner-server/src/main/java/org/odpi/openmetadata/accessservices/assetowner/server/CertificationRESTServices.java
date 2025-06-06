/*  SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.assetowner.server;


import org.odpi.openmetadata.commonservices.ffdc.rest.*;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.CertificationTypeElement;

import org.odpi.openmetadata.commonservices.ffdc.RESTCallLogger;
import org.odpi.openmetadata.commonservices.ffdc.RESTCallToken;
import org.odpi.openmetadata.commonservices.ffdc.RESTExceptionHandler;
import org.odpi.openmetadata.commonservices.generichandlers.CertificationHandler;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.RelatedElementStub;
import org.odpi.openmetadata.frameworks.openmetadata.properties.governance.CertificationProperties;
import org.odpi.openmetadata.frameworks.openmetadata.properties.governance.CertificationTypeProperties;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.commonservices.generichandlers.ReferenceableHandler;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * CertificationRESTServices is the java client for managing certification types and the certification of elements.
 */
public class CertificationRESTServices
{
    private static final AssetOwnerInstanceHandler instanceHandler = new AssetOwnerInstanceHandler();

    private static final RESTCallLogger restCallLogger = new RESTCallLogger(LoggerFactory.getLogger(CertificationRESTServices.class),
                                                                            instanceHandler.getServiceName());

    private final RESTExceptionHandler restExceptionHandler = new RESTExceptionHandler();

    /**
     * Default constructor
     */
    public CertificationRESTServices()
    {
    }
 

    /* ========================================
     * Certification Types
     */


    /**
     * Retrieve the certification type by the unique identifier assigned by this service when it was created.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param certificationTypeGUID identifier of the governance definition to retrieve
     *
     * @return properties of the certification type or
     *  InvalidParameterException guid or userId is null; guid is not recognized
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public CertificationTypeResponse getCertificationTypeByGUID(String serverName,
                                                                String userId,
                                                                String certificationTypeGUID)
    {
        final String methodName = "getCertificationTypeByGUID";
        final String guidParameterName = "certificationTypeGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        CertificationTypeResponse response = new CertificationTypeResponse();
        AuditLog                  auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

            response.setElement(handler.getGovernanceDefinitionByGUID(userId,
                                                                      certificationTypeGUID,
                                                                      guidParameterName,
                                                                      false,
                                                                      false,
                                                                      new Date(),
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
     * Retrieve the certification type by its assigned unique document identifier.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param documentIdentifier identifier to search for
     *
     * @return properties of the matching certification type or
     *  InvalidParameterException documentIdentifier or userId is null; documentIdentifier is not recognized
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public CertificationTypeResponse getCertificationTypeByDocId(String serverName,
                                                                 String userId,
                                                                 String documentIdentifier)
    {
        final String   methodName = "getCertificationTypeByDocId";

        final String   documentIdParameterName = "documentId";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        CertificationTypeResponse response = new CertificationTypeResponse();
        AuditLog                  auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

            List<CertificationTypeElement> certificationTypeElements = handler.getGovernanceDefinitionsByName(userId,
                                                                                                              OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                                                                              documentIdentifier,
                                                                                                              documentIdParameterName,
                                                                                                              0,
                                                                                                              0,
                                                                                                              false,
                                                                                                              false,
                                                                                                              new Date(),
                                                                                                              methodName);

            if ((certificationTypeElements != null) && (! certificationTypeElements.isEmpty()))
            {
                response.setElement(certificationTypeElements.get(0));
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
     * Retrieve all the certification types for a particular title.  The title can include regEx wildcards.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param requestBody short description of the certification
     * @param startFrom where to start from in the list of definitions
     * @param pageSize max number of results to return in one call
     *
     * @return list of matching certification types (null if no matching elements) or
     *  InvalidParameterException title or userId is null
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public CertificationTypesResponse getCertificationTypesByTitle(String                  serverName,
                                                                   String                  userId,
                                                                   int                     startFrom,
                                                                   int                     pageSize,
                                                                   SearchStringRequestBody requestBody)
    {
        final String methodName = "getCertificationTypesByTitle";
        final String titleParameterName = "title";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        CertificationTypesResponse response = new CertificationTypesResponse();
        AuditLog                   auditLog = null;

        try
        {
            if (requestBody != null)
            {
                auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

                CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

                response.setElements(handler.findGovernanceDefinitions(userId,
                                                                       OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                                       requestBody.getSearchString(),
                                                                       titleParameterName,
                                                                       startFrom,
                                                                       pageSize,
                                                                       false,
                                                                       false,
                                                                       new Date(),
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
     * Retrieve all the certification type definitions for a specific governance domain.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param domainIdentifier identifier to search for
     * @param startFrom where to start from in the list of definitions
     * @param pageSize max number of results to return in one call
     *
     * @return properties of the matching certification type definitions or
     *  InvalidParameterException domainIdentifier or userId is null; domainIdentifier is not recognized
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public CertificationTypesResponse getCertificationTypeByDomainId(String serverName,
                                                                     String userId,
                                                                     int    domainIdentifier,
                                                                     int    startFrom,
                                                                     int    pageSize)
    {
        final String methodName = "getCertificationTypeByDomainId";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        CertificationTypesResponse response = new CertificationTypesResponse();
        AuditLog                   auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

            CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

            response.setElements(handler.getGovernanceDefinitionsByDomain(userId,
                                                                          OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                                          domainIdentifier,
                                                                          startFrom,
                                                                          pageSize,
                                                                          false,
                                                                          false,
                                                                          new Date(),
                                                                          methodName));

        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }


    /* =======================================
     * Certifications
     */

    /**
     * Link an element to a certification type and include details of the certification in the relationship properties.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param elementGUID unique identifier of the element being certified
     * @param certificationTypeGUID unique identifier for the certification type
     * @param requestBody the properties of the certification
     *
     * @return guid or
     *  InvalidParameterException one of the properties is invalid
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public GUIDResponse certifyElement(String                  serverName,
                                       String                  userId,
                                       String                  elementGUID,
                                       String                  certificationTypeGUID,
                                       RelationshipRequestBody requestBody)
    {
        final String methodName = "certifyElement";

        final String elementGUIDParameterName = "elementGUID";
        final String certificationTypeGUIDParameterName = "certificationTypeGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        GUIDResponse response = new GUIDResponse();
        AuditLog     auditLog = null;

        try
        {
            if (requestBody != null)
            {
                if (requestBody.getProperties() instanceof CertificationProperties properties)
                {
                    auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

                    CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

                    response.setGUID(handler.certifyElement(userId,
                                                            null,
                                                            null,
                                                            elementGUID,
                                                            elementGUIDParameterName,
                                                            OpenMetadataType.REFERENCEABLE.typeName,
                                                            certificationTypeGUID,
                                                            certificationTypeGUIDParameterName,
                                                            OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                            properties.getCertificateId(),
                                                            properties.getStartDate(),
                                                            properties.getEndDate(),
                                                            properties.getConditions(),
                                                            properties.getCertifiedBy(),
                                                            properties.getCertifiedByTypeName(),
                                                            properties.getCertifiedByPropertyName(),
                                                            properties.getCustodian(),
                                                            properties.getCustodianTypeName(),
                                                            properties.getCustodianPropertyName(),
                                                            properties.getRecipient(),
                                                            properties.getRecipientTypeName(),
                                                            properties.getRecipientPropertyName(),
                                                            properties.getNotes(),
                                                            properties.getEffectiveFrom(),
                                                            properties.getEffectiveTo(),
                                                            false,
                                                            false,
                                                            new Date(),
                                                            methodName));
                }
                else
                {
                    restExceptionHandler.handleInvalidPropertiesObject(CertificationTypeProperties.class.getName(), methodName);
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
     * Update the properties of a certification.  Remember to include the certificationId in the properties if the element has multiple
     * certifications for the same certification type.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param certificationGUID unique identifier for the certification type
     * @param isMergeUpdate should the supplied properties overlay the existing properties or replace them
     * @param requestBody the properties of the certification
     *
     * @return void or
     *  InvalidParameterException one of the properties is invalid
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public VoidResponse updateCertification(String                  serverName,
                                            String                  userId,
                                            String                  certificationGUID,
                                            boolean                 isMergeUpdate,
                                            RelationshipRequestBody requestBody)
    {
        final String methodName = "updateCertification";
        final String certificationGUIDParameterName = "certificationGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        VoidResponse response = new VoidResponse();
        AuditLog     auditLog = null;

        try
        {
            if (requestBody != null)
            {
                if (requestBody.getProperties() instanceof CertificationProperties properties)
                {
                    auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);

                    CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

                    handler.updateCertification(userId,
                                                null,
                                                null,
                                                certificationGUID,
                                                certificationGUIDParameterName,
                                                properties.getCertificateId(),
                                                properties.getStartDate(),
                                                properties.getEndDate(),
                                                properties.getConditions(),
                                                properties.getCertifiedBy(),
                                                properties.getCertifiedByTypeName(),
                                                properties.getCertifiedByPropertyName(),
                                                properties.getCustodian(),
                                                properties.getCustodianTypeName(),
                                                properties.getCustodianPropertyName(),
                                                properties.getRecipient(),
                                                properties.getRecipientTypeName(),
                                                properties.getRecipientPropertyName(),
                                                properties.getNotes(),
                                                isMergeUpdate,
                                                null,
                                                null,
                                                false,
                                                false,
                                                new Date(),
                                                methodName);
                }
                else
                {
                    restExceptionHandler.handleInvalidPropertiesObject(CertificationProperties.class.getName(), methodName);
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
     * Remove the certification for an element.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param certificationGUID unique identifier for the certification type
     * @param requestBody external source information.
     *
     * @return void or
     *  InvalidParameterException one of the properties is invalid
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    @SuppressWarnings(value="unused")
    public VoidResponse decertifyElement(String                  serverName,
                                         String                  userId,
                                         String                  certificationGUID,
                                         RelationshipRequestBody requestBody)
    {
        final String methodName = "decertifyElement";
        final String certificationGUIDParameterName = "certificationGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        VoidResponse response = new VoidResponse();
        AuditLog     auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            CertificationHandler<CertificationTypeElement> handler = instanceHandler.getCertificationTypeHandler(userId, serverName, methodName);

            handler.decertifyElement(userId,
                                     null,
                                     null,
                                     certificationGUID,
                                     certificationGUIDParameterName,
                                     false,
                                     false,
                                     new Date(),
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
     * Return information about the elements linked to a certification.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param certificationTypeGUID unique identifier for the certification
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return properties of the certification or
     *  InvalidParameterException qualifiedName or userId is null
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public RelatedElementsResponse getCertifiedElements(String serverName,
                                                        String userId,
                                                        String certificationTypeGUID,
                                                        int    startFrom,
                                                        int    pageSize)
    {
        final String methodName = "getCertifiedElements";
        final String guidParameter = "certificationTypeGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        RelatedElementsResponse response = new RelatedElementsResponse();
        AuditLog                auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            ReferenceableHandler<RelatedElementStub> handler = instanceHandler.getRelatedElementHandler(userId, serverName, methodName);

            response.setElements(handler.getAttachedElements(userId,
                                                             certificationTypeGUID,
                                                             guidParameter,
                                                             OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                             OpenMetadataType.CERTIFICATION_RELATIONSHIP.typeGUID,
                                                             OpenMetadataType.CERTIFICATION_RELATIONSHIP.typeName,
                                                             OpenMetadataType.REFERENCEABLE.typeName,
                                                             null,
                                                             null,
                                                             1,
                                                             null,
                                                             null,
                                                             SequencingOrder.CREATION_DATE_RECENT,
                                                             null,
                                                             false,
                                                             false,
                                                             startFrom,
                                                             pageSize,
                                                             new Date(),
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
     * Return information about the certifications linked to an element.
     *
     * @param serverName name of the server instance to connect to
     * @param userId calling user
     * @param elementGUID unique identifier for the certification
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     *
     * @return properties of the certification or
     *  InvalidParameterException qualifiedName or userId is null
     *  PropertyServerException problem accessing property server
     *  UserNotAuthorizedException security access problem
     */
    public RelatedElementsResponse getCertifications(String serverName,
                                                     String userId,
                                                     String elementGUID,
                                                     int    startFrom,
                                                     int    pageSize)
    {
        final String methodName = "getLicences";
        final String guidParameterName = "elementGUID";

        RESTCallToken token = restCallLogger.logRESTCall(serverName, userId, methodName);

        RelatedElementsResponse response = new RelatedElementsResponse();
        AuditLog                auditLog = null;

        try
        {
            auditLog = instanceHandler.getAuditLog(userId, serverName, methodName);
            ReferenceableHandler<RelatedElementStub> handler = instanceHandler.getRelatedElementHandler(userId, serverName, methodName);

            response.setElements(handler.getAttachedElements(userId,
                                                             elementGUID,
                                                             guidParameterName,
                                                             OpenMetadataType.REFERENCEABLE.typeName,
                                                             OpenMetadataType.CERTIFICATION_RELATIONSHIP.typeGUID,
                                                             OpenMetadataType.CERTIFICATION_RELATIONSHIP.typeName,
                                                             OpenMetadataType.CERTIFICATION_TYPE.typeName,
                                                             null,
                                                             null,
                                                             2,
                                                             null,
                                                             null,
                                                             SequencingOrder.CREATION_DATE_RECENT,
                                                             null,
                                                             false,
                                                             false,
                                                             startFrom,
                                                             pageSize,
                                                             new Date(),
                                                             methodName));
        }
        catch (Throwable error)
        {
            restExceptionHandler.captureRuntimeExceptions(response, error, methodName, auditLog);
        }

        restCallLogger.logRESTCallReturn(token, response.toString());
        return response;
    }
}
