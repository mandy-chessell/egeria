/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */

package org.odpi.openmetadata.adapters.connectors.postgres.survey;

import org.odpi.openmetadata.adapters.connectors.postgres.controls.PostgresConfigurationProperty;
import org.odpi.openmetadata.adapters.connectors.postgres.ffdc.PostgresAuditCode;
import org.odpi.openmetadata.adapters.connectors.resource.jdbc.JDBCResourceConnector;
import org.odpi.openmetadata.frameworks.connectors.ConnectorBase;
import org.odpi.openmetadata.frameworks.connectors.ConnectorBroker;
import org.odpi.openmetadata.frameworks.connectors.ffdc.ConnectorCheckedException;
import org.odpi.openmetadata.frameworks.connectors.properties.ConnectionDetails;
import org.odpi.openmetadata.frameworks.openmetadata.types.OpenMetadataType;
import org.odpi.openmetadata.frameworks.surveyaction.AnnotationStore;
import org.odpi.openmetadata.frameworks.surveyaction.SurveyActionServiceConnector;
import org.odpi.openmetadata.frameworks.surveyaction.SurveyAssetStore;
import org.odpi.openmetadata.frameworks.surveyaction.controls.AnalysisStep;
import org.odpi.openmetadata.frameworks.surveyaction.properties.Annotation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JDBCResourceConnector provides basic implementation of {@link DataSource} interface in order to get a {@link Connection} to
 * target database. This is done via a static inner class, since {@link DataSource#getConnection()} clashes with
 * {@link ConnectorBase#getConnection()}.
 * <br><br>
 * The DataSource can be used directly.  There are also selected methods to issue common SQL statements to the database.
 */
public class PostgresServerSurveyActionService extends SurveyActionServiceConnector
{
    private final List<JDBCResourceConnector> jdbcResourceConnectors = new ArrayList<>();

    /**
     * Indicates that the survey action service is completely configured and can begin processing.
     *
     * @throws ConnectorCheckedException there is a problem within the discovery service.
     */
    @Override
    public void start() throws ConnectorCheckedException
    {
        final String  methodName = "start";

        super.start();

        try
        {
            AnnotationStore  annotationStore = surveyContext.getAnnotationStore();
            SurveyAssetStore assetStore      = surveyContext.getAssetStore();

            /*
             * The asset should have a special connector for databases.  If the connector is wrong the cast will fail.
             */
            connector = super.performCheckAssetAnalysisStep(JDBCResourceConnector.class, OpenMetadataType.SOFTWARE_SERVER.typeName);
            JDBCResourceConnector assetConnector = (JDBCResourceConnector)connector;
            assetConnector.start();

            annotationStore.setAnalysisStep(AnalysisStep.PROFILING_ASSOCIATED_RESOURCES.getName());

            DataSource jdbcDataSource = assetConnector.getDataSource();
            Connection jdbcConnection = jdbcDataSource.getConnection();

            final String sqlCommand1 = "SELECT oid, datname, datistemplate, datallowconn from pg_database;";

            PreparedStatement preparedStatement = jdbcConnection.prepareStatement(sqlCommand1);

            ResultSet resultSet = preparedStatement.executeQuery();

            List<String> validDatabases = new ArrayList<>();

            while (resultSet.next())
            {
                /*
                 * This first test removes databases that are templates or do not allow connections.
                 */
                if ((! resultSet.getBoolean("datistemplate")) &&
                        (resultSet.getBoolean("datallowconn")))
                {
                    String databaseName = resultSet.getString("datname");

                    validDatabases.add(databaseName);
                }
            }

            resultSet.close();
            preparedStatement.close();

            if (validDatabases.isEmpty())
            {
                auditLog.logMessage(methodName, PostgresAuditCode.NO_DATABASES.getMessageDefinition(surveyActionServiceName,
                                                                                                    assetStore.getAssetProperties().getQualifiedName(),
                                                                                                    assetStore.getAssetGUID()));
            }
            else
            {
                List<String> excludedDatabases = super.getArrayConfigurationProperty(PostgresConfigurationProperty.EXCLUDE_DATABASE_LIST.getName(),
                                                                                     connectionDetails.getConfigurationProperties(),
                                                                                     Collections.singletonList("postgres"));

                List<String> includedDatabases = super.getArrayConfigurationProperty(PostgresConfigurationProperty.INCLUDE_DATABASE_LIST.getName(),
                                                                                     connectionDetails.getConfigurationProperties());

                List<String> surveyDatabases = new ArrayList<>();

                for (String databaseName : validDatabases)
                {
                    if (surveyContext.elementShouldBeSurveyed(databaseName, excludedDatabases, includedDatabases))
                    {
                        surveyDatabases.add(databaseName);
                    }
                }

                PostgresDatabaseStatsExtractor statsExtractor = new PostgresDatabaseStatsExtractor(surveyDatabases,
                                                                                                   this);

                statsExtractor.getDatabaseStatistics(jdbcConnection);

                jdbcConnection.commit();

                annotationStore.setAnalysisStep(AnalysisStep.PRODUCE_INVENTORY.getName());

                for (String databaseName : surveyDatabases)
                {
                    java.sql.Connection databaseSpecificConnection = this.getDatabaseConnection(assetConnector, databaseName);

                    if (databaseSpecificConnection != null)
                    {
                        statsExtractor.getSchemaStatistics(databaseName, databaseSpecificConnection);
                    }
                }

                List<Annotation> annotations = statsExtractor.getAnnotations();
                if (annotations != null)
                {
                    for (Annotation annotation : annotations)
                    {
                        if (super.isActive())
                        {
                            annotationStore.addAnnotation(annotation, surveyContext.getAssetGUID());
                        }
                    }
                }
            }
        }
        catch (ConnectorCheckedException error)
        {
            throw error;
        }
        catch (Exception error)
        {
            super.handleUnexpectedException(methodName, error);
        }
    }


    /**
     * Return a JDBC connection that connects to a specific database.  This code is needed
     * because the databases are not catalogued at this time so we are creating connections to
     * each database based on the name of the database and the databaseURL used to connect to the
     * "postgres" database which describes the whole server.
     *
     * @param databaseName name of the database
     * @return jdbc connection
     */
    private java.sql.Connection getDatabaseConnection(JDBCResourceConnector serverConnector,
                                                      String databaseName)
    {
        final String methodName = "getDatabaseConnection";

        try
        {
            String serverNetworkAddress = serverConnector.getConnection().getEndpoint().getAddress();
            String databaseSpecificURL  = serverNetworkAddress.replace("/postgres", "/" + databaseName);

            ConnectionDetails databaseConnectionDetails = new ConnectionDetails(serverConnector.getConnection(),
                                                                                databaseSpecificURL);

            ConnectorBroker connectorBroker = new ConnectorBroker(auditLog);

            JDBCResourceConnector newConnector = (JDBCResourceConnector) connectorBroker.getConnector(databaseConnectionDetails);

            newConnector.start();

            jdbcResourceConnectors.add(newConnector);

            return newConnector.getDataSource().getConnection();
        }
        catch (Exception error)
        {
            logExceptionRecord(methodName,
                               PostgresAuditCode.UNEXPECTED_EXCEPTION.getMessageDefinition(surveyActionServiceName,
                                                                                           error.getClass().getName(),
                                                                                           methodName,
                                                                                           error.getMessage()),
                               error);
        }

        return null;
    }


    /**
     * Free up any resources held since the connector is no longer needed.
     *
     * @throws ConnectorCheckedException there is a problem within the connector.
     */
    @Override
    public synchronized void disconnect() throws ConnectorCheckedException
    {
        super.disconnect();

        for (JDBCResourceConnector resourceConnector: jdbcResourceConnectors)
        {
            if ((resourceConnector != null) && (resourceConnector.isActive()))
            {
                resourceConnector.disconnect();
            }
        }
    }
}
