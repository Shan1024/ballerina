/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.database.sql.actions;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.database.sql.Constants;
import org.ballerinalang.database.sql.SQLDataIterator;
import org.ballerinalang.database.sql.SQLDatasource;
import org.ballerinalang.database.sql.SQLDatasourceUtils;
import org.ballerinalang.database.table.BCursorTable;
import org.ballerinalang.model.ColumnDefinition;
import org.ballerinalang.model.types.BArrayType;
import org.ballerinalang.model.types.BStructureType;
import org.ballerinalang.model.types.BTupleType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BTypes;
import org.ballerinalang.model.types.BUnionType;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BByte;
import org.ballerinalang.model.values.BFloat;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BNewArray;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BTable;
import org.ballerinalang.model.values.BTypeDescValue;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.model.values.BValueArray;
import org.ballerinalang.stdlib.time.util.TimeUtils;
import org.ballerinalang.util.TableResourceManager;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructureTypeInfo;
import org.ballerinalang.util.exceptions.BallerinaException;
import org.ballerinalang.util.observability.ObservabilityConstants;
import org.ballerinalang.util.observability.ObserveUtils;
import org.ballerinalang.util.observability.ObserverContext;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import static org.ballerinalang.database.sql.Constants.PARAMETER_DIRECTION_FIELD;
import static org.ballerinalang.database.sql.Constants.PARAMETER_RECORD_TYPE_FIELD;
import static org.ballerinalang.database.sql.Constants.PARAMETER_SQL_TYPE_FIELD;
import static org.ballerinalang.database.sql.Constants.PARAMETER_VALUE_FIELD;
import static org.ballerinalang.util.observability.ObservabilityConstants.TAG_DB_TYPE_SQL;
import static org.ballerinalang.util.observability.ObservabilityConstants.TAG_KEY_DB_INSTANCE;
import static org.ballerinalang.util.observability.ObservabilityConstants.TAG_KEY_DB_STATEMENT;
import static org.ballerinalang.util.observability.ObservabilityConstants.TAG_KEY_DB_TYPE;
import static org.ballerinalang.util.observability.ObservabilityConstants.TAG_KEY_PEER_ADDRESS;

/**
 * {@code AbstractSQLAction} is the base class for all SQL Action.
 *
 * @since 0.8.0
 */
public abstract class AbstractSQLAction extends BlockingNativeCallableUnit {

    private Calendar utcCalendar;
    private static final BTupleType executeUpdateWithKeysTupleType = new BTupleType(
            Arrays.asList(BTypes.typeInt, new BArrayType(BTypes.typeString)));
    private static final String MYSQL = "mysql";

