/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.repositoryservices.ffdc.exception;

import org.odpi.openmetadata.frameworks.auditlog.messagesets.ExceptionMessageDefinition;
import org.odpi.openmetadata.frameworks.openmetadata.ffdc.OMFCheckedExceptionBase;

import java.io.Serial;
import java.util.Map;

/**
 * The InvalidParameterException is thrown by an OMRS Connector when the parameters passed to a repository
 * connector, or its accompanying metadata collection, are not valid.
 */
public class InvalidParameterException extends OMRSCheckedExceptionBase
{
    @Serial
    private static final long serialVersionUID = 1L;

    private   String  parameterName = null;


    /**
     * This is the typical constructor used for creating an InvalidParameterException.
     *
     * @param messageDefinition  content of the message
     * @param className   name of class reporting error
     * @param actionDescription   description of function it was performing when error detected
     * @param parameterName name of the invalid parameter if known
     */
    public InvalidParameterException(ExceptionMessageDefinition messageDefinition,
                                     String                     className,
                                     String                     actionDescription,
                                     String                     parameterName)
    {
        super(messageDefinition, className, actionDescription);

        this.parameterName = parameterName;
    }


    /**
     * This is the typical constructor used for creating an InvalidParameterException.
     * The properties allow additional information to be associated with the exception.
     *
     * @param messageDefinition  content of the message
     * @param className   name of class reporting error
     * @param actionDescription   description of function it was performing when error detected
     * @param parameterName name of the invalid parameter if known
     * @param relatedProperties  arbitrary properties that may help with diagnosing the problem.
     */
    public InvalidParameterException(ExceptionMessageDefinition messageDefinition,
                                     String                     className,
                                     String                     actionDescription,
                                     String                     parameterName,
                                     Map<String, Object>        relatedProperties)
    {
        super(messageDefinition, className, actionDescription, relatedProperties);

        this.parameterName = parameterName;
    }


    /**
     * This is the constructor used for creating an InvalidParameterException when an unexpected error has been caught.
     * The properties allow additional information to be associated with the exception.
     *
     * @param messageDefinition  content of the message
     * @param className   name of class reporting error
     * @param actionDescription   description of function it was performing when error detected
     * @param caughtError   previous error causing this exception
     * @param parameterName name of the invalid parameter if known
     */
    public InvalidParameterException(ExceptionMessageDefinition messageDefinition,
                                     String                     className,
                                     String                     actionDescription,
                                     Exception                  caughtError,
                                     String                     parameterName)
    {
        super(messageDefinition, className, actionDescription, caughtError);

        this.parameterName = parameterName;
    }


    /**
     * This is the constructor used for creating an InvalidParameterException when an unexpected error has been caught.
     * The properties allow additional information to be associated with the exception.
     *
     * @param messageDefinition  content of the message
     * @param className   name of class reporting error
     * @param actionDescription   description of function it was performing when error detected
     * @param caughtError   previous error causing this exception
     * @param parameterName name of the invalid parameter if known
     * @param relatedProperties  arbitrary properties that may help with diagnosing the problem.
     */
    public InvalidParameterException(ExceptionMessageDefinition messageDefinition,
                                     String                     className,
                                     String                     actionDescription,
                                     Exception                  caughtError,
                                     String                     parameterName,
                                     Map<String, Object>        relatedProperties)
    {
        super(messageDefinition, className, actionDescription, caughtError, relatedProperties);

        this.parameterName = parameterName;
    }


    /**
     * This is the constructor used when receiving an exception from a remote server.  The values are
     * stored directly in the response object and are passed explicitly to the new exception.
     * Notice that the technical aspects of the exception - such as class name creating the exception
     * are local values so that the implementation of the server is not exposed.
     *
     * @param httpCode   http response code to use if this exception flows over a REST call
     * @param className   name of class reporting error
     * @param actionDescription   description of function it was performing when error detected
     * @param errorMessage   description of error
     * @param errorMessageId unique identifier for the message
     * @param errorMessageParameters parameters that were inserted in the message
     * @param systemAction   actions of the system as a result of the error
     * @param userAction   instructions for correcting the error
     * @param caughtErrorClassName   previous error causing this exception
     * @param parameterName name of the invalid parameter if known
     * @param relatedProperties  arbitrary properties that may help with diagnosing the problem.
     */
    public InvalidParameterException(int                 httpCode,
                                     String              className,
                                     String              actionDescription,
                                     String              errorMessage,
                                     String              errorMessageId,
                                     String[]            errorMessageParameters,
                                     String              systemAction,
                                     String              userAction,
                                     String              caughtErrorClassName,
                                     String              parameterName,
                                     Map<String, Object> relatedProperties)
    {
        super(httpCode,
              className,
              actionDescription,
              errorMessage,
              errorMessageId,
              errorMessageParameters,
              systemAction,
              userAction,
              caughtErrorClassName,
              relatedProperties);

        this.parameterName = parameterName;
    }


