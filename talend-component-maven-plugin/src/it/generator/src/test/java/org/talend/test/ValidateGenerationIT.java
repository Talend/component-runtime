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
package org.talend.test;

import org.talend.test.input.GeneratorMapper;
import org.talend.test.input.GeneratorEmitter;
import org.talend.test.output.GeneratorOutput;

import org.junit.Test;

public class ValidateGenerationIT {

    @Test
    public void checkExists() {
        // ensure we can instantiate all components
        // if one generation failed the compilation would have failed anyway
        new GeneratorMapper();
        new GeneratorEmitter();
        new GeneratorOutput();

        // todo: more validations or is it ok if it compiles?
    }
}
