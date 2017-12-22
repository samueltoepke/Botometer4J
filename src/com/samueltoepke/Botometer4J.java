/*
Copyright 2017 Samuel Lee Toepke

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package ant_test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.commons.lang3.StringUtils;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
 
/**
* Botometer4J class. Object that creates a connection to the Twitter REST
* API and the Botometer API to facilitate easy access to Botometer from 
* Java code.
* 
* @author  Samuel Lee Toepke
* @version 1.0
*/
public class Botometer4J {
 
    private final static Logger logger = LogManager.getLogger(Botometer4J.class);
         
    private String mashape_key = "";
    private String consumer_key = "";
    private String consumer_secret = "";
    private String access_token = "";
    private String access_token_secret = "";
    
    /**
    * Constructor for the Botometer4J object. Requires accurate security
    * strings to access the Twitter REST API and the Mashape API.
    * 
	* @param mashape_key security key for Mashape.
 	* @param consumer_key security key for Twitter.
	* @param consumer_secret security key for Twitter.
	* @param access_token security key for Twitter.
	* @param access_token_secret security key for Twitter.
	* @return instantiated Botometer4J object.
	*/
    public Botometer4J(String mashape_key, String consumer_key, String consumer_secret, String access_token, String access_token_secret) {
        String method = "Botometer4J: ";
        logger.debug(method + "STARTING.");
		 
        if (StringUtils.isEmpty(mashape_key) || StringUtils.isEmpty(consumer_key) || StringUtils.isEmpty(consumer_secret) || StringUtils.isEmpty(access_token) || StringUtils.isEmpty(access_token_secret)) {
            logger.error(method + "constructor arguments incorrect, make sure all populated.");
            System.exit(1);
        } else {
	        this.mashape_key         = mashape_key;
	        this.consumer_key        = consumer_key;
	        this.consumer_secret     = consumer_secret;
	        this.access_token        = access_token;
	        this.access_token_secret = access_token_secret;		
			
            logger.debug(method + "mashape_key:         " + this.mashape_key);
            logger.debug(method + "consumer_key:        " + this.consumer_key);
            logger.debug(method + "consumer_secret:     " + this.consumer_secret);
            logger.debug(method + "access_token:        " + this.access_token);
            logger.debug(method + "access_token_secret: " + this.access_token_secret);
		}
          
        logger.debug(method + "ENDING.");
    }
     
