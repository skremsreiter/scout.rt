/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.security.auth.Subject;

import org.eclipse.scout.commons.nls.NlsLocale;
import org.eclipse.scout.rt.platform.job.PropertyMap;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.commons.servletfilter.IHttpServletRoundtrip;
import org.eclipse.scout.rt.shared.ISession;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.ui.UiDeviceType;
import org.eclipse.scout.rt.shared.ui.UiLayer;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ServerJobInputTest {

  @Before
  public void before() {
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    ScoutTexts.CURRENT.remove();
    UserAgent.CURRENT.remove();
    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.remove();
    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.remove();
  }

  @Test
  public void testEmpty() {
    ServerJobInput input = ServerJobInput.empty();
    assertNotNull(input.getContext());
    assertNull(input.getName());
    assertNull(input.getId());
    assertNull(input.getSubject());
    assertNull(input.getSession());
    assertTrue(input.isSessionRequired());
    assertNull(input.getUserAgent());
    assertNull(input.getLocale());
  }

  @Test
  public void testCopy() {
    ServerJobInput input = ServerJobInput.empty();
    input.getPropertyMap().put("A", "B");
    input.setName("name");
    input.setId("123");
    input.setSubject(new Subject());
    input.setSession(mock(IServerSession.class));
    input.setUserAgent(UserAgent.create(UiLayer.UNKNOWN, UiDeviceType.UNKNOWN, "n/a"));
    input.setLocale(Locale.CANADA_FRENCH);

    ServerJobInput copy = input.copy();

    assertNotSame(input.getContext(), copy.getContext());
    assertEquals(toSet(input.getPropertyMap().iterator()), toSet(copy.getPropertyMap().iterator()));
    assertEquals(input.getName(), copy.getName());
    assertEquals(input.getId(), copy.getId());
    assertSame(input.getSubject(), copy.getSubject());
    assertSame(input.getUserAgent(), copy.getUserAgent());
    assertSame(input.getLocale(), copy.getLocale());
    assertSame(input.getLocale(), copy.getLocale());
  }

  @Test
  public void testDefaultName() {
    assertNull(ServerJobInput.defaults().getName());
    assertEquals("ABC", ServerJobInput.defaults().setName("ABC").getName());
  }

  @Test
  public void testDefaultId() {
    assertNull(ServerJobInput.defaults().getId());
    assertEquals("123", ServerJobInput.defaults().setId("123").getId());
  }

  @Test
  public void testDefaultSubject() {
    assertNull(ServerJobInput.defaults().getSubject());

    Subject subject = new Subject();
    ServerJobInput input = Subject.doAs(subject, new PrivilegedAction<ServerJobInput>() {

      @Override
      public ServerJobInput run() {
        return ServerJobInput.defaults();
      }
    });
    assertSame(subject, input.getSubject());

    subject = new Subject();
    input = Subject.doAs(subject, new PrivilegedAction<ServerJobInput>() {

      @Override
      public ServerJobInput run() {
        return ServerJobInput.defaults();
      }
    });
    input.setSubject(null);
    assertNull(input.getSubject());
  }

  @Test
  public void testDefaultSessionRequired() {
    assertTrue(ServerJobInput.defaults().isSessionRequired());
  }

  @Test
  public void testSessionRequiredCopy() {
    assertTrue(ServerJobInput.empty().setSessionRequired(true).copy().isSessionRequired());
    assertFalse(ServerJobInput.empty().setSessionRequired(false).copy().isSessionRequired());
  }

  @Test
  public void testDefaultSession() {
    // No session on ThreadLocal
    ISession.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setSessionRequired(false).getSession());

    // Session on ThreadLocal
    IServerSession sessionThreadLocal = mock(IServerSession.class);
    ISession.CURRENT.set(sessionThreadLocal);
    assertSame(sessionThreadLocal, ServerJobInput.defaults().getSession());

    // Session on ThreadLocal, but set explicitly
    ISession.CURRENT.set(sessionThreadLocal);
    IServerSession explicitSession = mock(IServerSession.class);
    assertSame(explicitSession, ServerJobInput.defaults().setSession(explicitSession).getSession());

    // Session on ThreadLocal, but set explicitly to null
    ISession.CURRENT.set(sessionThreadLocal);
    assertNull(ServerJobInput.defaults().setSession(null).setSessionRequired(false).getSession());
  }

  @Test
  public void testDefaultLocale() {
    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> Locale from ThreadLocal
    ISession.CURRENT.set(session);
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.CANADA_FRENCH, ServerJobInput.defaults().getLocale());

    // ThreadLocal set, Session available --> Locale from ThreadLocal
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.CANADA_FRENCH, ServerJobInput.defaults().getLocale());

    // ThreadLocal not set, Session not available --> no fallback to JVM default Locale.
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertNull(ServerJobInput.defaults().getLocale());
  }

  @Test
  public void testDefaultLocaleAndSetNullLocale() {
    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> explicit Locale (null)
    ISession.CURRENT.set(session);
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertNull(ServerJobInput.defaults().setLocale(null).setSession(session).getLocale());

    // ThreadLocal set, Session not available --> explicit Locale (null)
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertNull(ServerJobInput.defaults().setLocale(null).setSession(session).getLocale());

    // ThreadLocal not set, Session not available --> explicit Locale (null)
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setLocale(null).setSession(session).getLocale());
  }

  @Test
  public void testDefaultLocaleAndSetNotNullLocale() {
    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> explicit Locale (JAPAN)
    ISession.CURRENT.set(session);
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.JAPAN, ServerJobInput.defaults().setLocale(Locale.JAPAN).setSession(session).getLocale());

    // ThreadLocal set, Session not available --> explicit Locale (JAPAN)
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.JAPAN, ServerJobInput.defaults().setLocale(Locale.JAPAN).setSession(session).getLocale());

    // ThreadLocal not set, Session not available --> explicit Locale (JAPAN)
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertEquals(Locale.JAPAN, ServerJobInput.defaults().setLocale(Locale.JAPAN).setSession(session).getLocale());
  }

  @Test
  public void testDerivedLocaleWhenSettingSession() {
    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set --> Locale form ThreadLocal
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.CANADA_FRENCH, ServerJobInput.defaults().setSession(session).getLocale());

    // ThreadLocal not set --> Null Locale
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setSession(session).getLocale());

    IServerSession currentSession = mock(IServerSession.class);

    // ThreadLocal-Session available, ThreadLocal set --> Locale from ThreadLocal
    ISession.CURRENT.set(currentSession);
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.CANADA_FRENCH, ServerJobInput.defaults().setSession(session).getLocale());

    // ThreadLocal-Session available, ThreadLocal not set --> Null Locale
    ISession.CURRENT.set(currentSession);
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setSession(session).getLocale());
  }

  @Test
  public void testDefaultUserAgent() {
    UserAgent userAgent = newUserAgent();

    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> UserAgent from ThreadLocal
    ISession.CURRENT.set(session);
    UserAgent.CURRENT.set(userAgent);
    assertSame(userAgent, ServerJobInput.defaults().getUserAgent());

    // ThreadLocal set, Session available --> UserAgent from ThreadLocal
    ISession.CURRENT.remove();
    UserAgent.CURRENT.set(userAgent);
    assertEquals(userAgent, ServerJobInput.defaults().getUserAgent());

    // ThreadLocal not set, Session not available
    ISession.CURRENT.remove();
    UserAgent.CURRENT.remove();
    assertNull(ServerJobInput.defaults().getUserAgent());
  }

  @Test
  public void testDefaultUserAgentAndSetNullUserAgent() {
    UserAgent userAgent = newUserAgent();
    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> explicit UserAgent (null)
    ISession.CURRENT.set(session);
    UserAgent.CURRENT.set(userAgent);
    assertNull(ServerJobInput.defaults().setUserAgent(null).setSession(session).getUserAgent());

    // ThreadLocal set, Session not available --> explicit UserAgent (null)
    ISession.CURRENT.remove();
    UserAgent.CURRENT.set(userAgent);
    assertNull(ServerJobInput.defaults().setUserAgent(null).setSession(session).getUserAgent());

    // ThreadLocal not set, Session not available --> explicit UserAgent (null)
    ISession.CURRENT.remove();
    UserAgent.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setUserAgent(null).setSession(session).getUserAgent());
  }

  @Test
  public void testDefaultUserAgentAndSetNotNullUserAgent() {
    UserAgent userAgent1 = newUserAgent();
    UserAgent userAgent2 = newUserAgent();

    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set, Session available --> explicit UserAgent (userAgent2)
    ISession.CURRENT.set(session);
    UserAgent.CURRENT.set(userAgent1);
    assertSame(userAgent2, ServerJobInput.defaults().setUserAgent(userAgent2).setSession(session).getUserAgent());

    // ThreadLocal set, Session not available --> explicit UserAgent (userAgent2)
    ISession.CURRENT.remove();
    UserAgent.CURRENT.set(userAgent1);
    assertSame(userAgent2, ServerJobInput.defaults().setUserAgent(userAgent2).setSession(session).getUserAgent());

    // ThreadLocal not set, Session not available --> explicit UserAgent (userAgent2)
    ISession.CURRENT.remove();
    NlsLocale.CURRENT.remove();
    assertSame(userAgent2, ServerJobInput.defaults().setUserAgent(userAgent2).setSession(session).getUserAgent());
  }

  @Test
  public void testDerivedUserAgentWhenSettingSession() {
    UserAgent userAgent1 = newUserAgent();

    IServerSession session = mock(IServerSession.class);

    // ThreadLocal set --> UserAgent form ThreadLocal
    ISession.CURRENT.remove();
    UserAgent.CURRENT.set(userAgent1);
    assertSame(userAgent1, ServerJobInput.defaults().setSession(session).getUserAgent());

    // ThreadLocal not set --> Null UserAgent
    ISession.CURRENT.remove();
    UserAgent.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setSession(session).getUserAgent());

    IServerSession currentSession = mock(IServerSession.class);

    // ThreadLocal-Session available, ThreadLocal set --> UserAgent from ThreadLocal
    ISession.CURRENT.set(currentSession);
    ISession.CURRENT.remove();
    UserAgent.CURRENT.set(userAgent1);
    assertSame(userAgent1, ServerJobInput.defaults().setSession(session).getUserAgent());

    // ThreadLocal-Session available, ThreadLocal not set --> Null UserAgent
    ISession.CURRENT.set(currentSession);
    ISession.CURRENT.remove();
    UserAgent.CURRENT.remove();
    assertNull(ServerJobInput.defaults().setSession(session).getUserAgent());
  }

  @Test
  public void testDefaultPropertyMap() {
    PropertyMap threadLocalContext = new PropertyMap();
    threadLocalContext.put("prop", "value");

    // No context on ThreadLocal
    PropertyMap.CURRENT.remove();
    assertNotNull(ServerJobInput.defaults().getContext());

    // Context on ThreadLocal
    PropertyMap.CURRENT.set(threadLocalContext);
    assertNotSame(threadLocalContext, ServerJobInput.defaults().getContext());
    assertEquals(toSet(threadLocalContext.iterator()), toSet(ServerJobInput.defaults().getPropertyMap().iterator()));
  }

  private static Set<Object> toSet(Iterator<?> iterator) {
    Set<Object> set = new HashSet<>();
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return set;
  }

  private static UserAgent newUserAgent() {
    return UserAgent.create(UiLayer.UNKNOWN, UiDeviceType.UNKNOWN, "n/a");
  }
}