    public AbstractSQLAction() {
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone(Constants.TIMEZONE_UTC));
    }

    protected void executeQuery(Context context, SQLDatasource datasource, String query, BValueArray parameters,
            BStructureType structType, boolean loadSQLTableToMemory) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            BValueArray generatedParams = constructParameters(context, parameters);
            conn = SQLDatasourceUtils.getDatabaseConnection(context, datasource, true);
            String processedQuery = createProcessedQueryString(query, generatedParams);
            stmt = getPreparedStatement(conn, datasource, processedQuery, loadSQLTableToMemory);
            createProcessedStatement(conn, stmt, generatedParams);
            rs = stmt.executeQuery();
            TableResourceManager rm = new TableResourceManager(conn, stmt, true);
            List<ColumnDefinition> columnDefinitions = SQLDatasourceUtils.getColumnDefinitions(rs);
            if (loadSQLTableToMemory) {
                CachedRowSet cachedRowSet = RowSetProvider.newFactory().createCachedRowSet();
                cachedRowSet.populate(rs);
                rs = cachedRowSet;
                rm.gracefullyReleaseResources();
            } else {
                rm.addResultSet(rs);
            }
            context.setReturnValues(constructTable(rm, context, rs, structType, columnDefinitions,
                    datasource.getDatabaseProductName()));
        } catch (Throwable e) {
            SQLDatasourceUtils.cleanupResources(rs, stmt, conn, true);
            throw new BallerinaException("execute query failed: " + e.getMessage(), e);
        }
    }

    protected void executeUpdate(Context context, SQLDatasource datasource, String query, BValueArray parameters) {
        Connection conn = null;
        PreparedStatement stmt = null;
        boolean isInTransaction = context.isInTransaction();
        try {
            BValueArray generatedParams = constructParameters(context, parameters);
            conn = SQLDatasourceUtils.getDatabaseConnection(context, datasource, false);
            String processedQuery = createProcessedQueryString(query, generatedParams);
            stmt = conn.prepareStatement(processedQuery);
            createProcessedStatement(conn, stmt, generatedParams, datasource.getDatabaseProductName());
            int count = stmt.executeUpdate();
            context.setReturnValues(new BInteger(count));
        } catch (SQLException e) {
            throw new BallerinaException("execute update failed: " + e.getMessage(), e);
        } finally {
            SQLDatasourceUtils.cleanupResources(stmt, conn, !isInTransaction);
        }
    }

    protected void executeUpdateWithKeys(Context context, SQLDatasource datasource, String query,
                                         BValueArray keyColumns, BValueArray parameters) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isInTransaction = context.isInTransaction();
        try {
            BValueArray generatedParams = constructParameters(context, parameters);
            conn = SQLDatasourceUtils.getDatabaseConnection(context, datasource, false);
            String processedQuery = createProcessedQueryString(query, generatedParams);
            int keyColumnCount = 0;
            if (keyColumns != null) {
                keyColumnCount = (int) keyColumns.size();
            }
            if (keyColumnCount > 0) {
                String[] columnArray = new String[keyColumnCount];
                for (int i = 0; i < keyColumnCount; i++) {
                    columnArray[i] = keyColumns.getString(i);
                }
                stmt = conn.prepareStatement(processedQuery, columnArray);
            } else {
                stmt = conn.prepareStatement(processedQuery, Statement.RETURN_GENERATED_KEYS);
            }
            createProcessedStatement(conn, stmt, generatedParams);
            int count = stmt.executeUpdate();
            BInteger updatedCount = new BInteger(count);
            rs = stmt.getGeneratedKeys();
            /*The result set contains the auto generated keys. There can be multiple auto generated columns
            in a table.*/
            BValueArray generatedKeys;
            if (rs.next()) {
                generatedKeys = getGeneratedKeys(rs);
            } else {
                generatedKeys = new BValueArray(BTypes.typeString);
            }
            BValueArray tuple = new BValueArray(executeUpdateWithKeysTupleType);
            tuple.add(0, updatedCount);
            tuple.add(1, generatedKeys);
            context.setReturnValues(tuple);
        } catch (SQLException e) {
            throw new BallerinaException("execute update with generated keys failed: " + e.getMessage(), e);
        } finally {
            SQLDatasourceUtils.cleanupResources(rs, stmt, conn, !isInTransaction);
        }
    }

    protected void executeProcedure(Context context, SQLDatasource datasource, String query, BValueArray parameters,
            BValueArray structTypes) {
        Connection conn = null;
        CallableStatement stmt = null;
        List<ResultSet> resultSets = null;
        boolean isInTransaction = context.isInTransaction();
        try {
            BValueArray generatedParams = constructParameters(context, parameters);
            conn = SQLDatasourceUtils.getDatabaseConnection(context, datasource, false);
            stmt = getPreparedCall(conn, datasource, query, generatedParams);
            createProcessedStatement(conn, stmt, generatedParams, datasource.getDatabaseProductName());
            boolean refCursorOutParamsPresent = generatedParams != null && isRefCursorOutParamPresent(generatedParams);
            boolean resultSetsReturned = false;
            TableResourceManager rm = null;
            boolean requiredToReturnTables = structTypes != null && structTypes.size() > 0;
            if (requiredToReturnTables) {
                resultSets = executeStoredProc(stmt, datasource.getDatabaseProductName());
                resultSetsReturned = !resultSets.isEmpty();
            } else {
                stmt.execute();
            }
            if (resultSetsReturned || refCursorOutParamsPresent) {
                rm = new TableResourceManager(conn, stmt, !isInTransaction);
            }
            setOutParameters(context, stmt, parameters, rm);
            if (resultSetsReturned) {
                rm.addAllResultSets(resultSets);
                // If a result set has been returned from the stored procedure it needs to be pushed in to return
                // values
                context.setReturnValues(constructTablesForResultSets(resultSets, rm, context, structTypes,
                        datasource.getDatabaseProductName()));
            } else if (!refCursorOutParamsPresent) {
                // Even if there aren't any result sets returned from the procedure there could be ref cursors
                // returned as OUT params. If there are present we cannot clean up the connection. If there is no
                // returned result set or ref cursor OUT params we should cleanup the connection.
                SQLDatasourceUtils.cleanupResources(resultSets, stmt, conn, !isInTransaction);
                context.setReturnValues();
            }
        } catch (Throwable e) {
            SQLDatasourceUtils.cleanupResources(resultSets, stmt, conn, !isInTransaction);
            throw new BallerinaException("execute stored procedure failed: " + e.getMessage(), e);
        }
    }

    private BValueArray constructTablesForResultSets(List<ResultSet> resultSets, TableResourceManager rm,
                                                     Context context, BValueArray structTypes,
                                                     String databaseProductName)
            throws SQLException {
        BType returnedTableType =
                ((BUnionType) context.getCallableUnitInfo().getRetParamTypes()[0]).getMemberTypes().get(0);
        BValueArray bTables = new BValueArray(returnedTableType);
        // TODO: "mysql" equality condition is part of the temporary fix to support returning the result set in the case
        // of stored procedures returning only one result set in MySQL. Refer ballerina-platform/ballerina-lang#8643
        if (databaseProductName.contains(MYSQL) && (structTypes != null && structTypes.size() > 1)) {
            throw new BallerinaException(
                    "Retrieving result sets from stored procedures returning more than one result set, is not "
                            + "supported");
        } else if (structTypes == null || resultSets.size() != structTypes.size()) {
            throw new BallerinaException(
                    "Mismatching record type count: " + (structTypes == null ? 0 : structTypes.size()) + " and "
                            + "returned result set count: " + resultSets.size() + " from the stored procedure");
        }
        for (int i = 0; i < resultSets.size(); i++) {
            bTables.add(i, constructTable(rm, context, resultSets.get(i),
                    (BStructureType) structTypes.getRefValue(i).value(), databaseProductName));
        }
        return bTables;
    }

    protected void executeBatchUpdate(Context context, SQLDatasource datasource, String query,
            BValueArray parameters) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        int[] updatedCount;
        int paramArrayCount = 0;
        boolean isInTransaction = context.isInTransaction();
        try {
            conn = SQLDatasourceUtils.getDatabaseConnection(context, datasource, false);
            stmt = conn.prepareStatement(query);
            conn.setAutoCommit(false);
            if (parameters != null) {
                paramArrayCount = (int) parameters.size();
                if (paramArrayCount == 0) {
                    stmt.addBatch();
                }
                for (int index = 0; index < paramArrayCount; index++) {
                    BValueArray params = (BValueArray) parameters.getRefValue(index);
                    BValueArray generatedParams = constructParameters(context, params);
                    createProcessedStatement(conn, stmt, generatedParams);
                    stmt.addBatch();
                }
            } else {
                stmt.addBatch();
            }
            updatedCount = stmt.executeBatch();
            if (!isInTransaction) {
                conn.commit();
            }
        } catch (BatchUpdateException e) {
            if (!isInTransaction) {
                conn.rollback();
            }
            updatedCount = e.getUpdateCounts();
        } catch (SQLException e) {
            conn.rollback();
            throw new BallerinaException("execute batch update failed: " + e.getMessage(), e);
        } finally {
            SQLDatasourceUtils.cleanupResources(stmt, conn, !isInTransaction);
        }
        //After a command in a batch update fails to execute properly and a BatchUpdateException is thrown, the driver
        // may or may not continue to process the remaining commands in the batch. If the driver does not continue
        // processing after a failure, the array returned by the method will have -3 (EXECUTE_FAILED) for those updates.
        long[] returnedCount = new long[paramArrayCount];
        Arrays.fill(returnedCount, Statement.EXECUTE_FAILED);
        BValueArray countArray = new BValueArray(returnedCount);
        if (updatedCount != null) {
            int iSize = updatedCount.length;
            for (int i = 0; i < iSize; ++i) {
                countArray.add(i, updatedCount[i]);
            }
        }
        context.setReturnValues(countArray);
    }

    protected BStructureType getStructType(Context context, int index) {
        BStructureType structType = null;
        BTypeDescValue type = (BTypeDescValue) context.getNullableRefArgument(index);
        if (type != null) {
            structType = (BStructureType) type.value();
        }
        return structType;
    }

    protected void checkAndObserveSQLAction(Context context, SQLDatasource datasource, String query) {
        Optional<ObserverContext> observerContext = ObserveUtils.getObserverContextOfCurrentFrame(context);
        observerContext.ifPresent(ctx -> {
            ctx.addTag(TAG_KEY_PEER_ADDRESS, datasource.getPeerAddress());
            ctx.addTag(TAG_KEY_DB_INSTANCE, datasource.getDatabaseName());
            ctx.addTag(TAG_KEY_DB_STATEMENT, query);
            ctx.addTag(TAG_KEY_DB_TYPE, TAG_DB_TYPE_SQL);
        });
    }

    protected void checkAndObserveSQLError(Context context, String message) {
        Optional<ObserverContext> observerContext = ObserveUtils.getObserverContextOfCurrentFrame(context);
        observerContext.ifPresent(ctx -> {
            ctx.addProperty(ObservabilityConstants.PROPERTY_ERROR, Boolean.TRUE);
            ctx.addProperty(ObservabilityConstants.PROPERTY_ERROR_MESSAGE, message);
        });
    }

    protected SQLDatasource retrieveDatasource(Context context) {
        BMap<String, BValue> bConnector = (BMap<String, BValue>) context.getRefArgument(0);
        return (SQLDatasource) bConnector.getNativeData(Constants.SQL_CLIENT);
    }

    private BValueArray constructParameters(Context context, BValueArray parameters) {
        BValueArray parametersNew = new BValueArray();
        int paramCount = (int) parameters.size();
        for (int i = 0; i < paramCount; ++i) {
            BRefType typeValue = parameters.getRefValue(i);
            BMap<String, BValue> paramStruct;
            if (typeValue.getType().getTag() == TypeTags.OBJECT_TYPE_TAG
                    || typeValue.getType().getTag() == TypeTags.RECORD_TYPE_TAG) {
                paramStruct = (BMap<String, BValue>) typeValue;
            } else {
                paramStruct = getSQLParameter(context);
                paramStruct.put(PARAMETER_SQL_TYPE_FIELD,
                        new BString(SQLDatasourceUtils.getSQLType(typeValue.getType())));
                paramStruct.put(PARAMETER_VALUE_FIELD, typeValue);
                paramStruct.put(PARAMETER_DIRECTION_FIELD, new BString(Constants.QueryParamDirection.DIR_IN));
            }
            parametersNew.add(i, paramStruct);
        }
        return parametersNew;
    }

    private static BMap<String, BValue> getSQLParameter(Context context) {
        PackageInfo sqlPackageInfo = context.getProgramFile().getPackageInfo(Constants.SQL_PACKAGE_PATH);
        StructureTypeInfo paramStructInfo = sqlPackageInfo.getStructInfo(Constants.SQL_PARAMETER);
        return new BMap<>(paramStructInfo.getType());
    }

    /**
     * If there are any arrays of parameter for types other than sql array, the given query is expanded by adding "?" s
     * to match with the array size.
     */
    private String createProcessedQueryString(String query, BValueArray parameters) {
        String currentQuery = query;
        if (parameters != null) {
            int start = 0;
            Object[] vals;
            int count;
            int paramCount = (int) parameters.size();
            for (int i = 0; i < paramCount; i++) {
                // types.bal Parameter
                BMap<String, BValue> paramValue = (BMap<String, BValue>) parameters.getRefValue(i);
                if (paramValue != null) {
                    String sqlType = getSQLType(paramValue);
                    BValue value = paramValue.get(PARAMETER_VALUE_FIELD);
                    if (value != null && (value.getType().getTag() == TypeTags.ARRAY_TAG
                            && ((BArrayType) value.getType()).getElementType().getTag() != TypeTags.BYTE_TAG)
                            && !Constants.SQLDataTypes.ARRAY.equalsIgnoreCase(sqlType)) {
                        count = (int) ((BNewArray) value).size();
                    } else {
                        count = 1;
                    }
                    vals = this.expandQuery(start, count, currentQuery);
                    start = (Integer) vals[0];
                    currentQuery = (String) vals[1];
                }
            }
        }
        return currentQuery;
    }

    /**
     * Search for the first occurrence of "?" from the given starting point and replace it with given number of "?"'s.
     */
    private Object[] expandQuery(int start, int count, String query) {
        StringBuilder result = new StringBuilder();
        int n = query.length();
        boolean doubleQuoteExists = false;
        boolean singleQuoteExists = false;
        int end = n;
        for (int i = start; i < n; i++) {
            if (query.charAt(i) == '\'') {
                singleQuoteExists = !singleQuoteExists;
            } else if (query.charAt(i) == '\"') {
                doubleQuoteExists = !doubleQuoteExists;
            } else if (query.charAt(i) == '?' && !(doubleQuoteExists || singleQuoteExists)) {
                result.append(query.substring(0, i));
                result.append(this.generateQuestionMarks(count));
                end = result.length() + 1;
                if (i + 1 < n) {
                    result.append(query.substring(i + 1));
                }
                break;
            }
        }
        return new Object[] { end, result.toString() };
    }

    private String generateQuestionMarks(int n) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < n; i++) {
            builder.append(Constants.QUESTION_MARK);
            if (i + 1 < n) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private PreparedStatement getPreparedStatement(Connection conn, SQLDatasource datasource, String query,
            boolean loadToMemory) throws SQLException {
        PreparedStatement stmt;
        boolean mysql = datasource.getDatabaseProductName().contains("mysql");
        /* In MySQL by default, ResultSets are completely retrieved and stored in memory.
           Following properties are set to stream the results back one row at a time.*/
        if (mysql && !loadToMemory) {
            stmt = conn.prepareStatement(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // To fulfill OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE findbugs validation.
            try {
                stmt.setFetchSize(Integer.MIN_VALUE);
            } catch (SQLException e) {
                stmt.close();
            }
        } else {
            stmt = conn.prepareStatement(query);
        }
        return stmt;
    }

    private CallableStatement getPreparedCall(Connection conn, SQLDatasource datasource, String query,
            BValueArray parameters) throws SQLException {
        CallableStatement stmt;
        boolean mysql = datasource.getDatabaseProductName().contains("mysql");
        if (mysql) {
            stmt = conn.prepareCall(query, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            /* Only stream if there aren't any OUT parameters since can't use streaming result sets with callable
               statements that have output parameters */
            if (parameters != null && !hasOutParams(parameters)) {
                stmt.setFetchSize(Integer.MIN_VALUE);
            }
        } else {
            stmt = conn.prepareCall(query);
        }
        return stmt;
    }

    private BValueArray getGeneratedKeys(ResultSet rs) throws SQLException {
        BValueArray generatedKeys = new BValueArray(BTypes.typeString);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        int columnType;
        String value;
        BigDecimal bigDecimal;
        for (int i = 1; i <= columnCount; i++) {
            columnType = metaData.getColumnType(i);
            switch (columnType) {
            case Types.INTEGER:
            case Types.TINYINT:
            case Types.SMALLINT:
                value = Integer.toString(rs.getInt(i));
                break;
            case Types.DOUBLE:
                value = Double.toString(rs.getDouble(i));
                break;
            case Types.FLOAT:
                value = Float.toString(rs.getFloat(i));
                break;
            case Types.BOOLEAN:
            case Types.BIT:
                value = Boolean.toString(rs.getBoolean(i));
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                bigDecimal = rs.getBigDecimal(i);
                if (bigDecimal != null) {
                    value = bigDecimal.toPlainString();
                } else {
                    value = null;
                }
                break;
            case Types.BIGINT:
                value = Long.toString(rs.getLong(i));
                break;
            default:
                value = rs.getString(i);
                break;
            }
            generatedKeys.add(i - 1, value);
        }
        return generatedKeys;
    }

    private void createProcessedStatement(Connection conn, PreparedStatement stmt, BValueArray param) {
        createProcessedStatement(conn, stmt, param, null);
    }

    private void createProcessedStatement(Connection conn, PreparedStatement stmt, BValueArray params,
            String databaseProductName) {
        if (params == null) {
            return;
        }
        int paramCount = (int) params.size();
        int currentOrdinal = 0;
        for (int index = 0; index < paramCount; index++) {
            BMap<String, BValue> paramStruct = (BMap<String, BValue>) params.getRefValue(index);
            if (paramStruct != null) {
                String sqlType = getSQLType(paramStruct);
                BValue value = paramStruct.get(PARAMETER_VALUE_FIELD);
                int direction = getParameterDirection(paramStruct);
                //If the parameter is an array and sql type is not "array" then treat it as an array of parameters
                if (value != null && (value.getType().getTag() == TypeTags.ARRAY_TAG
                        && ((BArrayType) value.getType()).getElementType().getTag() != TypeTags.BYTE_TAG)
                        && !Constants.SQLDataTypes.ARRAY.equalsIgnoreCase(sqlType)) {
                    int arrayLength = (int) ((BNewArray) value).size();
                    int typeTagOfArrayElement = ((BArrayType) value.getType()).getElementType().getTag();
                    for (int i = 0; i < arrayLength; i++) {
                        BValue paramValue;
                        switch (typeTagOfArrayElement) {
                        case TypeTags.INT_TAG:
                            paramValue = new BInteger(((BValueArray) value).getInt(i));
                            break;
                        case TypeTags.BYTE_TAG:
                            paramValue = new BByte(((BValueArray) value).getByte(i));
                            break;
                        case TypeTags.FLOAT_TAG:
                            paramValue = new BFloat(((BValueArray) value).getFloat(i));
                            break;
                        case TypeTags.STRING_TAG:
                            paramValue = new BString(((BValueArray) value).getString(i));
                            break;
                        case TypeTags.BOOLEAN_TAG:
                            paramValue = new BBoolean(((BValueArray) value).getBoolean(i) > 0);
                            break;
                        case TypeTags.DECIMAL_TAG:
                            paramValue = ((BValueArray) value).getRefValue(i);
                            break;
                        // The value parameter of the struct is an array of arrays. Only possibility that should be
                        // supported is, this being an array of byte arrays (blob)
                        // eg: [blob1, blob2, blob3] == [byteArray1, byteArray2, byteArray3]
                        case TypeTags.ARRAY_TAG:
                            BValue array = ((BValueArray) value).getRefValue(i);
                            // array cannot be null because the type tag is not union
                            if (((BArrayType) array.getType()).getElementType().getTag() == TypeTags.BYTE_TAG) {
                                paramValue = array;
                                break;
                            } else {
                                throw new BallerinaException("unsupported array type for parameter index: " + index
                                        + ". Array element type being an array is supported only when the inner array"
                                        + " element type is BYTE");
                            }
                        default:
                            throw new BallerinaException("unsupported array type for parameter index " + index);
                        }
                        if (Constants.SQLDataTypes.REFCURSOR.equals(sqlType) || Constants.SQLDataTypes.BLOB
                                .equals(sqlType)) {
                            setParameter(conn, stmt, sqlType, paramValue, direction, currentOrdinal,
                                    databaseProductName);
                        } else {
                            setParameter(conn, stmt, sqlType, paramValue, direction, currentOrdinal);
                        }
                        currentOrdinal++;
                    }
                } else {
                    if (Constants.SQLDataTypes.REFCURSOR.equals(sqlType) || Constants.SQLDataTypes.ARRAY
                            .equals(sqlType) || Constants.SQLDataTypes.BLOB.equals(sqlType)) {
                        setParameter(conn, stmt, sqlType, value, direction, currentOrdinal, databaseProductName);
                    } else {
                        setParameter(conn, stmt, sqlType, value, direction, currentOrdinal);
                    }
                    currentOrdinal++;
                }
            } else {
                SQLDatasourceUtils.setNullObject(stmt, index);
                currentOrdinal++;
            }
        }
    }

    private void setParameter(Connection conn, PreparedStatement stmt, String sqlType, BValue value, int direction,
            int index) {
        setParameter(conn, stmt, sqlType, value, direction, index, null);
    }

    private void setParameter(Connection conn, PreparedStatement stmt, String sqlType, BValue value, int direction,
            int index, String databaseProductName) {
        if (sqlType == null || sqlType.isEmpty()) {
            SQLDatasourceUtils.setStringValue(stmt, value, index, direction, Types.VARCHAR);
        } else {
            String sqlDataType = sqlType.toUpperCase(Locale.getDefault());
            switch (sqlDataType) {
            case Constants.SQLDataTypes.SMALLINT:
                SQLDatasourceUtils.setSmallIntValue(stmt, value, index, direction, Types.SMALLINT);
                break;
            case Constants.SQLDataTypes.VARCHAR:
                SQLDatasourceUtils.setStringValue(stmt, value, index, direction, Types.VARCHAR);
                break;
            case Constants.SQLDataTypes.CHAR:
                SQLDatasourceUtils.setStringValue(stmt, value, index, direction, Types.CHAR);
                break;
            case Constants.SQLDataTypes.LONGVARCHAR:
                SQLDatasourceUtils.setStringValue(stmt, value, index, direction, Types.LONGVARCHAR);
                break;
            case Constants.SQLDataTypes.NCHAR:
                SQLDatasourceUtils.setNStringValue(stmt, value, index, direction, Types.NCHAR);
                break;
            case Constants.SQLDataTypes.NVARCHAR:
                SQLDatasourceUtils.setNStringValue(stmt, value, index, direction, Types.NVARCHAR);
                break;
            case Constants.SQLDataTypes.LONGNVARCHAR:
                SQLDatasourceUtils.setNStringValue(stmt, value, index, direction, Types.LONGNVARCHAR);
                break;
            case Constants.SQLDataTypes.DOUBLE:
                SQLDatasourceUtils.setDoubleValue(stmt, value, index, direction, Types.DOUBLE);
                break;
            case Constants.SQLDataTypes.NUMERIC:
                SQLDatasourceUtils.setNumericValue(stmt, value, index, direction, Types.NUMERIC);
                break;
            case Constants.SQLDataTypes.DECIMAL:
                SQLDatasourceUtils.setNumericValue(stmt, value, index, direction, Types.DECIMAL);
                break;
            case Constants.SQLDataTypes.BIT:
            case Constants.SQLDataTypes.BOOLEAN:
                SQLDatasourceUtils.setBooleanValue(stmt, value, index, direction, Types.BIT);
                break;
            case Constants.SQLDataTypes.TINYINT:
                SQLDatasourceUtils.setTinyIntValue(stmt, value, index, direction, Types.TINYINT);
                break;
            case Constants.SQLDataTypes.BIGINT:
                SQLDatasourceUtils.setBigIntValue(stmt, value, index, direction, Types.BIGINT);
                break;
            case Constants.SQLDataTypes.INTEGER:
                SQLDatasourceUtils.setIntValue(stmt, value, index, direction, Types.INTEGER);
                break;
            case Constants.SQLDataTypes.REAL:
                SQLDatasourceUtils.setRealValue(stmt, value, index, direction, Types.REAL);
                break;
            case Constants.SQLDataTypes.FLOAT:
                SQLDatasourceUtils.setRealValue(stmt, value, index, direction, Types.FLOAT);
                break;
            case Constants.SQLDataTypes.DATE:
                SQLDatasourceUtils.setDateValue(stmt, value, index, direction, Types.DATE);
                break;
            case Constants.SQLDataTypes.TIMESTAMP:
            case Constants.SQLDataTypes.DATETIME:
                SQLDatasourceUtils.setTimeStampValue(stmt, value, index, direction, Types.TIMESTAMP, utcCalendar);
                break;
            case Constants.SQLDataTypes.TIME:
                SQLDatasourceUtils.setTimeValue(stmt, value, index, direction, Types.TIME, utcCalendar);
                break;
            case Constants.SQLDataTypes.BINARY:
                SQLDatasourceUtils.setBinaryValue(stmt, value, index, direction, Types.BINARY);
                break;
            case Constants.SQLDataTypes.BLOB:
                SQLDatasourceUtils.setBlobValue(stmt, value, index, direction, Types.BLOB);
                break;
            case Constants.SQLDataTypes.LONGVARBINARY:
                SQLDatasourceUtils.setBlobValue(stmt, value, index, direction, Types.LONGVARBINARY);
                break;
            case Constants.SQLDataTypes.VARBINARY:
                SQLDatasourceUtils.setBinaryValue(stmt, value, index, direction, Types.VARBINARY);
                break;
            case Constants.SQLDataTypes.CLOB:
                SQLDatasourceUtils.setClobValue(stmt, value, index, direction, Types.CLOB);
                break;
            case Constants.SQLDataTypes.NCLOB:
                SQLDatasourceUtils.setNClobValue(stmt, value, index, direction, Types.NCLOB);
                break;
            case Constants.SQLDataTypes.ARRAY:
                SQLDatasourceUtils.setArrayValue(conn, stmt, value, index, direction, Types.ARRAY, databaseProductName);
                break;
            case Constants.SQLDataTypes.STRUCT:
                SQLDatasourceUtils.setUserDefinedValue(conn, stmt, value, index, direction, Types.STRUCT);
                break;
            case Constants.SQLDataTypes.REFCURSOR:
                SQLDatasourceUtils.setRefCursorValue(stmt, index, direction, databaseProductName);
                break;
            default:
                throw new BallerinaException("unsupported datatype as parameter: " + sqlType + " index:" + index);
            }
        }
    }

    private boolean isRefCursorOutParamPresent(BValueArray params) {
        boolean refCursorOutParamPresent = false;
        int paramCount = (int) params.size();
        for (int index = 0; index < paramCount; index++) {
            BMap<String, BValue> paramValue = (BMap<String, BValue>) params.getRefValue(index);
            if (paramValue != null) {
                String sqlType = getSQLType(paramValue);
                int direction = getParameterDirection(paramValue);
                if (direction == Constants.QueryParamDirection.OUT && Constants.SQLDataTypes.REFCURSOR
                        .equals(sqlType)) {
                    refCursorOutParamPresent = true;
                    break;
                }
            }
        }
        return refCursorOutParamPresent;
    }

    private void setOutParameters(Context context, CallableStatement stmt, BValueArray params,
            TableResourceManager rm) {
        if (params == null) {
            return;
        }
        int paramCount = (int) params.size();
        for (int index = 0; index < paramCount; index++) {
            if (params.getRefValue(index).getType().getTag() != TypeTags.OBJECT_TYPE_TAG
                    && params.getRefValue(index).getType().getTag() != TypeTags.RECORD_TYPE_TAG) {
                continue;
            }
            BMap<String, BValue> paramValue = (BMap<String, BValue>) params.getRefValue(index);
            if (paramValue != null) {
                String sqlType = getSQLType(paramValue);
                int direction = getParameterDirection(paramValue);
                if (direction == Constants.QueryParamDirection.INOUT
                        || direction == Constants.QueryParamDirection.OUT) {
                    setOutParameterValue(context, stmt, sqlType, index, paramValue, rm);
                }
            } else {
                throw new BallerinaException("out value cannot set for null parameter with index: " + index);
            }
        }
    }

    private void setOutParameterValue(Context context, CallableStatement stmt, String sqlType, int index,
                                      BMap<String, BValue> paramValue, TableResourceManager resourceManager) {
        try {
            String sqlDataType = sqlType.toUpperCase(Locale.getDefault());
            switch (sqlDataType) {
            case Constants.SQLDataTypes.INTEGER: {
                int value = stmt.getInt(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BInteger(value)); //Value is the first position of the struct
            }
            break;
            case Constants.SQLDataTypes.VARCHAR: {
                String value = stmt.getString(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(value));
            }
            break;
            case Constants.SQLDataTypes.NUMERIC:
            case Constants.SQLDataTypes.DECIMAL: {
                BigDecimal value = stmt.getBigDecimal(index + 1);
                if (value == null) {
                    paramValue.put(PARAMETER_VALUE_FIELD, new BFloat(0));
                } else {
                    paramValue.put(PARAMETER_VALUE_FIELD, new BFloat(value.doubleValue()));
                }
            }
            break;
            case Constants.SQLDataTypes.BIT:
            case Constants.SQLDataTypes.BOOLEAN: {
                boolean value = stmt.getBoolean(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BBoolean(value));
            }
            break;
            case Constants.SQLDataTypes.TINYINT: {
                byte value = stmt.getByte(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BInteger(value));
            }
            break;
            case Constants.SQLDataTypes.SMALLINT: {
                short value = stmt.getShort(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BInteger(value));
            }
            break;
            case Constants.SQLDataTypes.BIGINT: {
                long value = stmt.getLong(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BInteger(value));
            }
            break;
            case Constants.SQLDataTypes.REAL:
            case Constants.SQLDataTypes.FLOAT: {
                float value = stmt.getFloat(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BFloat(value));
            }
            break;
            case Constants.SQLDataTypes.DOUBLE: {
                double value = stmt.getDouble(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BFloat(value));
            }
            break;
            case Constants.SQLDataTypes.CLOB: {
                Clob value = stmt.getClob(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.BLOB: {
                Blob value = stmt.getBlob(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.BINARY: {
                byte[] value = stmt.getBytes(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.DATE: {
                Date value = stmt.getDate(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.TIMESTAMP:
            case Constants.SQLDataTypes.DATETIME: {
                Timestamp value = stmt.getTimestamp(index + 1, utcCalendar);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.TIME: {
                Time value = stmt.getTime(index + 1, utcCalendar);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.ARRAY: {
                Array value = stmt.getArray(index + 1);
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(SQLDatasourceUtils.getString(value)));
            }
            break;
            case Constants.SQLDataTypes.STRUCT: {
                Object value = stmt.getObject(index + 1);
                String stringValue = "";
                if (value != null) {
                    if (value instanceof Struct) {
                        stringValue = SQLDatasourceUtils.getString((Struct) value);
                    } else {
                        stringValue = value.toString();
                    }
                }
                paramValue.put(PARAMETER_VALUE_FIELD, new BString(stringValue));
            }
            break;
            case Constants.SQLDataTypes.REFCURSOR: {
                ResultSet rs = (ResultSet) stmt.getObject(index + 1);
                BStructureType structType = getStructType(paramValue);
                if (structType != null) {
                    resourceManager.addResultSet(rs);
                    SQLDatasource datasource = retrieveDatasource(context);
                    paramValue.put(PARAMETER_VALUE_FIELD,
                            constructTable(resourceManager, context, rs, getStructType(paramValue),
                                    datasource.getDatabaseProductName()));
                } else {
                    throw new BallerinaException(
                            "The Struct Type for the result set pointed by the Ref Cursor cannot be null");
                }
                break;
            }
            default:
                throw new BallerinaException(
                        "unsupported datatype as out/inout parameter: " + sqlType + " index:" + index);
            }
        } catch (SQLException e) {
            throw new BallerinaException("error in getting out parameter value: " + e.getMessage(), e);
        }
    }

    private boolean hasOutParams(BValueArray params) {
        int paramCount = (int) params.size();
        for (int index = 0; index < paramCount; index++) {
            BMap<String, BValue> paramValue = (BMap<String, BValue>) params.getRefValue(index);
            int direction = getParameterDirection(paramValue);
            if (direction == Constants.QueryParamDirection.OUT || direction == Constants.QueryParamDirection.INOUT) {
                return true;
            }
        }
        return false;
    }

    private List<ResultSet> executeStoredProc(CallableStatement stmt, String databaseProductName) throws SQLException {
        boolean resultAndNoUpdateCount = stmt.execute();
        List<ResultSet> resultSets = new ArrayList<>();
        ResultSet result;
        while (true) {
            if (!resultAndNoUpdateCount) {
                // Current result is an update count(not a ResultSet) or there is no result at all
                int updateCount = stmt.getUpdateCount();
                if (updateCount == -1) {
                    // There is no result at all
                    break;
                }
            } else {
                // Current result is a ResultSet
                result = stmt.getResultSet();
                resultSets.add(result);
                if (databaseProductName.contains(MYSQL)) {
                    // TODO: "mysql" equality condition is part of the temporary fix to support returning the result
                    // set in the case of stored procedures returning only one result set in MySQL. Refer
                    // ballerina-platform/ballerina-lang#8643
                    break;
                }

            }
            // This point reaches if current result was an update count. So it is needed to capture any remaining
            // results
            try {
                resultAndNoUpdateCount = stmt.getMoreResults(Statement.KEEP_CURRENT_RESULT);
            } catch (SQLException e) {
                break;
            }
        }
        return resultSets;
    }

    private BTable constructTable(TableResourceManager rm, Context context, ResultSet rs, BStructureType structType,
             List<ColumnDefinition> columnDefinitions, String databaseProductName) {
        return new BCursorTable(new SQLDataIterator(rm, rs, utcCalendar, columnDefinitions, structType,
                TimeUtils.getTimeStructInfo(context), TimeUtils.getTimeZoneStructInfo(context), databaseProductName),
                structType);
    }

    private BTable constructTable(TableResourceManager rm, Context context, ResultSet rs, BStructureType structType,
            String databaseProductName) throws SQLException {
        List<ColumnDefinition> columnDefinitions = SQLDatasourceUtils.getColumnDefinitions(rs);
        return constructTable(rm, context, rs, structType, columnDefinitions, databaseProductName);
    }

    private String getSQLType(BMap<String, BValue> parameter) {
        String sqlType = "";
        BRefType refType = (BRefType) parameter.get(PARAMETER_SQL_TYPE_FIELD);
        if (refType != null) {
            sqlType = refType.stringValue();
        }
        return sqlType;
    }

    private BStructureType getStructType(BMap<String, BValue> parameter) {
        BTypeDescValue type = (BTypeDescValue) parameter.get(PARAMETER_RECORD_TYPE_FIELD);
        BStructureType structType = null;
        if (type != null) {
            structType = (BStructureType) type.value();
        }
        return structType;
    }

    private int getParameterDirection(BMap<String, BValue> parameter) {
        int direction = 0;
        BRefType dir = (BRefType) parameter.get(PARAMETER_DIRECTION_FIELD);
        if (dir != null) {
            String sqlType = dir.stringValue();
            switch (sqlType) {
            case Constants.QueryParamDirection.DIR_OUT:
                direction = Constants.QueryParamDirection.OUT;
                break;
            case Constants.QueryParamDirection.DIR_INOUT:
                direction = Constants.QueryParamDirection.INOUT;
                break;
            }
        }
        return direction;
    }
}
