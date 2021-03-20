# Documented

## Important points

### What is a container?

A **_container_** is a class which is in charge of providing dependencies in your codebase and knows how to create instances of other types in your app. It manages the graph of dependencies required to provide those instances by creating them and managing their lifecycle.

A container exposes methods to get instances of the types it provides. Those methods can return a new instance every time or always return the same instance. If the method always provides the same instance, we say that the type is **_scoped_** to the container.