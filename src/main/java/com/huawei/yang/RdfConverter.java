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

import static com.huawei.yang.RdfConverter.QUERY_KEYS.AUGMENTS;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.CHILD_STATEMENTS;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.CONTAINER_DETAILS;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.GROUPINGS;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.IDENTITIES;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.LEAF_DETAILS;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.MODULES;
import static com.huawei.yang.RdfConverter.QUERY_KEYS.TYPEDEFS;
import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class RdfConverter {

    public static final IRI SCHEMA_CONTEXT = iri("http://www.typefox.io/yang");
    public static final IRI SOURCE_CONTEXT = iri("http://www.typefox.io/yang/model");

    public static final IRI TARGET_SCHEMA_CONTEXT = iri("http://huawei.com/kg/def/yang");
    public static final IRI TARGET_CONTEXT = iri("http://huawei.com/kg/def/yang/model");

    public static final IRI PROPERTY_SOURCE = iri("http://huawei.com/kg/def/yang/hasSource");
    public static final IRI ENTITY_CONTAINER = iri("http://huawei.com/kg/def/yang/Container");
    public static final IRI ENTITY_LEAF = iri("http://huawei.com/kg/def/yang/Leaf");
    
    static abstract class QUERY_KEYS {
        public static final String MODULES = "modules";
        public static final String CHILD_STATEMENTS = "child-statements";
        public static final String CONTAINER_DETAILS = "container-details";
        public static final String LEAF_DETAILS = "leaf-details";
        public static final String IDENTITIES = "identities";
        public static final String TYPEDEFS = "typedefs";
        public static final String GROUPINGS = "groupings";
        public static final String AUGMENTS = "augments";
    }

    private final Model model = new DynamicModelFactory().createEmptyModel(); 
    private final Map<String, String> namedQueries = new HashMap<>();

    public RdfConverter() {
        init();
    }

    private void init() {
        namedQueries.put(MODULES, read("queries/modules.rq"));
        namedQueries.put(CHILD_STATEMENTS, read("queries/childStatements.rq"));
        namedQueries.put(CONTAINER_DETAILS, read("queries/containerDetails.rq"));
        namedQueries.put(LEAF_DETAILS, read("queries/leafDetails.rq"));
        namedQueries.put(IDENTITIES, read("queries/identities.rq"));
        namedQueries.put(TYPEDEFS, read("queries/typedefs.rq"));
        namedQueries.put(GROUPINGS, read("queries/groupings.rq"));
        namedQueries.put(AUGMENTS, read("queries/augments.rq"));
    }

    public void convert(String sourceFile, String targetFile) {
        System.out.printf("Converting '%s' file ...\n", sourceFile);
        loadModels(new File(sourceFile));
        Repository repository = new SailRepository(new MemoryStore());
        try (RepositoryConnection rc = repository.getConnection()) {
            rc.add(model);
            convertModules(rc);
            Model model = QueryResults.asModel(rc.getStatements(null, null, null, TARGET_CONTEXT));
            try (OutputStream out = new FileOutputStream(targetFile)) {
                RdfWriter.write(out, model);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.printf("File was created: %s\n", targetFile);
        } finally {
            repository.shutDown();
        }
    }

    private void loadModels(File sourceFile) {
        InputStream sourceSchemaInputStream = getClass().getClassLoader().getResourceAsStream("ontology/tyang.ttl");
        InputStream targetSchemaInputStream = getClass().getClassLoader().getResourceAsStream("ontology/yang.ttl");
        Repository repository = new SailRepository(new MemoryStore());
        try (RepositoryConnection rc = repository.getConnection()) {
            rc.add(sourceSchemaInputStream, RDFFormat.TURTLE, SCHEMA_CONTEXT);
            rc.add(sourceFile, SOURCE_CONTEXT);
            rc.add(targetSchemaInputStream, RDFFormat.TURTLE, TARGET_SCHEMA_CONTEXT);

            Model schemaModel = getModel(rc, SCHEMA_CONTEXT);
            model.addAll(schemaModel);
            Model sourceModel = getModel(rc, SOURCE_CONTEXT);
            model.addAll(sourceModel);
            Model targetSchemaModel = getModel(rc, TARGET_SCHEMA_CONTEXT);
            model.addAll(targetSchemaModel);
        } catch (RDFParseException e) {
            throw new RuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            repository.shutDown();
        }
    }

    private Model getModel(RepositoryConnection rc, IRI context) {
        RepositoryResult<Statement> statements = rc.getStatements(null, null, null, context);
        Model model = QueryResults.asModel(statements);
        return model;
    }

    private void convertModules(RepositoryConnection rc) {
        Model moduleModel = buildModules(rc);
        rc.add(moduleModel, TARGET_CONTEXT);
        Iterable<Statement> modules = moduleModel.getStatements(null, RDF.TYPE, null);
        for (Statement module : modules) {
            Optional<Literal> moduleName = Models.objectLiteral(moduleModel.getStatements(module.getSubject(), RDFS.LABEL, null));
            convertChildStatements(module, moduleName, moduleModel, rc);
            convertIdentities(module, moduleModel, rc);
            convertTypeDefs(module, moduleModel, rc);
            convertGroupings(module, moduleName, moduleModel, rc);
        }
        for (Statement module : modules) {
            Optional<Literal> moduleName = Models.objectLiteral(moduleModel.getStatements(module.getSubject(), RDFS.LABEL, null));
            convertAugments(module, moduleName, moduleModel, rc);
        }
    }

    void convertIdentities(Statement stmt, Model model, RepositoryConnection rc) {
        Model identityModel = buildModel(stmt, model, IDENTITIES, rc);
        rc.add(identityModel, TARGET_CONTEXT);
    }

    void convertAugments(Statement stmt, Optional<Literal> moduleName, Model model, RepositoryConnection rc) {
        Model augmentModel = buildModel(stmt, moduleName, model, AUGMENTS, rc);
        rc.add(augmentModel, TARGET_CONTEXT);
        Iterable<Statement> childStmts = augmentModel.getStatements(null, RDF.TYPE, null);
        for (Statement childStmt : childStmts) {
            enrichStatement(childStmt, augmentModel, rc);
            convertChildStatements(childStmt, moduleName, augmentModel, rc);
        }
    }

    void convertTypeDefs(Statement stmt, Model model, RepositoryConnection rc) {
        Model typeDefModel = buildModel(stmt, model, TYPEDEFS, rc);
        rc.add(typeDefModel, TARGET_CONTEXT);
    }

    void convertGroupings(Statement stmt, Optional<Literal> moduleName, Model model, RepositoryConnection rc) {
        Model groupingModel = buildModel(stmt, moduleName, model, GROUPINGS, rc);
        rc.add(groupingModel, TARGET_CONTEXT);
        Iterable<Statement> stmts = groupingModel.getStatements(null, RDF.TYPE, null);
        for (Statement childStmt : stmts) {
            enrichStatement(childStmt, groupingModel, rc);
            convertChildStatements(childStmt, moduleName, groupingModel, rc);
        }
    }

    void convertChildStatements(Statement parentStmt, Optional<Literal> moduleName, Model parentModel, RepositoryConnection rc) {
        Model model = buildChildStatements(parentStmt, moduleName, parentModel, rc);
        rc.add(model, TARGET_CONTEXT);
        Iterable<Statement> stmts = model.getStatements(null, RDF.TYPE, null);
        for (Statement stmt : stmts) {
            enrichStatement(stmt, model, rc);
            convertChildStatements(stmt, moduleName, model, rc);
        }
    }

    Model buildModules(RepositoryConnection rc) {
        GraphQuery graphQuery = rc.prepareGraphQuery(namedQueries.get(MODULES));
        GraphQueryResult result = graphQuery.evaluate();
        return QueryResults.asModel(result);
    }

    Model buildModel(Statement stmt, Model model, String namedQuery, RepositoryConnection rc) {
        return buildModel(stmt, Optional.empty(), model, namedQuery, rc);
    }

    Model buildModel(Statement stmt, Optional<Literal> moduleName, Model model, String namedQuery, RepositoryConnection rc) {
        Resource stmtIri = stmt.getSubject();
        Optional<IRI> sourceStmtIri = Models.objectIRI(model.getStatements(stmtIri, PROPERTY_SOURCE, null));
        GraphQuery graphQuery = rc.prepareGraphQuery(namedQueries.get(namedQuery));
        if (moduleName.isPresent()) {
            graphQuery.setBinding("_module_name", moduleName.get());
        }
        graphQuery.setBinding("_parent_stmt_iri", sourceStmtIri.get());
        graphQuery.setBinding("_parent_target_stmt_iri", stmtIri);
        GraphQueryResult result = graphQuery.evaluate();
        return QueryResults.asModel(result);
    }

    Model buildChildStatements(Statement stmt, Optional<Literal> moduleName, Model model, RepositoryConnection rc) {
        Resource stmtIri = stmt.getSubject();
        Optional<IRI> sourceStmt = Models.objectIRI(model.getStatements(stmtIri, PROPERTY_SOURCE, null));
        GraphQuery graphQuery = rc.prepareGraphQuery(namedQueries.get(CHILD_STATEMENTS));
        graphQuery.setBinding("_context_iri", SOURCE_CONTEXT);
        graphQuery.setBinding("_module_name", moduleName.get());
        graphQuery.setBinding("_parent_stmt_iri", sourceStmt.get());
        graphQuery.setBinding("_parent_target_stmt_iri", stmtIri);
        GraphQueryResult result = graphQuery.evaluate();
        return QueryResults.asModel(result);
    }

    Model buildContainerDetails(Statement stmt, Model model, RepositoryConnection rc) {
        GraphQuery graphQuery = rc.prepareGraphQuery(namedQueries.get(CONTAINER_DETAILS));
        Resource stmtIri = stmt.getSubject();
        Optional<IRI> sourceIri = Models.objectIRI(model.getStatements(stmtIri, PROPERTY_SOURCE, null));
        graphQuery.setBinding("_stmt_iri", sourceIri.get());
        graphQuery.setBinding("_target_stmt_iri", stmtIri);
        GraphQueryResult result = graphQuery.evaluate();
        return QueryResults.asModel(result);
    }

    Model buildLeafDetails(Statement stmt, Model model, RepositoryConnection rc) {
        GraphQuery graphQuery = rc.prepareGraphQuery(namedQueries.get(LEAF_DETAILS));
        Resource stmtIri = stmt.getSubject();
        Optional<IRI> sourceIri = Models.objectIRI(model.getStatements(stmtIri, PROPERTY_SOURCE, null));
        graphQuery.setBinding("_stmt_iri", sourceIri.get());
        graphQuery.setBinding("_target_stmt_iri", stmtIri);
        GraphQueryResult result = graphQuery.evaluate();
        return QueryResults.asModel(result);
    }

    void enrichStatement(Statement stmt, Model model, RepositoryConnection rc) {
        Optional<Model> outputModel = buildStatementDetails(stmt, model, rc);
        if (outputModel.isPresent()) {
            rc.add(outputModel.get(), TARGET_CONTEXT);
        }
    }

    Optional<Model> buildStatementDetails(Statement statement, Model model, RepositoryConnection rc) {
        Model outputModel = null;
        if (ENTITY_CONTAINER.equals(statement.getObject())) {
            outputModel = buildContainerDetails(statement, model, rc);
        } else if (ENTITY_LEAF.equals(statement.getObject())) {
            outputModel = buildLeafDetails(statement, model, rc);
        }
        return Optional.ofNullable(outputModel);
    }

    public static String asString(Model model) throws IOException {
        String out = null;
        try (OutputStream os = new ByteArrayOutputStream()) {
            Rio.write(model, os, RDFFormat.TURTLE);
            out = os.toString();
        } catch (IOException e) {
            throw e;
        }
        Model targetModel = null;
        try (InputStream is = new ByteArrayInputStream(out.getBytes())) {
            targetModel = Rio.parse(is, RDFFormat.TURTLE);
        }
        try (OutputStream os = new ByteArrayOutputStream()) {
            Rio.write(targetModel, os, RDFFormat.TURTLE);
            out = os.toString();
        } catch (IOException e) {
            throw e;
        }
        return out;
    }

    public String read(String resource) {
        try {
            return new String(getClass().getClassLoader().getResourceAsStream(resource).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Missed arguments");
            System.out.println("Usage: RdfConverter <sourceFile> <targetFile>");
            return;
        }

        String sourceFile = args[0];
        String targetFile = args[1];

        System.out.printf("sourceFile: %s\n", sourceFile);
        System.out.printf("targetFile: %s\n", targetFile);

        RdfConverter converter = new RdfConverter();
        converter.convert(sourceFile, targetFile);
    }

}
