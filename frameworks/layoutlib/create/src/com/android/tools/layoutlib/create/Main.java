/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.layoutlib.create;

import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.io.Files;


/**
 * Entry point for the layoutlib_create tool.
 * <p/>
 * The tool does not currently rely on any external configuration file.
 * Instead the configuration is mostly done via the {@link CreateInfo} class.
 * <p/>
 * For a complete description of the tool and its implementation, please refer to
 * the "README.txt" file at the root of this project.
 * <p/>
 * For a quick test, invoke this as follows:
 * <pre>
 * $ make layoutlib
 * </pre>
 * which does:
 * <pre>
 * $ make layoutlib_create &lt;bunch of framework jars&gt;
 * $ java -jar out/host/linux-x86/framework/layoutlib_create.jar \
 *        out/host/common/obj/JAVA_LIBRARIES/temp_layoutlib_intermediates/javalib.jar \
 *        out/target/common/obj/JAVA_LIBRARIES/core-libart_intermediates/classes.jar \
 *        out/target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar
 * </pre>
 */
public class Main {

    private static class Options {
        private boolean listAllDeps = false;
        private boolean listOnlyMissingDeps = false;
        private boolean createStubLib = false;
    }

    public static final int ASM_VERSION = Opcodes.ASM9;

    private static final Options sOptions = new Options();

    public static void main(String[] args) {

        Log log = new Log();

        ArrayList<String> osJarPath = new ArrayList<>();
        String[] osDestJar = { null };

        if (!processArgs(log, args, osJarPath, osDestJar)) {
            log.error("Usage: layoutlib_create [-v] [--create-stub] output.jar input.jar ...");
            log.error("Usage: layoutlib_create [-v] [--list-deps|--missing-deps] input.jar ...");
            System.exit(1);
        }

        if (sOptions.listAllDeps || sOptions.listOnlyMissingDeps) {
            System.exit(listDeps(osJarPath, log));

        } else {
            System.exit(createLayoutLib(osDestJar[0], osJarPath, log));
        }


        System.exit(1);
    }

