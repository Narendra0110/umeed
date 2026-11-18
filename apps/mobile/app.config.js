// Everything is configured in app.json. This file only adds the map plugin, so that the Google Maps
// key comes from the environment variable GOOGLE_MAPS_ANDROID_API_KEY instead of being stored in Git.
// The key is restricted in the Google Cloud console to the Umeed Android app. Without it the app
// still builds and runs; the map is just empty.
module.exports = ({ config }) => ({
  ...config,
  plugins: [
    ...(config.plugins ?? []),
    ['react-native-maps', { androidGoogleMapsApiKey: process.env.GOOGLE_MAPS_ANDROID_API_KEY ?? '' }],
  ],
});
