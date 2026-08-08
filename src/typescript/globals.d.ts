/// <reference path="./bukkit.d.ts" />

// The globals found in this file are documented here, but implemented in Java in the `runtime` project.
// They are dynamically inserted as global variables / functions in the CustomRealms runtime

declare namespace Java {
	/**
	 * Gets a Java type (class, enum, etc.) by its classpath.
	 * @param classpath the Java classpath to the type being requested
	 */
	function type<T = any>(classpath: string): T;

	type Value = any;
}

/**
 * The main plugin class, which is used to access the plugin's resources and methods.
 * @type {org.bukkit.plugin.java.JavaPlugin}
 */
declare const Plugin: org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers an event handler, responding to a specific type of event as defined by the Java classpath
 * to the corresponding Event class in the Bukkit API. This function returns a handle number that can
 * be used to unregister the event listener.
 *
 * The event value passed to the handler is the raw Java.Value object for the underlying Bukkit event
 * object.
 *
 * @param event_classpath the Java classpath to the Bukkit event being listened for
 * @param handler the handler function that will be triggered each time the event occurs
 */
declare function __events_register<T extends org.bukkit.event.Event>(
	event_classpath: string,
	handler: (event: T) => void
): number;

/**
 * Unregisters an event handler, so it will stop receiving events
 * @param handle the handle number of the previously-registered event handler
 */
declare function __events_unregister(handle: number): void;

/**
 * Registers a command handler, which is a function that is called each time a command is issued by a player. The function you
 * provide should return true if the command is recognized as corresponding to your plugin. If you don't recognize the command,
 * return false so that another plugin on the server will be able to respond to it.
 * @param handler the handler function to be called
 */
declare function __commands_register(
	name: string,
	handler: (
		sender: org.bukkit.command.CommandSender,
		label: string,
		args: JavaArray<string>
	) => boolean
): boolean;

declare namespace console {
	function log(...args: any[]): void;
	function warn(...args: any[]): void;
	function error(...args: any[]): void;
}

/**
 * Sets a timeout
 * @param callback the function to run after the delay
 * @param delay the delay in milliseconds
 * @returns the timeout ID
 */
declare function setTimeout(callback: () => void, delay: number): number;

/**
 * Clears a timeout
 * @param timeout the timeout ID to clear
 */
declare function clearTimeout(timeout: number): void;

/**
 * Sets an interval
 * @param callback the function to run repeatedly
 * @param delay the delay in milliseconds
 * @returns the interval ID
 */
declare function setInterval(callback: () => void, delay: number): number;

/**
 * Clears an interval
 * @param interval the interval ID to clear
 */
declare function clearInterval(interval: number): void;

/**
 * Runs a function on the next server tick.
 * @param callback the function to run immediately
 */
declare function setImmediate(callback: () => void): number;

/**
 * Clears an immediate
 * @param immediate the immediate ID to clear
 */
declare function clearImmediate(immediate: number): void;

/**
 * Queues a microtask to run as soon as possible.
 * @param callback the function to run as a microtask
 */
declare function queueMicrotask(callback: () => void): void;

/**
 * Adapts a JavaScript callback into a Java {@link java.util.function.Function}.
 *
 * When invoked by Java, the callback is scheduled to execute on the
 * CustomRealms JavaScript runtime, ensuring that JavaScript never executes
 * concurrently.
 *
 * @param fn The callback to invoke.
 * @returns A Java {@link java.util.function.Function} suitable for asynchronous Java APIs.
 */
declare function __main_thread<T, V>(fn: (value: T) => V): java.util.function.Function<T, V>;

declare namespace __fs {
	function read(path: string): Promise<string>;

	function readdir(path: string): Promise<string[]>;

	function exists(path: string): Promise<boolean>;

	function remove(path: string): Promise<void>;

	function mkdir(path: string, recursive: boolean): Promise<boolean>;

	function write(path: string, data: string): Promise<void>;
}
