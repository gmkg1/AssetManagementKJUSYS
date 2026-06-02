const { shareAll, withModuleFederationPlugin } = require('@angular-architects/module-federation/webpack');

module.exports = withModuleFederationPlugin({

  // ── Expose routes/components from this micro-frontend ──────────────────────
  // Uncomment and fill in when this app acts as a Remote:
  // name: 'assetManagement',
  // filename: 'remoteEntry.js',
  // exposes: {
  //   './Module': './src/app/asset-management/asset-management.module.ts',
  // },

  // ── Shared singleton libraries ─────────────────────────────────────────────
  // strictVersion: false so the app works standalone without a host shell
  shared: {
    ...shareAll({
      singleton: true,
      strictVersion: false,
      requiredVersion: 'auto',
    }),
  },

});
