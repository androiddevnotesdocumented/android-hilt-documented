# Documented

## Todo

### 1

```kotlin
@Singleton
class LoggerLocalDataSource @Inject constructor(private val logDao: LogDao) {
    ...
}
```

In the source code, LoggerLocalDataSource is not marked as Singleton.

Source: https://developer.android.com/codelabs/android-hilt#5

### 2 SOLVED

```kotlin

@ActivityScoped
class LoggerInMemoryDataSource @Inject constructor(
) : LoggerDataSource { ... }

```
In the source code, LoggerInMemoryDataSource is not marked as ActivityScoped.

Source: https://developer.android.com/codelabs/android-hilt#8

---

Answer: `@Binds` methods must have the scoping annotations if the type is scoped, so that's why the functions above are annotated with `@Singleton` and `@ActivityScoped`. If `@Binds` or `@Provides` are used as a binding for a type, the scoping annotations in the type are not used anymore, so you can go ahead and remove them from the different implementation classes.

Answer source: https://developer.android.com/codelabs/android-hilt#8


## Revisit this point

Notice that the instance of `LoggerLocalDataSource` will be the same as the one we used in `LogsFragment` since the type is scoped to the application container. However, the instance of `AppNavigator` will be different from the instance in `MainActivity` as we haven't scoped it to its respective `Activity` container.

Source: https://developer.android.com/codelabs/android-hilt#7

How to scope to the Activity container: https://developer.android.com/codelabs/android-hilt#8

## 

If you take a look at the starting code, you can see an instance of the `ServiceLocator` class stored in the `LogApplication` class. The `ServiceLocator` creates and stores dependencies that are obtained on demand by the classes that need them. You can think of it as a **_container_** of dependencies that is attached to the app's lifecycle, which means it will be destroyed when the app process is destroyed.


```kotlin
// LogApplication

class LogApplication : Application() {

    lateinit var serviceLocator: ServiceLocator

    override fun onCreate() {
        super.onCreate()
        serviceLocator = ServiceLocator(applicationContext)
    }
}


```

```kotlin

// ServiceLocator.kt

class ServiceLocator(applicationContext: Context) {

    private val logsDatabase = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java,
        "logging.db"
    ).build()

    val loggerLocalDataSource = LoggerLocalDataSource(logsDatabase.logDao())

    fun provideDateFormatter() = DateFormatter()

    fun provideNavigator(activity: FragmentActivity): AppNavigator {
        return AppNavigatorImpl(activity)
    }
}

```

Hilt removes the unnecessary boilerplate involved in manual dependency injection or service locator patterns in an Android application by generating code you would've created manually (e.g. the code in the `ServiceLocator` class).

In the next steps, you will use Hilt to replace the `ServiceLocator` class. After that, you'll add new features to the project to explore more Hilt functionality.

---

Instead of grabbing dependencies on demand from the `ServiceLocator` in our classes, we'll use Hilt to provide those dependencies for us.



## Important points

### What is a container?

A **_container_** is a class which is in charge of providing dependencies in your codebase and knows how to create instances of other types in your app. It manages the graph of dependencies required to provide those instances by creating them and managing their lifecycle.

A container exposes methods to get instances of the types it provides. Those methods can return a new instance every time or always return the same instance. If the method always provides the same instance, we say that the type is **_scoped_** to the container.

### What is @AndroidEntryPoint?

Annotating Android classes with `@AndroidEntryPoint` creates a dependencies container that follows the Android class lifecycle.

With `@AndroidEntryPoint`, Hilt will create a dependencies container that is attached to `LogsFragment`'s lifecycle and will be able to inject instances into `LogsFragment`. How can we reference fields that are injected by Hilt?

### What is @Inject and field injection?

We can make Hilt inject instances of different types with the `@Inject` annotation on the fields we want to be injected (i.e. `logger` and `dateFormatter`):