    private static int createLayoutLib(String osDestJar, ArrayList<String> osJarPath, Log log) {
        log.info("Output: %1$s", osDestJar);
        for (String path : osJarPath) {
            log.info("Input :      %1$s", path);
        }

        try {
            ICreateInfo info = new CreateInfo();
            AsmGenerator agen = new AsmGenerator(log, info);

            AsmAnalyzer aa = new AsmAnalyzer(log, osJarPath,
                    new String[] {                          // derived from
                        "android.app.Fragment",
                        "android.view.View",
                    },
                    new String[] {                          // include classes
                        "android.*", // for android.R
                        "android.annotation.NonNull",       // annotations
                        "android.annotation.Nullable",      // annotations
                        "android.app.ApplicationErrorReport", // needed for Glance LazyList
                        "android.app.DatePickerDialog",     // b.android.com/28318
                        "android.app.TimePickerDialog",     // b.android.com/61515
                        "android.content.*",
                        "android.content.res.*",
                        "android.database.ContentObserver", // for Digital clock
                        "android.graphics.*",
                        "android.graphics.drawable.**",
                        "android.icu.**",                   // needed by LayoutLib
                        "android.os.*",  // for android.os.Handler
                        "android.os.ext.*", // for android.os.ext.SdkExtensions, needed by Compose
                        "android.pim.*", // for datepicker
                        "android.preference.*",
                        "android.service.wallpaper.*",      // needed for Wear OS watch faces
                        "android.text.**",
                        "android.util.*",
                        "android.view.*",
                        "android.widget.*",
                        "com.android.i18n.phonenumbers.*",  // for TextView with autolink attribute
                        "com.android.internal.R**",
                        "com.android.internal.graphics.drawable.AnimationScaleListDrawable",
                        "com.android.internal.transition.EpicenterTranslateClipReveal",
                        "com.android.internal.util.*",
                        "com.android.internal.view.menu.ActionMenu",
                        "com.android.internal.widget.*",
                        "com.android.systemui.monet.*",     // needed for dynamic theming
                        "com.google.android.apps.common.testing.accessibility.**",
                        "com.google.android.libraries.accessibility.**",
                        "libcore.icu.ICU",                  // needed by ICU_Delegate in LayoutLib
                        "libcore.io.*",                     // needed to load /usr/share/zoneinfo
                        "org.apache.harmony.xml.*",
                    },
                    info.getExcludedClasses(),
                    new String[] {
                        "com/android/i18n/phonenumbers/data/*",
                        "android/icu/impl/data/**"
                    }, info.getMethodReplacers());
            agen.setAnalysisResult(aa.analyze());

            Map<String, byte[]> outputClasses = agen.generate();
            JarUtil.createJar(new FileOutputStream(osDestJar), outputClasses);
            log.info("Created JAR file %s", osDestJar);

            if (sOptions.createStubLib) {
                File osDestJarFile = new File(osDestJar);
                String extension = Files.getFileExtension(osDestJarFile.getName());
                if (!extension.isEmpty()) {
                    extension = '.' + extension;
                }
                String stubDestJarFile = osDestJarFile.getParent() + File.separatorChar +
                        Files.getNameWithoutExtension(osDestJarFile.getName()) + "-stubs" +
                        extension;

                Map<String, byte[]> toStubClasses = outputClasses.entrySet().stream().filter(entry -> entry.getKey().startsWith("android/")).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                JarUtil.createJar(new FileOutputStream(stubDestJarFile), toStubClasses,
                        input -> StubClassAdapter.stubClass(log, input));
                log.info("Created stub JAR file %s", stubDestJarFile);
            }

            // Throw an error if any class failed to get renamed by the generator
            //
            // IMPORTANT: if you're building the platform and you get this error message,
            // it means the renameClasses[] array in AsmGenerator needs to be updated: some
            // class should have been renamed but it was not found in the input JAR files.
            Set<String> notRenamed = agen.getClassesNotRenamed();
            if (notRenamed.size() > 0) {
                // (80-column guide below for error formatting)
                // 01234567890123456789012345678901234567890123456789012345678901234567890123456789
                log.error(
                  "ERROR when running layoutlib_create: the following classes are referenced\n" +
                  "by tools/layoutlib/create but were not actually found in the input JAR files.\n" +
                  "This may be due to some platform classes having been renamed.");
                for (String fqcn : notRenamed) {
                    log.error("- Class not found: %s", fqcn.replace('/', '.'));
                }
                for (String path : osJarPath) {
                    log.info("- Input JAR : %1$s", path);
                }
                return 1;
            }

            return 0;
        } catch (IOException e) {
            log.exception(e, "Failed to load jar");
        }

        return 1;
    }

    private static int listDeps(ArrayList<String> osJarPath, Log log) {
        DependencyFinder df = new DependencyFinder(log);
        try {
            List<Map<String, Set<String>>> result = df.findDeps(osJarPath);
            if (sOptions.listAllDeps) {
                df.printAllDeps(result);
            } else if (sOptions.listOnlyMissingDeps) {
                df.printMissingDeps(result);
            }
        } catch (IOException e) {
            log.exception(e, "Failed to load jar");
        }

        return 0;
    }

    /**
     * Returns true if args where properly parsed.
     * Returns false if program should exit with command-line usage.
     * <p/>
     * Note: the String[0] is an output parameter wrapped in an array, since there is no
     * "out" parameter support.
     */
    private static boolean processArgs(Log log, String[] args,
            ArrayList<String> osJarPath, String[] osDestJar) {
        boolean needs_dest = true;
        for (String s : args) {
            if (s.equals("-v")) {
                log.setVerbose(true);
            } else if (s.equals("--list-deps")) {
                sOptions.listAllDeps = true;
                needs_dest = false;
            } else if (s.equals("--missing-deps")) {
                sOptions.listOnlyMissingDeps = true;
                needs_dest = false;
            } else if (s.equals("--create-stub")) {
                sOptions.createStubLib = true;
            } else if (!s.startsWith("-")) {
                if (needs_dest && osDestJar[0] == null) {
                    osDestJar[0] = s;
                } else {
                    osJarPath.add(s);
                }
            } else {
                log.error("Unknown argument: %s", s);
                return false;
            }
        }

        if (osJarPath.isEmpty()) {
            log.error("Missing parameter: path to input jar");
            return false;
        }
        if (needs_dest && osDestJar[0] == null) {
            log.error("Missing parameter: path to output jar");
            return false;
        }

        return true;
    }
}