    /**
     * This is a copy constructor.
     *
     * @param caughtError  the error that resulted in this exception.
     * @param parameterName  new parameter name
     */
    public InvalidParameterException(OMFCheckedExceptionBase caughtError,
                                     String parameterName)
    {
        super(caughtError.getReportedErrorMessage(), caughtError);

        this.parameterName = parameterName;
    }


    /**
     * This is the typical constructor used for creating a InvalidParameterException.
     *
     * @param httpCode  http response code to use if this exception flows over a REST call
     * @param className  name of class reporting error
     * @param actionDescription  description of function it was performing when error detected
     * @param errorMessage  description of error
     * @param systemAction  actions of the system as a result of the error
     * @param userAction  instructions for correcting the error
     */
    @Deprecated
    public InvalidParameterException(int  httpCode, String className, String  actionDescription, String errorMessage, String systemAction, String userAction)
    {
        super(httpCode, className, actionDescription, errorMessage, systemAction, userAction);
    }

    /**
     * This is the typical constructor used for creating a InvalidParameterException.
     *
     * @param httpCode  http response code to use if this exception flows over a REST call
     * @param className  name of class reporting error
     * @param actionDescription  description of function it was performing when error detected
     * @param errorMessage  description of error
     * @param systemAction  actions of the system as a result of the error
     * @param userAction  instructions for correcting the error
     * @param parameterName name of the invalid parameter if known
     */
    @Deprecated
    public InvalidParameterException(int  httpCode, String className, String  actionDescription, String errorMessage, String systemAction, String userAction, String parameterName)
    {
        super(httpCode, className, actionDescription, errorMessage, systemAction, userAction);

        this.parameterName = parameterName;
    }


    /**
     * This is the constructor used for creating a InvalidParameterException that resulted from a previous error.
     *
     * @param httpCode  http response code to use if this exception flows over a REST call
     * @param className  name of class reporting error
     * @param actionDescription  description of function it was performing when error detected
     * @param errorMessage  description of error
     * @param systemAction  actions of the system as a result of the error
     * @param userAction  instructions for correcting the error
     * @param caughtError  the error that resulted in this exception.
     */
    @Deprecated
    public InvalidParameterException(int  httpCode, String className, String  actionDescription, String errorMessage, String systemAction, String userAction, Exception caughtError)
    {
        super(httpCode, className, actionDescription, errorMessage, systemAction, userAction, caughtError);
    }

    /**
     * This is the constructor used for creating a InvalidParameterException that resulted from a previous error.
     *
     * @param httpCode  http response code to use if this exception flows over a REST call
     * @param className  name of class reporting error
     * @param actionDescription  description of function it was performing when error detected
     * @param errorMessage  description of error
     * @param systemAction  actions of the system as a result of the error
     * @param userAction  instructions for correcting the error
     * @param parameterName name of the invalid parameter if known
     * @param caughtError  the error that resulted in this exception.
     */
    @Deprecated
    public InvalidParameterException(int  httpCode, String className, String  actionDescription, String errorMessage, String systemAction, String userAction, String parameterName, Exception caughtError)
    {
        super(httpCode, className, actionDescription, errorMessage, systemAction, userAction, caughtError);

        this.parameterName = parameterName;
    }


    /**
     * Return the invalid parameter's name, if known.
     *
     * @return string name
     */
    public String getParameterName()
    {
        return parameterName;
    }


    /**
     * JSON-style toString
     *
     * @return string of property names and values for this enum
     */
    @Override
    public String toString()
    {
        return "InvalidParameterException{" +
                       "parameterName='" + parameterName + '\'' +
                       ", reportedHTTPCode=" + getReportedHTTPCode() +
                       ", reportingClassName='" + getReportingClassName() + '\'' +
                       ", reportingActionDescription='" + getReportingActionDescription() + '\'' +
                       ", errorMessage='" + getReportedErrorMessage() + '\'' +
                       ", reportedSystemAction='" + getReportedSystemAction() + '\'' +
                       ", reportedUserAction='" + getReportedUserAction() + '\'' +
                       ", reportedCaughtException=" + getReportedCaughtException() +
                       ", relatedProperties=" + getRelatedProperties() +
                       '}';
    }
}
