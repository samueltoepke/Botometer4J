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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.ClassLoader;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
* HelloWorld class. To show workingness of Botometer Java library.
* @author  Samuel Lee Toepke
* @version 1.0
*/
public class HelloWorld {

    private final static Logger logger = LogManager.getLogger(HelloWorld.class);

   /**
    * Main method.
	* @param args String array that holds input arguments. Not used in this case.
	*/
    public static void main(String[] args) {
        String method = "Botometer Test Harness: ";
        logger.info(method + "STARTING.");

        // 0. Load Configuration File.
	logger.info(method + "0. Load Configuration File.");

        InputStream in    = ClassLoader.getSystemClassLoader().getResourceAsStream("config.properties");
        Properties  prop  = new Properties();

        try {
            prop.load(in);
        } catch (Exception e) {
            logger.error(e.toString());
        System.exit(1);
        }

        // 1. Instantiate Botometer4J
        logger.info(method + "1. Instantiate Botometer4J Object.");
        Botometer4J botometer = new Botometer4J(prop.getProperty("mashape_key"), prop.getProperty("consumer_key"), prop.getProperty("consumer_secret"), prop.getProperty("access_token"), prop.getProperty("access_token_secret"));

        // 2. Get Botometer Response
        logger.info(method + "2. Execute Botometer4J Query/Requests.");

        String screen_name = "stevemartintogo";
        //String screen_name = "rilokiley"; 
        //String screen_name = "popville";
        //String screen_name = "ieeespectrum";
        //String screen_name = "nsfvoyager2";

        String botometer_response = botometer.getBotometerResponseForScreenName(screen_name);
        logger.info(method + "....Querying Twitter Username: " + screen_name);
        logger.info(method + "....Result: " + botometer_response);

        logger.info(method + "ENDING.");
    }
}






















