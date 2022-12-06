package com.datasectech.jobanalyzer.codebuilder;

import java.security.SecureRandom;

/**
 * Generate compilable code from user submitted snippet
 *
 * @author Tasneem Yasmeen
 */
public class CodeGenerator {
    private static final SecureRandom RANDOM_NUMBER_GENERATOR = new SecureRandom();

    public static final String PACKAGE_NAME = "sdl";

    protected static String generateClassFromTemplate(String className, String codeSnippet) {
        return "package " + PACKAGE_NAME + "\n" +
                "\n" +
                "import org.apache.spark.SparkContext._\n" +
                "import org.apache.spark.sql.functions._\n" +
                "import org.apache.spark.sql.functions._\n" +
                "import org.apache.spark.sql.SparkSession\n" +
                "\n" +
                "object " + className + " {\n" +
                "\n" +
                "  def main(args: Array[String]): Unit = {\n" +
                "    val spark = SparkSession\n" +
                "      .builder\n" +
                "      .appName(\"SparkSnippetAnalyzer\")\n" +
                "      .getOrCreate()\n" +
                "    val sc = spark.sparkContext\n" +
                "    val sqlContext = spark.sqlContext\n" +
                "    import spark.implicits._\n" +
                "    import spark.sql\n" +
                codeSnippet + "\n" +
                "  }\n" +
                "}\n";
    }

    public static String generateClass(String codeSnippet, String invocationId) {
        String className = "Main" + invocationId;
        return generateClassFromTemplate(className, codeSnippet);
    }

    public static String generateRandomInvocationId() {
        return String.valueOf(Math.abs(RANDOM_NUMBER_GENERATOR.nextInt(899999) + 100000));
    }
}
