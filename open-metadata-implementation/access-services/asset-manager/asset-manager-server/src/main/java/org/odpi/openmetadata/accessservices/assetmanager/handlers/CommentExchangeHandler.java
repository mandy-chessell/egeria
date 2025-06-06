/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.assetmanager.handlers;

import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.CorrelatedMetadataElement;
import org.odpi.openmetadata.commonservices.generichandlers.CommentConverter;
import org.odpi.openmetadata.accessservices.assetmanager.metadataelements.CommentElement;
import org.odpi.openmetadata.frameworks.openmetadata.properties.MetadataCorrelationProperties;
import org.odpi.openmetadata.commonservices.ffdc.InvalidParameterHandler;
import org.odpi.openmetadata.commonservices.generichandlers.CommentHandler;
import org.odpi.openmetadata.frameworks.openmetadata.enums.CommentType;
import org.odpi.openmetadata.frameworks.openmetadata.properties.feedback.CommentProperties;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.commonservices.repositoryhandler.RepositoryHandler;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.metadatasecurity.server.OpenMetadataServerSecurityVerifier;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.repositoryconnector.OMRSRepositoryHelper;

import java.util.Date;
import java.util.List;

/**
 * CommentExchangeHandler is the server side handler for managing comments.
 */
public class CommentExchangeHandler extends ExchangeHandlerBase
{
    private final CommentHandler<CommentElement> commentHandler;

    private final static String commentGUIDParameterName = "commentGUID";

