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
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import hm.binkley.util.ServiceBinder.ServiceBinderChildModule;
import hm.binkley.util.ServiceBinder.ServiceBinderParentModule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.MetaInfServices;
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
    public void shouldBindServicesWithGuiceDirectly() {
        final Set<Class<? extends Bob>> found = new HashSet<Class<? extends Bob>>();
        for (final Bob bob : directGuice().getInstance(Key.get(new TypeLiteral<Set<Bob>>() {})))
            found.add(bob.getClass());

        final Set<Class<? extends Bob>> expected = new HashSet<Class<? extends Bob>>();
        expected.add(Fred.class);
        expected.add(Nancy.class);

        assertThat(found, is(equalTo(expected)));
    }

    @Test
    public void shouldConstructorInjectServicesWithGuiceDirectly() {
        assertThat(directGuice().getInstance(Nancy.class).catName, is(equalTo("Felix")));
    }

    @Test
    public void shouldMethodInjectServicesWithGuiceDirectly() {
        assertThat(directGuice().getInstance(Nancy.class).favoriteColor, is(equalTo("White")));
    }

    @Test
    public void shouldFieldInjectServicesWithGuiceDirectly() {
        assertThat(directGuice().getInstance(Nancy.class).nickName, is(equalTo("Fancy")));
    }

//    @Ignore("Guice cannot bind Module")
    @Test
    public void shouldBindServicesWithInjectedGuice() {
        final Set<Class<? extends Bob>> found = new HashSet<Class<? extends Bob>>();
        for (final Bob bob : injectedGuice().getInstance(Key.get(new TypeLiteral<Set<Bob>>() {})))
            found.add(bob.getClass());

        final Set<Class<? extends Bob>> expected = new HashSet<Class<? extends Bob>>();
        expected.add(Fred.class);
        expected.add(Nancy.class);

        assertThat(found, is(equalTo(expected)));
    }

//    @Ignore("Guice cannot bind Module")
    @Test
    public void shouldConstructorInjectServicesWithInjectedGuice() {
        assertThat(injectedGuice().getInstance(Nancy.class).catName, is(equalTo("Felix")));
    }

//    @Ignore("Guice cannot bind Module")
    @Test
    public void shouldMethodInjectServicesWithInjectedGuice() {
        assertThat(injectedGuice().getInstance(Nancy.class).favoriteColor, is(equalTo("White")));
    }

//    @Ignore("Guice cannot bind Module")
    @Test
    public void shouldFieldInjectServicesWithInjectedGuice() {
        assertThat(injectedGuice().getInstance(Nancy.class).nickName, is(equalTo("Fancy")));
    }

    @Ignore("WIP")
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

    @Ignore("WIP")
    @Test
    public void shouldConstructorInjectServicesWithSpring() {
        assertThat(spring().getBean(Nancy.class).catName, is(equalTo("Felix")));
    }

    @Ignore("WIP")
    @Test
    public void shouldMethodInjectServicesWithSpring() {
        assertThat(spring().getBean(Nancy.class).favoriteColor, is(equalTo("White")));
    }

    @Ignore("WIP")
    @Test
    public void shouldFieldInjectServicesWithSpring() {
        assertThat(spring().getBean(Nancy.class).nickName, is(equalTo("Fancy")));
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
        @Named("nick-name")
        private String nickName;
        private String favoriteColor;

        @Inject
        public Nancy(@Named("cat-name") final String catName) {
            this.catName = catName;
        }

        @Inject
        public void setFavoriteColor(@Named("favorite-color") final String favoriteColor) {
            this.favoriteColor = favoriteColor;
        }
    }

    @MetaInfServices(Module.class)
    public static final class TestModule
            extends AbstractModule {
        @Override
        protected void configure() {
            bindConstant().annotatedWith(named("cat-name")).to("Felix");
            bindConstant().annotatedWith(named("nick-name")).to("Fancy");
            bindConstant().annotatedWith(named("favorite-color")).to("White");
            with(binder()).bind(Bob.class);
        }
    }

    private static Injector directGuice() {
        return createInjector(new TestModule());
    }

    private static Injector injectedGuice() {
        final Injector parent = createInjector(new ServiceBinderParentModule());
        return parent.createChildInjector(parent.getInstance(ServiceBinderChildModule.class));
    }

    private static GenericApplicationContext spring() {
        final GenericApplicationContext context = new GenericApplicationContext();
        context.registerBeanDefinition("cat-name", string("Felix"));
        context.registerBeanDefinition("nick-name", string("Nancy"));
        context.registerBeanDefinition("favorite-color", string("White"));
        with(context).bind(Bob.class);
        context.refresh();
        return context;
    }

    private static GenericBeanDefinition string(final String value) {
        final GenericBeanDefinition bean = new GenericBeanDefinition();
        bean.setBeanClass(String.class);
        final ConstructorArgumentValues values = new ConstructorArgumentValues();
        values.addGenericArgumentValue(value);
        bean.setConstructorArgumentValues(values);
        return bean;
    }
}
