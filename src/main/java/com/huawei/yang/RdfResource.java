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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.URIConverter.Saveable;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.xtext.naming.IQualifiedNameProvider;

public class RdfResource extends ResourceImpl {

    private final IQualifiedNameProvider qualifiedNameProvider;

    public RdfResource(URI uri, IQualifiedNameProvider qualifiedNameProvider) {
        super(uri);
        this.qualifiedNameProvider = qualifiedNameProvider;
    }

    @Override
    protected void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException {
        super.doLoad(inputStream, options);
    }

    @Override
    protected void doSave(OutputStream outputStream, Map<?, ?> options) throws IOException {
        if (outputStream instanceof URIConverter.Saveable) {
            URIConverter.Saveable saveable = (Saveable) outputStream;
            saveable.saveResource(this);
        } else {
            EObjectMapper mapper = new EObjectMapper(qualifiedNameProvider);
            write(outputStream, mapper.to(this));
        }

    }

    private void write(OutputStream outputStream, Model graph) {
        RdfWriter.write(outputStream, graph);
    }

}
