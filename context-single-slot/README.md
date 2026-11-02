# context-single-slot

The default `CurrentContextStorage` mechanism: one slot shared by every thread and coroutine.

The SDK depends on this module and installs `createSingleSlotCurrentContextStorage()` when no
other storage is configured or installed. You can also install it yourself without an SDK:

```kotlin
installContextStorage(createSingleSlotCurrentContextStorage())
```

Any thread can change the current context for every other thread, so this is best suited to
apps that mostly pass contexts explicitly. Use `context-threadlocal` for per-thread isolation.
