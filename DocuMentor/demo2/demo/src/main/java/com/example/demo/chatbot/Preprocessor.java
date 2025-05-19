package com.example.demo.chatbot;
import dev.langchain4j.data.document.Document;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import java.util.Properties;
import java.util.stream.Collectors;
public class Preprocessor {
    private static final StanfordCoreNLP pipeline;
    static {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
    }
    public static String Preprocess(Document content) {
        if (content == null || content.text() == null || content.text().isBlank()) {
            return "";
        }
        CoreDocument coreDocument = new CoreDocument(content.text());
        pipeline.annotate(coreDocument);


        return coreDocument.sentences().stream()
                .map(sentence -> sentence.tokens().stream()
                        .map(CoreLabel::lemma)
                        .collect(Collectors.joining(" ")))
                .map(s -> s.toLowerCase().replaceAll("\\s+", " ").trim())
                .collect(Collectors.joining(" "));
    }
}
