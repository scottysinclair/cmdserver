package scott.javarun;

import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;

public class JavaRun {
	public void run(String text) throws Exception {
		File tempFile = File.createTempFile("jav", ".java");
		tempFile.deleteOnExit();
		String className = tempFile.getName();
		className = className.substring(0, className.length()-5);

		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)));
		out.println("import java.io.*;");
		out.println("import java.net.*;");
		out.println("import java.util.*;");
		out.println("public class " + className + "{");
		out.println("public static void main(String args[]) throws Exception {");
		out.println(text);
		out.println("}");
		out.println("}");
		out.flush();
		out.close();

		com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();
		String[] args = new String[] {"-classpath", System.getProperty("java.class.path"), "-d", tempFile.getParentFile().getCanonicalPath(), tempFile.getCanonicalPath() };
		StringWriter output = new StringWriter();
		int status = javac.compile(args, new PrintWriter(output));
		switch (status) {
        case 0:  // OK
            // Make the class file temporary as well
            File classFile = new File(tempFile.getParentFile(), className + ".class");
            classFile.deleteOnExit();

            try {
                // Try to access the class and run its main method
                URLClassLoader classLoader = new URLClassLoader( new URL[]{tempFile.getParentFile().toURL()});
                Class clazz = classLoader.loadClass( className );
                Method main = clazz.getMethod("main", new Class[] { String[].class });
                main.invoke(null, new Object[] { new String[0] });
            } catch (InvocationTargetException ex) {
                // Exception in the main method we just tried to run

                System.out.println("Exception in main: " + ex.getTargetException());
                ex.getTargetException().printStackTrace();
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
            break;
        default:
            System.out.println(output.toString());

		}
	}

	public static void main(String args[]) {
		try {
			JavaRun javarun = new JavaRun();
			System.out.print(">");
			LineNumberReader lin = new LineNumberReader( new InputStreamReader(System.in) );
			String input = lin.readLine();
			String last = null;
			StringBuffer code = new StringBuffer();
			while(input != null && !"QUIT".equalsIgnoreCase(input)) {
				if ("GO".equalsIgnoreCase(input) && code.length() > 0) {
					last = code.toString();
					javarun.run(last);
					code = new StringBuffer();
				}
				else if ("again".equalsIgnoreCase(input)) {
					javarun.run(last);
				}
				else {
					code.append(input);
				}
				System.out.print(">");
				input = lin.readLine();
			}
	 }
	 catch(Exception x) {
		 x.printStackTrace();
	 }
	}
}