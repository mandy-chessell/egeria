/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.frameworks.connectors.properties.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.ElementClassification;
import org.odpi.openmetadata.frameworks.openmetadata.metadataelements.ElementType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Validate that the ConnectorType bean can be cloned, compared, serialized, deserialized and printed as a String.
 */
public class TestConnectorType
{
    private ElementType                 type                 = new ElementType();
    private List<ElementClassification> classifications      = new ArrayList<>();
    private Map<String, String>         additionalProperties = new HashMap<>();
    private List<String>                recognizedAdditionalProperties    = new ArrayList<>();
    private List<String>                recognizedSecuredProperties       = new ArrayList<>();
    private List<String>                recognizedConfigurationProperties = new ArrayList<>();


    /**
     * Default constructor
     */
    public TestConnectorType()
    {
        recognizedAdditionalProperties.add("TestValue");
        recognizedSecuredProperties.add("TestValue");
        recognizedConfigurationProperties.add("TestValue");
    }


    /**
     * Set up an example object to test.
     *
     * @return filled in object
     */
    private ConnectorType getTestObject()
    {
        ConnectorType testObject = new ConnectorType();

        testObject.setType(type);
        testObject.setGUID("TestGUID");
        testObject.setClassifications(classifications);

        testObject.setQualifiedName("TestQualifiedName");
        testObject.setAdditionalProperties(additionalProperties);

        testObject.setDisplayName("TestDisplayName");
        testObject.setDescription("TestDescription");
        testObject.setConnectorProviderClassName("TestConnectorProviderClassName");
        testObject.setRecognizedAdditionalProperties(recognizedAdditionalProperties);
        testObject.setRecognizedSecuredProperties(recognizedSecuredProperties);
        testObject.setRecognizedSecuredProperties(recognizedConfigurationProperties);

        return testObject;
    }


    /**
     * Validate that the object that comes out of the test has the same content as the original test object.
     *
     * @param resultObject object returned by the test
     */
    private void validateResultObject(ConnectorType resultObject)
    {
        assertTrue(resultObject.getType().equals(type));
        assertTrue(resultObject.getGUID().equals("TestGUID"));
        assertTrue(resultObject.getClassifications() != null);

        assertTrue(resultObject.getQualifiedName().equals("TestQualifiedName"));
        assertTrue(resultObject.getAdditionalProperties() != null);

        assertTrue(resultObject.getDisplayName().equals("TestDisplayName"));
        assertTrue(resultObject.getDescription().equals("TestDescription"));
        assertTrue(resultObject.getConnectorProviderClassName().equals("TestConnectorProviderClassName"));
        assertTrue(resultObject.getRecognizedAdditionalProperties().equals(recognizedAdditionalProperties));
        assertTrue(resultObject.getRecognizedSecuredProperties().equals(recognizedSecuredProperties));
    }


    /**
     * Validate that a connectorType type object is returned.
     */
    @Test public void  testConnectorType()
    {
        assertTrue(ConnectorType.getConnectorTypeType() != null);
    }


    /**
     * Validate that the object is initialized properly
     */
    @Test public void testNullObject()
    {
        ConnectorType nullObject = new ConnectorType();

        assertTrue(nullObject.getType() == null);
        assertTrue(nullObject.getGUID() == null);
        assertTrue(nullObject.getClassifications() == null);

        assertTrue(nullObject.getQualifiedName() == null);
        assertTrue(nullObject.getAdditionalProperties() == null);

        assertTrue(nullObject.getDisplayName() == null);
        assertTrue(nullObject.getDescription() == null);
        assertTrue(nullObject.getConnectorProviderClassName() == null);
        assertTrue(nullObject.getRecognizedAdditionalProperties() == null);
        assertTrue(nullObject.getRecognizedSecuredProperties() == null);
        assertTrue(nullObject.getRecognizedConfigurationProperties() == null);

        nullObject = new ConnectorType(null);

        assertTrue(nullObject.getType() == null);
        assertTrue(nullObject.getGUID() == null);
        assertTrue(nullObject.getClassifications() == null);

        assertTrue(nullObject.getQualifiedName() == null);
        assertTrue(nullObject.getAdditionalProperties() == null);

        assertTrue(nullObject.getDisplayName() == null);
        assertTrue(nullObject.getDescription() == null);
        assertTrue(nullObject.getConnectorProviderClassName() == null);
        assertTrue(nullObject.getRecognizedAdditionalProperties() == null);
        assertTrue(nullObject.getRecognizedSecuredProperties() == null);
        assertTrue(nullObject.getRecognizedConfigurationProperties() == null);

        nullObject.setRecognizedAdditionalProperties(new ArrayList<>());
        nullObject.setRecognizedSecuredProperties(new ArrayList<>());
        nullObject.setRecognizedConfigurationProperties(new ArrayList<>());

        assertTrue(nullObject.getRecognizedAdditionalProperties() != null);
        assertTrue(nullObject.getRecognizedSecuredProperties() != null);
        assertTrue(nullObject.getRecognizedConfigurationProperties() != null);
    }


    /**
     * Validate that 2 different objects with the same content are evaluated as equal.
     * Also that different objects are considered not equal.
     */
    @Test public void testEquals()
    {
        assertFalse(getTestObject().equals(null));
        assertFalse(getTestObject().equals("DummyString"));
        assertTrue(getTestObject().equals(getTestObject()));

        ConnectorType sameObject = getTestObject();
        assertTrue(sameObject.equals(sameObject));

        ConnectorType differentObject = getTestObject();
        differentObject.setGUID("Different");
        assertFalse(getTestObject().equals(differentObject));
    }


    /**
     *  Validate that 2 different objects with the same content have the same hash code.
     */
    @Test public void testHashCode()
    {
        assertTrue(getTestObject().hashCode() == getTestObject().hashCode());
    }


    /**
     *  Validate that an object cloned from another object has the same content as the original
     */
    @Test public void testClone()
    {
        validateResultObject(new ConnectorType(getTestObject()));
    }


    /**
     * Validate that an object generated from a JSON String has the same content as the object used to
     * create the JSON String.
     */
    @Test public void testJSON()
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String       jsonString   = null;

        /*
         * This class
         */
        try
        {
            jsonString = objectMapper.writeValueAsString(getTestObject());
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        try
        {
            validateResultObject(objectMapper.readValue(jsonString, ConnectorType.class));
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        /*
         * Through superclass
         */
        Referenceable referenceable = getTestObject();

        try
        {
            jsonString = objectMapper.writeValueAsString(referenceable);
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        try
        {
            validateResultObject((ConnectorType) objectMapper.readValue(jsonString, Referenceable.class));
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        /*
         * Through superclass
         */
        ElementBase elementBase = getTestObject();

        try
        {
            jsonString = objectMapper.writeValueAsString(elementBase);
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        try
        {
            validateResultObject((ConnectorType) objectMapper.readValue(jsonString, ElementBase.class));
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        /*
         * Through superclass
         */
        ElementBase propertyBase = getTestObject();

        try
        {
            jsonString = objectMapper.writeValueAsString(propertyBase);
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }

        try
        {
            validateResultObject((ConnectorType) objectMapper.readValue(jsonString, ElementBase.class));
        }
        catch (Exception   exc)
        {
            assertTrue(false, "Exception: " + exc.getMessage());
        }
    }


    /**
     * Test that toString is overridden.
     */
    @Test public void testToString()
    {
        assertTrue(getTestObject().toString().contains("ConnectorType"));
    }
}
