// Copyright © 2013-2017 Esko Luontola and other Retrolambda contributors
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.retrolambda;

import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import static net.orfjackal.retrolambda.api.RetrolambdaApi.*;

public class SystemPropertiesConfig implements Config {

    private static final List<String> requiredProperties = new ArrayList<>();
    private static final Map<String, String> alternativeProperties = new HashMap<>();
    private static final List<String> propertiesHelp = new ArrayList<>();

    private final Properties p;

    public SystemPropertiesConfig(Properties p) {
        this.p = p;
    }

    public boolean isFullyConfigured() {
        return hasAllRequiredProperties();
    }

    private boolean hasAllRequiredProperties() {
        for (String requiredParameter : requiredProperties) {
            if (!isConfigured(requiredParameter)) {
                return false;
            }
        }
        return true;
    }

    private boolean isConfigured(String parameter) {
        if (p.getProperty(parameter) != null) {
            return true;
        }
        for (Map.Entry<String, String> alt : alternativeProperties.entrySet()) {
            if (alt.getValue().equals(parameter) &&
                    p.getProperty(alt.getKey()) != null) {
                return true;
            }
        }
        return false;
    }


    // bytecode version

    static {
        optionalParameterHelp(BYTECODE_VERSION,
                "Major version number for the generated bytecode. For a list, see",
                "offset 7 at http://en.wikipedia.org/wiki/Java_class_file#General_layout",
                "Default value is " + Opcodes.V1_7 + " (i.e. Java 7)");
    }

    @Override
    public int getBytecodeVersion() {
        return Integer.parseInt(p.getProperty(BYTECODE_VERSION, "" + Opcodes.V1_7));
    }


    // default methods

    static {
        optionalParameterHelp(DEFAULT_METHODS,
                "Whether to backport default methods and static methods on interfaces.",
                "LIMITATIONS: All backported interfaces and all classes which implement",
                "them or call their static methods must be backported together,",
                "with one execution of Retrolambda.",
                "Disabled by default. Enable by setting to \"true\"");

    }

    @Override
    public boolean isDefaultMethodsEnabled() {
        return Boolean.parseBoolean(p.getProperty(DEFAULT_METHODS, "false"));
    }


    // input dir

    static {
        requiredParameterHelp(INPUT_DIR,
                "Input directory from where the original class files are read.");
    }

    @Override
    public Path getInputDir() {
        String inputDir = p.getProperty(INPUT_DIR);
        if (inputDir != null) {
            return Paths.get(inputDir);
        }
        throw new IllegalArgumentException("Missing required property: " + INPUT_DIR);
    }


    // output dir

    static {
        optionalParameterHelp(OUTPUT_DIR,
                "Output directory into where the generated class files are written.",
                "Defaults to same as " + INPUT_DIR);
    }

    @Override
    public Path getOutputDir() {
        String outputDir = p.getProperty(OUTPUT_DIR);
        if (outputDir != null) {
            return Paths.get(outputDir);
        }
        return getInputDir();
    }


    // classpath

    static {
        requiredParameterHelp(CLASSPATH,
                "Classpath containing the original class files and their dependencies.",
                "Uses ; or : as the path separator, see java.io.File#pathSeparatorChar");
        alternativeParameterHelp(CLASSPATH_FILE, CLASSPATH,
                "File listing the classpath entries.",
                "Alternative to " + CLASSPATH + " for avoiding the command line",
                "length limit. The file must list one file per line with UTF-8 encoding.");
    }

    @Override
    public List<Path> getClasspath() {
        String classpath = p.getProperty(CLASSPATH);
        if (classpath != null) {
            return parsePathList(classpath);
        }
        String classpathFile = p.getProperty(CLASSPATH_FILE);
        if (classpathFile != null) {
            return readPathList(Paths.get(classpathFile));
        }
        throw new IllegalArgumentException("Missing required property: " + CLASSPATH);
    }

