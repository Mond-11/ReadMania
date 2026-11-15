import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { setAccountBlockedHandler, setUnauthorizedHandler } from '../../../shared/api/index.js';
import { ACCESS } from '../../../shared/labels.js';
import { notifyError } from '../../../shared/notify.js';
import { CURRENT_USER_KEY, resetSession } from '../api.js';
import { AUTH_PATHS } from '../paths.js';

// Reacts to session events reported by the API client. Renders nothing; lives once in Layout.
export function AuthSessionHandlers() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const location = useLocation();
  const locationRef = useRef(location);

  useEffect(() => {
    locationRef.current = location;
  }, [location]);

  useEffect(() => {
    // 401 on a protected request: the session has expired. A Гість getting 401 is not news, so only a known user is logged out.
    setUnauthorizedHandler(() => {
      if (!queryClient.getQueryData(CURRENT_USER_KEY)) return;
      resetSession(queryClient, null);
      notifyError(ACCESS.sessionExpired, { raw: true });
      const { pathname, search } = locationRef.current;
      navigate(AUTH_PATHS.login, { state: { from: pathname + search } });
    });

    // M6: the Менеджер blocked the account while the session was open. The login page shows the reason and the unblock request (M7).
    setAccountBlockedHandler((error) => {
      const email = queryClient.getQueryData(CURRENT_USER_KEY)?.email ?? '';
      resetSession(queryClient, null);
      navigate(AUTH_PATHS.login, {
        replace: true,
        state: { blocked: { reason: error.blockReason ?? error.detail, blockedAt: error.blockedAt, email } },
      });
    });

    return () => {
      setUnauthorizedHandler(null);
      setAccountBlockedHandler(null);
    };
  }, [navigate, queryClient]);

  return null;
}
