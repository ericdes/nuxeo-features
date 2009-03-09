package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.ecm.platform.web.common.tx.TransactionsHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter to handle Transactions and Requests synchronization. This filter is
 * useful when accessing web resources that are not protected by Seam Filter.
 * This is the case for specific Servlets, WebEngine, XML-RPC connector ...
 * 
 * @author tiry
 */
public class NuxeoRequestControllerFilter implements Filter {

    protected static final String SESSION_LOCK_KEY = "NuxeoSessionLockKey";

    protected static final String SYNCED_REQUEST_FLAG = "NuxeoSessionAlreadySync";

    protected static final int LOCK_TIMOUT_S = 120;

    protected static RequestControllerManager rcm;

    private static final Log log = LogFactory.getLog(NuxeoRequestControllerFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        rcm = Framework.getLocalService(RequestControllerManager.class);

        if (rcm == null) {
            log.error("Unable to get RequestControlerManager service");
            throw new ServletException(
                    "RequestControlerManager can not be found");
        }
        log.debug("Staring NuxeoRequestControler filter");
    }

    protected String doFormatLogMessage(HttpServletRequest request, String info) {
        String remoteHost = RemoteHostGuessExtractor.getRemoteHost(request);
        Principal principal = request.getUserPrincipal();
        String principalName = principal != null ? principal.getName() : "none";
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "none";
        String threadName = Thread.currentThread().getName();
        return "remote=" + remoteHost + ",principal=" + principalName
                + ",uri=" + uri + ",session="+sessionId+",thread="+threadName+",info=" + info;
    }
    
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(httpRequest, "Entering NuxeoRequestControler filter"));
        }

        RequestFilterConfig config = rcm.getConfigForRequest(httpRequest);

        boolean useSync = config.needSynchronization();
        boolean useTx = config.needTransaction();

        if (!useSync && !useTx) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(httpRequest,
                        "Existing NuxeoRequestControler filter : nothing to be done"));
            }
            chain.doFilter(request, response);
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(httpRequest,
                    "Handling request with tx=" + useTx + " and sync="
                            + useSync));
        }

        boolean sessionSynched = false;
        if (useSync) {
            sessionSynched = simpleSyncOnSession(httpRequest);
        }
        boolean txStarted = false;
        try {
            if (useTx) {
                txStarted = startUserTransaction(httpRequest);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            log.error(doFormatLogMessage(httpRequest, "Unhandled error was cauth by the Filter"), e);
            if (txStarted) {
                if (log.isDebugEnabled()) {
                    log.debug(doFormatLogMessage(httpRequest, "Marking transaction for RollBack"));
                }
                markTransactionForRollBack(httpRequest);
            }
            throw new ServletException(e);
        } finally {
            if (txStarted) {
                commitOrRollBackUserTransaction(httpRequest);
            }
            if (sessionSynched) {
                simpleReleaseSyncOnSession(httpRequest);
            }
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(httpRequest,"Exiting NuxeoRequestControler filter"));
            }
        }
    }

    /**
     * Starts a new {@link UserTransaction}.
     * 
     * @return true if the transaction was successfully translated, false
     *         otherwise
     */
    protected boolean startUserTransaction(HttpServletRequest request) {
        try {
            TransactionsHelper.getUserTransaction().begin();
        } catch (Exception e) {
            log.error(doFormatLogMessage(request,"Unable to start transaction"), e);
            return false;
        }
        return true;
    }

    /**
     * Marks the {@link UserTransaction} for rollBack.
     */
    protected void markTransactionForRollBack(HttpServletRequest request) {
        try {
            TransactionsHelper.getUserTransaction().setRollbackOnly();
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "NuxeoRequestControler setting transaction to RollBackOnly"));
            }
        } catch (Exception e) {
            log.error(doFormatLogMessage(request, "Unable to rollback transaction"), e);
        }
    }

    /**
     * Commits or rollbacks the {@link UserTransaction} depending on the
     * Transaction status.
     */
    protected void commitOrRollBackUserTransaction(HttpServletRequest request) {
        try {
            if (TransactionsHelper.isTransactionActiveOrMarkedRollback()) {
                if (TransactionsHelper.isTransactionMarkedRollback()) {
                    if (log.isDebugEnabled()) {
                        log.debug(doFormatLogMessage(request, "can not commit transaction since it is marked RollBack only"));
                    }
                    TransactionsHelper.getUserTransaction().rollback();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(doFormatLogMessage(request,"NuxeoRequestControler commiting transaction"));
                    }
                    TransactionsHelper.getUserTransaction().commit();
                }
            }
        } catch (Exception e) {
                log.error(doFormatLogMessage(request,
                    "Unable to commit/rollback transaction"), e);
        }
    }

    /**
     * Synchronizes the HttpSession.
     * <p>
     * Uses a {@link Lock} object in the HttpSession and locks it. If
     * HttpSession is not created, exits without locking anything.
     */
    protected boolean simpleSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "HttpSession does not exist, this request won't be synched"));
            }
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request, "Trying to sync on session "));
        }

        if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
            if (log.isWarnEnabled()) {
                log.warn(doFormatLogMessage(request,
                        "Request has already be synced, filter is reentrant, exiting without locking"));
            }
            return false;
        }

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new ReentrantLock();
            session.setAttribute(SESSION_LOCK_KEY, lock);
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(doFormatLogMessage(request, "Unable to acuire lock for Session sync"), e);
            return false;
        }

        if (locked) {
            request.setAttribute(SYNCED_REQUEST_FLAG, true);
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "Request synced on session"));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "Sync timeout"));
            }
        }
        
        return locked;
    }

    /**
     * Releases the {@link Lock} if present in the HttpSession.
     */
    protected void simpleReleaseSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(
                        request,
                        "No more HttpSession : can not unlock !, HttpSession must have been invalidated"));
            }
            return;
        }
        log.debug("Trying to unlock on session " + session.getId()
                + " on Thread " + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            log.error("Unable to find session lock, HttpSession may have been invalidated");
        } else {
            lock.unlock();
            if (log.isDebugEnabled()) {
                log.debug("session unlocked on Thread ");
            }
        }
    }

    public void destroy() {
        rcm = null;
    }

}
