/*
*   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
package org.ballerinalang.test.expressions.stamp;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.types.BAnydataType;
import org.ballerinalang.model.types.BJSONType;
import org.ballerinalang.model.types.BMapType;
import org.ballerinalang.model.types.BRecordType;
import org.ballerinalang.model.types.BStringType;
import org.ballerinalang.model.types.TypeTags;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.exceptions.BLangRuntimeException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test cases for stamping Map type variables.
 *
 * @since 0.983.0
 */
public class MapStampInbuiltFunctionTest {

    private CompileResult compileResult;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/expressions/stamp/map-stamp-expr-test.bal");
    }


    //----------------------------- Map Stamp Test cases ------------------------------------------------------


    @Test
    public void testStampIntMapToRecord() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntMapToRecord");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BRecordType.class);
        Assert.assertEquals(mapValue.getType().getName(), "IntRecord");

        Assert.assertEquals(mapValue.get("a").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("a").stringValue(), "1");

        Assert.assertEquals(mapValue.get("b").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("b").stringValue(), "2");
    }

    @Test
    public void testStampIntMapToJSON() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntMapToJSON");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BJSONType.class);

        Assert.assertEquals(mapValue.get("a").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("a").stringValue(), "1");

        Assert.assertEquals(mapValue.get("b").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("b").stringValue(), "2");
    }

    @Test
    public void testStampIntMapToAnydata() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntMapToAnydata");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BAnydataType.class);
    }


    @Test
    public void testStampIntMapToIntMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntMapToIntMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BMapType.class);
        Assert.assertEquals((((BMapType) mapValue.getType()).getConstrainedType().getTag()), TypeTags.INT_TAG);
    }

    @Test
    public void testStampIntMapToAnydataMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntMapToAnydataMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.get("a").getType().getClass(), BAnydataType.class);
        Assert.assertEquals(mapValue.get("a").stringValue(), "1");

        Assert.assertEquals(mapValue.get("b").getType().getClass(), BAnydataType.class);
        Assert.assertEquals(mapValue.get("b").stringValue(), "2");
    }


    @Test
    public void testStampAnydataMapToIntMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToIntMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.get("a").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("a").stringValue(), "1");

        Assert.assertEquals(mapValue.get("b").getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(mapValue.get("b").stringValue(), "2");
    }

    @Test
    public void testStampAnydataMapToStringMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToStringMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.get("firstName").getType().getClass(), BStringType.class);
        Assert.assertEquals(mapValue.get("firstName").stringValue(), "mohan");

        Assert.assertEquals(mapValue.get("lastName").getType().getClass(), BStringType.class);
        Assert.assertEquals(mapValue.get("lastName").stringValue(), "raj");
    }

    @Test
    public void testStampAnydataToIntMapWithoutExplicitConstraintType() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToStringMapWithoutExplicitConstraintType");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.get("firstName").getType().getClass(), BStringType.class);
        Assert.assertEquals(mapValue.get("firstName").stringValue(), "mohan");

        Assert.assertEquals(mapValue.get("lastName").getType().getClass(), BStringType.class);
        Assert.assertEquals(mapValue.get("lastName").stringValue(), "raj");
    }

    @Test
    public void testStampAnydataMapToRecord() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToRecord");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 5);

        Assert.assertEquals(mapValue.getType().getClass(), BRecordType.class);
        Assert.assertEquals(mapValue.getType().getName(), "Teacher");

        Assert.assertEquals(mapValue.get("name").stringValue(), "Raja");
        Assert.assertEquals(mapValue.get("name").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("age").stringValue(), "25");
        Assert.assertEquals(mapValue.get("age").getType().getTag(), TypeTags.INT_TAG);

        Assert.assertEquals(mapValue.get("status").stringValue(), "single");
        Assert.assertEquals(mapValue.get("status").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("batch").stringValue(), "LK2014");
        Assert.assertEquals(mapValue.get("batch").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("school").stringValue(), "Hindu College");
        Assert.assertEquals(mapValue.get("school").getType().getClass(), BStringType.class);

    }

    @Test
    public void testStampAnydataMapToJSON() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToJSON");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 5);

        Assert.assertEquals(mapValue.getType().getClass(), BJSONType.class);

        Assert.assertEquals(mapValue.get("name").stringValue(), "Raja");
        Assert.assertEquals(mapValue.get("name").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("age").stringValue(), "25");
        Assert.assertEquals(mapValue.get("age").getType().getTag(), TypeTags.INT_TAG);

        Assert.assertEquals(mapValue.get("status").stringValue(), "single");
        Assert.assertEquals(mapValue.get("status").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("batch").stringValue(), "LK2014");
        Assert.assertEquals(mapValue.get("batch").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("school").stringValue(), "Hindu College");
        Assert.assertEquals(mapValue.get("school").getType().getClass(), BStringType.class);

    }

    @Test
    public void testStampAnydataMapToAnydata() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToAnydata");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 5);

        Assert.assertEquals(mapValue.getType().getClass(), BAnydataType.class);

        Assert.assertEquals(mapValue.get("name").stringValue(), "Raja");
        Assert.assertEquals(mapValue.get("name").getType().getClass(), BStringType.class);

        Assert.assertEquals(mapValue.get("age").stringValue(), "25");
        Assert.assertEquals(mapValue.get("age").getType().getTag(), TypeTags.INT_TAG);

        Assert.assertEquals(mapValue.get("status").stringValue(), "single");
        Assert.assertEquals(mapValue.get("status").getType().getClass(), BStringType.class);

    }

    @Test
    public void testStampAnydataMapToSimilarOpenRecordMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToSimilarOpenRecordMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getMap().get("a").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("a")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("a")).getMap().get("school")).getType().
                getClass(), BAnydataType.class);

        Assert.assertEquals(mapValue.getMap().get("b").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("b")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("b")).getMap().get("school")).getType().
                getClass(), BAnydataType.class);
    }

    @Test
    public void testStampAnydataMapToRecordMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToRecordMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getMap().get("a").getType().getName(), "Teacher");
        Assert.assertEquals(((BMap) mapValue.getMap().get("a")).getMap().size(), 5);

        Assert.assertEquals(mapValue.getMap().get("b").getType().getName(), "Teacher");
        Assert.assertEquals(((BMap) mapValue.getMap().get("b")).getMap().size(), 5);
    }

    @Test
    public void testStampAnydataMapToJSONMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataMapToJSONMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) mapValue.getType()).getConstrainedType().getClass(), BJSONType.class);

        Assert.assertEquals(mapValue.get("a").getType().getClass(), BJSONType.class);
        Assert.assertEquals(mapValue.get("b").getType().getClass(), BJSONType.class);
    }


    @Test
    public void testStampRecordMapToAnydataMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampRecordMapToAnydataMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getMap().get("a").getType().getClass(), BAnydataType.class);
        Assert.assertEquals(((BMap) mapValue.getMap().get("a")).getMap().size(), 5);

        Assert.assertEquals(mapValue.getMap().get("b").getType().getClass(), BAnydataType.class);
        Assert.assertEquals(((BMap) mapValue.getMap().get("b")).getMap().size(), 5);
    }

    @Test
    public void testStampRecordMapToSimilarOpenRecordMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampRecordMapToSimilarOpenRecordMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getMap().get("a").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("a")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("a")).getMap().get("school")).getType().
                getClass(), BAnydataType.class);

        Assert.assertEquals(mapValue.getMap().get("b").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("b")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) mapValue.getMap().get("b")).getMap().get("school")).getType().
                getClass(), BAnydataType.class);
    }

    @Test
    public void testStampRecordMapToJSONMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampRecordMapToJSONMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);

        Assert.assertEquals(mapValue.getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) mapValue.getType()).getConstrainedType().getClass(), BJSONType.class);

        Assert.assertEquals(mapValue.getMap().get("a").getType().getClass(), BJSONType.class);
        Assert.assertEquals(((BMap) mapValue.getMap().get("a")).getMap().size(), 5);

        Assert.assertEquals(mapValue.getMap().get("b").getType().getClass(), BJSONType.class);
        Assert.assertEquals(((BMap) mapValue.getMap().get("b")).getMap().size(), 5);
    }

    @Test
    public void testStampJSONMapToRecordMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampJSONMapToRecordMap");
        BMap<String, BValue> employee0 = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(employee0.size(), 2);

        Assert.assertEquals(employee0.getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) employee0.getType()).getConstrainedType().getClass(), BRecordType.class);
        Assert.assertEquals(((BMapType) employee0.getType()).getConstrainedType().getName(), "Employee");

        Assert.assertEquals(employee0.getMap().get("a").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("a")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("a")).getMap().get("school")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("a")).getMap().get("batch")).getType().getClass(),
                BStringType.class);


        Assert.assertEquals(employee0.getMap().get("b").getType().getName(), "Employee");
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("b")).getMap().get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("b")).getMap().get("school")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) employee0.getMap().get("b")).getMap().get("batch")).getType().getClass(),
                BStringType.class);
    }

    @Test
    public void testStampRecordTypeMultiDimensionMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampRecordTypeMultiDimensionMap");
        BMap<String, BValue> employee0 = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(employee0.size(), 2);
        Assert.assertEquals(employee0.getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) employee0.getType()).getConstrainedType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) ((BMapType) employee0.getType()).getConstrainedType()).getConstrainedType().
                getName(), "Employee");

        Assert.assertEquals(employee0.getMap().get("aa").getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) employee0.getMap().get("aa").getType()).getConstrainedType().getClass(),
                BRecordType.class);
        Assert.assertEquals(((BMapType) employee0.getMap().get("aa").getType()).getConstrainedType().getName(),
                "Employee");

        Assert.assertEquals(employee0.getMap().get("bb").getType().getClass(), BMapType.class);
        Assert.assertEquals(((BMapType) employee0.getMap().get("bb").getType()).getConstrainedType().getClass(),
                BRecordType.class);
        Assert.assertEquals(((BMapType) employee0.getMap().get("bb").getType()).getConstrainedType().getName(),
                "Employee");


        Assert.assertEquals(((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("a")).getType().getName(),
                "Employee");
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("a"))).getMap().
                        get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("a"))).getMap().
                        get("school")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("a"))).getMap().
                        get("batch")).getType().getClass(),
                BStringType.class);


        Assert.assertEquals(((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("b")).getType().getName(),
                "Employee");
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("b"))).getMap().
                        get("age")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("b"))).getMap().
                        get("school")).getType().getClass(),
                BAnydataType.class);
        Assert.assertEquals(((BValue) (((BMap) ((BMap) employee0.getMap().get("bb")).getMap().get("b"))).getMap().
                        get("batch")).getType().getClass(),
                BStringType.class);
    }

    @Test
    public void testStampAnydataToIntMultiDimensionMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampAnydataToIntMultiDimensionMap");
        BMap<String, BValue> mapValue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapValue.size(), 2);
        Assert.assertEquals(mapValue.getMap().size(), 2);

        Assert.assertEquals(((BValue) ((BMap) ((BMap) mapValue.getMap().get("a")).getMap().get("aa")).
                getMap().get("aa")).getType().getTag(), TypeTags.INT_TAG);
        Assert.assertEquals(((BValue) ((BMap) ((BMap) mapValue.getMap().get("a")).getMap().get("aa")).
                getMap().get("aa")).stringValue(), "11");
    }

    @Test
    public void testStampIntToAnydataMultiDimensionMap() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampIntToAnydataMultiDimensionMap");
        BMap<String, BValue> mapVaue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapVaue.size(), 2);
        Assert.assertEquals(mapVaue.getMap().size(), 2);

        Assert.assertEquals(((BValue) ((BMap) ((BMap) mapVaue.getMap().get("a")).getMap().get("aa")).
                getMap().get("aa")).getType().getClass(), BAnydataType.class);
        Assert.assertEquals(((BValue) ((BMap) ((BMap) mapVaue.getMap().get("a")).getMap().get("aa")).
                getMap().get("aa")).stringValue(), "11");
    }

    @Test
    public void testStampConstraintMapToAnydata() {

        BValue[] results = BRunUtil.invoke(compileResult, "stampConstraintMapToAnydata");
        BMap<String, BValue> mapVaue = (BMap<String, BValue>) results[0];

        Assert.assertEquals(results.length, 1);
        Assert.assertEquals(mapVaue.size(), 2);

        Assert.assertEquals(mapVaue.getType().getClass(), BAnydataType.class);
        Assert.assertEquals(mapVaue.getMap().get("a").getType().getName(), "Teacher");
        Assert.assertEquals(mapVaue.getMap().get("a").getType().getClass(), BRecordType.class);
        Assert.assertEquals(mapVaue.getMap().get("b").getType().getName(), "Teacher");
        Assert.assertEquals(mapVaue.getMap().get("b").getType().getClass(), BRecordType.class);
    }

    //---------------------------------- Negative Test cases ----------------------------------------------

    @Test(expectedExceptions = BLangRuntimeException.class,
            expectedExceptionsMessageRegExp = "error: incompatible stamp operation: 'map<string>' value cannot be " +
                    "stamped as 'EmployeeClosedRecord'.*")
    public void testStampMapToRecordNegative() {
        BRunUtil.invoke(compileResult, "stampMapToRecordNegative");
    }

}

