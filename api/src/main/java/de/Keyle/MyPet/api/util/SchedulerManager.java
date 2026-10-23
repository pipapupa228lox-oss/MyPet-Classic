/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single scheduling entry point of MyPetClassic.
 * <p>
 * On a regular Paper/Spigot server this delegates to {@link org.bukkit.scheduler.BukkitScheduler}. On Folia the
 * Bukkit scheduler does not exist ({@code Bukkit.getScheduler()} throws), so the exact same calls are routed to
 * the regionised schedulers: the global region scheduler for server wide work, the async scheduler for database
 * and HTTP work and the entity/region schedulers for everything that touches a pet or a player.
 * <p>
 * The Folia API is accessed reflectively because the MyPet API module is compiled against the plain Bukkit API
 * (add-ons may still target Spigot), so no Paper-only type may leak into this module's compile classpath.
 */
public final class SchedulerManager {

    /**
     * Handle of a scheduled task. Only {@link #cancel()} is needed by MyPet.
     */
    public interface Task {
        Task NONE = () -> {
        };

        void cancel();
    }

    private static final long MILLIS_PER_TICK = 50L;
    private static final Runnable RETIRED = () -> {
    };

    private static final boolean FOLIA = detectFolia();

    // reflective Folia API
    private static Method bukkitGlobal;
    private static Method bukkitRegion;
    private static Method bukkitAsync;
    private static Method entityScheduler;
    private static Method globalRun;
    private static Method globalRunDelayed;
    private static Method globalTimer;
    private static Method globalCancel;
    private static Method regionRun;
    private static Method regionRunDelayed;
    private static Method regionTimer;
    private static Method asyncRun;
    private static Method asyncRunDelayed;
    private static Method asyncTimer;
    private static Method asyncCancel;
    private static Method entityRun;
    private static Method entityRunDelayed;
    private static Method entityTimer;
    private static Method taskCancel;

    private static final List<Task> repeatingTasks = new ArrayList<>();

    static {
        if (FOLIA) {
            try {
                ClassLoader loader = SchedulerManager.class.getClassLoader();
                Class<?> global = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler", false, loader);
                Class<?> region = Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler", false, loader);
                Class<?> async = Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler", false, loader);
                Class<?> entity = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler", false, loader);
                Class<?> scheduled = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask", false, loader);

                Class<?> bukkit = Bukkit.class;
                bukkitGlobal = bukkit.getMethod("getGlobalRegionScheduler");
                bukkitRegion = bukkit.getMethod("getRegionScheduler");
                bukkitAsync = bukkit.getMethod("getAsyncScheduler");
                entityScheduler = Entity.class.getMethod("getScheduler");

                globalRun = global.getMethod("run", Plugin.class, Consumer.class);
                globalRunDelayed = global.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                globalTimer = global.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                globalCancel = global.getMethod("cancelTasks", Plugin.class);

                regionRun = region.getMethod("run", Plugin.class, Location.class, Consumer.class);
                regionRunDelayed = region.getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
                regionTimer = region.getMethod("runAtFixedRate", Plugin.class, Location.class, Consumer.class, long.class, long.class);

                asyncRun = async.getMethod("runNow", Plugin.class, Consumer.class);
                asyncRunDelayed = async.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
                asyncTimer = async.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
                asyncCancel = async.getMethod("cancelTasks", Plugin.class);

                entityRun = entity.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunDelayed = entity.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                entityTimer = entity.getMethod("runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);

                taskCancel = scheduled.getMethod("cancel");
            } catch (ReflectiveOperationException | LinkageError e) {
                MyPetApi.getLogger().severe("Folia was detected but its scheduler API could not be loaded: " + e);
            }
        }
    }

    private SchedulerManager() {
    }

    /**
     * @return {@code true} when the server runs on Folia (regionised threading)
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    /**
     * @return {@code true} when the caller already runs on the thread that owns the given entity
     */
    public static boolean isOwnedByCurrentRegion(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (!FOLIA) {
            return Bukkit.isPrimaryThread();
        }
        try {
            return (Boolean) Bukkit.class.getMethod("isOwnedByCurrentRegion", Entity.class).invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    // ------------------------------------------------------------------ global

    public static Task run(Runnable runnable) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTask(plugin(), runnable));
        }
        return wrap(invoke(globalRun, globalScheduler(), plugin(), consumer(runnable)));
    }