    /**
     * Construct the comment exchange handler with information needed to work with comment related objects
     * for Asset Manager OMAS.
     *
     * @param serviceName      name of this service
     * @param serverName       name of the local server
     * @param invalidParameterHandler handler for managing parameter errors
     * @param repositoryHandler     manages calls to the repository services
     * @param repositoryHelper provides utilities for manipulating the repository services objects
     * @param localServerUserId userId for this server
     * @param securityVerifier open metadata security services verifier
     * @param supportedZones list of zones that the access service is allowed to serve instances from.
     * @param defaultZones list of zones that the access service should set in all new instances.
     * @param publishZones list of zones that the access service sets up in published instances.
     * @param auditLog destination for audit log events.
     */
    public CommentExchangeHandler(String                             serviceName,
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
        super(serviceName,
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

        commentHandler = new CommentHandler<>(new CommentConverter<>(repositoryHelper, serviceName, serverName),
                                              CommentElement.class,
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



    /* ========================================================
     * Managing the externalIds and related correlation properties.
     */


    /**
     * Update each returned element with details of the correlation properties for the supplied asset manager.
     *
     * @param userId calling user
     * @param assetManagerGUID unique identifier of software server capability representing the caller
     * @param assetManagerName unique name of software server capability representing the caller
     * @param results list of elements
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    private void addCorrelationPropertiesToComments(String                userId,
                                                    String                assetManagerGUID,
                                                    String                assetManagerName,
                                                    List<CommentElement>  results,
                                                    String                methodName) throws InvalidParameterException,
                                                                                               UserNotAuthorizedException,
                                                                                               PropertyServerException
    {
        if (results != null)
        {
            for (CorrelatedMetadataElement comment : results)
            {
                if ((comment != null) && (comment.getElementHeader() != null) && (comment.getElementHeader().getGUID() != null))
                {
                    comment.setCorrelationHeaders(this.getCorrelationProperties(userId,
                                                                                comment.getElementHeader().getGUID(),
                                                                                commentGUIDParameterName,
                                                                                OpenMetadataType.COMMENT.typeName,
                                                                                assetManagerGUID,
                                                                                assetManagerName,
                                                                                false,
                                                                                false,
                                                                                null,
                                                                                methodName));
                }
            }
        }
    }


    /**
     * Create a new comment.
     *
     * @param userId calling user
     * @param guid unique identifier of the element to attach the comment to
     * @param guidParameterName parameter for guid
     * @param correlationProperties  properties to help with the mapping of the elements in the external asset manager and open metadata
     * @param isPublic is this visible to other people
     * @param commentProperties properties to store
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime when should the elements be effected for - null is anytime; new Date() is now
     * @param methodName calling method
     *
     * @return unique identifier of the new metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public String createComment(String                        userId,
                                String                        guid,
                                String                        guidParameterName,
                                MetadataCorrelationProperties correlationProperties,
                                boolean                       isPublic,
                                CommentProperties             commentProperties,
                                boolean                       forLineage,
                                boolean                       forDuplicateProcessing,
                                Date                          effectiveTime,
                                String                        methodName) throws InvalidParameterException,
                                                                                 UserNotAuthorizedException,
                                                                                 PropertyServerException
    {
        final String propertiesParameterName    = "commentProperties";
        final String commentText = "commentProperties.getText";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateObject(commentProperties, propertiesParameterName, methodName);
        invalidParameterHandler.validateName(commentProperties.getCommentText(), commentText, methodName);

        int commentType = CommentType.STANDARD_COMMENT.getOrdinal();

        if (commentProperties.getCommentType() != null)
        {
            commentType = commentProperties.getCommentType().getOrdinal();
        }

        String commentGUID = commentHandler.attachNewComment(userId,
                                                             getExternalSourceGUID(correlationProperties),
                                                             getExternalSourceName(correlationProperties),
                                                             guid,
                                                             guid,
                                                             guidParameterName,
                                                             commentType,
                                                             commentProperties.getCommentText(),
                                                             isPublic,
                                                             commentProperties.getEffectiveFrom(),
                                                             commentProperties.getEffectiveTo(),
                                                             forLineage,
                                                             forDuplicateProcessing,
                                                             effectiveTime,
                                                             methodName);

        if (commentGUID != null)
        {
            this.createExternalIdentifier(userId,
                                          commentGUID,
                                          commentGUIDParameterName,
                                          OpenMetadataType.COMMENT.typeName,
                                          correlationProperties,
                                          forLineage,
                                          forDuplicateProcessing,
                                          effectiveTime,
                                          methodName);
        }

        return commentGUID;
    }


    /**
     * Update the metadata element representing a comment.
     *
     * @param userId calling user
     * @param correlationProperties  properties to help with the mapping of the elements in the external asset manager and open metadata
     * @param commentGUID unique identifier of the metadata element to update
     * @param commentProperties new properties for this element
     * @param isMergeUpdate should the properties be merged with the existing properties or completely over-write them
     * @param isPublic is this visible to other people
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime when should the elements be effected for - null is anytime; new Date() is now
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void updateComment(String                        userId,
                              MetadataCorrelationProperties correlationProperties,
                              String                        commentGUID,
                              CommentProperties             commentProperties,
                              boolean                       isMergeUpdate,
                              boolean                       isPublic,
                              boolean                       forLineage,
                              boolean                       forDuplicateProcessing,
                              Date                          effectiveTime,
                              String                        methodName) throws InvalidParameterException,
                                                                                UserNotAuthorizedException,
                                                                                PropertyServerException
    {
        final String propertiesParameterName    = "commentProperties";
        final String qualifiedNameParameterName = "commentProperties.qualifiedName";

        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(commentGUID, commentGUIDParameterName, methodName);
        invalidParameterHandler.validateObject(commentProperties, propertiesParameterName, methodName);
        if (! isMergeUpdate)
        {
            invalidParameterHandler.validateName(commentProperties.getQualifiedName(), qualifiedNameParameterName, methodName);
        }

        int commentType = CommentType.STANDARD_COMMENT.getOrdinal();

        if (commentProperties.getCommentType() != null)
        {
            commentType = commentProperties.getCommentType().getOrdinal();
        }

        this.validateExternalIdentifier(userId,
                                        commentGUID,
                                        commentGUIDParameterName,
                                        OpenMetadataType.COMMENT.typeName,
                                        correlationProperties,
                                        forLineage,
                                        forDuplicateProcessing,
                                        effectiveTime,
                                        methodName);

        commentHandler.updateComment(userId,
                                     getExternalSourceGUID(correlationProperties),
                                     getExternalSourceName(correlationProperties),
                                     commentGUID,
                                     commentGUIDParameterName,
                                     commentProperties.getQualifiedName(),
                                     commentType,
                                     commentProperties.getCommentText(),
                                     isPublic,
                                     isMergeUpdate,
                                     commentProperties.getEffectiveFrom(),
                                     commentProperties.getEffectiveTo(),
                                     forLineage,
                                     forDuplicateProcessing,
                                     effectiveTime,
                                     methodName);
    }


    /**
     * Remove the metadata element representing a comment.  This will delete the comment and all comment replies.
     *
     * @param userId calling user
     * @param correlationProperties properties to help with the mapping of the elements in the external asset manager and open metadata
     * @param commentGUID unique identifier of the metadata element to remove
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime optional date for effective time of the query.  Null means any effective time
     * @param methodName calling method
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public void removeComment(String                        userId,
                              MetadataCorrelationProperties correlationProperties,
                              String                        commentGUID,
                              boolean                       forLineage,
                              boolean                       forDuplicateProcessing,
                              Date                          effectiveTime,
                              String                        methodName) throws InvalidParameterException,
                                                                               UserNotAuthorizedException,
                                                                               PropertyServerException
    {
        invalidParameterHandler.validateUserId(userId, methodName);
        invalidParameterHandler.validateGUID(commentGUID, commentGUIDParameterName, methodName);

        this.validateExternalIdentifier(userId,
                                        commentGUID,
                                        commentGUIDParameterName,
                                        OpenMetadataType.COMMENT.typeName,
                                        correlationProperties,
                                        forLineage,
                                        forDuplicateProcessing,
                                        effectiveTime,
                                        methodName);

        if (correlationProperties != null)
        {
            commentHandler.removeCommentFromElement(userId,
                                                    this.getExternalSourceGUID(correlationProperties),
                                                    this.getExternalSourceName(correlationProperties),
                                                    commentGUID,
                                                    commentGUIDParameterName,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);
        }
        else
        {
            commentHandler.removeCommentFromElement(userId,
                                                    null,
                                                    null,
                                                    commentGUID,
                                                    commentGUIDParameterName,
                                                    forLineage,
                                                    forDuplicateProcessing,
                                                    effectiveTime,
                                                    methodName);
        }
    }


    /**
     * Return the comments attached to an element.
     *
     * @param userId calling user
     * @param assetManagerGUID unique identifier of software server capability representing the caller
     * @param assetManagerName unique name of software server capability representing the caller
     * @param elementGUID    unique identifier for the element where the like is attached.
     * @param elementGUIDParameterName name of parameter for elementGUID
     * @param startFrom paging start point
     * @param pageSize maximum results that can be returned
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime  the time that the retrieved elements must be effective for (null for any time, new Date() for now)
     * @param methodName calling method
     *
     * @return list of matching metadata elements
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public List<CommentElement> getAttachedComments(String  userId,
                                                    String  assetManagerGUID,
                                                    String  assetManagerName,
                                                    String  elementGUID,
                                                    String  elementGUIDParameterName,
                                                    int     startFrom,
                                                    int     pageSize,
                                                    boolean forLineage,
                                                    boolean forDuplicateProcessing,
                                                    Date    effectiveTime,
                                                    String  methodName) throws InvalidParameterException,
                                                                               UserNotAuthorizedException,
                                                                               PropertyServerException
    {
        List<CommentElement> results = commentHandler.getComments(userId,
                                                                  elementGUID,
                                                                  elementGUIDParameterName,
                                                                  OpenMetadataType.REFERENCEABLE.typeName,
                                                                  startFrom,
                                                                  pageSize,
                                                                  forLineage,
                                                                  forDuplicateProcessing,
                                                                  effectiveTime,
                                                                  methodName);

        addCorrelationPropertiesToComments(userId, assetManagerGUID, assetManagerName, results , methodName);

        return results;
    }


    /**
     * Retrieve the comment metadata element with the supplied unique identifier.
     *
     * @param userId calling user
     * @param assetManagerGUID unique identifier of software server capability representing the caller
     * @param assetManagerName unique name of software server capability representing the caller
     * @param commentGUID unique identifier of the requested metadata element
     * @param forLineage return elements marked with the Memento classification?
     * @param forDuplicateProcessing do not merge elements marked as duplicates?
     * @param effectiveTime when should the elements be effected for - null is anytime; new Date() is now
     * @param methodName calling method
     *
     * @return matching metadata element
     *
     * @throws InvalidParameterException  one of the parameters is invalid
     * @throws UserNotAuthorizedException the user is not authorized to issue this request
     * @throws PropertyServerException    there is a problem reported in the open metadata server(s)
     */
    public CommentElement getCommentByGUID(String  userId,
                                           String  assetManagerGUID,
                                           String  assetManagerName,
                                           String  commentGUID,
                                           boolean forLineage,
                                           boolean forDuplicateProcessing,
                                           Date    effectiveTime,
                                           String  methodName) throws InvalidParameterException,
                                                                      UserNotAuthorizedException,
                                                                      PropertyServerException
    {
        final String guidParameterName  = "commentGUID";

        CommentElement comment = commentHandler.getBeanFromRepository(userId,
                                                                      commentGUID,
                                                                      guidParameterName,
                                                                      OpenMetadataType.COMMENT.typeName,
                                                                      forLineage,
                                                                      forDuplicateProcessing,
                                                                      effectiveTime,
                                                                      methodName);

        if (comment != null)
        {
            comment.setCorrelationHeaders(this.getCorrelationProperties(userId,
                                                                        commentGUID,
                                                                        guidParameterName,
                                                                        OpenMetadataType.COMMENT.typeName,
                                                                        assetManagerGUID,
                                                                        assetManagerName,
                                                                        forLineage,
                                                                        forDuplicateProcessing,
                                                                        effectiveTime,
                                                                        methodName));
        }

        return comment;
    }
}
