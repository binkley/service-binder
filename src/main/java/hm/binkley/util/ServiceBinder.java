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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import org.springframework.beans.factory.serviceloader.ServiceLoaderFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.lang.Character.charCount;
import static java.lang.Character.isWhitespace;
import static java.lang.ClassLoader.getSystemClassLoader;
import static java.lang.Integer.toHexString;
import static java.lang.Thread.currentThread;
import static org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;

/**
 * {@code ServiceBinder} is {@link ServiceLoader} with injection.  Create a service binder for Guice
 * using {@link #with(Binder)} or Spring Framework using {@link #with(BeanDefinitionRegistry)}. The
 * service binder is reuseable. <p/> To discover and bind implementations of a service, use {@link
 * #bind(Class)} or {@link #bind(Class, ClassLoader)}. <p/> All exeptions thrown internally appear
 * as {@link ServiceConfigurationError}.
 *
 * @param <E> the exception type thrown internally, not visible outside declaration
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Needs documentation.
 */
public final class ServiceBinder<E extends Exception> {
    private static final String PREFIX = "META-INF/services/";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern COMMENT = Pattern.compile("#.*");

    private final With<E> with;

    /**
     * Creates a service binder for Guice with the given <var>binder</var>.  Discovered classes are
     * bound to the injector with the multibindings extension: <pre>
     * bindings.addBinding().to(implementation)</pre>
     *
     * Note - this does not work for {@link Module}s in Guice 3.0.
     *
     * @param binder the Guice binder, never missing
     *
     * @return the service binder, never missing
     */
    @Nonnull
    public static ServiceBinder<RuntimeException> with(@Nonnull final Binder binder) {
        return new ServiceBinder<RuntimeException>(new WithGuice(binder));
    }

    /**
     * Creates a service binder for Spring Framkwork with the given bean definition
     * <var>registry</var>.  Discovered classes are bound to the context with: <pre>
     * registry.registerBeanDefinition(implementation.getName(),
     *     new RootBeanDefinition(implementation, AUTOWIRE_CONSTRUCTOR, true))</pre>
     *
     * @param registry the Spring bean definition registry, never missing
     *
     * @return the service binder, never missing
     *
     * @see ServiceLoaderFactoryBean
     */
    @Nonnull
    public static ServiceBinder<ClassNotFoundException> with(
            @Nonnull final BeanDefinitionRegistry registry) {
        return new ServiceBinder<ClassNotFoundException>(new WithSpring(registry));
    }

    /**
     * Binds injected instances of the <var>service</var> type token to the binding instance using
     * the system class loader.
     *
     * @param service the service type token, never missing
     * @param <T> the service type
     */
    public <T> void bind(@Nonnull final Class<T> service) {
        bind(service, currentThread().getContextClassLoader());
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + with.getClass().getSimpleName() + "]@" +
                toHexString(hashCode());
    }

    /**
     * Binds injected instances of the <var>service</var> type tokento the binding instance using
     * the given <var>classLoader</var>, or the system class loader if {@code null}.
     *
     * @param service the service type token, never missing
     * @param classLoader the classloader, if {@code null} the system class loader
     * @param <T> the service type
     */
    public <T> void bind(@Nonnull final Class<T> service, @Nullable ClassLoader classLoader) {
        if (null == classLoader)
            classLoader = getSystemClassLoader();
        final Enumeration<URL> configs = configs(service, classLoader);
        while (configs.hasMoreElements())
            bind(service, classLoader, configs.nextElement(), with);
    }

    private ServiceBinder(final With<E> with) {
        this.with = with;
    }

    private static <T> Enumeration<URL> configs(final Class<T> service,
            final ClassLoader classLoader) {
        try {
            return classLoader.getResources(PREFIX + service.getName());
        } catch (final IOException e) {
            return fail(service, "Cannot load configuration", e);
        }
    }

    private static <T, E extends Exception> void bind(final Class<T> service,
            final ClassLoader classLoader, final URL config, final With<E> with) {
        final BufferedReader reader = config(service, config);
        try {
            with.bind(service, implementations(service, classLoader, config, reader));
        } catch (final Exception e) { // Cannot declare a generic catch using <E>
            fail(service, config, "Cannot bind implemntations", e);
        } finally {
            try {
                reader.close();
            } catch (final IOException e) {
                fail(service, config, "Cannot close", e);
            }
        }
    }

    private static <T> BufferedReader config(final Class<T> service, final URL config) {
        try {
            return new BufferedReader(new InputStreamReader(config.openStream(), UTF8));
        } catch (final IOException e) {
            return fail(service, config, "Cannot read service configuration", e);
        }
    }

    private static <T> List<Class<? extends T>> implementations(final Class<T> service,
            final ClassLoader classLoader, final URL config, final BufferedReader reader) {
        final List<Class<? extends T>> implementations = new ArrayList<Class<? extends T>>();
        String implementation;
        while (null != (implementation = implementation(service, config, reader)))
            if (!skip(implementation))
                implementations.add(loadClass(service, classLoader, config, implementation));
        return implementations;
    }

    private static <T> String implementation(final Class<T> service, final URL config,
            final BufferedReader reader) {
        try {
            final String line = reader.readLine();
            if (null == line)
                return null;
            else
                return COMMENT.matcher(line).replaceFirst("").trim();
        } catch (final IOException e) {
            return fail(service, config, "Cannot read service configuration", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<? extends T> loadClass(final Class<T> service,
            final ClassLoader classLoader, final URL config, final String className) {
        try {
            return (Class<? extends T>) classLoader.loadClass(className);
        } catch (final ClassNotFoundException e) {
            return fail(service, config, "Cannot bind implementation for " + className, e);
        }
    }

    private static boolean skip(final String s) {
        if (null == s)
            return true;
        final int len = s.length();
        if (0 == len)
            return true;
        for (int i = 0; i < len; ) {
            final int cp = s.codePointAt(i);
            if (!isWhitespace(cp))
                return false;
            i += charCount(cp);
        }
        return true;
    }

    private static <R> R fail(final Class<?> service, final String message, final Exception cause) {
        throw new ServiceConfigurationError(service.getName() + ": " + message, cause);
    }

    private static <R> R fail(final Class<?> service, final URL config, final String message,
            final Exception cause) {
        throw new ServiceConfigurationError(service.getName() + ": " + config + ": " + message,
                cause);
    }

    private interface With<E extends Exception> {
        <T> void bind(final Class<T> service, final Iterable<Class<? extends T>> implementation)
                throws E;
    }

    private static class WithGuice
            implements With<RuntimeException> {
        private final Binder binder;

        public WithGuice(final Binder binder) {
            this.binder = binder;
        }

        @Override
        public <T> void bind(final Class<T> service,
                final Iterable<Class<? extends T>> implementations) {
            final Multibinder<T> bindings = newSetBinder(binder, service);
            for (final Class<? extends T> implementation : implementations)
                bindings.addBinding().to(implementation);
        }
    }

    private static class WithSpring
            implements With<ClassNotFoundException> {
        private final BeanDefinitionRegistry registry;

        public WithSpring(final BeanDefinitionRegistry registry) {
            this.registry = registry;
        }

        @Override
        public <T> void bind(final Class<T> service,
                final Iterable<Class<? extends T>> implementations)
                throws ClassNotFoundException {
            for (final Class<? extends T> implementation : implementations)
                registry.registerBeanDefinition(implementation.getName(),
                        new RootBeanDefinition(implementation, AUTOWIRE_CONSTRUCTOR, true));
        }
    }
}
