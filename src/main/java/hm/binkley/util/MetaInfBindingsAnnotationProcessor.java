/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>.
 */

package hm.binkley.util;

import org.kohsuke.MetaInfServices;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static javax.lang.model.SourceVersion.RELEASE_6;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * {@code MetaInfBindingsAnnotationProcessor} <b>needs documentation</b>.
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Needs documentation.
 */
@MetaInfServices(Processor.class)
@SuppressWarnings({"Since15"})
@SupportedAnnotationTypes("hm.binkley.util.MetaInfBindings")
@SupportedSourceVersion(RELEASE_6)
public class MetaInfBindingsAnnotationProcessor
        extends AbstractProcessor {
    private static final String PREFIX = "META-INF/bindings/";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
            final RoundEnvironment roundEnv) {
        if (roundEnv.processingOver())
            return false;

        final Map<String, Set<String>> services = new HashMap<String, Set<String>>();

        final Elements elements = processingEnv.getElementUtils();

        // discover services from the current compilation sources
        for (final Element e : roundEnv.getElementsAnnotatedWith(MetaInfBindings.class)) {
            final MetaInfBindings a = e.getAnnotation(MetaInfBindings.class);
            if (a == null)
                continue; // input is malformed, ignore
            if (!e.getKind().isClass() && !e.getKind().isInterface())
                continue; // ditto
            final TypeElement type = (TypeElement) e;
            final TypeElement contract = getContract(type, a);
            if (contract == null)
                continue; // error should have already been reported

            final String cn = elements.getBinaryName(contract).toString();
            Set<String> v = services.get(cn);
            if (v == null)
                services.put(cn, v = new TreeSet<String>());
            v.add(elements.getBinaryName(type).toString());
        }

        // also load up any existing values, since this compilation may be partial
        final Filer filer = processingEnv.getFiler();
        for (final Map.Entry<String, Set<String>> e : services.entrySet())
            try {
                final String contract = e.getKey();
                final FileObject f = filer.getResource(CLASS_OUTPUT, "", PREFIX + contract);
                final BufferedReader r = new BufferedReader(
                        new InputStreamReader(f.openInputStream(), UTF8));
                String line;
                while ((line = r.readLine()) != null) {
                    e.getValue().add(line);
                }
                r.close();
            } catch (final FileNotFoundException x) {
                // doesn't exist
            } catch (final IOException x) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "Failed to load existing service definition files: " + x);
            }

        // now write them back out
        for (final Map.Entry<String, Set<String>> e : services.entrySet())
            try {
                final String contract = e.getKey();
                processingEnv.getMessager().printMessage(Kind.NOTE, "Writing " + PREFIX + contract);
                final FileObject f = filer.createResource(CLASS_OUTPUT, "", PREFIX + contract);
                final PrintWriter pw = new PrintWriter(
                        new OutputStreamWriter(f.openOutputStream(), UTF8));
                for (final String value : e.getValue()) {
                    pw.println(value);
                }
                pw.close();
            } catch (final IOException x) {
                processingEnv.getMessager()
                        .printMessage(Kind.ERROR, "Failed to write service definition files: " + x);
            }

        return false;
    }

    private TypeElement getContract(final TypeElement type, final MetaInfBindings a) {
        // explicitly specified?
        try {
            a.value();
            throw new AssertionError();
        } catch (final MirroredTypeException e) {
            final TypeMirror m = e.getTypeMirror();
            if (m.getKind() == TypeKind.VOID) {
                // contract inferred from the signature
                final boolean hasBaseClass = type.getSuperclass().getKind() != TypeKind.NONE
                        && !isObject(type.getSuperclass());
                final boolean hasInterfaces = !type.getInterfaces().isEmpty();
                if (hasBaseClass ^ hasInterfaces) {
                    if (hasBaseClass)
                        return (TypeElement) ((DeclaredType) type.getSuperclass()).asElement();
                    return (TypeElement) ((DeclaredType) type.getInterfaces().get(0)).asElement();
                }

                error(type, "Contract type was not specified, but it couldn't be inferred.");
                return null;
            }

            if (m instanceof DeclaredType) {
                final DeclaredType dt = (DeclaredType) m;
                return (TypeElement) dt.asElement();
            } else {
                error(type, "Invalid type specified as the contract");
                return null;
            }
        }

    }

    private boolean isObject(final TypeMirror t) {
        if (t instanceof DeclaredType) {
            final DeclaredType dt = (DeclaredType) t;
            return ((TypeElement) dt.asElement()).getQualifiedName().toString()
                    .equals("java.lang.Object");
        }
        return false;
    }

    private void error(final Element source, final String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg, source);
    }
}
