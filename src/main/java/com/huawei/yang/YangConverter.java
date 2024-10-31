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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.validation.IssueSeverities;

import com.google.inject.Injector;

import io.typefox.yang.YangStandaloneSetup;
import io.typefox.yang.validation.YangIssueSeverityProvider;

public class YangConverter {

    static {
        System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false");
    }

    private void convert(String yangDir, String outputFile) {
        Injector injector = new YangStandaloneSetup().createInjectorAndDoEMFRegistration();
        System.out.printf("Getting YANG files from '%s' folder ...\n", yangDir);
        Set<File> yangFiles = getYangFiles(new File(yangDir));
        System.out.printf("Parsing %s YANG files, this may take several minutes ...\n", yangFiles.size());
        XtextResourceSet xtextResourceSet = parseYangFiles(yangFiles, injector);
        System.out.println("Validating the resources ...");
        validate(xtextResourceSet, injector);
        System.out.println("Serializing the resources ...");
        serialize(xtextResourceSet, new File(outputFile), injector);
        System.out.printf("File was created: %s\n", outputFile);
    }

    private Set<File> getYangFiles(File yangDir) {
        Set<File> yangFiles = new LinkedHashSet<>(Arrays.asList(yangDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".yang");
            }            
        })));
        return yangFiles;
    }

    private XtextResourceSet parseYangFiles(Set<File> yangFiles, Injector injector) {
        XtextResourceSet xtextResourceSet = injector.getInstance(XtextResourceSet.class);
        yangFiles.forEach(yangFile -> {
            Resource resource = xtextResourceSet.createResource(URI.createFileURI(yangFile.getPath()));
            try {
                resource.load(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        xtextResourceSet.getResources().forEach(r -> {
            EcoreUtil.resolveAll(r);
        });

        Set<EObject> objects = new HashSet<>();
        Map<EObject, Collection<Setting>> proxies = EcoreUtil.UnresolvedProxyCrossReferencer.find(xtextResourceSet);
        for (Map.Entry<EObject, Collection<Setting>> entry : proxies.entrySet()) {
            for (Setting setting : entry.getValue()) {
                EObject objectWithDanglingReference = setting.getEObject();
                objects.add(objectWithDanglingReference);
            }
        }
        EcoreUtil.deleteAll(objects, false);

        return xtextResourceSet;
    }

    private void validate(XtextResourceSet xtextResourceSet, Injector injector) {
        IResourceValidator validator = injector.getInstance(IResourceValidator.class);
        YangIssueSeverityProvider severityProvider = injector.getInstance(YangIssueSeverityProvider.class);

        xtextResourceSet.getResources().forEach(resource -> {
            List<Issue> issues = validator.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
            IssueSeverities issueSeverities = severityProvider.getIssueSeverities(resource);
            issues.forEach(i -> {
                Severity severity = issueSeverities.getSeverity(i.getCode());
                switch (severity) {
                    case ERROR:
                        System.out.printf("ERROR: %s, %s\n", i.getMessage(), i.getUriToProblem());
                        break;
                    case WARNING:
                        System.out.printf("WARNING: %s, %s\n", i.getMessage(), i.getUriToProblem());
                        break;
                    case IGNORE:
                        System.out.printf("IGNORE: %s, %s\n", i.getMessage(), i.getUriToProblem());
                        break;
                    case INFO:
                        System.out.printf("INFO: %s, %s\n", i.getMessage(), i.getUriToProblem());
                        break;
                    default:
                        break;
                }
            });
        });
    }

    private void serialize(XtextResourceSet xtextResourceSet, File outputFile, Injector injector) {
        IQualifiedNameProvider qualifiedNameProvider = injector.getInstance(IQualifiedNameProvider.class);
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("rdf", new RdfResourceFactory(qualifiedNameProvider));
        Resource resource = resourceSet.createResource(URI.createFileURI(outputFile.getPath()));
        xtextResourceSet.getResources().forEach(r -> {
            resource.getContents().add(r.getContents().get(0));
        });
        try {
            resource.save(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Missed arguments");
            System.out.println("Usage: YangConverter <yangDir> <outputFile>");
            return;
        }

        String yangDir = args[0];
        String outputFile = args[1];

        System.out.printf("yangDir: %s\n", yangDir);
        System.out.printf("outputFile: %s\n", outputFile);

        YangConverter converter = new YangConverter();
        converter.convert(yangDir, outputFile);
    }

}
