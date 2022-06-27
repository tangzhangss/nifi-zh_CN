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
package org.apache.nifi.processors.poi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.nifi.csv.CSVUtils;
import org.apache.nifi.flowfile.attributes.CoreAttributes;
import org.apache.nifi.util.LogMessage;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvertExcelToCSVProcessorTest {

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(ConvertExcelToCSVProcessor.class);
    }

    @Test
    public void testMultipleSheetsGeneratesMultipleFlowFiles() throws IOException {

        Map<String, String> attributes = new HashMap<>();
        attributes.put("test", "attribute");

        final URL resourceUrl = getClass().getResource("/TwoSheets.xlsx");
        assertNotNull(resourceUrl);

        testRunner.enqueue(new File(resourceUrl.getPath()).toPath(), attributes);
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 2);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ffSheetA = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheetA = Long.parseLong(ffSheetA.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(4, rowsSheetA);
        assertTrue(ffSheetA.getAttribute(ConvertExcelToCSVProcessor.SHEET_NAME).equalsIgnoreCase("TestSheetA"));
        assertEquals("TwoSheets.xlsx", ffSheetA.getAttribute(ConvertExcelToCSVProcessor.SOURCE_FILE_NAME));

        //Since TestRunner.run() will create a random filename even if the attribute is set in enqueue manually we just check that "_{SHEETNAME}.csv is present
        assertTrue(ffSheetA.getAttribute(CoreAttributes.FILENAME.key()).endsWith("_TestSheetA.csv"));
        assertEquals("attribute", ffSheetA.getAttribute("test"));

        MockFlowFile ffSheetB = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(1);
        long rowsSheetB = Long.parseLong(ffSheetB.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(3, rowsSheetB);
        assertTrue(ffSheetB.getAttribute(ConvertExcelToCSVProcessor.SHEET_NAME).equalsIgnoreCase("TestSheetB"));
        assertEquals("TwoSheets.xlsx", ffSheetB.getAttribute(ConvertExcelToCSVProcessor.SOURCE_FILE_NAME));

        //Since TestRunner.run() will create a random filename even if the attribute is set in enqueue manually we just check that "_{SHEETNAME}.csv is present
        assertTrue(ffSheetB.getAttribute(CoreAttributes.FILENAME.key()).endsWith("_TestSheetB.csv"));
        assertEquals("attribute", ffSheetB.getAttribute("test"));

    }

    @Test
    public void testDataFormatting() {
        testRunner.enqueue(getClass().getResourceAsStream("/dataformatting.xlsx"));

        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "false");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        ff.assertContentEquals("Numbers,Timestamps,Money\n" +
                "1234.4559999999999,42736.5,123.45\n" +
                "1234.4559999999999,42736.5,123.45\n" +
                "1234.4559999999999,42736.5,123.45\n" +
                "1234.4559999999999,42736.5,1023.45\n" +
                "1234.4559999999999,42736.5,1023.45\n" +
                "987654321,42736.5,1023.45\n" +
                "987654321,,\n" +
                "987654321,,\n");
    }

    @Test
    public void testQuoting() {
        testRunner.enqueue(getClass().getResourceAsStream("/dataformatting.xlsx"));

        testRunner.setProperty(CSVUtils.QUOTE_MODE, CSVUtils.QUOTE_MINIMAL);
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        char decimalSeparator = decimalFormatSymbols.getDecimalSeparator();
        char groupingSeparator = decimalFormatSymbols.getGroupingSeparator();
        ff.assertContentEquals(("Numbers,Timestamps,Money\n" +
                addQuotingIfNeeded(String.format("1234%1$s456", decimalSeparator)) + "," + DateTimeFormatter.ofPattern("d/M/yy").format(localDt) + "," +
                    addQuotingIfNeeded(String.format("$   123%1$s45", decimalSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("1234%1$s46", decimalSeparator)) + "," + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + "," +
                    addQuotingIfNeeded(String.format("£   123%1$s45", decimalSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("1234%1$s5", decimalSeparator)) + ",\"" + DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy").format(localDt) + "\"," +
                    addQuotingIfNeeded(String.format("¥   123%1$s45", decimalSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("1%2$s234%1$s46", decimalSeparator, groupingSeparator)) + "," + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + "," +
                    addQuotingIfNeeded(String.format("$   1%2$s023%1$s45", decimalSeparator, groupingSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("1%2$s234%1$s4560", decimalSeparator, groupingSeparator)) + "," + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + "," +
                    addQuotingIfNeeded(String.format("£   1%2$s023%1$s45", decimalSeparator, groupingSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("9%1$s88E+08", decimalSeparator)) + "," + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + "," +
                    addQuotingIfNeeded(String.format("¥   1%2$s023%1$s45", decimalSeparator, groupingSeparator)) + "\n" +
                addQuotingIfNeeded(String.format("9%1$s877E+08", decimalSeparator)) + ",,\n" +
                addQuotingIfNeeded(String.format("9%1$s8765E+08", decimalSeparator)) + ",,\n").replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    /**
     * Workaround for interaction between {@link DecimalFormatSymbols} and use of custom {@link java.util.Locale}.
     */
    private static String getExponentSeparator(final DecimalFormatSymbols decimalFormatSymbols) {
        final String exponentSeparator = decimalFormatSymbols.getExponentSeparator();
        return (exponentSeparator.equals("e") ? "e" : exponentSeparator + "+");
    }

    @Test
    public void testSkipRows() {
        testRunner.enqueue(getClass().getResourceAsStream("/dataformatting.xlsx"));

        testRunner.setProperty(ConvertExcelToCSVProcessor.ROWS_TO_SKIP, "2");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(7, rowsSheet, "Row count does match expected value.");

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String decimalSeparator = decimalFormatSymbols.getDecimalSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = decimalFormatSymbols.getGroupingSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("1234%1$s46," + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + ",£   123%1$s45\n" +
                "1234%1$s5," + DateTimeFormatter.ofPattern("EEEE\\, MMMM dd\\, yyyy").format(localDt) + ",¥   123%1$s45\n" +
                "1%2$s234%1$s46," + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + ",$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560," + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + ",£   1%2$s023%1$s45\n" +
                "9%1$s88E+08," + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + ",¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08,,\n" +
                "9%1$s8765E+08,,\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testSkipRowsWithEL() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("rowsToSkip", "2");
        testRunner.enqueue(getClass().getResourceAsStream("/dataformatting.xlsx"), attributes);

        testRunner.setProperty(ConvertExcelToCSVProcessor.ROWS_TO_SKIP, "${rowsToSkip}");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(7, rowsSheet, "Row count does match expected value.");

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String decimalSeparator = decimalFormatSymbols.getDecimalSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = decimalFormatSymbols.getGroupingSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("1234%1$s46," + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + ",£   123%1$s45\n" +
                "1234%1$s5," + DateTimeFormatter.ofPattern("EEEE\\, MMMM dd\\, yyyy").format(localDt) + ",¥   123%1$s45\n" +
                "1%2$s234%1$s46," + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + ",$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560," + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + ",£   1%2$s023%1$s45\n" +
                "9%1$s88E+08," + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + ",¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08,,\n" +
                "9%1$s8765E+08,,\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testSkipColumns() throws Exception {
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath());

        testRunner.setProperty(ConvertExcelToCSVProcessor.COLUMNS_TO_SKIP, "2");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String decimalSeparator = decimalFormatSymbols.getDecimalSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = decimalFormatSymbols.getGroupingSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("Numbers,Money\n" +
                "1234%1$s456,$   123%1$s45\n" +
                "1234%1$s46,£   123%1$s45\n" +
                "1234%1$s5,¥   123%1$s45\n" +
                "1%2$s234%1$s46,$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560,£   1%2$s023%1$s45\n" +
                "9%1$s88E+08,¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08,\n" +
                "9%1$s8765E+08,\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testSkipColumnsWithEL() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("columnsToSkip", "2");
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath(), attributes);

        testRunner.setProperty(ConvertExcelToCSVProcessor.COLUMNS_TO_SKIP, "${columnsToSkip}");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String decimalSeparator = decimalFormatSymbols.getDecimalSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = decimalFormatSymbols.getGroupingSeparator() == ',' ? "\\,"  : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("Numbers,Money\n" +
                "1234%1$s456,$   123%1$s45\n" +
                "1234%1$s46,£   123%1$s45\n" +
                "1234%1$s5,¥   123%1$s45\n" +
                "1%2$s234%1$s46,$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560,£   1%2$s023%1$s45\n" +
                "9%1$s88E+08,¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08,\n" +
                "9%1$s8765E+08,\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testCustomDelimiters() throws Exception {
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath());

        testRunner.setProperty(CSVUtils.VALUE_SEPARATOR, "|");
        testRunner.setProperty(CSVUtils.RECORD_SEPARATOR, "\\r\\n");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String valueSeparator = testRunner.getProcessContext().getProperty(CSVUtils.VALUE_SEPARATOR).evaluateAttributeExpressions(ff).getValue();
        String decimalSeparator = (String.valueOf(decimalFormatSymbols.getDecimalSeparator()).equals(valueSeparator))
                ? ("\\" + decimalFormatSymbols.getDecimalSeparator()) : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = String.valueOf(decimalFormatSymbols.getGroupingSeparator()).equals(valueSeparator)
                ? "\\" + decimalFormatSymbols.getGroupingSeparator() : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("Numbers|Timestamps|Money\r\n" +
                "1234%1$s456|" + DateTimeFormatter.ofPattern("d/M/yy").format(localDt) + "|$   123%1$s45\r\n" +
                "1234%1$s46|" + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + "|£   123%1$s45\r\n" +
                "1234%1$s5|" + DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy").format(localDt) + "|¥   123%1$s45\r\n" +
                "1%2$s234%1$s46|" + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + "|$   1%2$s023%1$s45\r\n" +
                "1%2$s234%1$s4560|" + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + "|£   1%2$s023%1$s45\r\n" +
                "9%1$s88E+08|" + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + "|¥   1%2$s023%1$s45\r\n" +
                "9%1$s877E+08||\r\n" +
                "9%1$s8765E+08||\r\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testCustomValueSeparatorWithEL() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("csv.delimiter", "|");
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath(), attributes);

        testRunner.setProperty(CSVUtils.VALUE_SEPARATOR, "${csv.delimiter}");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String valueSeparator = testRunner.getProcessContext().getProperty(CSVUtils.VALUE_SEPARATOR).evaluateAttributeExpressions(ff).getValue();
        String decimalSeparator = (String.valueOf(decimalFormatSymbols.getDecimalSeparator()).equals(valueSeparator))
                ? ("\\" + decimalFormatSymbols.getDecimalSeparator()) : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = String.valueOf(decimalFormatSymbols.getGroupingSeparator()).equals(valueSeparator)
                ? "\\" + decimalFormatSymbols.getGroupingSeparator() : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("Numbers|Timestamps|Money\n" +
                "1234%1$s456|" + DateTimeFormatter.ofPattern("d/M/yy").format(localDt) + "|$   123%1$s45\n" +
                "1234%1$s46|" + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + "|£   123%1$s45\n" +
                "1234%1$s5|" + DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy").format(localDt) + "|¥   123%1$s45\n" +
                "1%2$s234%1$s46|" + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + "|$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560|" + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + "|£   1%2$s023%1$s45\n" +
                "9%1$s88E+08|" + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + "|¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08||\n" +
                "9%1$s8765E+08||\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testCustomQuoteCharWithEL() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("csv.quote", "'");
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath(), attributes);

        testRunner.setProperty(CSVUtils.QUOTE_CHAR, "${csv.quote}");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");
        testRunner.setProperty(CSVUtils.QUOTE_MODE, CSVUtils.QUOTE_ALL);

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        String quoteCharValue = testRunner.getProcessContext().getProperty(CSVUtils.QUOTE_CHAR).evaluateAttributeExpressions(ff).getValue();
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        char decimalSeparator = decimalFormatSymbols.getDecimalSeparator();
        char groupingSeparator = decimalFormatSymbols.getGroupingSeparator();
        ff.assertContentEquals(("'Numbers','Timestamps','Money'\n" +
                addQuotingIfNeeded(String.format("1234%1$s456", decimalSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("d/M/yy").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("$   123%1$s45", decimalSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("1234%1$s46", decimalSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("£   123%1$s45", decimalSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("1234%1$s5", decimalSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("¥   123%1$s45", decimalSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("1%2$s234%1$s46", decimalSeparator, groupingSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("$   1%2$s023%1$s45", decimalSeparator, groupingSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("1%2$s234%1$s4560", decimalSeparator, groupingSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("£   1%2$s023%1$s45", decimalSeparator, groupingSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("9%1$s88E+08", decimalSeparator), quoteCharValue, true) + "," + quoteCharValue +
                    DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + quoteCharValue + "," +
                    addQuotingIfNeeded(String.format("¥   1%2$s023%1$s45", decimalSeparator, groupingSeparator), quoteCharValue, true) + "\n" +
                addQuotingIfNeeded(String.format("9%1$s877E+08", decimalSeparator), quoteCharValue, true) + ",,\n" +
                addQuotingIfNeeded(String.format("9%1$s8765E+08", decimalSeparator), quoteCharValue, true) + ",,\n").replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    @Test
    public void testCustomEscapeCharWithEL() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("csv.escape", "^");
        testRunner.enqueue(new File("src/test/resources/dataformatting.xlsx").toPath(), attributes);

        testRunner.setProperty(CSVUtils.ESCAPE_CHAR, "${csv.escape}");
        testRunner.setProperty(ConvertExcelToCSVProcessor.FORMAT_VALUES, "true");

        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long rowsSheet = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(9, rowsSheet);

        LocalDateTime localDt = LocalDateTime.of(2017, 1, 1, 12, 0, 0);
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        String escapeCharValue = testRunner.getProcessContext().getProperty(CSVUtils.ESCAPE_CHAR).evaluateAttributeExpressions(ff).getValue();
        String decimalSeparator = String.valueOf(decimalFormatSymbols.getDecimalSeparator()).equals(",")
                ? escapeCharValue + decimalFormatSymbols.getDecimalSeparator() : String.valueOf(decimalFormatSymbols.getDecimalSeparator());
        String groupingSeparator = String.valueOf(decimalFormatSymbols.getGroupingSeparator()).equals(",")
                ? escapeCharValue + decimalFormatSymbols.getGroupingSeparator() : String.valueOf(decimalFormatSymbols.getGroupingSeparator());
        ff.assertContentEquals(String.format("Numbers,Timestamps,Money\n" +
                "1234%1$s456," + DateTimeFormatter.ofPattern("d/M/yy").format(localDt) + ",$   123%1$s45\n" +
                "1234%1$s46," + DateTimeFormatter.ofPattern("hh:mm:ss a").format(localDt) + ",£   123%1$s45\n" +
                "1234%1$s5," + DateTimeFormatter.ofPattern(String.format("EEEE%1$s, MMMM dd%1$s, yyyy", escapeCharValue)).format(localDt) + ",¥   123%1$s45\n" +
                "1%2$s234%1$s46," + DateTimeFormatter.ofPattern("d/M/yy HH:mm").format(localDt) + ",$   1%2$s023%1$s45\n" +
                "1%2$s234%1$s4560," + DateTimeFormatter.ofPattern("hh:mm a").format(localDt) + ",£   1%2$s023%1$s45\n" +
                "9%1$s88E+08," + DateTimeFormatter.ofPattern("yyyy/MM/dd/ HH:mm").format(localDt) + ",¥   1%2$s023%1$s45\n" +
                "9%1$s877E+08,,\n" +
                "9%1$s8765E+08,,\n", decimalSeparator, groupingSeparator).replace("E+", getExponentSeparator(decimalFormatSymbols)));
    }

    /**
     * Validates that all sheets in the Excel document are exported.
     *
     * @throws Exception
     *  Any exception thrown during execution.
     */
    @Test
    public void testProcessAllSheets() throws Exception {

        testRunner.enqueue(new File("src/test/resources/CollegeScorecard.xlsx").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long l = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(10, l);

        testRunner.clearProvenanceEvents();
        testRunner.clearTransferState();

        testRunner.enqueue(new File("src/test/resources/TwoSheets.xlsx").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 2);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        l = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(4, l);

        ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(1);
        l = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(3, l);
    }

    /**
     * Validates that the manually specified sheet is exported from the Excel document.
     *
     * @throws Exception
     *  Any exception thrown during execution.
     */
    @Test
    public void testProcessASpecificSheetThatDoesExist() throws Exception {

        testRunner.setProperty(ConvertExcelToCSVProcessor.DESIRED_SHEETS, "Scorecard");
        testRunner.enqueue(new File("src/test/resources/CollegeScorecard.xlsx").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long l = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(10, l);
    }

    /**
     * Tests for a syntactically valid Excel XSSF document with a manually specified Excel sheet that does not exist.
     * In this scenario only the Original relationship should be invoked.
     *
     * @throws Exception
     *  Any exception thrown during execution.
     */
    @Test
    public void testNonExistantSpecifiedSheetName() throws Exception {

        testRunner.setProperty(ConvertExcelToCSVProcessor.DESIRED_SHEETS, "NopeIDoNotExist");
        testRunner.enqueue(new File("src/test/resources/CollegeScorecard.xlsx").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 0);  //We aren't expecting any output to success here because the sheet doesn't exist
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);
    }

    /**
     * Validates that a sheet contains blank cells can be converted to a CSV without missing columns.
     *
     * @throws Exception
     *  Any exception thrown during execution.
     */
    @Test
    public void testProcessASheetWithBlankCells() throws Exception {

        testRunner.setProperty(ConvertExcelToCSVProcessor.DESIRED_SHEETS, "Sheet1");
        testRunner.enqueue(new File("src/test/resources/with-blank-cells.xlsx").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 1);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 0);

        MockFlowFile ff = testRunner.getFlowFilesForRelationship(ConvertExcelToCSVProcessor.SUCCESS).get(0);
        long l = Long.parseLong(ff.getAttribute(ConvertExcelToCSVProcessor.ROW_NUM));
        assertEquals(8, l);

        ff.assertContentEquals(new File("src/test/resources/with-blank-cells.csv"));
    }

    /**
     * Tests for graceful handling and error messaging of unsupported .XLS files.
     */
    @Test
    public void testHandleUnsupportedXlsFile() throws Exception {

        testRunner.enqueue(new File("src/test/resources/Unsupported.xls").toPath());
        testRunner.run();

        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.SUCCESS, 0);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.ORIGINAL, 0);
        testRunner.assertTransferCount(ConvertExcelToCSVProcessor.FAILURE, 1);

        List<LogMessage> errorMessages = testRunner.getLogger().getErrorMessages();
        assertEquals(1, errorMessages.size());
        String messageText = errorMessages.get(0).getMsg();
        assertTrue(messageText.contains("Excel") && messageText.contains("OLE2"));
    }

    private String addQuotingIfNeeded(String csvField) {
        return addQuotingIfNeeded(csvField, "\"", false);
    }

    private String addQuotingIfNeeded(String csvField, String csvQuote, boolean force) {
        return csvField.contains(",") || force ? String.format("%2$s%1$s%2$s", csvField, csvQuote) : csvField;
    }
}