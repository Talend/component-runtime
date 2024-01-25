/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.di.record;

import routines.system.Document;
import routines.system.Dynamic;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

public class VisitorsTest {

    protected static RecordBuilderFactory factory = new RecordBuilderFactoryImpl("rowStruct");

    protected static final String NAME = "rowStructName::챵::®";

    protected static final short SHORT = Short.MAX_VALUE;

    protected static final int INT = Integer.MAX_VALUE;

    protected static final long LONG = Long.MAX_VALUE;

    protected static final float FLOAT = Float.MAX_VALUE;

    protected static final double DOUBLE = Double.MAX_VALUE;

    protected static final Date DATE = new Date(2021, 2, 11, 14, 00, 19);

    protected static final BigDecimal BIGDEC = new BigDecimal("123485789748.8480275637");

    protected static final ZonedDateTime ZONED_DATE_TIME =
            ZonedDateTime.of(1946, 02, 03, 11, 6, 9, 0, ZoneId.of("UTC"));

    protected static final Instant INSTANT = Timestamp.valueOf("2021-04-19 13:37:07.123456").toInstant();

    protected static final byte[] BYTES0 =
            { -2, -1, 0, 72, 0, 101, 0, 108, 0, 108, 0, 111, 0, 32, 0, 87, 0, 111, 0, 114, 0, 108, 0, 100, 0, 33 };

    protected static final byte[] BYTES1 = "Hello RowStruct !".getBytes();

    protected static final Record RECORD =
            factory.newRecordBuilder().withInt("ntgr", 1).withString("str", "one").build();

    protected static final Object OBJECT = new Object();

    protected static final Document DOCUMENT = new Document();

    protected static final List<String> STRINGS = Arrays.asList("one", "two", "three", "four", "five");

    protected static final List<Integer> INTEGERS = Arrays.asList(1, 2, 3, 4, 5);

    protected static final List<Long> LONGS = Arrays.asList(1l, 2l, 3l, 4l, 5l);

    protected static final List<Float> FLOATS = Arrays.asList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);

    protected static final List<Double> DOUBLES = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);

    protected static final List<Boolean> BOOLEANS = Arrays.asList(true, false, true);

    protected static final List<byte[]> BYTES = Arrays.asList(BYTES0, BYTES1);

    protected static final List<ZonedDateTime> DATES = Arrays.asList(ZONED_DATE_TIME, ZONED_DATE_TIME);

    protected static final List<Record> RECORDS = Arrays.asList(RECORD, RECORD);

    protected static final List<BigDecimal> BIG_DECIMALS = Arrays.asList(BIGDEC, BIGDEC, BIGDEC, BIGDEC);

    @Getter
    @ToString
    public static class RowStruct implements routines.system.IPersistableRow {

        public String id;

        public Boolean idIsNullable() {
            return false;
        }

        public Boolean idIsKey() {
            return true;
        }

        public String name;

        public Boolean nameIsNullable() {
            return false;
        }

        public Boolean nameIsKey() {
            return true;
        }

        public String nameDefault() {
            return "John";
        }

        public String nameComment() {
            return "A small comment on name field...";
        }

        public String nameOriginalDbColumnName() {
            return "namy";
        }

        public short shortP;

        public Short shortC;

        public int intP;

        public Integer intC;

        public long longP;

        public Long longC;

        public float floatP;

        public Integer floatPLength() {
            return 10;
        }

        public Integer floatPPrecision() {
            return 2;
        }

        public Float floatC;

        public double doubleP;

        public Double doubleC;

        public Date date0;

        public Boolean date0IsKey() {
            return false;
        }

        public String date0Pattern() {
            return "YYYY-mm-dd HH:MM:ss";
        }

        public Date date1;

        public Date date2;

        public Date date3;

        public Date date4;

        public byte[] bytes0;

        public byte[] bytes1;

        public BigDecimal bigDecimal0;

        public BigDecimal bigDecimal1;

        public Integer bigDecimal0Length() {
            return 30;
        }

        public Integer bigDecimal0Precision() {
            return 10;
        }

        public Integer bigDecimal1Length() {
            return 30;
        }

        public Integer bigDecimal1Precision() {
            return 10;
        }

        public boolean bool0;

        public Boolean bool1;

        public List<Integer> array0;

        public Object object0;

        public Object object1;

        public Dynamic dynamic;

        public Document document;

        public Boolean dynamicIsNullable() {
            return false;
        }

        public Boolean dynamicIsKey() {
            return true;
        }

        public String dynamicComment() {
            return "dYnAmIc";
        }

        public Integer dynamicLength() {
            return 30;
        }

        public Integer dynamicPrecision() {
            return 10;
        }

        public String dynamicPattern() {
            return "YYYY-mm-ddTHH:MM";
        }

        public String h;

        public Character char0;

        public boolean hAshcOdEdIrtY;

        @Getter(value = AccessLevel.NONE)
        public boolean hashCodeDirty = true;

        @Getter(value = AccessLevel.NONE)
        public String loopKey = "loopyyyy";

        @Getter(value = AccessLevel.NONE)
        public String lookKey = "lookKIIII";

        @Override
        public void writeData(final ObjectOutputStream objectOutputStream) {
            throw new UnsupportedOperationException("#writeData()");
        }

        @Override
        public void readData(final ObjectInputStream objectInputStream) {
            throw new UnsupportedOperationException("#readData()");
        }
    }

}
