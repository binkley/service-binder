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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Test;
import org.kohsuke.MetaInfServices;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static com.google.inject.Guice.createInjector;
import static hm.binkley.util.ServiceBinder.on;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@code ServiceBinderTest} tests {@link ServiceBinder}.
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 * @todo Needs documentation.
 */
public class ServiceBinderTest {
    @Test
    public void shouldBindServices() {
        final Set<Class<? extends Bob>> found = new HashSet<Class<? extends Bob>>();
        for (final Bob bob : createInjector(new TestModule())
                .getInstance(Key.get(new TypeLiteral<Set<Bob>>() {})))
            found.add(bob.getClass());

        final Set<Class<? extends Bob>> expected = new HashSet<Class<? extends Bob>>();
        expected.add(Fred.class);
        expected.add(Nancy.class);

        assertThat(found, is(equalTo(expected)));
    }

    @Test
    public void shouldInjectServices() {
        assertThat(createInjector(new TestModule()).getInstance(Nancy.class).test,
                is(equalTo(this)));
    }

    public interface Bob {}

    @MetaInfServices(Bob.class)
    public static final class Fred
            implements Bob {}

    @MetaInfServices(Bob.class)
    public static final class Nancy
            implements Bob {
        private final ServiceBinderTest test;

        @Inject
        public Nancy(final ServiceBinderTest test) {
            this.test = test;
        }
    }

    public final class TestModule
            extends AbstractModule {
        @Override
        protected void configure() {
            bind(ServiceBinderTest.class).toInstance(ServiceBinderTest.this);
            on(binder()).bind(Bob.class, Bob.class.getClassLoader());
        }
    }
}