```kotlin

@AndroidEntryPoint
class LogsFragment : Fragment() {

    /**
    * Hilt will be in charge of populating logger field for us. 
    */
    @Inject lateinit var logger: LoggerLocalDataSource
    @Inject lateinit var dateFormatter: DateFormatter

    ...
}

```

This is called _field injection_.

To perform field injection, use the `@Inject` annotation on Android class fields you want to be injected by Hilt.

_Warning:_ Fields injected by Hilt cannot be _private_.

Under the hood, Hilt will populate those fields in the `onAttach()` lifecycle method with instances built in the dependencies container that Hilt automatically generated for `LogsFragment`..

To tell Hilt how to provide instances of a type, add the @Inject annotation to the constructor of the class you want Hilt to inject.

```kotlin

class DateFormatter @Inject constructor() { ... }

```

With this, Hilt knows how to provide instances of `DateFormatter`.

### What is binding?

The information that tells Hilt how to provide instances of different types are also called **bindings**.

In our example, Hilt has two bindings so far, telling it how to provide instances of 1) `DateFormatter` and 2) `LoggerLocalDataSource`.

### What is @Singleton?

The annotation that scopes an instance to the application container is `@Singleton`. This annotation will make the application container always provide the same instance regardless of whether the type is used as a dependency of another type or if it needs to be field injected.

### Transitive dependencies example

Now Hilt knows how to provide instances of `LoggerLocalDataSource`. However, this time the type has transitive dependencies! To provide an instance of `LoggerLocalDataSource`, Hilt also needs to know how to provide an instance of `LogDao`.

### @Inject for interface

Unfortunately, because `LogDao` is an interface, we cannot annotate its constructor with `@Inject` because interfaces don't have constructors.

### What is module?

**Modules are used to add bindings to Hilt**, or in other words, to tell Hilt how to provide instances of different types. In Hilt modules, you can include bindings for **types that cannot be constructor-injected** such as interfaces or classes that are not contained in your project. An example of this is `OkHttpClient` - you need to use its builder to create an instance.

