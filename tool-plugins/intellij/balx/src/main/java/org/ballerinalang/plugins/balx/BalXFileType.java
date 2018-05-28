/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.ballerinalang.plugins.balx;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

/**
 * Represents a Ballerina file.
 */
public class BalXFileType implements FileType {

    public static final BalXFileType INSTANCE = new BalXFileType();

    private BalXFileType() {

    }

    @NotNull
    @Override
    public String getName() {
        return "BALX";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Ballerina executable file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "balx";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return BalXIcons.FILE;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return null;
    }
}