    public static Task runLater(Runnable runnable, long delayTicks) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTaskLater(plugin(), runnable, delayTicks));
        }
        return wrap(invoke(globalRunDelayed, globalScheduler(), plugin(), consumer(runnable), delay(delayTicks)));
    }

    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTaskTimer(plugin(), runnable, delayTicks, periodTicks));
        }
        return track(wrap(invoke(globalTimer, globalScheduler(), plugin(), consumer(runnable), delay(delayTicks), period(periodTicks))));
    }

    // ------------------------------------------------------------------- async

    public static Task runAsync(Runnable runnable) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTaskAsynchronously(plugin(), runnable));
        }
        return wrap(invoke(asyncRun, asyncScheduler(), plugin(), consumer(runnable)));
    }

    public static Task runAsyncLater(Runnable runnable, long delayTicks) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, delayTicks));
        }
        return wrap(invoke(asyncRunDelayed, asyncScheduler(), plugin(), consumer(runnable), delay(delayTicks) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS));
    }

    public static Task runAsyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            return bukkit(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), runnable, delayTicks, periodTicks));
        }
        return track(wrap(invoke(asyncTimer, asyncScheduler(), plugin(), consumer(runnable), delay(delayTicks) * MILLIS_PER_TICK, period(periodTicks) * MILLIS_PER_TICK, TimeUnit.MILLISECONDS)));
    }

    // ------------------------------------------------------- entity / location

    /**
     * Runs the task on the thread that owns the entity. On Paper this is the main thread (as before).
     */
    public static Task runAtEntity(Entity entity, Runnable runnable) {
        if (!FOLIA || entity == null) {
            return run(runnable);
        }
        return wrap(invoke(entityRun, entityScheduler(entity), plugin(), consumer(runnable), RETIRED));
    }

    public static Task runAtEntityLater(Entity entity, Runnable runnable, long delayTicks) {
        if (!FOLIA || entity == null) {
            return runLater(runnable, delayTicks);
        }
        return wrap(invoke(entityRunDelayed, entityScheduler(entity), plugin(), consumer(runnable), RETIRED, delay(delayTicks)));
    }

    public static Task runTimerAtEntity(Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        if (!FOLIA || entity == null) {
            return runTimer(runnable, delayTicks, periodTicks);
        }
        return track(wrap(invoke(entityTimer, entityScheduler(entity), plugin(), consumer(runnable), RETIRED, delay(delayTicks), period(periodTicks))));
    }

    public static Task runAtLocation(Location location, Runnable runnable) {
        if (!FOLIA || location == null) {
            return run(runnable);
        }
        return wrap(invoke(regionRun, regionScheduler(), plugin(), location, consumer(runnable)));
    }

    public static Task runAtLocationLater(Location location, Runnable runnable, long delayTicks) {
        if (!FOLIA || location == null) {
            return runLater(runnable, delayTicks);
        }
        return wrap(invoke(regionRunDelayed, regionScheduler(), plugin(), location, consumer(runnable), delay(delayTicks)));
    }

    public static Task runTimerAtLocation(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        if (!FOLIA || location == null) {
            return runTimer(runnable, delayTicks, periodTicks);
        }
        return track(wrap(invoke(regionTimer, regionScheduler(), plugin(), location, consumer(runnable), delay(delayTicks), period(periodTicks))));
    }

    // --------------------------------------------------------------- utilities

    /**
     * Replacement for {@code BukkitScheduler#callSyncMethod}. Blocking, so only use it for tiny lookups.
     */
    public static <T> T callSync(Supplier<T> supplier, long timeoutMillis) throws ExecutionException, InterruptedException, TimeoutException {
        if (!FOLIA) {
            return Bukkit.getScheduler().callSyncMethod(plugin(), supplier::get).get(timeoutMillis, TimeUnit.MILLISECONDS);
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        run(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new ExecutionException(e.getCause());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException(e.getMessage());
        }
    }

    /**
     * Cancels every task MyPet scheduled. Called on plugin disable.
     */
    public static void cancelAll() {
        List<Task> tasks;
        synchronized (repeatingTasks) {
            tasks = new ArrayList<>(repeatingTasks);
            repeatingTasks.clear();
        }
        for (Task task : tasks) {
            task.cancel();
        }
        if (!FOLIA) {
            Bukkit.getScheduler().cancelTasks(plugin());
            return;
        }
        invoke(globalCancel, globalScheduler(), plugin());
        invoke(asyncCancel, asyncScheduler(), plugin());
    }

    // ------------------------------------------------------------------ private

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static Plugin plugin() {
        return MyPetApi.getPlugin();
    }

    private static Consumer<Object> consumer(Runnable runnable) {
        return task -> runnable.run();
    }

    /**
     * Folia rejects a delay or period of zero, Bukkit accepts it ("next tick").
     */
    private static long delay(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long period(long ticks) {
        return Math.max(1L, ticks);
    }

    private static Object globalScheduler() {
        return invoke(bukkitGlobal, null);
    }

    private static Object regionScheduler() {
        return invoke(bukkitRegion, null);
    }

    private static Object asyncScheduler() {
        return invoke(bukkitAsync, null);
    }

    private static Object entityScheduler(Entity entity) {
        return invoke(entityScheduler, entity);
    }

    private static Object invoke(Method method, Object target, Object... arguments) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, arguments);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
            MyPetApi.getLogger().warning("Could not schedule MyPet task: " + cause);
            return null;
        }
    }

    private static Task bukkit(org.bukkit.scheduler.BukkitTask task) {
        return task == null ? Task.NONE : task::cancel;
    }

    private static Task wrap(Object scheduledTask) {
        if (scheduledTask == null) {
            return Task.NONE;
        }
        return () -> invoke(taskCancel, scheduledTask);
    }

    private static Task track(Task task) {
        if (task != Task.NONE) {
            synchronized (repeatingTasks) {
                repeatingTasks.add(task);
            }
        }
        return task;
    }
}
