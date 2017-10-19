// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;

import org.talend.component.api.service.Service;

@Service
public class FileService {

    public PrintStream createOutput(final File file) throws FileNotFoundException {
        return new PrintStream(new FileOutputStream(file));
    }

    public BufferedReader createInput(final File file) throws FileNotFoundException {
        return new BufferedReader(new FileReader(file));
    }
}
