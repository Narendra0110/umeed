// Everything is configured in app.json. This file only adds what must not be fixed in Git:
//
// 1. The map plugin, so that the Google Maps key comes from the environment variable
//    GOOGLE_MAPS_ANDROID_API_KEY instead of being stored in Git. The key is restricted in the Google Cloud
//    console to the Umeed Android app. Without it the app still builds and runs; the map is just empty.
//
// 2. Test builds over Wi-Fi (build-test-apk.cmd sets UMEED_TEST_BUILD=1): Android blocks plain http:// in
//    a standalone app, but the PC's backend at home has no HTTPS. Only test builds allow it. A normal or
//    Play Store build never does, and talks to the hosted backend over HTTPS (M03).
const testBuild = process.env.UMEED_TEST_BUILD === '1';

function withTestBuild(plugins) {
  if (!testBuild) {
    return plugins;
  }
  return plugins.map((plugin) => {
    if (Array.isArray(plugin) && plugin[0] === 'expo-build-properties') {
      const options = plugin[1] ?? {};
      return ['expo-build-properties', { ...options, android: { ...options.android, usesCleartextTraffic: true } }];
    }
    return plugin;
  });
}

module.exports = ({ config }) => ({
  ...config,
  plugins: [
    ...withTestBuild(config.plugins ?? []),
    ['react-native-maps', { androidGoogleMapsApiKey: process.env.GOOGLE_MAPS_ANDROID_API_KEY ?? '' }],
  ],
});
