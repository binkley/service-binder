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
final GenericApplicationContext context = new GenericApplicationContext();
final GenericBeanDefinition catName = new GenericBeanDefinition();
catName.setBeanClass(String.class);
final ConstructorArgumentValues value = new ConstructorArgumentValues();
value.addGenericArgumentValue("Felix");
catName.setConstructorArgumentValues(value);
context.registerBeanDefinition("cat-name", catName);
ServiceBinder.with(context).bind(Bob.class);
```

# Extras

You may find Kohsuke's [META-INF/services generator](https://github.com/binkley/service-binder)
useful to annotate your service implementations: it generates the META-INF services file for you.

When using the maven shade plugin you may also find [the services transformer]
(https://maven.apache.org/plugins/maven-shade-plugin/examples/resource-transformers.html#ServicesResourceTransformer)
useful to merge `META-INF/services` files.
