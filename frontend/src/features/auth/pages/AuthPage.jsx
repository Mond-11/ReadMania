import { Tabs } from '@mantine/core';
import { useLocation, useNavigate } from 'react-router';
import { AUTH } from '../../../shared/labels.js';
import { AuthCard } from '../components/AuthCard.jsx';
import { LoginForm } from '../components/LoginForm.jsx';
import { RegisterForm } from '../components/RegisterForm.jsx';
import { AUTH_PATHS } from '../paths.js';

// One page «Вхід і реєстрація» (UX lab 3) with two tabs; each tab has its own URL: /login and /register.
export default function AuthPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const tab = location.pathname === AUTH_PATHS.register ? 'register' : 'login';

  // History state (`from`, `blocked`) is kept when switching tabs, so the user still returns where they started.
  const switchTab = (value) => {
    if (value && value !== tab) navigate(AUTH_PATHS[value], { replace: true, state: location.state });
  };

  return (
    <AuthCard title={AUTH.pageTitle} crumb={tab === 'login' ? AUTH.loginTab : AUTH.registerTab}>
      <Tabs value={tab} onChange={switchTab} keepMounted={false}>
        <Tabs.List grow mb="md">
          <Tabs.Tab value="login">{AUTH.loginTab}</Tabs.Tab>
          <Tabs.Tab value="register">{AUTH.registerTab}</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="login">
          <LoginForm blocked={location.state?.blocked} />
        </Tabs.Panel>
        <Tabs.Panel value="register">
          <RegisterForm />
        </Tabs.Panel>
      </Tabs>
    </AuthCard>
  );
}
