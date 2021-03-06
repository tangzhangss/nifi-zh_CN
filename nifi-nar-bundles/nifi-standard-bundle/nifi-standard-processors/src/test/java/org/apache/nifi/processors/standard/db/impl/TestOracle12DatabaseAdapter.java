/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.processors.standard.db.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.nifi.processors.standard.db.DatabaseAdapter;
import org.junit.Assert;
import org.junit.Test;

public class TestOracle12DatabaseAdapter {

    private final DatabaseAdapter db = new Oracle12DatabaseAdapter();

    @Test
    public void testGeneration() throws Exception {
        String sql1 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "", "", null, null);
        String expected1 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename";
        Assert.assertEquals(sql1, expected1);

        String sql2 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "that=\'some\"\' value\'", "", null, null);
        String expected2 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE that=\'some\"\' value\'";
        Assert.assertEquals(sql2, expected2);

        String sql3 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "that=\'some\"\' value\'", "might DESC", null, null);
        String expected3 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE that=\'some\"\' value\' ORDER BY might DESC";
        Assert.assertEquals(sql3, expected3);

        String sql4 = db.getSelectStatement("database.tablename", "", "that=\'some\"\' value\'", "might DESC", null, null);
        String expected4 = "SELECT * FROM database.tablename WHERE that=\'some\"\' value\' ORDER BY might DESC";
        Assert.assertEquals(sql4, expected4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoTableName() throws Exception {
        db.getSelectStatement("", "some(set),of(columns),that,might,contain,methods,a.*", "", "", null, null);
    }

    @Test
    public void testPagingQuery() throws Exception {
        String sql1 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "", "contain", 100L, 0L);
        String expected1 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename ORDER BY contain FETCH NEXT 100 ROWS ONLY";
        Assert.assertEquals(sql1, expected1);

        String sql2 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "", "contain", 10000L, 123456L);
        String expected2 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename ORDER BY contain OFFSET 123456 ROWS FETCH NEXT 10000 ROWS ONLY";
        Assert.assertEquals(sql2, expected2);

        String sql3 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "methods='strange'", "contain", 10000L, 123456L);
        String expected3 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE methods='strange' ORDER BY contain OFFSET 123456 ROWS FETCH NEXT 10000 ROWS ONLY";
        Assert.assertEquals(sql3, expected3);

        String sql4 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "", "", 100L, null);
        String expected4 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename FETCH NEXT 100 ROWS ONLY";
        Assert.assertEquals(sql4, expected4);
    }

    @Test
    public void testPagingQueryUsingColumnValuesForPartitioning() throws Exception {
        String sql1 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "1=1", "contain", 100L, 0L, "contain");
        String expected1 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE 1=1 AND contain >= 0 AND contain < 100";
        Assert.assertEquals(expected1, sql1);

        String sql2 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "1=1", "contain", 10000L, 123456L, "contain");
        String expected2 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE 1=1 AND contain >= 123456 AND contain < 133456";
        Assert.assertEquals(expected2, sql2);

        String sql3 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "methods='strange'", "contain", 10000L, 123456L, "contain");
        String expected3 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename WHERE methods='strange' AND contain >= 123456 AND contain < 133456";
        Assert.assertEquals(expected3, sql3);

        // Paging (limit/offset) is only supported when an orderByClause is supplied, note that it is not honored here
        String sql4 = db.getSelectStatement("database.tablename", "some(set),of(columns),that,might,contain,methods,a.*", "", "", 100L, null, "contain");
        String expected4 = "SELECT some(set),of(columns),that,might,contain,methods,a.* FROM database.tablename";
        Assert.assertEquals(expected4, sql4);
    }

    @Test
    public void testSupportsUpsert() throws Exception {
        assertTrue(db.getClass().getSimpleName() + " should support upsert", db.supportsUpsert());
    }

    @Test
    public void testGetUpsertStatementWithNullTableName() throws Exception {
        testGetUpsertStatement(null, Arrays.asList("notEmpty"), Arrays.asList("notEmpty"), new IllegalArgumentException("Table name cannot be null or blank"));
    }

    @Test
    public void testGetUpsertStatementWithBlankTableName() throws Exception {
        testGetUpsertStatement("", Arrays.asList("notEmpty"), Arrays.asList("notEmpty"), new IllegalArgumentException("Table name cannot be null or blank"));
    }

    @Test
    public void testGetUpsertStatementWithNullColumnNames() throws Exception {
        testGetUpsertStatement("notEmpty", null, Arrays.asList("notEmpty"), new IllegalArgumentException("Column names cannot be null or empty"));
    }

    @Test
    public void testGetUpsertStatementWithEmptyColumnNames() throws Exception {
        testGetUpsertStatement("notEmpty", Collections.emptyList(), Arrays.asList("notEmpty"), new IllegalArgumentException("Column names cannot be null or empty"));
    }

    @Test
    public void testGetUpsertStatementWithNullKeyColumnNames() throws Exception {
        testGetUpsertStatement("notEmpty", Arrays.asList("notEmpty"), null, new IllegalArgumentException("Key column names cannot be null or empty"));
    }

    @Test
    public void testGetUpsertStatementWithEmptyKeyColumnNames() throws Exception {
        testGetUpsertStatement("notEmpty", Arrays.asList("notEmpty"), Collections.emptyList(), new IllegalArgumentException("Key column names cannot be null or empty"));
    }

    @Test
    public void testGetUpsertStatement() throws Exception {
        // GIVEN
        String tableName = "table";
        List<String> columnNames = Arrays.asList("column1","column2", "column3", "column4");
        Collection<String> uniqueKeyColumnNames = Arrays.asList("column2","column4");

        String expected = "MERGE INTO table USING (SELECT ? column1, ? column2, ? column3, ? column4 FROM DUAL) n" +
        " ON (table.column2 = n.column2 AND table.column4 = n.column4) WHEN NOT MATCHED THEN" +
        " INSERT (column1, column2, column3, column4) VALUES (n.column1, n.column2, n.column3, n.column4)" +
        " WHEN MATCHED THEN UPDATE SET table.column1 = n.column1, table.column3 = n.column3";

        // WHEN
        // THEN
        testGetUpsertStatement(tableName, columnNames, uniqueKeyColumnNames, expected);
    }

    private void testGetUpsertStatement(String tableName, List<String> columnNames, Collection<String> uniqueKeyColumnNames, IllegalArgumentException expected) {
        try {
            testGetUpsertStatement(tableName, columnNames, uniqueKeyColumnNames, (String)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    private void testGetUpsertStatement(String tableName, List<String> columnNames, Collection<String> uniqueKeyColumnNames, String expected) {
        // WHEN
        String actual = db.getUpsertStatement(tableName, columnNames, uniqueKeyColumnNames);

        // THEN
        assertEquals(expected, actual);
    }

}