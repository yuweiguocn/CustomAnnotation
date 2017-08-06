package io.github.yuweiguocn.processor;

import com.google.auto.service.AutoService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import io.github.yuweiguocn.annotation.CustomAnnotation;

@AutoService(Processor.class)
public class CustomProcessor extends AbstractProcessor {
    public static final String CUSTOM_ANNOTATION = "yuweiguoCustomAnnotation";
    private Filer filer;
    private Messager messager;
    private List<String> result = new ArrayList<>();
    private int round;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new LinkedHashSet<String>();
        annotataions.add(CustomAnnotation.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new LinkedHashSet<String>();
        options.add(CUSTOM_ANNOTATION);
        return options;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            String resultPath = processingEnv.getOptions().get(CUSTOM_ANNOTATION);
            if (resultPath == null) {
                messager.printMessage(Diagnostic.Kind.ERROR, "No option " + CUSTOM_ANNOTATION +
                        " passed to annotation processor");
                return false;
            }

            round++;
            messager.printMessage(Diagnostic.Kind.NOTE, "round " + round + " process over " + roundEnv.processingOver());
            Iterator<? extends TypeElement> iterator = annotations.iterator();
            while (iterator.hasNext()) {
                messager.printMessage(Diagnostic.Kind.NOTE, "name is " + iterator.next().getSimpleName().toString());
            }

            if (roundEnv.processingOver()) {
                if (!annotations.isEmpty()) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Unexpected processing state: annotations still available after processing over");
                    return false;
                }
            }

            if (annotations.isEmpty()) {
                return false;
            }

            for (Element element : roundEnv.getElementsAnnotatedWith(CustomAnnotation.class)) {
                if (element.getKind() != ElementKind.METHOD) {
                    messager.printMessage(
                            Diagnostic.Kind.ERROR,
                            String.format("Only methods can be annotated with @%s", CustomAnnotation.class.getSimpleName()),
                            element);
                    return true; // 退出处理
                }

                if (!element.getModifiers().contains(Modifier.PUBLIC)) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be public", element);
                    return true;
                }

                ExecutableElement execElement = (ExecutableElement) element;
                TypeElement classElement = (TypeElement) execElement.getEnclosingElement();
                result.add(classElement.getSimpleName().toString() + "#" + execElement.getSimpleName().toString());
            }
            if (!result.isEmpty()) {
                generateFile(resultPath);
            } else {
                messager.printMessage(Diagnostic.Kind.WARNING, "No @CustomAnnotation annotations found");
            }
            result.clear();
        } catch (Exception e) {
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.ERROR, "Unexpected error in CustomProcessor: " + e);
        }
        return true;
    }

    private void generateFile(String path) {
        BufferedWriter writer = null;
        try {
            JavaFileObject sourceFile = filer.createSourceFile(path);
            int period = path.lastIndexOf('.');
            String myPackage = period > 0 ? path.substring(0, period) : null;
            String clazz = path.substring(period + 1);
            writer = new BufferedWriter(sourceFile.openWriter());
            if (myPackage != null) {
                writer.write("package " + myPackage + ";\n\n");
            }
            writer.write("import java.util.ArrayList;\n");
            writer.write("import java.util.List;\n\n");
            writer.write("/** This class is generated by CustomProcessor, do not edit. */\n");
            writer.write("public class " + clazz + " {\n");
            writer.write("    private static final List<String> ANNOTATIONS;\n\n");
            writer.write("    static {\n");
            writer.write("        ANNOTATIONS = new ArrayList<>();\n\n");
            writeMethodLines(writer);
            writer.write("    }\n\n");
            writer.write("    public static List<String> getAnnotations() {\n");
            writer.write("        return ANNOTATIONS;\n");
            writer.write("    }\n\n");
            writer.write("}\n");
        } catch (IOException e) {
            throw new RuntimeException("Could not write source for " + path, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //Silent
                }
            }
        }
    }

    private void writeMethodLines(BufferedWriter writer) throws IOException {
        for (int i = 0; i < result.size(); i++) {
            writer.write("        ANNOTATIONS.add(\"" + result.get(i) + "\");\n");
        }
    }

}
