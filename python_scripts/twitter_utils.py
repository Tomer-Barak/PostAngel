from requests_oauthlib import OAuth1Session
import os
import json
import dotenv
from pathlib import Path
import requests
import datetime
from typing import Dict, List, Optional
import sys

dotenv.load_dotenv(Path(__file__).resolve().parent.parent / "postmuse.env")

consumer_key = os.environ.get("TWITTER_API_KEY")
consumer_secret = os.environ.get("TWITTER_API_KEY_SECRET")
access_token = os.environ.get("TWITTER_ACCESS_TOKEN")
access_token_secret = os.environ.get("TWITTER_ACCESS_TOKEN_SECRET")
bearer_token = os.environ.get("TWITTER_BEARER_TOKEN")

# # Configure logging
# logging.basicConfig(
#     level=logging.INFO,
#     format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
#     handlers=[
#         logging.StreamHandler(sys.stdout),
#         logging.FileHandler(Path(__file__).resolve().parent.parent / "postmuse.log")
#     ]
# )
# logger = logging.getLogger("twitter_agent")

# File paths
CONFIG_FILE = Path(__file__).resolve().parent / "twitter_config.json"
USERS_FILE = Path(__file__).resolve().parent / "users_to_track.json"

def bearer_oauth(r):
    """
    Method required by bearer token authentication.
    """

    r.headers["Authorization"] = f"Bearer {bearer_token}"
    r.headers["User-Agent"] = "v2UserTweetsPython"
    return r

def read_messages(user_id=1454680326):
    url = "https://api.twitter.com/2/users/{}/tweets".format(user_id)
    params = {"tweet.fields": "created_at"}
    response = requests.request("GET", url, auth=bearer_oauth, params=params)
    if response.status_code != 200:
        raise Exception(
            "Request returned an error: {} {}".format(
                response.status_code, response.text
            )
        )
    json_response = response.json()
    print(json.dumps(json_response, indent=2, sort_keys=True))


def send_message(tweet):

    oauth = OAuth1Session(
        consumer_key,
        client_secret=consumer_secret,
        resource_owner_key=access_token,
        resource_owner_secret=access_token_secret,
    )

    # Making the request
    response = oauth.post(
        "https://api.twitter.com/2/tweets",
        json=tweet,
    )

    if response.status_code != 201:
        raise Exception(
            "Request returned an error: {} {}".format(response.status_code, response.text)
        )

    print("Response code: {}".format(response.status_code))

    # Saving the response as JSON
    json_response = response.json()
    print(json.dumps(json_response, indent=4, sort_keys=True))

def create_access_tokens():

    # Get request token
    request_token_url = "https://api.twitter.com/oauth/request_token?oauth_callback=oob&x_auth_access_type=write"
    oauth = OAuth1Session(consumer_key, client_secret=consumer_secret)

    try:
        fetch_response = oauth.fetch_request_token(request_token_url)
    except ValueError:
        print(
            "There may have been an issue with the consumer_key or consumer_secret you entered."
        )
        exit()

    resource_owner_key = fetch_response.get("oauth_token")
    resource_owner_secret = fetch_response.get("oauth_token_secret")
    print("Got OAuth token: %s" % resource_owner_key)

    # Get authorization
    base_authorization_url = "https://api.twitter.com/oauth/authorize"
    authorization_url = oauth.authorization_url(base_authorization_url)
    print("Please go here and authorize: %s" % authorization_url)
    verifier = input("Paste the PIN here: ")

    # Get the access token
    access_token_url = "https://api.twitter.com/oauth/access_token"
    oauth = OAuth1Session(
        consumer_key,
        client_secret=consumer_secret,
        resource_owner_key=resource_owner_key,
        resource_owner_secret=resource_owner_secret,
        verifier=verifier,
    )
    oauth_tokens = oauth.fetch_access_token(access_token_url)

    print(f"Got access token: {oauth_tokens['oauth_token']}")
    print(f"Got access token secret: {oauth_tokens['oauth_token_secret']}")


