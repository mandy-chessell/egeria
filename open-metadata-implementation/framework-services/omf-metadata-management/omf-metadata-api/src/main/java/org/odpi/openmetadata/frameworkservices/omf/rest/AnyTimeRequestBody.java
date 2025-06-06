/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.frameworkservices.omf.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.odpi.openmetadata.commonservices.ffdc.rest.EffectiveTimeRequestBody;

import java.util.Date;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * AnyTimeRequestBody describes the start and stop time of the historical query.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class AnyTimeRequestBody extends EffectiveTimeRequestBody
{
    private Date    asOfTime               = null;
    private boolean forLineage             = false;
    private boolean forDuplicateProcessing = false;

    /**
     * Default constructor
     */
    public AnyTimeRequestBody()
    {
        super();
    }


    /**
     * Copy/clone constructor
     *
     * @param template object to copy
     */
    public AnyTimeRequestBody(AnyTimeRequestBody template)
    {
        super(template);

        if (template != null)
        {
            asOfTime               = template.getAsOfTime();
            forLineage             = template.getForLineage();
            forDuplicateProcessing = template.getForDuplicateProcessing();
        }
    }


    /**
     * Return the repository date/time to use for the query.
     *
     * @return date object
     */
    public Date getAsOfTime()
    {
        return asOfTime;
    }


    /**
     * Set up the repository date/time to use for the query.
     *
     * @param asOfTime date object
     */
    public void setAsOfTime(Date asOfTime)
    {
        this.asOfTime = asOfTime;
    }


    /**
     * Return whether this request is to update lineage memento elements.
     *
     * @return flag
     */
    public boolean getForLineage()
    {
        return forLineage;
    }


    /**
     * Set up whether this request is to update lineage memento elements.
     *
     * @param forLineage flag
     */
    public void setForLineage(boolean forLineage)
    {
        this.forLineage = forLineage;
    }


    /**
     * Return whether this request is updating an element as part of a deduplication exercise.
     *
     * @return flag
     */
    public boolean getForDuplicateProcessing()
    {
        return forDuplicateProcessing;
    }


    /**
     * Set up whether this request is updating an element as part of a deduplication exercise.
     *
     * @param forDuplicateProcessing flag
     */
    public void setForDuplicateProcessing(boolean forDuplicateProcessing)
    {
        this.forDuplicateProcessing = forDuplicateProcessing;
    }


    /**
     * JSON-style toString
     *
     * @return return string containing the property names and values
     */
    @Override
    public String toString()
    {
        return "AnyTimeRequestBody{" +
                "asOfTime=" + asOfTime +
                ", forLineage=" + forLineage +
                ", forDuplicateProcessing=" + forDuplicateProcessing +
                "} " + super.toString();
    }



    /**
     * Return comparison result based on the content of the properties.
     *
     * @param objectToCompare test object
     * @return result of comparison
     */
    @Override
    public boolean equals(Object objectToCompare)
    {
        if (this == objectToCompare)
        {
            return true;
        }
        if (objectToCompare == null || getClass() != objectToCompare.getClass())
        {
            return false;
        }
        if (! super.equals(objectToCompare))
        {
            return false;
        }
        AnyTimeRequestBody that = (AnyTimeRequestBody) objectToCompare;
        return forLineage == that.forLineage &&
                forDuplicateProcessing == that.forDuplicateProcessing &&
                Objects.equals(asOfTime, that.asOfTime);
    }


    /**
     * Return hash code for this object
     *
     * @return int hash code
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), asOfTime, forLineage, forDuplicateProcessing);
    }
}