   /**
    * Takes a Twitter screen_name as input, reach out to Twitter REST 
    * API to build query for Botometer, reach out to Botometer to get 
    * the 'bot-like' data. Do not include "@" at the beginning of the 
    * screen name.
    * 
	* @param screen_name The Twitter user for which to get the data.
	* @return JSON String per the documentation https://market.mashape.com/OSoMe/botometer
	*/
    public String getBotometerResponseForScreenName(String screen_name) {
		String togo = "";
		String method = "getBotometerResponseForScreenName: ";
		logger.debug(method + "STARTING.");
		logger.debug(method + "input: " + screen_name);
		
		// Making sure all necessary variables loaded.
		if (StringUtils.isEmpty(this.mashape_key) || StringUtils.isEmpty(this.consumer_key) || StringUtils.isEmpty(this.consumer_secret) || StringUtils.isEmpty(this.access_token) || StringUtils.isEmpty(this.access_token_secret)) {
            logger.error(method + "security arguments not loaded, run constructor first.");
            System.exit(1);
        }
        
        // Making sure input isn't empty.
		if (StringUtils.isEmpty(screen_name)) {
            logger.error(method + "screen_name argument incorrect, make sure populated.");
            System.exit(1);
        }

		// Get Connection to Twitter Web Service.
   		logger.debug(method + "get Connection to Twitter Web Service.");
        ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(this.consumer_key);
		cb.setOAuthConsumerSecret(this.consumer_secret);
		cb.setOAuthAccessToken(this.access_token);
		cb.setOAuthAccessTokenSecret(this.access_token_secret);
        cb.setJSONStoreEnabled(true);
		
		TwitterFactory tf = new TwitterFactory(cb.build());
		Twitter twitter = tf.getInstance();
		
		JsonObject mashape_request_body = new JsonObject();  // Will include User information, Tweets and Mentions
		
		try {
			// 0. Get User Information
    		User   user     = twitter.showUser(screen_name);
    		
    		JsonObject user_json = new JsonObject();
    		user_json.addProperty("id", Long.toString(user.getId()));
    		user_json.addProperty("screen_name", user.getScreenName());
    		
    		mashape_request_body.add("user", user_json);
    		    		    		
    		logger.debug(method + "User information retrieved.");
    		    				    
		    // 1. Get Twitter Stream Information
		    //https://stackoverflow.com/questions/17887984/is-it-possible-to-get-more-than-100-tweets
		    int pages = 1;
		    int max_tweets = 200; // 200 Tweets per: https://market.mashape.com/OSoMe/botometer/overview
            List<Status> statuses = new ArrayList();

            for (;;) {
                int size = statuses.size(); 
                Paging page = new Paging(pages++, 101); 
                statuses.addAll(twitter.getUserTimeline(screen_name, page));
    
                if (statuses.size() == size || statuses.size() > max_tweets)
                    break;
            }
		    
		    int count = 0;
		    JsonArray statuses_json = new JsonArray();
		    for (Status status : statuses) {
				statuses_json.add(getJsonFromStatus(status));
	            
                count++;
                
                if (count == max_tweets)
                    break;
			}

            mashape_request_body.add("timeline", statuses_json);
		    logger.debug(method + "# Tweets retrieved: " + statuses.size());
		    
		    // 2. Get Mentions Information
		    // https://stackoverflow.com/questions/18800610/how-to-retrieve-more-than-100-results-using-twitter4j
	        List<Status> mentions = new ArrayList();
		    
		    Query query = new Query("@" + screen_name);
		    query.setCount(100); // 100 Mentions per: https://market.mashape.com/OSoMe/botometer/overview
		    query.setResultType(Query.RECENT);
	
	        QueryResult result = twitter.search(query);
            mentions.addAll(result.getTweets());
      
            JsonArray mentions_array = new JsonArray();
		    for (Status status : mentions) 
				mentions_array.add(getJsonFromStatus(status));
	        			
			mashape_request_body.add("mentions", mentions_array);
		    logger.debug(method + "# Mentions retrieved: " + mentions.size());
		    logger.debug(method + "Completed JSON Payload: " + mashape_request_body.toString());
		    		    
		    // 3. Create/Execute Mashape Request using Unirest
		    // https://market.mashape.com/OSoMe/botometer	    
		    HttpResponse<JsonNode> response = Unirest.post("https://osome-botometer.p.mashape.com/2/check_account")
                .header("X-Mashape-Key", this.mashape_key)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(mashape_request_body.toString())
                .asJson();
		    
		    logger.debug("Mashape Response Status: " + response.getStatus());
		    logger.debug("Mashape Response Body:   " + response.getBody());
		    
		    togo = response.getBody().toString();

            Unirest.shutdown();

        } catch (TwitterException te) {
            te.printStackTrace();
            logger.error(method + " Twitter Exception.");
            logger.error(te.toString());
        } catch (IOException io) {
            io.printStackTrace();
            logger.error(io.toString());
        } catch (UnirestException ue) {
            ue.printStackTrace();
            logger.error(ue.toString());
        }
		
		logger.debug(method + "ENDING.");
		return togo;		
	}
	
