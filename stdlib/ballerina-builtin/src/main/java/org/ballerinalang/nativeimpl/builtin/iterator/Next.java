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
import org.ballerinalang.model.types.BStructType;
import org.ballerinalang.model.types.BTupleType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BUnionType;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BArray;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BNewArray;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.util.codegen.StructInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 0.972.1
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "next",
        receiver = @Receiver(type = TypeKind.STRUCT, structType = "ArrayIterator",
                structPackage = "ballerina.builtin"),
        returnType = {@ReturnType(type = TypeKind.TUPLE)},
        isPublic = true
)
public class Next extends BlockingNativeCallableUnit {

    @Override
    public void execute(Context context) {
        // Get the struct.
        BStruct argument = ((BStruct) context.getRefArgument(0));
        // Get data from the struct.
        Object data = argument.getNativeData(Iterator.DATA);
        // Get the index from the struct.
        Object currentIndex = argument.getNativeData(Iterator.CURRENT_INDEX);
        // If the required data and the index is found, we process the values.
        if (data != null && currentIndex != null) {
            BNewArray array = (BNewArray) data;
            int index = (int) currentIndex;
            if (array.size() != index) {
                BValue item = array.getBValue(index);
                argument.addNativeData(Iterator.CURRENT_INDEX, index + 1);
                // Todo - validate
                StructInfo structInfo = ((BStructType) ((BUnionType) context.getCallableUnitInfo().getRetParamTypes()
                        [0]).getMemberTypes().get
                        (0)).structInfo;
                BStruct struct = BLangVMStructs.createBStruct(structInfo);

                BRefValueArray returnValue = new BRefValueArray();
                returnValue.add(0, new BInteger(index));
                returnValue.add(1, getValue(item));
                struct.setRefField(0, returnValue);

                context.setReturnValues(returnValue);
            }
        }
    }

    public static BRefType getValue(BValue value) {
        return (BRefType) value;
    }
}
