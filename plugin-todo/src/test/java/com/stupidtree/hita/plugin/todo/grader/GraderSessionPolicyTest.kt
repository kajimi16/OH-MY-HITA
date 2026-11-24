package com.stupidtree.hita.plugin.todo.grader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GraderSessionPolicyTest {

    @Test
    fun sessionCookieAcceptsExactlyOneCleanSessionId() {
        assertEquals("sessionid=abc123", GraderSessionPolicy.sessionCookie("foo=1; sessionid=abc123; bar=2"))
        assertNull(GraderSessionPolicy.sessionCookie("foo=1"))
        assertNull(GraderSessionPolicy.sessionCookie("sessionid="))
        assertNull(GraderSessionPolicy.sessionCookie("sessionid=a; sessionid=b"))
        assertNull(GraderSessionPolicy.sessionCookie("sessionid=a b"))
        assertNull(GraderSessionPolicy.sessionCookie("sessionid=abc\n"))
    }

    @Test
    fun fetchWithoutSessionCookieFailsBeforeAnyRequest() {
        assertThrows(GraderAuthenticationException::class.java) {
            GraderWebSource().fetch("foo=1")
        }
        assertThrows(GraderAuthenticationException::class.java) {
            GraderWebSource().fetch("sessionid=\u0001")
        }
    }
}
