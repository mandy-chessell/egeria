/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.frameworkservices.omf.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * TypeLimitedHistoricalFindRequest provides an extension to the search parameters to include the
 * point in time that the request should be based on.  This extension is used since
 * historical queries are optional support.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class TypeLimitedHistoricalFindRequest extends TypeLimitedFindRequest
{
    private static final long    serialVersionUID = 1L;

    private Date asOfTime = null;

    /**
     * Default constructor
     */
    public TypeLimitedHistoricalFindRequest()
    {
        super();
    }


    /**
     * Copy/clone constructor
     *
     * @param template object to copy
     */
    public TypeLimitedHistoricalFindRequest(TypeLimitedHistoricalFindRequest template)
    {
        super(template);

        if (template != null)
        {
            this.asOfTime = template.getAsOfTime();
        }
    }


    /**
     * Return the point in time for the search.
     *
     * @return date object
     */
    public Date getAsOfTime()
    {
        return asOfTime;
    }


    /**
     * Set up the point in time for the search.
     *
     * @param asOfTime date object
     */
    public void setAsOfTime(Date asOfTime)
    {
        this.asOfTime = asOfTime;
    }


    /**
     * Standard toString method.
     *
     * @return print out of variables in a JSON-style
     */
    @Override
    public String toString()
    {
        return "TypeLimitedHistoricalFindRequest{" +
                "asOfTime=" + asOfTime +
                ", typeGUID='" + getTypeGUID() + '\'' +
                ", sequencingProperty='" + getSequencingProperty() + '\'' +
                ", sequencingOrder=" + getSequencingOrder() +
                ", offset=" + getOffset() +
                ", pageSize=" + getPageSize() +
                ", limitResultsByStatus=" + getLimitResultsByStatus() +
                '}';
    }


    /**
     * Compare the values of the supplied object with those stored in the current object.
     *
     * @param objectToCompare supplied object
     * @return boolean result of comparison
     */
    @Override
    public boolean equals(Object objectToCompare)
    {
        if (this == objectToCompare)
        {
            return true;
        }
        if (!(objectToCompare instanceof TypeLimitedHistoricalFindRequest))
        {
            return false;
        }
        if (!super.equals(objectToCompare))
        {
            return false;
        }
        TypeLimitedHistoricalFindRequest
                that = (TypeLimitedHistoricalFindRequest) objectToCompare;
        return Objects.equals(getAsOfTime(), that.getAsOfTime());
    }


    /**
     * Create a hash code for this element type.
     *
     * @return int hash code
     */
    @Override
    public int hashCode()
    {

        return Objects.hash(super.hashCode(), getAsOfTime());
    }
}
