package arcnode.nullprotect.server

import java.lang.reflect.Method

fun <T> Class<*>.getFieldObject(name: String, inst: Any? = null): T {
    val fd = this.getDeclaredField(name)
    fd.trySetAccessible()
    return fd.get(inst) as T
}

fun Class<*>.getMethodOpt(name: String): Method? =
    try {
        val md = this.getDeclaredMethod(name)
        md.trySetAccessible()
        md
    } catch (t: Throwable) {
        null
    }
