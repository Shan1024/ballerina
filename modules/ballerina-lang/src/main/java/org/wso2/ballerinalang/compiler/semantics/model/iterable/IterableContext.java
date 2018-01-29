/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.ballerinalang.compiler.semantics.model.iterable;

import java.util.LinkedList;

/**
 * Represents Chain of Iterable Operation.
 * This will converted into a function at code generation.
 *
 * @since 0.96.1
 */
public class IterableContext {

    public LinkedList<Operation> operations = new LinkedList<>();
    public boolean processed;

    public void addOperation(Operation operation) {
        Operation prv = operations.size() > 0 ? operations.getLast() : null;
        operation.previous = prv;
        if (operation.previous != null) {
            prv.next = operation;
        }
        operations.add(operation);
    }

    public boolean isLastOperationTerminal() {
        if (operations.size() == 0) {
            return false;
        }
        return operations.getLast().kind.isTerminal();
    }

    public boolean isProcessed() {
        return processed;
    }
}