   /**
    * Takes a Status, and create a JSON object that
    * can be parsed by the Botometer web service. Direct deserialization
    * using Gson would be ideal, but the fields don't line up exactly.
    * 
	* @param status object.
	* @return JsonObject
	*/
    private JsonObject getJsonFromStatus(Status status) {
		JsonObject togo = null;
        String method = "getJsonFromStatus: ";
        logger.debug(method + "STARTING.");
		
        if (status == null) {
            logger.error(method + "constructor arguments incorrect, make sure all populated.");
            System.exit(1);
        } 
        
        JsonObject status_json = new JsonObject();
        
        status_json.addProperty("id", status.getId());
        status_json.addProperty("id_str", Long.toString(status.getId()));
        status_json.addProperty("possibly_sensitive", status.isPossiblySensitive());
        status_json.addProperty("favorited", status.isFavorited());
        status_json.addProperty("lang", status.getLang());
        status_json.addProperty("retweeted", status.isRetweeted());
        status_json.addProperty("retweet_count", status.getRetweetCount());
        status_json.addProperty("truncated", status.isTruncated());
        status_json.addProperty("contributors", (String) null); // All 'null' in Botometer sample payload.
        status_json.addProperty("favorite_count", status.getFavoriteCount());
        status_json.addProperty("source", status.getSource());
        status_json.addProperty("text", status.getText());
        status_json.addProperty("in_reply_to_screen_name", status.getInReplyToScreenName());  
        status_json.addProperty("coordinates", (String) null); // All 'null' in Botometer sample payload.
        status_json.addProperty("geo", (String) null);         // All 'null' in Botometer sample payload.
        status_json.addProperty("place", (String) null);       // All 'null' in Botometer sample payload.
		status_json.addProperty("created_at", getFormattedDate(status.getCreatedAt()));

        if (status.getInReplyToUserId() == -1) {
            status_json.addProperty("in_reply_to_user_id", (String) null);
            status_json.addProperty("in_reply_to_user_id_str", (String) null);
        } else {
			status_json.addProperty("in_reply_to_user_id", status.getInReplyToUserId());
            status_json.addProperty("in_reply_to_user_id_str", Long.toString(status.getInReplyToUserId()));
		}
        
        if (status.getInReplyToStatusId() == -1) {
            status_json.addProperty("in_reply_to_status_id", (String) null);
            status_json.addProperty("in_reply_to_status_id_str", (String) null);
        } else {
			status_json.addProperty("in_reply_to_status_id", status.getInReplyToStatusId());
            status_json.addProperty("in_reply_to_status_id_str", Long.toString(status.getInReplyToStatusId()));
		}
		
        if (status.getQuotedStatus() == null) // Field "only surfaces when the Tweet is a 'quote tweet'." https://developer.twitter.com/en/docs/tweets/data-dictionary/overview/tweet-object
            status_json.addProperty("is_quote_status", false);
        else
            status_json.addProperty("is_quote_status", true);
        
        JsonArray urls_array = new JsonArray();
        URLEntity[] url_entities = status.getURLEntities();
        
        for (int i = 0; i < url_entities.length; i++) {
            JsonObject temp_entity = new JsonObject();
            JsonArray temp_array = new JsonArray();
            
            temp_entity.addProperty("url", url_entities[i].getURL());
            temp_entity.addProperty("expanded_url", url_entities[i].getExpandedURL());
            temp_entity.addProperty("display_url", url_entities[i].getDisplayURL());
            
            temp_array.add(url_entities[i].getStart());
            temp_array.add(url_entities[i].getEnd());
            temp_entity.add("indices", temp_array);
        
            urls_array.add(temp_entity);
        }
        
        JsonArray symbols_array = new JsonArray(); // All 'null' in Botometer sample payload.
                
        JsonArray hashtags_array = new JsonArray();
        HashtagEntity[] hashtag_entities = status.getHashtagEntities();
        
        for (int i = 0; i < hashtag_entities.length; i++) {
            JsonObject temp_entity = new JsonObject();
            JsonArray temp_array = new JsonArray();
            
            temp_entity.addProperty("text", hashtag_entities[i].getText());
            
            temp_array.add(hashtag_entities[i].getStart());
            temp_array.add(hashtag_entities[i].getEnd());
            temp_entity.add("indices", temp_array);
        
            hashtags_array.add(temp_entity);
        }
        
        JsonArray user_mentions_array = new JsonArray();
        UserMentionEntity[] user_mention_entities = status.getUserMentionEntities();
        
        for (int i = 0; i < user_mention_entities.length; i++) {
            JsonObject temp_entity = new JsonObject();
            JsonArray temp_array = new JsonArray();
            
            temp_entity.addProperty("name", user_mention_entities[i].getName());
            temp_entity.addProperty("id_str", Long.toString(user_mention_entities[i].getId()));
            temp_entity.addProperty("id", user_mention_entities[i].getId());
            temp_entity.addProperty("screen_name", user_mention_entities[i].getScreenName());
            
            temp_array.add(user_mention_entities[i].getStart());
            temp_array.add(user_mention_entities[i].getEnd());
            temp_entity.add("indices", temp_array);
        
            user_mentions_array.add(temp_entity);
        }
                
        JsonObject entities = new JsonObject();
        entities.add("urls", urls_array);
        entities.add("symbols", symbols_array);
        entities.add("user_mentions", user_mentions_array);
        entities.add("hashtags", hashtags_array);
        
        status_json.add("entities", entities);
        
        if (status.getRetweetedStatus() != null)
			status_json.add("retweeted_status", getJsonFromStatus(status.getRetweetedStatus()));
	            
	    status_json.add("user", getJsonFromUser(status.getUser()));        
	            
        togo = status_json;
          
        logger.debug(method + "ENDING.");
        return togo;
    }
    
