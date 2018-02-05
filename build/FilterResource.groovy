/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource
import org.codehaus.plexus.interpolation.StringSearchInterpolator

log.info "Filtering ${from} ${to}"
def interpolator = new StringSearchInterpolator()
interpolator.addValueSource(new PropertiesBasedValueSource(project.properties))
interpolator.addValueSource(new PropertiesBasedValueSource(session.executionProperties))
new File(to).text = interpolator.interpolate(new File(from).text)
