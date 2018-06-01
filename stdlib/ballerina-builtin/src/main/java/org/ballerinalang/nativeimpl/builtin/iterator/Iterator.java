package org.ballerinalang.nativeimpl.builtin.iterator;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.BLangVMStructs;
import org.ballerinalang.bre.bvm.BlockingNativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BStringArray;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.StructInfo;


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
        if (argument instanceof BStringArray) {
            PackageInfo packageInfo = context.getProgramFile().getPackageInfo("ballerina.builtin");
            StructInfo structInfo = packageInfo.getStructInfo("ArrayIterator");
            BStruct struct = BLangVMStructs.createBStruct(structInfo);
            struct.addNativeData(DATA, argument);
            struct.addNativeData(CURRENT_INDEX, 0);
            context.setReturnValues(struct);
        }
    }
}
