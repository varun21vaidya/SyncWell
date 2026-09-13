import "dotenv/config";

export default ({ config }) => {
  return {
    ...config,
    name: "SyncWell",
    slug: "syncwell",
    owner: "vrn2107",
    version: "1.0.0",
    scheme: "syncwell",
    orientation: "portrait",
    userInterfaceStyle: "automatic",
    icon: "./assets/icon.png",
    android: {
      package: process.env.ANDROID_PACKAGE || "com.syncwell.app",
      adaptiveIcon: {
        foregroundImage: "./assets/adaptive-icon-foreground.png",
        backgroundImage: "./assets/adaptive-icon-background.png",
      },
      permissions: [
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.WAKE_LOCK",
      ],
    },
    extra: {
      eas: {
        projectId: "33907780-d28a-4ed3-affd-aa629ef28c43",
      },
    },
    plugins: [
      "expo-router",
      "expo-font",
      "expo-splash-screen",
      [
        "expo-build-properties",
        { android: { compileSdkVersion: 35, buildToolsVersion: "35.0.0" } },
      ],
      ["expo-dev-client", { launchMode: "most-recent" }],
      [
        "@sentry/react-native/expo",
        { organization: "syncwell", project: "syncwell-mobile" },
      ],
      [
        "react-native-health-connect",
        {
          permissions: ["READ_STEPS"],
          rationaleMarkdown: "Track daily steps.",
        },
      ],
      ["expo-notifications", { icon: "./assets/icon.png", color: "#10B981" }],
      [
        "@react-native-google-signin/google-signin",
        { webClientId: process.env.EXPO_PUBLIC_GOOGLE_OAUTH_CLIENT_ID },
      ],
      [
        "expo-build-properties",
        {
          android: {
            compileSdkVersion: 35,
            targetSdkVersion: 34,
            buildToolsVersion: "35.0.0",
            kotlinVersion: "1.9.25",
          },
        },
      ],
    ],
  };
};
