# PostMuse App

![PostMuse Logo](icon.png)

An Android application that helps users create and respond to social media posts using AI and a personal knowledge base.

## Download

You can download the app directly from:
- [GitHub Releases](https://github.com/Tomer-Barak/PostMuse/releases) - Latest APK
- [F-Droid](https://f-droid.org/packages/com.postmuse.screenshare/) - Available after F-Droid review process

## Features

- Create AI-generated social media posts based on your personal topics
- Share screenshots of social media posts to get suggested responses
- Intelligent response generation that promotes your preferred topics when relevant
- Uses OpenAI's Vision API to analyze screenshots of social media posts
- Uses OpenAI's language models to create contextual responses and posts
- Configurable settings for your OpenAI API Key
- Maintain a personal knowledge base of topics you want to promote
- Open source and privacy-focused

## Setup

1. Clone this repository
2. Open the project in Android Studio
3. Build and run the application on your device
4. You'll need to set up your OpenAI API key in the Settings screen
5. Configure your knowledge base by adding topics you want to promote
6. By default, the app will use OpenAI's Vision API and language models

## How to Use

### For Social Media Post Creation:
1. Open the app from your app drawer
2. Select "Create Post" from the main menu
3. Choose from your list of topics or themes to focus on
4. The app will generate a social media post based on your topics
5. Copy the post and share directly to your preferred social media platform

### For Response Suggestions:
1. Take a screenshot of a social media post you want to respond to
2. Tap the "Share" button
3. Select "Send to PostMuse" from the share menu
4. The app will:
   - Analyze the screenshot using OpenAI's Vision API
   - Compare the content to your knowledge base of topics
   - Generate a contextually relevant response that promotes your topics when appropriate
5. Edit the suggested response if needed and share it to social media

### Configuring App Settings

1. Open the app from your app drawer
2. Tap the "Settings" button on the main screen
3. You can configure the following settings:
   - **OpenAI API Key**: Your personal API key from OpenAI (required for using the AI services)
   - **Knowledge Base**: Add, edit, or remove topics you want to promote in your posts and responses
4. Tap "Save" to apply the changes

## Technical Details

- Uses OkHttp library for secure API requests to OpenAI
- Supports Android API level 26 (Android 8.0) and above
- Handles images shared from any application that supports Android's share functionality
- Securely processes images using OpenAI's Vision API
- Employs advanced LLM prompting to generate contextually relevant posts and responses
- Maintains a local knowledge base of user-defined topics
- Intelligently determines when and how to promote user topics in responses
- Presents AI-generated content in an easy-to-use, editable interface
- Open source design with privacy-focused implementation


### How PostMuse Works

PostMuse combines multiple AI capabilities to help users create and respond to social media content:

#### Screenshot Analysis Flow:
1. When you share a social media post screenshot, it's processed by OpenAI's Vision API
2. The app analyzes the content to understand the post's topic and context
3. It then compares this context with your knowledge base of preferred topics
4. If there's a relevant match, it generates a response that naturally incorporates your topic
5. If no direct match, it provides a contextual response based on the post content

#### Post Creation Flow:
1. You select topics from your knowledge base
2. The app uses OpenAI's language models to craft a social media post
3. The post is designed to effectively communicate about your selected topics
4. You can edit the post before sharing it to your preferred platforms


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
