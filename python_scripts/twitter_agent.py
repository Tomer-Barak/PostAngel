import json
from pathlib import Path
import os
from typing import Dict, List, Optional, Union, Tuple
import requests
import datetime
from . import twitter_utils
import threading
import random

ROUND_ROBIN_CACHE_FILE = Path(__file__).resolve().parent / "round_robin_cache.json"


# Configure logging
# logger = logging.getLogger("twitter_agent")


class TwitterAgent:
    """
    Agent for monitoring Twitter and responding to promote specific topics.
    This implements the workflow pipeline for analyzing tweets and determining promotional responses.
    """

    def __init__(self):
        """Initialize the Twitter agent with configuration"""
        # Remove direct config and user file loading
        self.check_and_reset_limits()

    @property
    def config(self) -> Dict:
        """Get the current configuration from twitter_utils."""
        return twitter_utils.load_config()

    @property
    def users(self) -> Dict:
        """Get the current users from twitter_utils."""
        return twitter_utils.load_users()

    def check_and_reset_limits(self) -> None:
        """Check if a month has passed and reset limits if needed (delegated to twitter_utils)."""
        twitter_utils.check_and_reset_limits()

    def increment_read_counter(self, count: int = 1) -> None:
        """Increment the counter for read messages (delegated to twitter_utils)."""
        twitter_utils.update_read_counter(count)

    def increment_send_counter(self, count: int = 1) -> None:
        """Increment the counter for sent messages (delegated to twitter_utils)."""
        twitter_utils.update_send_counter(count)

    def can_read_more(self, count: int = 1) -> bool:
        """Check if we're still within the monthly read limit (delegated to twitter_utils)."""
        return twitter_utils.can_read_messages(count)

    def can_send_more(self, count: int = 1) -> bool:
        """Check if we're still within the monthly send limit (delegated to twitter_utils)."""
        return twitter_utils.can_send_messages(count)

    def get_topic_description(self, topic: str) -> str:
        """
        Get a description of the topic to be promoted by loading it from a text file.
        
        Args:
            topic: The topic to describe (e.g., "AI_Ethics", "Sustainable_Energy")
            
        Returns:
            A string describing the topic loaded from the corresponding file
        """
        # Convert topic name to a valid filename (replace spaces with underscores)
        topic_filename = topic.replace(" ", "_")

        # Path to the topic description file
        topic_file_path = Path(__file__).resolve().parent / "Topics" / f"{topic_filename}.txt"

        try:
            # Check if the file exists
            if not topic_file_path.exists():
                return f"No description available for {topic}. Please create a file named {topic_filename}.txt in the Topics directory."

            # Read the topic description from the file
            with open(topic_file_path, 'r', encoding='utf-8') as file:
                description = file.read().strip()

            return description

        except Exception as e:
            return f"Error loading description for {topic}. Please check the Topics directory and ensure the file exists and is readable."

    def analyze_tweet(self, tweet, topic: str) -> Tuple[bool, Optional[str]]:
        """
        Analyze a tweet to determine if there's an opportunity to promote the given topic.
        
        Args:
            tweet: The tweet data to analyze
            topic: The topic to check for promotion opportunities
            
        Returns:
            A tuple of (has_opportunity, response_idea)
            - has_opportunity: Boolean indicating whether there's a good opportunity to respond
            - response_idea: String with the response idea if has_opportunity is True, None otherwise
        """
        # Get the topic description to use in the analysis
        topic_description = self.get_topic_description(topic)

        # Construct the prompt for the AI to analyze the tweet
        prompt = self._construct_analysis_prompt(tweet, topic, topic_description)

        try:
            import openai
            import os

            # Initialize the OpenAI client with DeepInfra
            client = openai.OpenAI(
                api_key=os.environ.get("DEEPSEEK_API_TOKEN"),
                base_url="https://api.deepseek.com"
            )

            # Call AI to analyze the tweet
            response = client.chat.completions.create(
                model="deepseek-chat",
                messages=[
                    {"role": "system",
                     "content": "You are a Twitter assistant analyzing tweets for promotional opportunities."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=512
            )

            # Extract the response content
            ai_response = response.choices[0].message.content

            # Parse the response to extract opportunity and idea
            has_opportunity = False
            response_idea = None

            # Look for the OPPORTUNITY: and IDEA: tags in the response
            for line in ai_response.split('\n'):
                if line.strip().startswith("OPPORTUNITY:"):
                    opportunity_text = line.strip().replace("OPPORTUNITY:", "").strip().upper()
                    has_opportunity = opportunity_text == "YES"
                elif line.strip().startswith("IDEA:"):
                    response_idea = line.strip().replace("IDEA:", "").strip()
                    if not response_idea:  # If the idea is empty, set it to None
                        response_idea = None

            return has_opportunity, response_idea

        except Exception as e:
            return False, None

    def _construct_analysis_prompt(self, tweet: Dict, topic: str, topic_description: str) -> str:
        """
        Construct a prompt for the AI to analyze whether a tweet presents an opportunity to promote a topic.
        
        Args:
            tweet: The tweet data
            topic: The topic to check for promotion opportunities
            topic_description: Description of the topic
            
        Returns:
            Prompt string for the AI
        """
        instructions = f"""
        You are analyzing a tweet to determine if there's an opportunity to promote a specific topic.
        
        Your task:
        1. Analyze the content of the tweet.
        2. Determine if there's a natural way to respond to this tweet while promoting the topic.
        3. If there is, provide a brief idea for a response tweet that promotes the topic.
        4. If there isn't a natural way to respond, indicate this clearly.
        
        Your response should be formatted as:
        OPPORTUNITY: [YES/NO]
        IDEA: [Your response idea if OPPORTUNITY is YES, otherwise leave blank]
        
        Keep in mind:
        - The response should feel natural and relevant to the original tweet
        - The promotion should be subtle and not forced
        - The response should be respectful and professional
        """

        if type(tweet) == str:
            Tweet_str = tweet
        else:
            tweet_content = tweet.get("text", "")
            tweet_author = tweet.get("author_username", "")
            Tweet_str = f"""Author: @{tweet_author}
                        Content: {tweet_content}"""

        prompt = f"""{instructions}

        ## Topic to promote: {topic}
        
        ## Topic description:
        {topic_description}
        -------------------------------------------------------------------
        
        ## Tweet:
        {Tweet_str}
        
        -------------------------------------------------------------------

        Analyze the tweet and provide your assessment.
        """

        return prompt

    def process_tweet(self, tweet: Dict, topic: str) -> Optional[str]:
        """
        Process a tweet and determine if/how to respond to promote a topic.
        
        Args:
            tweet: The tweet data to analyze
            topic: The topic to check for promotion opportunities
            
        Returns:
            Response tweet text if there's an opportunity, None otherwise
        """
        if not self.can_read_more():
            return None

        # Increment the read counter since we're analyzing a tweet
        self.increment_read_counter()

        # Analyze the tweet to see if there's an opportunity
        has_opportunity, response_idea = self.analyze_tweet(tweet, topic)

        if has_opportunity and self.can_send_more():
            # In a real implementation, we would generate the actual response text here
            # For now, we'll use the response idea as the response
            response_text = response_idea

            # Increment the send counter since we're sending a response
            self.increment_send_counter()

            return response_text

        return None

    def generate_promotional_tweet(self, topic: str) -> Optional[str]:
        """
        Generate a new promotional tweet about a specific topic.
        
        Args:
            topic: The topic to create a promotional tweet for (e.g., "AI_Ethics", "Sustainable_Energy")
            
        Returns:
            Generated tweet text if within monthly limits, None otherwise
        """
        import openai
        import os

        # Check if we're within the monthly send limit
        if not self.can_send_more():
            return None

        # Get the topic description to use in the generation
        topic_description = self.get_topic_description(topic)

        # Construct the prompt for the AI to generate a promotional tweet
        prompt = self._construct_generation_prompt(topic, topic_description)

        try:
            # Initialize the OpenAI client with DeepInfra
            client = openai.OpenAI(
                api_key=os.environ.get("DEEPSEEK_API_TOKEN"),
                base_url="https://api.deepseek.com"
            )

            # Call AI to generate the promotional tweet
            response = client.chat.completions.create(
                model="'deepseek-chat'",
                messages=[
                    {"role": "system", "content": "You are a Twitter assistant creating promotional tweets."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=80
            )

            # Extract the response content
            ai_response = response.choices[0].message.content

            # Parse the response to extract the tweet
            tweet_text = None

            # Look for the TWEET: tag in the response
            for line in ai_response.split('\n'):
                if line.strip().startswith("TWEET:"):
                    tweet_text = line.strip().replace("TWEET:", "").strip()
                    break

            # If no TWEET: tag found, use the entire response (but trim it to Twitter's character limit)
            if not tweet_text:
                tweet_text = ai_response.strip()

            # Twitter has a 280 character limit per tweet
            # if len(tweet_text) > 280:
            #     tweet_text = tweet_text[:277] + "..."

            # Increment the send counter since we're creating a new tweet
            # self.increment_send_counter()

            return tweet_text

        except Exception as e:
            return None

    def _construct_generation_prompt(self, topic: str, topic_description: str) -> str:
        """
        Construct a prompt for the AI to generate a promotional tweet about a topic.
        
        Args:
            topic: The topic to create a promotional tweet for
            topic_description: Description of the topic
            
        Returns:
            Prompt string for the AI
        """
        instructions = f"""
        You are creating a tweet to promote a specific topic.
        
        Your task:
        1. Create an engaging, informative tweet that promotes the topic.
        2. Make the tweet feel authentic, not like an advertisement.
        3. Include relevant hashtags if appropriate.
        4. Ensure the tweet is under 280 characters!
        
        Your response should be formatted as:
        TWEET: [Your tweet text including any hashtags]
        
        Keep in mind:
        - The tweet should be engaging and encourage interaction
        - The promotion should be subtle and thoughtful
        - The tweet should be conversational and personable
        - Feel free to ask thought-provoking questions, share interesting facts, or offer insights
        """

        prompt = f"""{instructions}

        Topic to promote: {topic}
        
        Topic description:
        {topic_description}
        
        Create an engaging promotional tweet for this topic.
        """

        return prompt

    def fetch_and_analyze_tweets_round_robin(self, users: List[Dict], topic: str) -> Optional[Dict]:
        """
        Implements round-robin fetching and analyzing of tweets for a list of users.
        Returns a dict with info if an opportunity is found, else None.
        """
        # Thread lock for cache file
        lock = threading.Lock()

        # Load or initialize cache
        with lock:
            if ROUND_ROBIN_CACHE_FILE.exists():
                with open(ROUND_ROBIN_CACHE_FILE, 'r', encoding='utf-8') as f:
                    cache = json.load(f)
            else:
                cache = {"last_user_idx": -1, "last_analyzed": {}}

        user_count = len(users)
        if user_count == 0:
            return None

        # Start from next user in round robin
        start_idx = (cache.get("last_user_idx", -1) + 1) % user_count
        checked = 0
        found_opp = None

        while checked < user_count:
            user = users[start_idx]
            user_id = user["user_id"]
            username = user["username"]
            last_analyzed_id = cache["last_analyzed"].get(str(user_id))

            # Fetch 5 most recent tweets
            tweets = twitter_utils.get_user_tweets(user_id, max_results=5)
            # Tweets should be dicts with 'id' and 'created_at'
            # Sort newest to oldest (Twitter API usually returns newest first)
            new_tweets = []
            for tweet in tweets:
                if last_analyzed_id is None or str(tweet["id"]) > str(last_analyzed_id):
                    new_tweets.append(tweet)
                else:
                    break  # Tweets are sorted, so stop at first old one

            newest_analyzed_id = None
            for tweet in new_tweets:
                if newest_analyzed_id is None:
                    newest_analyzed_id = tweet["id"]
                has_opp, idea = self.analyze_tweet(tweet, topic)
                if has_opp:
                    found_opp = {
                        "user": username,
                        "tweet": tweet,
                        "response_idea": idea
                    }
                    break
            # After analyzing, update cache with the newest tweet id analyzed (if any)
            if newest_analyzed_id is not None:
                cache["last_analyzed"][str(user_id)] = newest_analyzed_id
                with lock:
                    with open(ROUND_ROBIN_CACHE_FILE, 'w', encoding='utf-8') as f:
                        json.dump(cache, f, indent=2)
            if found_opp:
                # Update round robin index
                cache["last_user_idx"] = start_idx
                with lock:
                    with open(ROUND_ROBIN_CACHE_FILE, 'w', encoding='utf-8') as f:
                        json.dump(cache, f, indent=2)
                notify_user_of_opportunity(found_opp)
                return found_opp
            # No opportunity, move to next user
            start_idx = (start_idx + 1) % user_count
            checked += 1
        # Update round robin index even if no opp found
        cache["last_user_idx"] = (start_idx - 1) % user_count
        with lock:
            with open(ROUND_ROBIN_CACHE_FILE, 'w', encoding='utf-8') as f:
                json.dump(cache, f, indent=2)
        return None

    def get_topics(self):
        """
        Reads the content of Twitter/Topics and returns a list of topics.
        Each topic is a dict with 'topic' (filename without .txt) and 'description' (from the Description: line, or empty string).
        """
        topics_dir = Path(__file__).resolve().parent / "Topics"
        topics = []
        for file in topics_dir.glob("*.txt"):
            topic_name = file.stem
            description = ""
            try:
                with open(file, 'r', encoding='utf-8') as f:
                    for line in f:
                        line = line.strip()
                        if line.lower().startswith("description:"):
                            description = line[len("description:"):].strip()
                            break
            except Exception as e:
                pass
            topics.append({"topic": topic_name, "description": description})
        return topics

    def search_opportunity(self, tweet: str):
        """
        Runs analyze_tweet with the given tweet on all possible topics and returns the output for all topics.
        Returns a list of dicts: { 'topic', 'has_opportunity', 'response_idea' }
        """
        results = 'None found'
        topics = self.get_topics()
        random.shuffle(topics)
        for topic_info in topics:
            topic = topic_info['topic']
            has_opportunity, response_idea = self.analyze_tweet(tweet, topic)
            if has_opportunity:
                return {
                    'topic': topic,
                    'has_opportunity': has_opportunity,
                    'response_idea': response_idea
                }
        return results


# Example usage
def check_promotion_opportunity(tweet_data: Dict, topic: str) -> Tuple[bool, Optional[str]]:
    """
    Function to check if a tweet presents an opportunity to promote a topic.
    
    Args:
        tweet_data: The tweet data to analyze
        topic: The topic to check for promotion opportunities (e.g., "X", "Y", or "Z")
        
    Returns:
        A tuple of (has_opportunity, response_idea)
    """
    agent = TwitterAgent()
    has_opportunity, response_idea = agent.analyze_tweet(tweet_data, topic)
    return has_opportunity, response_idea


def generate_topic_promotion(topic: str) -> Optional[str]:
    """
    Function to generate a promotional tweet about a specific topic.
    
    Args:
        topic: The topic to create a promotional tweet for (e.g., "X", "Y", or "Z")
        
    Returns:
        Generated tweet text if within monthly limits, None otherwise
    """
    agent = TwitterAgent()
    return agent.generate_promotional_tweet(topic)


def send_message_to_bot(chat_id, sender, message):
    url = "http://rp:5000/webhook"  # The webhook URL
    message_data = {
        "message": {
            "chat": {"id": chat_id},
            "from": {"username": sender, "first_name": sender},
            "text": 'Twitter agent has found an opportunity. Tell about it to Tomer by using the RESPOND action:\n' + message
        }
    }
    # Send the message to the bot
    response = requests.post(url, json=message_data)
    if response.status_code == 200:
        print("Message sent successfully!")
    else:
        print(f"Failed to send message. Status code: {response.status_code}, Response: {response.text}")


def notify_user_of_opportunity(opportunity_info: Dict):
    """
    Notify the user about a found opportunity using the bot notification scheme.
    """
    chat_id = 6457672353
    sender = "twitter_agent"
    tweet = opportunity_info.get('tweet', {})
    user = opportunity_info.get('user', '')
    response_idea = opportunity_info.get('response_idea', '')
    tweet_url = f"https://twitter.com/{user}/status/{tweet.get('id', '')}"
    message = f"User: @{user}\nTweet: {tweet.get('text', '')}\nLink: {tweet_url}\n\nSuggested response:\n{response_idea}"
    send_message_to_bot(chat_id, sender, message)


if __name__ == "__main__":
    pass

    # Example usage of the TwitterAgent class
    agent = TwitterAgent()

    # # Example tweet data
    tweet = '''SPC: Evolving Self-Play Critic via Adversarial Games for LLM Reasoning

'we introduce SelfPlay Critic (SPC), a novel approach where a critic model evolves its ability to assess reasoning steps through adversarial self-play games, eliminating the need for manual step-level annotation. SPC involves fine-tuning two copies of a base model to play two roles, namely a "sneaky generator" that deliberately produces erroneous steps designed to be difficult to detect, and a "critic" that analyzes the correctness of reasoning steps. These two models engage in an adversarial game in which the generator aims to fool the critic, while the critic model seeks to identify the generator's errors.'
    '''
    #

    print(agent.search_opportunity(tweet))
    exit()
    # # # Check for promotion opportunity
    # has_opportunity, response_idea = agent.analyze_tweet(tweet, "scale_symmetry")
    # print(f"Has opportunity: {has_opportunity}, Response idea: {response_idea}")
    # #
    # # Generate a promotional tweet
    # generated_tweet = agent.generate_promotional_tweet("scale_math")
    # print(f"Generated promotional tweet: {generated_tweet}")

    # exit()

    # # Example of checking and resetting limits
    # agent.check_and_reset_limits()
    # print(f"Current monthly reads: {agent.config['limits']['current_month_reads']}")
    # print(f"Current monthly sends: {agent.config['limits']['send_messages_monthly_limit']}")
    # print(f"Last reset date: {agent.config['limits']['last_reset_date']}")
    # print(f"Read limit: {agent.config['limits']['read_messages_monthly_limit']}")
    # print(f"Send limit: {agent.config['limits']['send_messages_monthly_limit']}")
    # print(f"Can read more: {agent.can_read_more()}")
    # print(f"Can send more: {agent.can_send_more()}")

    # # --- DEMONSTRATION: Read next unread tweet for each user and analyze for promotion ---
    # print("\n--- Checking next unread tweet for each tracked user ---")
    # from twitter_utils import read_next_tweet_from_users

    # topic = "AI_Ethics"  # Example topic
    # next_tweets = read_next_tweet_from_users()
    # for entry in next_tweets:
    #     tweet = entry["tweet"]
    #     username = entry["username"]
    #     print(f"\nUser: @{username}\nTweet: {tweet.get('text')}")
    #     has_opp, idea = agent.analyze_tweet({
    #         "text": tweet.get("text"),
    #         "author_username": username
    #     }, topic)
    #     print(f"Promotion opportunity: {has_opp}")
    #     if has_opp:
    #         print(f"Suggested response idea: {idea}")
    #     else:
    #         print("No suitable promotion opportunity found.")
    # print("--- End demonstration ---\n")
