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
package org.talend.components.server.configuration;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import org.apache.deltaspike.core.api.config.ConfigResolver;

@NoArgsConstructor(access = PRIVATE)
public final class ConfigurationConverters {

    public static class SetConverter implements ConfigResolver.Converter<Set<String>> {

        @Override
        public Set<String> convert(final String value) {
            final Set<String> out = new HashSet<>();
            StringBuilder currentValue = new StringBuilder();
            int length = value.length();
            for (int i = 0; i < length; i++) {
                char c = value.charAt(i);
                if (c == '\\') {
                    if (i < length - 1) {
                        char nextC = value.charAt(i + 1);
                        currentValue.append(nextC);
                        i++;
                    }
                } else if (c == ',') {
                    String trimedVal = currentValue.toString().trim();
                    if (trimedVal.length() > 0) {
                        out.add(trimedVal);
                    }

                    currentValue.setLength(0);
                } else {
                    currentValue.append(c);
                }
            }

            String trimedVal = currentValue.toString().trim();
            if (trimedVal.length() > 0) {
                out.add(trimedVal);
            }
            return out;
        }
    }
}
