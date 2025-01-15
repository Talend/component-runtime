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
package org.talend.sdk.component.maven;

import java.io.PrintStream;

public class Bootstrap {

    public void css(final PrintStream stream) {
        stream
                .println("    <link rel=\"stylesheet\" "
                        + "href=\"https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.1.3/css/bootstrap.min.css\" "
                        + "integrity=\"sha256-eSi1q2PG6J7g7ib17yAaWMcrr5GrtohYChqibrV7PBE=\" "
                        + "crossorigin=\"anonymous\" />");
        stream
                .println("      <link rel=\"stylesheet\" "
                        + "href=\"https://cdnjs.cloudflare.com/ajax/libs/mdbootstrap/4.5.13/css/mdb.min.css\" "
                        + "integrity=\"sha256-REjZf+g6soUYYHr2eOC4oR1+kiH4sQqdE1fTBWMiKiw=\" "
                        + "crossorigin=\"anonymous\" />");
    }

    public void js(final PrintStream stream) {
        stream
                .println("   <script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/3.3.1/jquery.min.js\" "
                        + "integrity=\"sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=\" "
                        + "crossorigin=\"anonymous\"></script>");
        stream
                .println("   <script "
                        + "src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.4/umd/popper.min.js\" "
                        + "integrity=\"sha256-EGs9T1xMHdvM1geM8jPpoo8EZ1V1VRsmcJz8OByENLA=\" "
                        + "crossorigin=\"anonymous\"></script>");
        stream
                .println("   <script "
                        + "src=\"https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.1.3/js/bootstrap.min.js\" "
                        + "integrity=\"sha256-VsEqElsCHSGmnmHXGQzvoWjWwoznFSZc6hs7ARLRacQ=\" "
                        + "crossorigin=\"anonymous\"></script>");
    }
}
