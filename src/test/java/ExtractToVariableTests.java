import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class ExtractToVariableTests {
    final String code = "public Graph makeGraph(Edge[] edges) {\n" +
            "      Graph graph = new HashMap<>(edges.length);\n" +
            " \n" +
            "      //one pass to find all vertices\n" +
            "      for (Edge e : edges) {\n" +
            "         if (!graph.containsKey(e.v1)) graph.put(e.v1, new Vertex(e.v1));\n" +
            "         if (!graph.containsKey(e.v2)) graph.put(e.v2, new Vertex(e.v2));\n" +
            "      }\n" +
            " \n" +
            "      //another pass to set neighbouring vertices\n" +
            "      for (Edge e : edges) {\n" +
            "         graph.get(e.v1).neighbours.put(graph.get(e.v2), e.dist);\n" +
            "         //graph.get(e.v2).neighbours.put(graph.get(e.v1), e.dist); // also do this for an undirected graph\n" +
            "      }\n" +
            "   }";
    JavaParser parser = new JavaParser();
    private MethodDeclaration methodAST;

    @Before public void reParse() {
        methodAST = parser.parseMethodDeclaration(code).getResult().get();
    }

    @Test public void testExtractSimpleExpr() {
        boolean changed = Refactor.extract(methodAST,
                new Range(new Position(2,35), new Position(2,46)),
                "init",
                new PrimitiveType(PrimitiveType.Primitive.INT));
        assertTrue(changed);
        assertEquals(
            "public Graph makeGraph(Edge[] edges) {\n" +
            "    int init = edges.length;\n" +
            "    Graph graph = new HashMap<>(init);\n" +
            "    // one pass to find all vertices\n" +
            "    for (Edge e : edges) {\n" +
            "        if (!graph.containsKey(e.v1))\n" +
            "            graph.put(e.v1, new Vertex(e.v1));\n" +
            "        if (!graph.containsKey(e.v2))\n" +
            "            graph.put(e.v2, new Vertex(e.v2));\n" +
            "    }\n" +
            "    // another pass to set neighbouring vertices\n" +
            "    for (Edge e : edges) {\n" +
            "        graph.get(e.v1).neighbours.put(graph.get(e.v2), e.dist);\n" +
            "        // graph.get(e.v2).neighbours.put(graph.get(e.v1), e.dist); // also do this for an undirected graph\n" +
            "    }\n" +
            "}",
            methodAST.toString()
        );
    }
    @Test public void testExtractInCondition() {
        boolean changed = Refactor.extract(methodAST,
                new Range(new Position(6,56), new Position(6,71)),
                "newVertex",
                new ClassOrInterfaceType("Vertex"));
        assertTrue(changed);
        assertEquals(
        "public Graph makeGraph(Edge[] edges) {\n" +
                "    Graph graph = new HashMap<>(edges.length);\n" +
                "    // one pass to find all vertices\n" +
                "    for (Edge e : edges) {\n" +
                "        if (!graph.containsKey(e.v1)) {\n" +
                "            Vertex newVertex = new Vertex(e.v1);\n" +
                "            graph.put(e.v1, newVertex);\n" +
                "        }\n" +
                "        if (!graph.containsKey(e.v2))\n" +
                "            graph.put(e.v2, new Vertex(e.v2));\n" +
                "    }\n" +
                "    // another pass to set neighbouring vertices\n" +
                "    for (Edge e : edges) {\n" +
                "        graph.get(e.v1).neighbours.put(graph.get(e.v2), e.dist);\n" +
                "        // graph.get(e.v2).neighbours.put(graph.get(e.v1), e.dist); // also do this for an undirected graph\n" +
                "    }\n" +
                "}",
                methodAST.toString()
        );
    }

    @Test public void testAlreadyAVariable() {
        boolean changed = Refactor.extract(methodAST,
                new Range(new Position(2,21), new Position(2,47)),
                "init",
                new ClassOrInterfaceType("Graph"));

        assertFalse(changed);
        assertEquals(parser.parseMethodDeclaration(code).getResult().get().toString(),methodAST.toString());
    }
    @Test public void testExtractBadRange() {
        boolean changed = Refactor.extract(methodAST,
                new Range(new Position(2,24), new Position(2,47)),
                "init",
                new PrimitiveType(PrimitiveType.Primitive.BOOLEAN));

        assertFalse(changed);
        assertEquals(parser.parseMethodDeclaration(code).getResult().get().toString(),methodAST.toString());
    }
}
