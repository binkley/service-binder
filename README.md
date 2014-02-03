service-binder
==============

Injection parallel to the
[JDK ServiceLoader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html).

It comes in two flavors:

* Guice
* Spring Framework

# Motivation

Using `ServiceLoader` to find modules to install into Guice is straight-forward:

```
@Override
protected void configure() {
    for (final Module : ServiceLoader.load(Module.class))
        install(module);
}
```

However this does not provide modules with injection.  Spring Framework handles this better.

Using `ServiceBinder` provides the same discovery mechanism and provides injection.  It is not
limited to modules.

# Pick an injector

Use [`ServiceBinder.with(Binder)`](src/main/java/hm/binkley/util/ServiceBinder.java) for Guice
or `ServiceBinder.with(BeanDefinitionRegistry)` for Spring Framework.  These return an `With`
implementation specific to your choice.

# Bind services

Use `With.bind(Class)` or `With.bind(Class, ClassLoader)`.  If not provided `bind()` uses the
thread-context class loader.

# Guice example

```java
public interface Bob {}

@MetaInfServices
public static final class Fred
        implements Bob {}

@MetaInfServices
public static final class Nancy
        implements Bob {
    @Inject
    public Nancy(@Named("cat-name") final String catName) {}
}

public final class SampleModule
        extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(named("cat-name")).to("Felix");
        with(binder()).bind(Bob.class);
    }
}
```

# Extras

You may find Kohsuke's [META-INF/services generator](https://github.com/binkley/service-binder)
useful to annotate your service implementations: it generates the META-INF services file for you.
