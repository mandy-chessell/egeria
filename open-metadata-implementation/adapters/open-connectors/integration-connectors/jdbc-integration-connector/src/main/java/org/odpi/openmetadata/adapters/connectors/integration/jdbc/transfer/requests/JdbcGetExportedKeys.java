/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.adapters.connectors.integration.jdbc.transfer.requests;

import org.apache.commons.lang3.function.TriFunction;
import org.odpi.openmetadata.adapters.connectors.integration.jdbc.transfer.JdbcMetadata;
import org.odpi.openmetadata.adapters.connectors.integration.jdbc.transfer.model.JdbcForeignKey;
import org.odpi.openmetadata.frameworks.auditlog.AuditLog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.odpi.openmetadata.adapters.connectors.integration.jdbc.ffdc.JDBCIntegrationConnectorAuditCode.EXCEPTION_READING_JDBC;

/**
 * Manages the getExportedKeys call to jdbc
 */
class JdbcGetExportedKeys implements TriFunction<String, String, String, List<JdbcForeignKey>> {

    private final JdbcMetadata jdbcMetadata;
    private final AuditLog auditLog;

    JdbcGetExportedKeys(JdbcMetadata jdbcMetadata, AuditLog auditLog) {
        this.jdbcMetadata = jdbcMetadata;
        this.auditLog = auditLog;
    }

    /**
     * Get foreign keys as described by the foreign key columns referenced by primary key columns of target table
     *
     * @param schemaName schema name
     * @param tableName table name
     *
     * @return foreign keys
     */
    @Override
    public List<JdbcForeignKey> apply(String catalog, String schemaName, String tableName) {
        String methodName = "JdbcGetExportedKeys";
        try {
            return Optional.ofNullable(jdbcMetadata.getExportedKeys(catalog, schemaName, tableName))
                    .orElseGet(ArrayList::new);
        } catch (SQLException sqlException) {
            auditLog.logException("Reading exported keys from JDBC for catalog " + catalog + ",  schema" + schemaName + " and table " + tableName,
                    EXCEPTION_READING_JDBC.getMessageDefinition(methodName, sqlException.getMessage()), sqlException);
        }
        return new ArrayList<>();
    }
}