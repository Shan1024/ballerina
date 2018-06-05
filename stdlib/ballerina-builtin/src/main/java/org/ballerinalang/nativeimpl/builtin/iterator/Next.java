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
import org.ballerinalang.model.types.BObjectType;
import org.ballerinalang.model.types.BRecordType;
import org.ballerinalang.model.types.BType;
import org.ballerinalang.model.types.BUnionType;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BNewArray;
import org.ballerinalang.model.values.BRefType;
import org.ballerinalang.model.values.BRefValueArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.util.codegen.CallableUnitInfo;
import org.ballerinalang.util.codegen.ObjectTypeInfo;
import org.ballerinalang.util.codegen.RecordTypeInfo;

import java.util.List;

/**
 * @since 0.972.1
 */
@BallerinaFunction(
        orgName = "ballerina", packageName = "builtin",
        functionName = "next",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "ArrayIterator",
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
        // Get the index from the native data in the struct.
        Object currentIndex = argument.getNativeData(Iterator.CURRENT_INDEX);
        // If the required data and the index is found, we process the values.
        if (data == null || currentIndex == null) {
            return;
        }
        // Todo - Validate in semantic analyzer.
        // If the data is not a type of array, we don't do anything.
        if (!(data instanceof BNewArray)) {
            return;
        }
        // Cast the data to an BNewArray.
        BNewArray array = (BNewArray) data;
        // Cast the index to integer type.
        int index = (int) currentIndex;
        // Check whether we have reached the end of the array.
        if (array.size() == index) {
            return;
        }
        // Get the value in the index.
        BValue item = array.getBValue(index);
        // Update the index pointer in native data.
        argument.addNativeData(Iterator.CURRENT_INDEX, index + 1);
        // Get the callable unit info from the context. This is needed to get the return type.
        CallableUnitInfo callableUnitInfo = context.getCallableUnitInfo();
        // Get the 1st return type. This will be a union type.
        BType unionType = callableUnitInfo.getRetParamTypes()[0];
        // Get the member types of the union types.
        List<BType> memberTypes = ((BUnionType) unionType).getMemberTypes();
        // Get the first value from the union type. This will be an anonymous object.
        BType objectType = memberTypes.get(0);
        if (!(objectType instanceof BRecordType)) {
            return;
        }
        // Get the record info type.
        RecordTypeInfo recordTypeInfo = ((BRecordType) objectType).recordTypeInfo;
        // Create new record.
        BStruct record = BLangVMStructs.createBStruct(recordTypeInfo);
        // We need to set fields of the record.
        BRefValueArray returnValue = new BRefValueArray();
        // The first field should be the index.
        returnValue.add(0, new BInteger(index));
        // The second field should be the value.
        returnValue.add(1, getValue(item));
        // Add the fields to the record.
        record.setRefField(0, returnValue);
        // Set the return value.
        context.setReturnValues(returnValue);
    }

    public static BRefType getValue(BValue value) {
        return (BRefType) value;
    }
}