A Hilt module is a class annotated with `@Module` and `@InstallIn`. `@Module` tells Hilt that this is a module and `@InstallIn` tells Hilt the containers where the bindings are available by specifying a Hilt component. You can think of a Hilt component as a container. The full list of components can be found [here](https://developer.android.com/training/dependency-injection/hilt-android#generated-components).

**For each Android class that can be injected by Hilt, there's an associated Hilt component**. For example, the `Application` container is associated with `SingletonComponent`, and the `Fragment` container is associated with `FragmentComponent`.

Since `LoggerLocalDataSource` is scoped to the application container, the `LogDao` binding needs to be available in the application container. We specify that requirement using the `@InstallIn` annotation by passing in the class of the Hilt component associated with it (i.e. `SingletonComponent:class`):

```kotlin

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

}

```

### Optimized providers in Hilt

In Kotlin, modules that only contain `@Provides` functions can be `object` classes. This way, providers get optimized and almost in-lined in generated code.

### What is @Provides?

We can annotate a function with `@Provides` in Hilt modules to tell Hilt how to provide types that cannot be constructor injected.

The function body of a function that is annotated with `@Provides` will be executed every time Hilt needs to provide an instance of that type. The return type of the `@Provides`-annotated function tells Hilt the binding type, the type that the function provides instances of.. The function parameters are the dependencies of that type.

```kotlin

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    fun provideLogDao(database: AppDatabase): LogDao {
        return database.logDao()
    }
}

```

### Container with default bindings?

Each Hilt container comes with a set of default bindings that can be injected as dependencies into your custom bindings. This is the case with `applicationContext`. To access it, you need to annotate the field with `@ApplicationContext`.

```kotlin

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "logging.db"
        ).build()
    }

```

### Aware of the Activity

Now, Hilt has all the necessary information to inject the instances in `LogsFragment`. However, before running the app, Hilt needs to be aware of the `Activity` that hosts the `Fragment` in order to work. We'll need to use `@AndroidEntryPoint`.

```kotlin

@AndroidEntryPoint
class MainActivity : AppCompatActivity() { ... }

```


### What is @Binds?

Because `AppNavigator` is an interface, we cannot use constructor injection. To tell Hilt what implementation to use for an interface, you can use the `@Binds` annotation on a function inside a Hilt module.

`@Binds` must annotate an abstract function (since it's abstract, it doesn't contain any code and the class needs to be abstract too). The return type of the abstract function is the interface we want to provide an implementation for (i.e. `AppNavigator`). The implementation is specified by adding a unique parameter with the interface implementation type (i.e. `AppNavigatorImpl`).

### Which module to choose?

The `DatabaseModule` module is installed in the `SingletonComponent`, so the bindings are available in the application container. Our new navigation information (i.e. `AppNavigator`) needs information specific to the activity because`AppNavigatorImpl` has an `Activity` as a dependency. Therefore, it must be installed in the `Activity` container instead of the `Application` container, since that's where information about the `Activity` is available.

### @Binds and @Provides

Hilt Modules cannot contain both non-static and abstract binding methods, so you cannot place `@Binds` and `@Provides` annotations in the same class.


### Predefined binding

`AppNavigatorImpl` depends on a `FragmentActivity`. Because an `AppNavigator` instance is provided in the `Activity` container , `FragmentActivity` is already available as a [predefined binding](https://developer.android.com/training/dependency-injection/hilt-android#component-default).

```kotlin
class AppNavigatorImpl @Inject constructor(
    private val activity: FragmentActivity
) : AppNavigator {
    ...
}
```

### Make the class field injectable

As we've learnt before, in order to make the class be field injected by Hilt, we have to:

1.  Annotate the `ButtonsFragment` with `@AndroidEntryPoint`,
2.  Remove private modifier from `logger` and `navigator` fields and annotate them with `@Inject`,
3.  Remove fields initialization code (i.e. `onAttach` and `populateFields` methods).

```kotlin

@AndroidEntryPoint
class ButtonsFragment : Fragment() {

    @Inject lateinit var logger: LoggerLocalDataSource
    @Inject lateinit var navigator: AppNavigator

...

}

```


### Scoping to the Activity container

To be able to use `LoggerInMemoryDataSource` as an implementation detail, we need to tell Hilt how to provide instances of this type. As before, we annotate the class constructor with `@Inject`:

```kotlin

class LoggerInMemoryDataSource @Inject constructor(
) : LoggerDataSource { ... }

```

Since our application consists of only one Activity (also called a _single-Activity_ application), we should have an instance of the `LoggerInMemoryDataSource` in the `Activity` container and reuse that instance across `Fragment`s.

We can achieve the in-memory logging behavior by scoping `LoggerInMemoryDataSource` to the `Activity` container: every `Activity` created will have its own container, a different instance. On each container, the same instance of `LoggerInMemoryDataSource` will be provided when the loggeris needed as a dependency or for field injection. Also, the same instance will be provided in containers below the [Components hierarchy](https://developer.android.com/training/dependency-injection/hilt-android#component-hierarchy).

Following the [scoping to Components documentation](https://developer.android.com/training/dependency-injection/hilt-android#component-scopes), to scope a type to the `Activity` container, we need to annotate the type with `@ActivityScoped`:

```kotlin

@ActivityScoped
class LoggerInMemoryDataSource @Inject constructor(
) : LoggerDataSource { ... }

```

### Two implementations for the same interface


Let's create a new file in the `di` folder called `LoggingModule.kt`. Since the different implementations of `LoggerDataSource` are scoped to different containers, we cannot use the same module: `LoggerInMemoryDataSource` is scoped to the `Activity` container and `LoggerLocalDataSource` to the `Application` container.

Fortunately, we can define bindings for both modules in the same file we just created:

```kotlin

@InstallIn(SingletonComponent::class)
@Module
abstract class LoggingDatabaseModule {

    @Singleton
    @Binds
    abstract fun bindDatabaseLogger(impl: LoggerLocalDataSource): LoggerDataSource
}

@InstallIn(ActivityComponent::class)
@Module
abstract class LoggingInMemoryModule {

    @ActivityScoped
    @Binds
    abstract fun bindInMemoryLogger(impl: LoggerInMemoryDataSource): LoggerDataSource
}


```

`@Binds` methods must have the scoping annotations if the type is scoped, so that's why the functions above are annotated with `@Singleton` and `@ActivityScoped`. If `@Binds` or `@Provides` are used as a binding for a type, the scoping annotations in the type are not used anymore, so you can go ahead and remove them from the different implementation classes.

If you try to build the project now, you'll see a `DuplicateBindings` error!

```
error: [Dagger/DuplicateBindings] com.example.android.hilt.data.LoggerDataSource is bound multiple times

```

This is because the `LoggerDataSource` type is being injected in our `Fragment`s but **Hilt doesn't know which implementation to use because there are two bindings of the same type!** How can Hilt know which one to use?

### What is qualifier?

**To tell Hilt how to provide different implementations (multiple bindings) of the same type, you can use qualifiers.**

A qualifier is an annotation used to identify a binding.

We need to define a qualifier per implementation since each qualifier will be used to identify a binding. When injecting the type in an Android class or having that type as a dependency of other classes, the qualifier annotation needs to be used to avoid ambiguity.

Now, these qualifiers must annotate the `@Binds` (or `@Provides` in case we need it) functions that provide each implementation. See the full code and notice the qualifiers usage in the `@Binds` methods:

```kotlin

@Qualifier
annotation class InMemoryLogger

@Qualifier
annotation class DatabaseLogger

@InstallIn(SingletonComponent::class)
@Module
abstract class LoggingDatabaseModule {

    @DatabaseLogger
    @Singleton
    @Binds
    abstract fun bindDatabaseLogger(impl: LoggerLocalDataSource): LoggerDataSource
}

@InstallIn(ActivityComponent::class)
@Module
abstract class LoggingInMemoryModule {

    @InMemoryLogger
    @ActivityScoped
    @Binds
    abstract fun bindInMemoryLogger(impl: LoggerInMemoryDataSource): LoggerDataSource
}

```

Also, these qualifiers must be used at the injection point with the implementation we want to be injected. In this case, we're going to use the `LoggerInMemoryDataSource` implementation in our `Fragment`s.

**Important:** As the `@DatabaseLogger` qualifier is installed in `SingletonComponent`, it could be injected into the `LogApplication` class. However, as `@InMemoryLogger` is installed in `ActivityComponent`, it cannot be injected into the `LogApplication` class because the application container doesn't know about that binding.

Open `LogsFragment` and use the `@InMemoryLogger` qualifier on the logger field to tell Hilt to inject an instance of `LoggerInMemoryDataSource`:

```kotlin

@AndroidEntryPoint
class LogsFragment : Fragment() {

    @InMemoryLogger
    @Inject lateinit var logger: LoggerDataSource
    ...
}

```

### What is @EntryPoint?

`@EntryPoint` annotation which is used to **inject dependencies in classes not supported by Hilt**.

As we saw before, Hilt comes with support for the most common Android components. However, you might need to perform field injection in classes that either are not supported directly by Hilt or cannot use Hilt.

In those cases, you can use `@EntryPoint`. An entry point is the boundary place where you can get Hilt-provided objects from code that cannot use Hilt to inject its dependencies. It is the point where code first enters into containers managed by Hilt.

**An entry point is an interface with an accessor method for each binding type we want** (including its qualifier). Also, the interface must be annotated with `@InstallIn` to specify the component in which to install the entry point.

Notice that the interface is annotated with the `@EntryPoint` and it's installed in the `SingletonComponent` since we want the dependency from an instance of the `Application` container. Inside the interface, we expose methods for the bindings we want to access, in our case, `LogDao`.