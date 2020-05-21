package margotua.examples;
/*
import net.steppschuh.markdowngenerator.MarkdownBuilder;
import net.steppschuh.markdowngenerator.text.TextBuilder;
import net.steppschuh.markdowngenerator.text.code.CodeBlock;

import java.util.Arrays;

import static net.steppschuh.markdowngenerator.Markdown.bold;
import static net.steppschuh.markdowngenerator.Markdown.italic;
import static net.steppschuh.markdowngenerator.Markdown.task;

public class Markdown {

    public static MarkdownBuilder createSimpleObj() {
        MarkdownBuilder builder = new TextBuilder()
                .heading("Markdown Builder")
                .append("Demonstrating: ")
                .append(bold("Bold Text"))
                .newParagraph()
                .beginList()
                .append("I should be an item")
                .append(italic("I should be an italic item"))
                .end()
                .newParagraph()
                .beginQuote()
                .append("I should be a quote").newLine()
                .append("I should still be a quote")
                .end()
                .newParagraph()
                .beginCodeBlock(CodeBlock.LANGUAGE_JAVA)
                .append("// I should be code").newLine()
                .append("dummyMethod(this);")
                .end()
                .newParagraph()
                .append("Over.");
        return builder;
    }

    public static String run(MarkdownBuilder input) {
        String out = input.toString();
        System.out.println(out);
        return out;
    }
/*
    public static void main(String[] args) {
        System.err.println("start = " + java.time.LocalTime.now());
        System.err.println(Arrays.toString(args));
        //Object list = createObj();
        Object list = createSimpleObj();
        System.err.println("object created = " + java.time.LocalTime.now());
        DeepSimpleJSONTainter tainter;
        if(args.length == 1 ) {
            tainter = new DeepSimpleJSONTainter(args[0]);
        } else {
            tainter = new DeepSimpleJSONTainter();
        }
        Object tainted = tainter.taint(list);
        System.err.println("tainted = " + java.time.LocalTime.now());
        run(tainted);
        System.err.println("done = " + java.time.LocalTime.now());
    }
}*/
