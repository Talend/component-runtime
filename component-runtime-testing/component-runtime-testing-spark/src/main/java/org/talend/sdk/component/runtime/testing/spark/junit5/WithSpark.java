/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.testing.spark.junit5;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.talend.sdk.component.runtime.testing.spark.junit5.internal.SparkExtension;

/**
 * Will start a local spark cluster - but not in the same JVM.
 */
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(SparkExtension.class)
public @interface WithSpark {

    /**
     * @return number of slave to start.
     */
    int slaves() default 1;

    /**
     * @return scala version to use.
     */
    String scalaVersion() default "2.11";

    /**
     * @return spark version.
     */
    String sparkVersion() default "2.4.0";

    /**
     * @return where to find winutils for hadoop.
     */
    String hadoopBase() default "https://github.com/steveloughran/winutils/blob/master";

    /**
     * @return the hadoop version - for winutils.
     */
    String hadoopVersion() default "2.6.4";

    /**
     * @return should winutils be installed automatically.
     */
    boolean installWinUtils() default true;
}
