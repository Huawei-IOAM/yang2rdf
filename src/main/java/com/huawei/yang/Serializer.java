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

import static org.eclipse.rdf4j.model.util.Values.iri;

import java.util.Collection;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;

public class Serializer {

    private static final String NAMESPACE_CLASS = "http://www.typefox.io/yang/Yang#";
    private static final String NAMESPACE_INSTANCE_PREFIX = "http://www.typefox.io/yang/";

    private final IQualifiedNameProvider qualifiedNameProvider;

    public Serializer(IQualifiedNameProvider qualifiedNameProvider) {
        this.qualifiedNameProvider = qualifiedNameProvider;
    }

    public void to(Resource resource, Model graph) {
        resource.getContents().forEach(eo -> {
            to(eo, graph, SimpleValueFactory.getInstance());
        });
    }

    private void to(EObject eObject, Model graph, SimpleValueFactory valueFactory) {
        createTypeStatement(eObject, graph, valueFactory);
        eObject.eClass().getEAllAttributes().forEach(ea -> {
            serialize(ea, eObject, graph, valueFactory);
        });
        eObject.eClass().getEAllReferences().forEach(er -> {
            serialize(er, eObject, graph, valueFactory);
        });
    }

    private void createTypeStatement(EObject eObject, Model graph, SimpleValueFactory valueFactory) {
        IRI subject = toIRI(eObject);
        IRI predicate = RDF.TYPE;
        IRI object = toIRI(eObject.eClass());
        graph.add(subject, predicate, object);
    }

    private void serialize(EAttribute eAttribute, EObject eObject, Model graph, SimpleValueFactory valueFactory) {
        if (eAttribute.isDerived() || eAttribute.isTransient() || !eObject.eIsSet(eAttribute)) {
            return;
        }
        Object value = eObject.eGet(eAttribute);
        if (eAttribute.isMany()) {
            Collection<?> collection = (Collection<?>) value;
            collection.forEach(o -> {
                IRI subject = toIRI(eObject);
                IRI predicate = toIRI(eAttribute);
                Literal object = toLiteral(o, eAttribute, valueFactory);
                graph.add(subject, predicate, object);
            });
        } else {
            IRI subject = toIRI(eObject);
            IRI predicate = toIRI(eAttribute);
            Literal object = toLiteral(value, eAttribute, valueFactory);
            graph.add(subject, predicate, object);
        }
    }

    private void serialize(EReference eReference, EObject eObject, Model graph, SimpleValueFactory valueFactory) {
        if (eReference.isDerived() || eReference.isTransient() || !eObject.eIsSet(eReference)) {
            return;
        }
        Object value = eObject.eGet(eReference);
        if (eReference.isMany()) {
            Collection<?> collection = (Collection<?>) value;
            collection.forEach(o -> {
                EObject eo = (EObject) o;
                serializeOne(eObject, eReference, eo, graph, valueFactory);
            });
        } else {
            EObject eo = (EObject) value;
            serializeOne(eObject, eReference, eo, graph, valueFactory);
        }
    }

    private void serializeOne(EObject eObject, EReference eReference, EObject value, Model graph, SimpleValueFactory valueFactory) {
        if (eReference.isContainment()) {
            to(value, graph, valueFactory);
        }
        IRI subject = toIRI(eObject);
        IRI predicate = toIRI(eReference);
        IRI object = toIRI(value);
        graph.add(subject, predicate, object);
    }

    private IRI toIRI(EObject eObject) {
        String type = eObject.eClass().getInstanceClass().getSimpleName();
        String fragment = EcoreUtil.getURI(eObject).fragment();
        QualifiedName qName = qualifiedNameProvider.getFullyQualifiedName(eObject);
        StringBuilder namespace = new StringBuilder(NAMESPACE_INSTANCE_PREFIX);
        namespace.append(type);
        if (qName != null) {
            fragment = qName.toString("/");
            namespace.append("/");
        }
        return iri(ParsedIRI.create(namespace + fragment).normalize().toString());
    }

    private IRI toIRI(EClass eClass) {
        String fragment = EcoreUtil.getURI(eClass).fragment();
        QualifiedName qName = qualifiedNameProvider.getFullyQualifiedName(eClass);
        if (qName != null) {
            fragment = qName.skipFirst(1).toString("/");
        }
        return iri(ParsedIRI.create(NAMESPACE_CLASS + fragment).normalize().toString());
    }

    private IRI toIRI(EAttribute eAttribute) {
        String fragment = EcoreUtil.getURI(eAttribute).fragment();
        QualifiedName qName = qualifiedNameProvider.getFullyQualifiedName(eAttribute);
        if (qName != null) {
            fragment = qName.skipFirst(1).toString("/");
        }
        return iri(ParsedIRI.create(NAMESPACE_CLASS + fragment).normalize().toString());
    }

    private IRI toIRI(EReference eReference) {
        String fragment = EcoreUtil.getURI(eReference).fragment();
        QualifiedName qName = qualifiedNameProvider.getFullyQualifiedName(eReference);
        if (qName != null) {
            fragment = qName.skipFirst(1).toString("/");
        }
        return iri(ParsedIRI.create(NAMESPACE_CLASS + fragment).normalize().toString());
    }

    private Literal toLiteral(Object value, EAttribute eAttribute, SimpleValueFactory valueFactory) {
        String s = EcoreUtil.convertToString(eAttribute.getEAttributeType(), value);
        Literal l = valueFactory.createLiteral(s);
        return l;
    }

}
