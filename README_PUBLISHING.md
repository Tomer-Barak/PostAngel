# PostMuse

An Android application for creating social media content and generating contextual responses based on a personal knowledge base.

## Features

- Create AI-generated social media posts based on your preferred topics
- Share screenshots of social media posts to get suggested responses
- Intelligently incorporate your topics into responses when relevant
- Maintain a personal knowledge base of topics you want to promote
- Configure your own OpenAI API key for secure processing
- Open source and privacy-focused design

## Publishing to Google Play Store

### 1. Creating a Signing Key

Before generating a signed APK or App Bundle, you need to create a signing key:

```bash
keytool -genkey -v -keystore postmuse.keystore -alias postmuse -keyalg RSA -keysize 2048 -validity 10000
```

This will prompt you for:
- Keystore password
- Key password
- Name, organization, location details

Store the keystore file and passwords securely. **If you lose them, you cannot update your app on Google Play Store!**

### 2. Configure Signing in build.gradle

Update the `signingConfigs` section in `app/build.gradle`:

```groovy
signingConfigs {
    release {
        storeFile file('path/to/your/postmuse.keystore')
        storePassword "your-store-password"
        keyAlias "barakbot"
        keyPassword "your-key-password"
    }
}
```

### 3. Building a Signed App Bundle (Recommended)

In Android Studio:
1. Select Build > Generate Signed Bundle/APK
2. Select Android App Bundle
3. Follow the prompts to select your keystore, key alias, and passwords
4. Select release build variant
5. Click Finish

The bundle will be saved in `app/release/app-release.aab`

### 4. Building a Signed APK (Alternative)

In Android Studio:
1. Select Build > Generate Signed Bundle/APK
2. Select APK
3. Follow the prompts to select your keystore, key alias, and passwords
4. Select release build variant
5. Click Finish

The APK will be saved in `app/release/app-release.apk`

### 5. Google Play Store Submission

1. Create a Google Play Developer account ($25 one-time fee)
2. Create a new application in the Google Play Console
3. Fill in all required information:
   - App details
   - Store listing content (use the content from `play_store_listing.md`)
   - Graphics assets
   - Content rating questionnaire
   - Pricing & distribution settings
4. Upload the signed AAB or APK
5. Submit for review

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle and build the project

## Testing

Run the app on a device or emulator with:
```bash
./gradlew installDebug
```

Run tests with:
```bash
./gradlew test
```