    private static List<Path> parsePathList(String paths) {
        return Stream.of(paths.split(File.pathSeparator))
                .filter(path -> !path.isEmpty())
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    private static List<Path> readPathList(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .filter(line -> !line.isEmpty())
                    .map(Paths::get)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file, e);
        }
    }


    // incremental files

    static {
        optionalParameterHelp(INCLUDED_FILES,
                "List of files to process, instead of processing all files.",
                "This is useful for a build tool to support incremental compilation.",
                "Uses ; or : as the path separator, see java.io.File#pathSeparatorChar");
        alternativeParameterHelp(INCLUDED_FILES_FILE, INCLUDED_FILES,
                "File listing the files to process, instead of processing all files.",
                "Alternative to " + INCLUDED_FILES + " for avoiding the command line",
                "length limit. The file must list one file per line with UTF-8 encoding.");
    }

    @Override
    public List<Path> getIncludedFiles() {
        String files = p.getProperty(INCLUDED_FILES);
        if (files != null) {
            return parsePathList(files);
        }
        String filesFile = p.getProperty(INCLUDED_FILES_FILE);
        if (filesFile != null) {
            return readPathList(Paths.get(filesFile));
        }
        return null;
    }


    // useJavac8ReadLabelHack

    static {
        optionalParameterHelp(JAVAC_HACKS,
                "Attempts to fix javac bugs (type-annotation emission for local variables).",
                "Disabled by default. Enable by setting to \"true\"");

    }

    @Override
    public boolean isJavacHacksEnabled() {
        return Boolean.parseBoolean(p.getProperty(JAVAC_HACKS, "false"));
    }


    // quiet

    static {
        optionalParameterHelp(QUIET,
                "Reduces the amount of logging.",
                "Disabled by default. Enable by setting to \"true\"");

    }

    @Override
    public boolean isQuiet() {
        return Boolean.parseBoolean(p.getProperty(QUIET, "false"));
    }


    // help

    public String getHelp() {
        String options = requiredProperties.stream()
                .map(key -> "-D" + key + "=?")
                .reduce((a, b) -> a + " " + b)
                .get();
        return "Usage: java " + options + " [-javaagent:retrolambda.jar] -jar retrolambda.jar\n" +
                "\n" +
                "Retrolambda takes Java 8 classes and backports lambda expressions and\n" +
                "some other language features to work on Java 7, 6 or 5.\n" +
                "Web site: https://github.com/luontola/retrolambda\n" +
                "\n" +
                "Copyright (c) 2013-2017  Esko Luontola and other Retrolambda contributors\n" +
                "This software is released under the Apache License 2.0.\n" +
                "The license text is at http://www.apache.org/licenses/LICENSE-2.0\n" +
                "\n" +
                "Configurable system properties:\n" +
                "\n" +
                propertiesHelp.stream().reduce((a, b) -> a + "\n" + b).get() +
                "\n" +
                "If the Java agent is used, then Retrolambda will use it to capture the\n" +
                "lambda classes generated by Java. Otherwise Retrolambda will hook into\n" +
                "Java's internal lambda dumping API, which is more susceptible to suddenly\n" +
                "stopping to work between Java releases.\n";
    }

    private static void requiredParameterHelp(String key, String... lines) {
        requiredProperties.add(key);
        propertiesHelp.add(formatPropertyHelp(key, "required", lines));
    }

    private static void alternativeParameterHelp(String key, String replaces, String... lines) {
        alternativeProperties.put(key, replaces);
        propertiesHelp.add(formatPropertyHelp(key, "alternative", lines));
    }

    private static void optionalParameterHelp(String key, String... lines) {
        propertiesHelp.add(formatPropertyHelp(key, "", lines));
    }

    private static String formatPropertyHelp(String key, String tag, String... lines) {
        tag = tag.isEmpty() ? "" : " (" + tag + ")";
        String help = "  " + key + tag + "\n";
        for (String line : lines) {
            help += "      " + line + "\n";
        }
        return help;
    }
}
