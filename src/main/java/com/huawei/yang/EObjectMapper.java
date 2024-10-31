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

import java.util.ArrayList;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

public class EObjectMapper {

    private final IQualifiedNameProvider qualifiedNameProvider;

    public EObjectMapper(IQualifiedNameProvider qualifiedNameProvider) {
        this.qualifiedNameProvider = qualifiedNameProvider;
    }

    public Model to(Resource resource) {
        return to(new LinkedHashModel(new ArrayList<>()), resource);
    }

    private Model to(Model graph, Resource resource) {
        Serializer serializer = new Serializer(qualifiedNameProvider);
        serializer.to(resource, graph);
        return graph;
    }

}
