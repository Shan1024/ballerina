/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nativeimpl.builtin.iterator;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BNewArray;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructureTypeInfo;

/**
 * Native function ballerina.model.string:base64Decode.
 *
 * @since 0.972.1
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "iterator",
        args = {@Argument(name = "a", type = TypeKind.ANY)},
        returnType = {@ReturnType(type = TypeKind.ANY)},
        isPublic = true
)
public class Iterator extends BlockingNativeCallableUnit {

    public static final String DATA = "DATA";
    public static final String CURRENT_INDEX = "CURRENT_INDEX";

    @Override
    public void execute(Context context) {
        BValue argument = context.getRefArgument(0);
        PackageInfo packageInfo = context.getProgramFile().getPackageInfo("ballerina.builtin");
        StructureTypeInfo structInfo = packageInfo.getStructInfo("ArrayIterator");
        BStruct struct = BLangVMStructs.createBStruct(structInfo);
        if (argument instanceof BStruct) {
            // Handle objects.
            BRefType refField = ((BStruct) context.getRefArgument(0)).getRefField(0);
            struct.addNativeData(DATA, refField);
        } else if (argument instanceof BNewArray) {
            // Handle arrays.
            struct.addNativeData(DATA, argument);
        }
        struct.addNativeData(CURRENT_INDEX, 0);
        context.setReturnValues(struct);
    }
}
