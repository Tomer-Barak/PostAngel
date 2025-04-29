# PostMuse App

An Android application that helps users create and respond to social media posts using AI and a personal knowledge base.

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
5. Edit the post as needed and share directly to your preferred social media platform

### For Response Suggestions:
1. Take a screenshot of a social media post you want to respond to
2. Tap the "Share" button
3. Select "Analyze with PostMuse" from the share menu
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
   - **Response Style**: Configure the tone and style of your AI-generated content
4. Tap "Save" to apply the changes

## Technical Details

- Uses OkHttp library for secure API requests to OpenAI
- Supports Android API level 26 (Android 8.0) and above
- Handles images shared from any application that supports Android's share functionality
- Securely processes images using OpenAI's GPT-4 Vision API
- Employs advanced LLM prompting to generate contextually relevant posts and responses
- Maintains a local knowledge base of user-defined topics
- Intelligently determines when and how to promote user topics in responses
- Presents AI-generated content in an easy-to-use, editable interface
- Open source design with privacy-focused implementation

## Development

The app's settings are stored in SharedPreferences and can be changed through the Settings screen. The defaults are:
- Server URL: "https://api.openai.com/v1/chat/completions"
- OpenAI API Key: "" (must be provided by the user)

These settings are managed by the `PrefsUtil` class:

```kotlin
object PrefsUtil {
    // Gets the server URL from SharedPreferences
    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }
    
    // Sets the server URL in SharedPreferences
    fun setServerUrl(context: Context, serverUrl: String) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, serverUrl).apply()
    }
    
    // Gets the OpenAI API key from SharedPreferences
    fun getOpenAIApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENAI_API_KEY, "") ?: ""
    }
    
    // Sets the OpenAI API key in SharedPreferences
    fun setOpenAIApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_OPENAI_API_KEY, apiKey).apply()
    }
}
```

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

#### OpenAI API Integration:

For Vision Analysis:
```json
{
  "model": "gpt-4-vision-preview",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Analyze this social media post and describe its content and topic."
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,..."
          }
        }
      ]
    }
  ],
  "max_tokens": 500
}
```

For Response Generation:
```json
{
  "model": "gpt-4-1106-preview",
  "messages": [
    {
      "role": "system",
      "content": "Generate a relevant response to a social media post that naturally incorporates the user's preferred topics when appropriate."
    },
    {
      "role": "user",
      "content": "Post content: [Content from Vision API analysis]\nMy topics: [Topics from knowledge base]\nCreate a natural response that promotes one of my topics if relevant."
    }
  ],
  "max_tokens": 300
}
```

For Post Creation:
```json
{
  "model": "gpt-4-1106-preview",
  "messages": [
    {
      "role": "system",
      "content": "Create an engaging social media post about the user's selected topics."
    },
    {
      "role": "user",
      "content": "Create a social media post about: [Selected topics]"
    }
  ],
  "max_tokens": 350
}
```

## Advanced Features: Python Integration

PostMuse includes optional Python scripts that can extend its functionality for power users:

### Twitter Integration
The `python_scripts` directory contains utilities to:
- Monitor Twitter/X for relevant conversations about your topics
- Automatically identify opportunities to promote your knowledge base topics
- Generate responses using the same AI models as the Android app
- Manage usage limits and track engagement

To use these scripts:
1. Create a `postmuse.env` file in the project root with your Twitter API credentials
2. Configure your topics in the knowledge base
3. Run the Twitter agent to monitor and respond to relevant conversations

```bash
# Example of running the Twitter agent
cd python_scripts
python -m twitter_agent
```

### Technical Integration Notes
- The Android app and Python scripts share the same knowledge base format
- You can export your knowledge base from the app to use with the Python scripts
- The same OpenAI models are used in both the app and Python scripts for consistency

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
