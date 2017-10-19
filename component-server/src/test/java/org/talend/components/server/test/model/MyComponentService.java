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
package org.talend.components.server.test.model;

import java.util.Date;

import org.talend.component.api.service.Service;
import org.talend.component.api.service.healthcheck.HealthCheck;
import org.talend.component.api.service.healthcheck.HealthCheckStatus;

@Service
public class MyComponentService {

    @HealthCheck(family = "chain")
    public HealthCheckStatus doCheck(final ListInput.MyDataSet dataSet) {
        return new HealthCheckStatus(HealthCheckStatus.Status.OK, "All good");
    }

    public void logTime() { // usable by components
        System.out.println(new Date());
    }
}
