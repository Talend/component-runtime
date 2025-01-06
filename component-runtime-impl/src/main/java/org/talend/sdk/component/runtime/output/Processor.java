/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.output;

import org.talend.sdk.component.runtime.base.Lifecycle;

public interface Processor extends Lifecycle {

    void beforeGroup();

    // impl note: the output factory is mainly here for beam case, don't propagate
    // it to the mainstream API
    // since it will never work in the studio with current generation logic
    void afterGroup(OutputFactory output);

    void onNext(InputFactory input, OutputFactory output);
}
