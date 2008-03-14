/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestJenaGraph.java 25624 2007-10-02 15:14:38Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.jena;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.BlankImpl;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.services.RelationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.hp.hpl.jena.rdf.model.Model;

@SuppressWarnings({ "unchecked", "IOResourceOpenedButNotSafelyClosed" })
public class TestJenaGraph extends NXRuntimeTestCase {

    private JenaGraph graph;

    private List<Statement> statements;

    private String namespace;

    private Resource doc1;

    private Resource doc2;

    private QNameResource isBasedOn;

    private QNameResource references;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deploy("jena-test-bundle.xml");
        RelationService service = (RelationService) Framework.getRuntime().getComponent(
                RelationService.NAME);
        Graph graph = service.getGraphByName("myrelations");
        assertNotNull(graph);
        assertEquals(JenaGraph.class, graph.getClass());
        this.graph = (JenaGraph) graph;
        this.statements = new ArrayList<Statement>();
        this.doc1 = new ResourceImpl(
                "http://www.ecm.org/uid/DOC200600013_02.01");
        this.doc2 = new ResourceImpl(
                "http://www.ecm.org/uid/DOC200600015_01.00");
        this.namespace = "http://purl.org/dc/terms/";
        this.isBasedOn = new QNameResourceImpl(this.namespace, "IsBasedOn");
        this.references = new QNameResourceImpl(this.namespace, "References");
        this.statements.add(new StatementImpl(doc2, isBasedOn, doc1));
        this.statements.add(new StatementImpl(
                doc1,
                references,
                new ResourceImpl(
                        "http://www.wikipedia.com/Enterprise_Content_Management")));
        this.statements.add(new StatementImpl(doc2, references,
                new LiteralImpl("NXRuntime")));
        Collections.sort(this.statements);
    }

    private String getTestFile() {
        String filePath = "test.rdf";
        return FileUtils.getResourcePathFromContext(filePath);
    }

    public void testGetGraph() {
        Model jenaGraph = this.graph.openGraph().getGraph();
        Map<?, ?> map = jenaGraph.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
    }

    public void testSetOptions() {
        Map<String, String> options = new HashMap<String, String>();
        options.put("backend", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("backend", "memory");
        graph.setOptions(options);

        options.put("databaseDoCompressUri", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("databaseDoCompressUri", "true");
        graph.setOptions(options);

        options.put("databaseTransactionEnabled", "dummy");
        try {
            graph.setOptions(options);
            fail("Should have raised IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        options.put("databaseTransactionEnabled", "false");
        graph.setOptions(options);
    }

    public void testSetNamespaces() {
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("dummy", "http://dummy");

        boolean forceReload = false;
        Model jenaGraph = graph.openGraph(forceReload).getGraph();
        Map<?, ?> map = jenaGraph.getNsPrefixMap();
        // old namespaces are kept
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
        assertNull(map.get("dummy"));

        graph.setNamespaces(namespaces);

        // not set yet on the graph, have to rebuild it
        jenaGraph = graph.openGraph(forceReload).getGraph();
        map = jenaGraph.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
        assertNull(map.get("dummy"));

        // rebuild graph
        forceReload = true;
        jenaGraph = graph.openGraph(forceReload).getGraph();
        map = jenaGraph.getNsPrefixMap();
        // old namespaces are still set on the named graph lots
        assertNull(map.get("rdf"));
        assertNull(map.get("dcterms"));
        assertEquals("http://dummy", map.get("dummy"));
    }

    public void testAdd() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
    }

    public void testRemove() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
        graph.remove(stmts);
        assertSame(2L, graph.size());
    }

    public void testGetStatements() {
        List<Statement> stmts = new ArrayList<Statement>();
        assertEquals(stmts, graph.getStatements());
        graph.add(statements);
        stmts = graph.getStatements();
        Collections.sort(stmts);
        assertEquals(statements, stmts);
    }

    public void testGetStatementsPattern() {
        List<Statement> expected = new ArrayList<Statement>();
        assertEquals(expected, graph.getStatements());
        graph.add(statements);

        List<Statement> stmts = graph.getStatements(new StatementImpl(null,
                null, null));
        Collections.sort(stmts);
        expected = statements;
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc1, null, null));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(null, references, null));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc1, references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management")));
        expected.add(new StatementImpl(doc2, references, new LiteralImpl(
                "NXRuntime")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(doc2, null, doc1));
        Collections.sort(stmts);
        expected = new ArrayList<Statement>();
        expected.add(new StatementImpl(doc2, isBasedOn, doc1));
        assertEquals(expected, stmts);

        // test with unknown nodes
        expected = new ArrayList<Statement>();
        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), new ResourceImpl("http://propertty"),
                new ResourceImpl("http://object")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), null, new LiteralImpl("literal")));
        assertEquals(expected, stmts);

        stmts = graph.getStatements(new StatementImpl(new ResourceImpl(
                "http://subject"), null, new BlankImpl("blank")));
        assertEquals(expected, stmts);
    }

    public void testGetSubjects() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getSubjects(references, new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management"));
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(references, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(null, doc1);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getSubjects(null, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        expected.add(doc2);
        Collections.sort(expected);
        assertEquals(expected, res);
    }

    public void testGetPredicates() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getPredicates(doc2, doc1);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(isBasedOn);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(doc2, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(isBasedOn);
        expected.add(references);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(null, doc1);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(isBasedOn);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getPredicates(null, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(isBasedOn);
        expected.add(references);
        Collections.sort(expected);
        assertEquals(expected, res);
    }

    public void testGetObject() {
        graph.add(statements);
        List<Node> expected;
        List<Node> res;

        res = graph.getObjects(doc2, isBasedOn);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(doc2, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(null, references);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management"));
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);

        res = graph.getObjects(null, null);
        Collections.sort(res);
        expected = new ArrayList<Node>();
        expected.add(doc1);
        expected.add(new ResourceImpl(
                "http://www.wikipedia.com/Enterprise_Content_Management"));
        expected.add(new LiteralImpl("NXRuntime"));
        Collections.sort(expected);
        assertEquals(expected, res);
    }

    public void testHasStatement() {
        graph.add(statements);
        assertFalse(graph.hasStatement(null));
        assertTrue(graph.hasStatement(new StatementImpl(doc2, isBasedOn, doc1)));
        assertFalse(graph.hasStatement(new StatementImpl(doc2, isBasedOn, doc2)));
        assertTrue(graph.hasStatement(new StatementImpl(doc2, isBasedOn, null)));
        assertFalse(graph.hasStatement(new StatementImpl(null, null, doc2)));
    }

    public void testHasResource() {
        graph.add(statements);
        assertFalse(graph.hasResource(null));
        assertTrue(graph.hasResource(doc1));
        assertFalse(graph.hasResource(new ResourceImpl("http://foo")));
    }

    public void testSize() {
        assertSame(0L, graph.size());
        List<Statement> stmts = new ArrayList<Statement>();
        stmts.add(new StatementImpl(doc1, isBasedOn, new LiteralImpl("foo")));
        graph.add(stmts);
        assertSame(1L, graph.size());
        graph.add(statements);
        assertSame(4L, graph.size());
    }

    public void testClear() {
        assertSame(0L, graph.size());
        graph.add(statements);
        assertSame(3L, graph.size());
        graph.clear();
        assertSame(0L, graph.size());
    }

    public void testQuery() {
        graph.add(statements);
        String queryString;
        queryString = "SELECT ?subj ?pred ?obj " + "WHERE {"
                + "      ?subj ?pred ?obj " + "       }";
        QueryResult res = graph.query(queryString, "sparql", null);
        assertSame(3, res.getCount());
        List<String> variableNames = new ArrayList<String>();
        variableNames.add("subj");
        variableNames.add("pred");
        variableNames.add("obj");
        assertEquals(variableNames, res.getVariableNames());

        queryString = "PREFIX dcterms: <http://purl.org/dc/terms/> "
                + "SELECT ?subj ?obj " + "WHERE {"
                + "      ?subj dcterms:References ?obj ." + "       }";
        res = graph.query(queryString, "sparql", null);
        assertSame(2, res.getCount());
        variableNames.remove("pred");
        assertEquals(variableNames, res.getVariableNames());
    }

    public void testRead() throws Exception {
        InputStream in = new FileInputStream(getTestFile());
        assertSame(0L, graph.size());
        graph.read(in, null, null);
        assertNotSame(0L, graph.size());
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    public void testReadPath() throws Exception {
        assertSame(0L, graph.size());
        graph.read(getTestFile(), null, null);
        assertNotSame(0L, graph.size());
        List<Statement> statements = graph.getStatements();
        Collections.sort(statements);
        // assertSame(statements.size(), this.statements.size());
        // for (int i = 0; i < statements.size(); i++) {
        // assertEquals(statements.get(i), this.statements.get(i));
        // }
        assertEquals(statements, this.statements);
    }

    public void testWrite() throws Exception {
        graph.add(statements);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        graph.write(out, null, null);
        InputStream written = new ByteArrayInputStream(out.toByteArray());
        InputStream expected = new FileInputStream(getTestFile());
        assertEquals(FileUtils.read(expected).replaceAll("\r?\n", ""),
                FileUtils.read(written).replaceAll("\r?\n", ""));
    }

    public void testWritePath() throws Exception {
        graph.add(statements);
        File file = File.createTempFile("test", ".rdf");
        String path = file.getPath();
        graph.write(path, null, null);
        InputStream written = new FileInputStream(new File(path));
        InputStream expected = new FileInputStream(getTestFile());
        assertEquals(FileUtils.read(expected).replaceAll("\r?\n", ""),
                FileUtils.read(written).replaceAll("\r?\n", ""));
    }

    // XXX AT: test serialization of the graph because the RelationServiceBean
    // will attempt to keep references to graphs it manages.
    public void testSerialization() throws Exception {
        graph.add(statements);

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(graph);
        oos.close();
        assertTrue(out.toByteArray().length > 0);

        // deserialize
        byte[] pickled = out.toByteArray();
        InputStream in = new ByteArrayInputStream(pickled);
        ObjectInputStream ois = new ObjectInputStream(in);
        Object o = ois.readObject();

        JenaGraph newGraph = (JenaGraph) o;

        // new graph has same properties than old one but statements are lost
        // because they were stored in a memory graph
        assertSame(0L, newGraph.size());
        Model newModel = newGraph.openGraph().getGraph();
        Map<?, ?> map = newModel.getNsPrefixMap();
        assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                map.get("rdf"));
        assertEquals("http://purl.org/dc/terms/", map.get("dcterms"));
    }
}