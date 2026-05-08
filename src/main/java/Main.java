public class Main {
    public static void main(String[] args) throws Exception {

        String source = BabyCobolParserUtils.readResource("/examples/test.babycob");

        String processedSource = BabyCobolParserUtils.preprocess(source);

        String tree = BabyCobolParserUtils.parseTree(processedSource);

        System.out.println("<== PROCESSED CODE ==>");
        System.out.println(processedSource);

        System.out.println("\n<== PARSE TREE ==>");
        System.out.println(tree);
    }
}