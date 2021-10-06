import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import org.junit.Test;


public class ParseUnparseTests {
    JavaParser jp = new JavaParser();

    @Test public void testParsing() {
        String classCode = "class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n" +
                "}";
        ParseResult<CompilationUnit> res1 = jp.parse(classCode);
        if (!res1.isSuccessful()) {
            res1.getProblems().forEach(problem -> System.out.println(problem.getCause()));
            assertTrue("Shouldn't fail.", false);
        }
        CompilationUnit cu = res1.getResult().get();
        assertEquals(1, cu.getTypes().size());
        assertEquals("HelloWorld", cu.getType(0).getName().asString());
    }

    @Test public void testPartialParsing() {
        String classBodyCode =  "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n";
        ParseResult<BodyDeclaration> res2 = jp.parseBodyDeclaration(classBodyCode);
        res2.getProblems().forEach(problem -> System.out.println(problem.getCause()));
        assertTrue(res2.isSuccessful());

        BodyDeclaration classBody = res2.getResult().get();
        MethodDeclaration method = classBody.asMethodDeclaration();
        assertEquals("main", method.getName().asString());

        ParseResult<MethodDeclaration> res3 = jp.parseMethodDeclaration(classBodyCode);
        res3.getProblems().forEach(problem -> System.out.println(problem.getCause()));
        assertTrue(res3.isSuccessful());
    }

    @Test public void testASTtoString() {
        String classCode = "class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n" +
                "}";

        ParseResult<CompilationUnit> res1 = jp.parse(classCode);
        CompilationUnit cu = res1.getResult().get();

        assertEquals("class HelloWorld {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}\n", cu.toString());

        assertEquals("[public , static ]",
                cu.getType(0).asClassOrInterfaceDeclaration().getMember(0)
                        .asMethodDeclaration().getModifiers().toString());
    }
    @Test public void testModifyAndWrite1() {
        String classCode = "class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n" +
                "}";
        ParseResult<CompilationUnit> res1 = jp.parse(classCode);
        CompilationUnit cu = res1.getResult().get();
        cu.getType(0).setName("HelloWorld2");
        assertEquals("class HelloWorld2 {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}\n", cu.toString());

    }

    @Test public void testModifyAndWrite2() {
        String classCode = "class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n" +
                "}";
        ParseResult<CompilationUnit> res1 = jp.parse(classCode);
        CompilationUnit cu = res1.getResult().get();
        ClassOrInterfaceDeclaration classDecl = cu.getType(0).asClassOrInterfaceDeclaration();

        MethodDeclaration newFunction = jp.parseMethodDeclaration("public void foo(){}").getResult().get();

        classDecl.addMember(newFunction.clone()); //API docs recommend cloning nodes being moved around so parents aren't confused.

        assertEquals("class HelloWorld {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n\n" +
                "    public void foo() {\n" +
                "    }\n" +
                "}\n", cu.toString());

    }

    @Test public void testModifyAndWrite3() {
        String classCode = "class HelloWorld {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\"); \n" +
                "    }\n" +
                "}";
        ParseResult<CompilationUnit> res1 = jp.parse(classCode);
        CompilationUnit cu = res1.getResult().get();
        ClassOrInterfaceDeclaration classDecl = cu.getType(0).asClassOrInterfaceDeclaration();

        classDecl.accept(new ModifierVisitor<Void>() { //not entirely an in-place modification

            @Override public Visitable visit(MethodCallExpr call, Void arg) {
                if (call.getNameAsString().equals("println")) {
                    Expression newPrint = new BinaryExpr(
                            new ArrayAccessExpr(new NameExpr("args"),new IntegerLiteralExpr(0)),
                            new StringLiteralExpr("abc"),
                            BinaryExpr.Operator.PLUS
                    );
                    call.setArgument(0,newPrint);
                }
                return super.visit(call, arg);
            }
        },null);

        assertEquals("class HelloWorld {\n" +
                "\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(args[0] + \"abc\");\n" +
                "    }\n" +
                "}\n", cu.toString());

    }
}
