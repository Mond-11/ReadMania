// Each feature exports an array of routes from features/<name>/routes.jsx; new features are picked up automatically.
const modules = import.meta.glob('../features/*/routes.jsx', { eager: true });

export const featureRoutes = Object.values(modules).flatMap((m) => m.default ?? []);
