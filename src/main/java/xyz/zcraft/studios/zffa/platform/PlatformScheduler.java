package xyz.zcraft.studios.zffa.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlatformScheduler {
    private final Plugin plugin;
    private final Object globalRegionScheduler;
    private final Object asyncScheduler;
    private final boolean folia;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.globalRegionScheduler = scheduler("getGlobalRegionScheduler");
        this.asyncScheduler = scheduler("getAsyncScheduler");
        this.folia = globalRegionScheduler != null && asyncScheduler != null;
    }

    public boolean folia() {
        return folia;
    }

    public void run(Runnable runnable) {
        if (!folia) {
            Bukkit.getScheduler().runTask(plugin, runnable);
            return;
        }
        invokeScheduler(globalRegionScheduler, "run", new Class<?>[]{Plugin.class, Consumer.class},
                plugin, consumer(runnable));
    }

    public void runAsync(Runnable runnable) {
        if (!folia) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return;
        }
        invokeScheduler(asyncScheduler, "runNow", new Class<?>[]{Plugin.class, Consumer.class},
                plugin, consumer(runnable));
    }

    public ScheduledTaskHandle runLater(Runnable runnable, long delayTicks) {
        if (!folia) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            return task::cancel;
        }
        Object task = invokeScheduler(globalRegionScheduler, "runDelayed", new Class<?>[]{Plugin.class, Consumer.class, long.class},
                plugin, consumer(runnable), Math.max(1L, delayTicks));
        return handle(task);
    }

    public ScheduledTaskHandle runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!folia) {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            return task::cancel;
        }
        Object task = invokeScheduler(globalRegionScheduler, "runAtFixedRate",
                new Class<?>[]{Plugin.class, Consumer.class, long.class, long.class},
                plugin, consumer(runnable), Math.max(1L, delayTicks), Math.max(1L, periodTicks));
        return handle(task);
    }

    public ScheduledTaskHandle runTimer(Consumer<ScheduledTaskHandle> consumer, long delayTicks, long periodTicks) {
        final ScheduledTaskHandle[] handle = new ScheduledTaskHandle[1];
        handle[0] = runTimer(() -> consumer.accept(handle[0]), delayTicks, periodTicks);
        return handle[0];
    }

    public CompletableFuture<Boolean> teleport(Player player, Location location) {
        try {
            Method method = player.getClass().getMethod("teleportAsync", Location.class);
            Object result = method.invoke(player, location);
            if (result instanceof CompletableFuture<?> future) {
                return future.thenApply(value -> Boolean.TRUE.equals(value));
            }
        } catch (ReflectiveOperationException ignored) {
        }
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        run(() -> {
            try {
                future.complete(player.teleport(location));
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    private Object scheduler(String methodName) {
        try {
            Method method = Bukkit.getServer().getClass().getMethod(methodName);
            return method.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Consumer<Object> consumer(Runnable runnable) {
        return ignored -> runnable.run();
    }

    private Object invokeScheduler(Object scheduler, String method, Class<?>[] parameterTypes, Object... args) {
        try {
            return scheduler.getClass().getMethod(method, parameterTypes).invoke(scheduler, args);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke platform scheduler method " + method, exception);
        }
    }

    private ScheduledTaskHandle handle(Object task) {
        return () -> {
            if (task == null) return;
            try {
                task.getClass().getMethod("cancel").invoke(task);
            } catch (ReflectiveOperationException ignored) {
            }
        };
    }
}
