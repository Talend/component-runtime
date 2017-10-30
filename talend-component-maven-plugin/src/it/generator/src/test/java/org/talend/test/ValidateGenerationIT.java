/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.test;

import org.junit.Test;
import org.talend.test.input.GeneratorEmitter;
import org.talend.test.input.GeneratorMapper;
import org.talend.test.output.GeneratorOutput;

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