   /**
    * Takes a User (from a Status), and create a JSON object that
    * can be parsed by the Botometer web service. Direct deserialization
    * using Gson would be ideal, but the fields don't line up exactly.
    * 
	* @param user object.
	* @return JsonObject
	*/
    private JsonObject getJsonFromUser(User user) {
		JsonObject togo = null;
        String method = "getJsonFromUser: ";
        logger.debug(method + "STARTING.");
		
        if (user == null) {
            logger.error(method + "constructor arguments incorrect, make sure all populated.");
            System.exit(1);
        } 
        
        JsonObject user_json = new JsonObject();
        
        user_json.addProperty("favourites_count", user.getFavouritesCount());
        user_json.addProperty("followers_count", user.getFollowersCount());
        user_json.addProperty("default_profile_image", user.isDefaultProfileImage());
        user_json.addProperty("description", user.getDescription());
        user_json.addProperty("url", user.getURL());
        user_json.addProperty("profile_background_color", user.getProfileBackgroundColor());
        user_json.addProperty("statuses_count", user.getStatusesCount());
        user_json.addProperty("follow_request_sent", user.isFollowRequestSent());
        user_json.addProperty("created_at", getFormattedDate(user.getCreatedAt()));
        user_json.addProperty("profile_image_url_https", user.getProfileImageURLHttps());
        user_json.addProperty("friends_count", user.getFriendsCount());
        user_json.addProperty("profile_image_url", user.getProfileImageURL());
        user_json.addProperty("profile_background_image_url_https", user.getProfileBackgroundImageUrlHttps());
        user_json.addProperty("profile_sidebar_fill_color", user.getProfileSidebarFillColor());
        user_json.addProperty("verified", user.isVerified());
        user_json.addProperty("contributors_enabled", user.isContributorsEnabled());
        user_json.addProperty("is_translator", user.isTranslator());
        user_json.addProperty("default_profile", user.isDefaultProfile());
        user_json.addProperty("lang", user.getLang());
        user_json.addProperty("protected", user.isProtected());
        user_json.addProperty("location", user.getLocation());
        user_json.addProperty("profile_text_color", user.getProfileTextColor());
        user_json.addProperty("profile_banner_url", user.getProfileBannerURL());
        user_json.addProperty("screen_name", user.getScreenName());
        user_json.addProperty("id_str", Long.toString(user.getId()));
        user_json.addProperty("name", user.getName());
        user_json.addProperty("profile_background_tile", user.isProfileBackgroundTiled());
        user_json.addProperty("profile_sidebar_border_color", user.getProfileSidebarBorderColor());
        user_json.addProperty("utc_offset", user.getUtcOffset());
        user_json.addProperty("id", user.getId());
        user_json.addProperty("profile_link_color", user.getProfileLinkColor());
        user_json.addProperty("profile_background_image_url", user.getProfileBackgroundImageURL());
        user_json.addProperty("listed_count", user.getListedCount());
        user_json.addProperty("geo_enabled", user.isGeoEnabled());
        user_json.addProperty("profile_use_background_image", user.isProfileUseBackgroundImage());
        user_json.addProperty("time_zone", user.getTimeZone());
        
        JsonObject entities = new JsonObject();
                            
        JsonObject description_object = new JsonObject();
        description_object.add("urls", new JsonArray());
        entities.add("description", description_object);
        
        JsonArray urls_array = new JsonArray();
        JsonObject temp_entity = new JsonObject();
        JsonArray temp_array = new JsonArray();
        temp_entity.addProperty("url", user.getURLEntity().getURL());
        temp_entity.addProperty("expanded_url", user.getURLEntity().getExpandedURL());
        temp_entity.addProperty("display_url", user.getURLEntity().getDisplayURL());
        temp_array.add(user.getURLEntity().getStart());
        temp_array.add(user.getURLEntity().getEnd());
        temp_entity.add("indices", temp_array);
        urls_array.add(temp_entity);
        
        JsonObject url_object = new JsonObject();
        url_object.add("urls", urls_array);
        entities.add("url", url_object);
            
        user_json.add("entities", entities);
        
        // Couldn't find a direct mapping from Twitter4J to the Botometer API
        //   for the following fields. Service still appears to work without them.
        //user_json.addProperty("notifications", user.);
        //user_json.addProperty("following", user.);
        //user_json.addProperty("has_extended_profile", user.);
        //user_json.addProperty("is_translation_enabled", user.);
        //user_json.addProperty("translator_type", user.);
        
        togo = user_json;
          
        logger.debug(method + "ENDING.");
        return togo;
    }
    
   /**
    * Takes a java.util.Date and returns a String in the format
    * that is expected by the Botometer service. E.g. "Mon Sep 06 13:31:27 +0000 2010".
    * 
	* @param date object.
	* @return String
	*/
    private String getFormattedDate(Date date) {
		String togo = "";

        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		togo = sdf.format(date);
        
        return togo;
    }     
}


























