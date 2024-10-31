/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2024. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.yang;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

public class RdfResourceFactory extends ResourceFactoryImpl {

    private final IQualifiedNameProvider qualifiedNameProvider;

    public RdfResourceFactory(IQualifiedNameProvider qualifiedNameProvider) {
        this.qualifiedNameProvider = qualifiedNameProvider;
    }

    @Override
    public Resource createResource(URI uri) {
        return new RdfResource(uri, qualifiedNameProvider);
    }

}
