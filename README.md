# Documented

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

    @Inject lateinit var logger: LoggerLocalDataSource
    @Inject lateinit var dateFormatter: DateFormatter

    ...
}

```

This is called _field injection_.

To perform field injection, use the `@Inject` annotation on Android class fields you want to be injected by Hilt.

_Warning:_ Fields injected by Hilt cannot be _private_.

Under the hood, Hilt will populate those fields in the `onAttach()` lifecycle method with instances built in the dependencies container that Hilt automatically generated for `LogsFragment`..