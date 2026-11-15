export const lazyPage = (loader) => ({
  lazy: async () => ({ Component: (await loader()).default }),
});
