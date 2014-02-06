service-binder
==============

Injection parallel to the
[JDK ServiceLoader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html).

It comes in two flavors:

* Guice
* Spring Framework

# Motivation

_This section needs revision in light of non-support in Guice 3.0 of injecting modules._

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

Use [`ServiceBinder.with(Binder)`](src/main/java/hm/binkley/util/ServiceBinder.java#L73) for Guice
or [`ServiceBinder.with(BeanDefinitionRegistry)`]
(src/main/java/hm/binkley/util/ServiceBinder.java#L77) for Spring Framework.  These return an `With`
implementation specific to your choice.

# Bind services

Use [`With.bind(Class)`](src/main/java/hm/binkley/util/ServiceBinder.java#L82) or
[`With.bind(Class, ClassLoader)`](src/main/java/hm/binkley/util/ServiceBinder.java#L82).  If
not provided `bind()` uses the thread-context class loader.

# Examples

Examples assume these services:

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
```

## Guice example

```java
public final class SampleModule
        extends AbstractModule {
    @Override
    protected void configure() {
        bindConstant().annotatedWith(named("cat-name")).to("Felix");
        ServiceBinder.with(binder()).bind(Bob.class);
    }
}
```

## Spring example

```java
@Configuration
public static class AppConfig {
    @Bean(name = "cat-name")
    public String catName() {
        return "Felix";
    }
}
```

```java
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
context.register(AppConfig.class);
ServiceBinder.with(context).bind(Bob.class);
context.refresh();
```

# Extras

You may find Kohsuke's [META-INF/services generator](https://github.com/binkley/service-binder)
useful to annotate your service implementations: it generates the META-INF services file for you.

When using the maven shade plugin you may also find [the services transformer]
(https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ServicesResourceTransformer)
useful to merge `META-INF/services` files.

# Releases

## 0.3

* Use ServiceConfigurationError rather than ServiceBinderError
* Documentation
* Improved Spring unit tests

## 0.2

* Support for Guice
* Support for Spring Framework

## 0.1

Not released.