def load_config() -> Dict:
    """Load the Twitter agent configuration."""
    try:
        with open(CONFIG_FILE, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        default_config = {
            "limits": {
                "read_messages_monthly_limit": 100,
                "send_messages_monthly_limit": 500,
                "current_month_reads": 0,
                "current_month_sends": 0,
                "last_reset_date": datetime.datetime.now().strftime("%Y-%m-%d")
            }
        }
        save_config(default_config)
        return default_config

def save_config(config: Dict) -> None:
    """Save the Twitter agent configuration."""
    with open(CONFIG_FILE, 'w') as f:
        json.dump(config, f, indent=4)

def load_users() -> Dict:
    """Load the list of users to track."""
    try:
        with open(USERS_FILE, 'r') as f:
            return json.load(f)
    except FileNotFoundError:
        return {"users": []}

def save_users(users: Dict) -> None:
    """Save the list of users to track."""
    with open(USERS_FILE, 'w') as f:
        json.dump(users, f, indent=4)

def check_and_reset_limits() -> Dict:
    """Check if it's a new month and reset the usage counters if needed."""
    config = load_config()
    today = datetime.datetime.now()
    last_reset = datetime.datetime.strptime(config["limits"]["last_reset_date"], "%Y-%m-%d")
    
    # If it's a new month, reset the counters
    if today.year > last_reset.year or (today.year == last_reset.year and today.month > last_reset.month):
        config["limits"]["current_month_reads"] = 0
        config["limits"]["current_month_sends"] = 0
        config["limits"]["last_reset_date"] = today.strftime("%Y-%m-%d")
        save_config(config)
    
    return config

def can_read_messages(count: int = 1) -> bool:
    """Check if we can read more messages this month."""
    config = check_and_reset_limits()
    current_reads = config["limits"]["current_month_reads"]
    monthly_limit = config["limits"]["read_messages_monthly_limit"]
    
    if current_reads + count <= monthly_limit:
        return True
    else:
        return False

def update_read_counter(count: int = 1) -> None:
    """Update the read messages counter."""
    config = load_config()
    config["limits"]["current_month_reads"] += count
    save_config(config)

def can_send_messages(count: int = 1) -> bool:
    """Check if we can send more messages this month."""
    config = check_and_reset_limits()
    current_sends = config["limits"]["current_month_sends"]
    monthly_limit = config["limits"]["send_messages_monthly_limit"]
    
    if current_sends + count <= monthly_limit:
        return True
    else:
        return False

def update_send_counter(count: int = 1) -> None:
    """Update the send messages counter."""
    config = load_config()
    config["limits"]["current_month_sends"] += count
    save_config(config)

def get_user_tweets(user_id: str, max_results: int = 10) -> List[Dict]:
    """Get tweets from a specific user."""
    url = f"https://api.twitter.com/2/users/{user_id}/tweets"

    params = {
        "tweet.fields": "created_at,text,referenced_tweets",
        "expansions": "referenced_tweets.id",
        "max_results": max_results
    }

    headers = {
        "Authorization": f"Bearer {bearer_token}"
    }

    response = requests.get(url, headers=headers, params=params)

    if response.status_code != 200:
        print(f"Error: {response.status_code} - {response.text}")
        return []

    data = response.json()

    tweets = data.get("data", [])
    referenced = {tweet["id"]: tweet for tweet in data.get("includes", {}).get("tweets", [])}

    full_texts = []
    for tweet in tweets:
        if "referenced_tweets" in tweet:
            # If it's a retweet or quote, get the original text
            referenced_id = tweet["referenced_tweets"][0]["id"]
            original = referenced.get(referenced_id)
            if original:
                full_texts.append(original["text"])
            else:
                full_texts.append(tweet["text"])  # fallback
        else:
            full_texts.append(tweet["text"])

    return full_texts

def read_user_messages(max_tweets_per_user: int = 5, specific_users: Optional[List[str]] = None) -> Dict:
    """
    Read messages from users in the track list.
    
    Args:
        max_tweets_per_user: Maximum number of tweets to fetch per user
        specific_users: Optional list of usernames to check (if None, check all users)
    
    Returns:
        Dictionary with results of the operation
    """
    results = {
        "success": False,
        "tweets_read": 0,
        "users_checked": 0,
        "errors": []
    }
    
    # Load users and config
    users_data = load_users()
    if not users_data.get("users"):
        results["errors"].append("No users found in tracking list")
        return results
        
    users_to_check = users_data["users"]
    
    # Filter specific users if requested
    if specific_users:
        users_to_check = [u for u in users_to_check if u["username"].lower() in [name.lower() for name in specific_users]]
        if not users_to_check:
            results["errors"].append("None of the specified users found in tracking list")
            return results
    
    # Calculate total expected tweets
    total_expected_tweets = len(users_to_check) * max_tweets_per_user
    
    # Check if we have enough quota
    if not can_read_messages(total_expected_tweets):
        remaining_quota = load_config()["limits"]["read_messages_monthly_limit"] - load_config()["limits"]["current_month_reads"]
        results["errors"].append(f"Not enough remaining quota. Need {total_expected_tweets}, have {remaining_quota}")
        return results
    
    # Read tweets from each user
    total_tweets_read = 0
    users_checked = 0
    
    for user in users_to_check:
        user_id = user["user_id"]
        username = user["username"]
        
        logger.info(f"Reading tweets from {username} (ID: {user_id})")
        
        tweets = get_user_tweets(user_id, max_results=max_tweets_per_user)
        tweets_read = len(tweets)
        if tweets_read > 0:
            total_tweets_read += tweets_read
            users_checked += 1
            user["last_checked"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        else:
            pass
    
    # Update user data with last checked timestamps
    save_users(users_data)
    
    # Update our read counter
    if total_tweets_read > 0:
        update_read_counter(total_tweets_read)
    
    # Update results
    results["success"] = True
    results["tweets_read"] = total_tweets_read
    results["users_checked"] = users_checked
    return results

def get_remaining_quota() -> Dict:
    """Get information about remaining read/write quotas."""
    config = check_and_reset_limits()
    limits = config["limits"]
    
    return {
        "read": {
            "used": limits["current_month_reads"],
            "limit": limits["read_messages_monthly_limit"],
            "remaining": limits["read_messages_monthly_limit"] - limits["current_month_reads"],
        },
        "write": {
            "used": limits["current_month_sends"],
            "limit": limits["send_messages_monthly_limit"],
            "remaining": limits["send_messages_monthly_limit"] - limits["current_month_sends"],
        },
        "last_reset": limits["last_reset_date"],
        "next_reset": get_next_reset_date(limits["last_reset_date"])
    }

def get_next_reset_date(last_reset_date: str) -> str:
    """Calculate when the quota will reset next."""
    last_reset = datetime.datetime.strptime(last_reset_date, "%Y-%m-%d")
    
    # Move to first day of next month
    if last_reset.month == 12:
        next_reset = datetime.datetime(last_reset.year + 1, 1, 1)
    else:
        next_reset = datetime.datetime(last_reset.year, last_reset.month + 1, 1)
        
    return next_reset.strftime("%Y-%m-%d")

def read_next_tweet_from_users(usernames: Optional[List[str]] = None, newest_first: bool = True) -> List[Dict]:
    """
    For each user (in the given order), fetch their next unread tweet (newest first),
    update last_seen_tweet_id, and return a list of tweets (one per user).
    Only increments quota for tweets actually returned.
    
    Args:
        usernames: Optional ordered list of usernames to check. If None, use all users.
        newest_first: If True, fetch the newest unread tweet. If False, fetch the oldest unread tweet not yet seen.
    Returns:
        List of dicts: [{ 'username': ..., 'user_id': ..., 'tweet': ... }]
    """
    users_data = load_users()
    if not users_data.get("users"):
        return []

    users = users_data["users"]
    if usernames:
        username_set = [u.lower() for u in usernames]
        users = [u for u in users if u["username"].lower() in username_set]
        # preserve order
        users.sort(key=lambda u: username_set.index(u["username"].lower()))

    results = []
    tweets_read = 0
    for user in users:
        user_id = user["user_id"]
        username = user["username"]
        last_seen_id = user.get("last_seen_tweet_id")
        # Always fetch 5 tweets to be robust (Twitter API default max is 5 for recent)
        tweets = get_user_tweets(user_id, max_results=5)
        if not tweets:
            continue
        # Sort tweets by id (Twitter returns newest first)
        if not newest_first:
            tweets = list(reversed(tweets))
        # Find the first tweet that is newer than last_seen_tweet_id
        next_tweet = None
        for tweet in tweets:
            if last_seen_id is None or str(tweet["id"]) != str(last_seen_id):
                next_tweet = tweet
                break
        if next_tweet:
            # Update last_seen_tweet_id
            user["last_seen_tweet_id"] = next_tweet["id"]
            # Optionally update last_checked timestamp
            user["last_checked"] = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            results.append({
                "username": username,
                "user_id": user_id,
                "tweet": next_tweet
            })
            tweets_read += 1
        else:
            pass
    # Save updated user data
    if tweets_read > 0:
        save_users(users_data)
        update_read_counter(tweets_read)
    return results

if __name__ == '__main__':
    read_next_tweet_from_users()