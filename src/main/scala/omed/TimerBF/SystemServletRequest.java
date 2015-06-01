package omed.TimerBF;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

/**
 * Класс для замены контекста запроса
 */

public class SystemServletRequest implements HttpServletRequest {
    private int domain;
    private String sessionId;

    public void setSession(int domain, String session) {
        this.domain =domain;
        this.sessionId = session;
    }
    HttpSession session = new HttpSession() {
        @Override
        public long getCreationTime() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String getId() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public long getLastAccessedTime() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public ServletContext getServletContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setMaxInactiveInterval(int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public int getMaxInactiveInterval() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public HttpSessionContext getSessionContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object getAttribute(String s) {
            if(s == "sessionId") return sessionId;
            if(s == "domainId") return domain;
            if(s == "isSuperUser") return true;
            if(s == "timeZone") return TimeZone.getTimeZone("GMT+04");
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Object getValue(String s) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public Enumeration getAttributeNames() {

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public String[] getValueNames() {
            return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void setAttribute(String s, Object o) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void putValue(String s, Object o) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeAttribute(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void removeValue(String s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void invalidate() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isNew() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    @Override
    public String getAuthType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Cookie[] getCookies() {
        return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getDateHeader(String s) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getHeader(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration getHeaders(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration getHeaderNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getIntHeader(String s) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getMethod() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPathInfo() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPathTranslated() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getContextPath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getQueryString() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRemoteUser() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Principal getUserPrincipal() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRequestedSessionId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRequestURI() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getServletPath() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HttpSession getSession(boolean b) {
        return session;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public HttpSession getSession() {
        return session;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getAttribute(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration getAttributeNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getCharacterEncoding() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getContentLength() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getContentType() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getParameter(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration getParameterNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getParameterValues(String s) {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map getParameterMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getProtocol() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getScheme() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getServerName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getServerPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRemoteAddr() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRemoteHost() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAttribute(String s, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeAttribute(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Locale getLocale() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration getLocales() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isSecure() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getRealPath(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getRemotePort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getLocalName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getLocalAddr() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getLocalPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
