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
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.MetaInfServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.inject.Guice.createInjector;
import static com.google.inject.name.Names.named;
import static hm.binkley.util.ServiceBinder.with;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@code ServiceBinderTest} tests {@link ServiceBinder}.
 *
 * @author <a href="mailto:binkley@alumni.rice.edu">B. K. Oxley (binkley)</a>
 */
public final class ServiceBinderTest {
    @Before
    public void setUp() {
        Logger.getLogger("org.springframework").setLevel(Level.WARNING);
    }

    @Test
    public void shouldBindServicesWithGuice() {
        final Set<Class<? extends Bob>> found = new HashSet<Class<? extends Bob>>();
        for (final Bob bob : guice().getInstance(Key.get(new TypeLiteral<Set<Bob>>() {})))
            found.add(bob.getClass());

        final Set<Class<? extends Bob>> expected = new HashSet<Class<? extends Bob>>();
        expected.add(Fred.class);
        expected.add(Nancy.class);

        assertThat(found, is(equalTo(expected)));
    }

    @Test
    public void shouldInjectServicesWithGuice() {
        assertThat(guice().getInstance(Nancy.class).catName, is(equalTo("Felix")));
    }

    @Test
    public void shouldBindServicesWithSpring() {
        final Set<Class<? extends Bob>> found = new HashSet<Class<? extends Bob>>();
        for (final Bob bob : spring().getBeansOfType(Bob.class).values())
            found.add(bob.getClass());

        final Set<Class<? extends Bob>> expected = new HashSet<Class<? extends Bob>>();
        expected.add(Fred.class);
        expected.add(Nancy.class);

        assertThat(found, is(equalTo(expected)));
    }

    @Test
    public void shouldInjectServicesWithSpring() {
        assertThat(spring().getBean(Nancy.class).catName, is(equalTo("Felix")));
    }

    public interface Bob {}

    @MetaInfServices
    public static final class Fred
            implements Bob {}

    @MetaInfServices
    public static final class Nancy
            implements Bob {
        private final String catName;

        @Inject
        public Nancy(@Named("cat-name") @Value("${cat-name}") final String catName) {
            this.catName = catName;
        }
    }

    public static final class TestModule
            extends AbstractModule {
        @Override
        protected void configure() {
            bindConstant().annotatedWith(named("cat-name")).to("Felix");
            with(binder()).bind(Bob.class);
        }
    }

    private static Injector guice() {
        return createInjector(new TestModule());
    }

    private static GenericApplicationContext spring() {
        final GenericApplicationContext context = new GenericApplicationContext();
        final GenericBeanDefinition catName = new GenericBeanDefinition();
        catName.setBeanClass(String.class);
        final ConstructorArgumentValues value = new ConstructorArgumentValues();
        value.addGenericArgumentValue("Felix");
        catName.setConstructorArgumentValues(value);
        context.registerBeanDefinition("cat-name", catName);
        with(context).bind(Bob.class);
        context.refresh();
        return context;
    }
}
