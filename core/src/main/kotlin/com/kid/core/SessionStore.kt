package com.kid.core

/**
 * The latest copy of each session, shared by every controller on one client, so a permission changed through
 * settings reaches the permission gate on the same session.
 */
@KIDInternalApi
public class SessionStore {
    private val latest = mutableMapOf<String, Session>()
    private val listeners = mutableMapOf<Any, (Session) -> Unit>()

    public fun publish(session: Session) {
        if (latest[session.sessionId] == session) return
        latest[session.sessionId] = session
        listeners.values.toList().forEach { it(session) }
    }

    public fun current(sessionId: String): Session? = latest[sessionId]

    public fun subscribe(listener: (Session) -> Unit): Any {
        val token = Any()
        listeners[token] = listener
        return token
    }

    public fun unsubscribe(token: Any) {
        listeners.remove(token)
    }
}
