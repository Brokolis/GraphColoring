package graphColoring.utils

class EventHandler<T>(val name: String?, val handler: T)

inline fun <T> MutableList<EventHandler<T>>.invoke(crossinline f: (handler: T) -> Unit) {
	this.forEach { f(it.handler) }
}

fun <T> addHandler(list: MutableList<EventHandler<T>>, name: String? = null, handler: T) {
	list.add(EventHandler(name, handler))
}

fun <T> removeHandler(list: MutableList<EventHandler<T>>, name: String?) {
	list.removeIf { it.name == name }
}

fun <T> removeHandler(list: MutableList<EventHandler<T>>, handler: T) {
	list.removeIf { it.handler == handler }
}