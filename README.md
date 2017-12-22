# Botometer4J Java Library

This project leverages Twitter4J to connect to the Twitter REST API, pull down information pertinent to a user,
then query the Botometer REST API via Mashape. The Botometer4J object is meant to abstract connecting to the services
and generating necessary payload per Botometer instructions. The user will be able to instantiate an object with 
proper security keys, then query the object with a Twitter screenname. JSON is returned which describes the bot-ness
of the screen name, per the format in the Botometer instructions.

Make sure to go into "./resources/config.properties" and change the keys/tokens to those that were provided from
Twitter and Mashape. Full example can be seen in "./src/com/samueltoepke/HelloWorld.java".

This is meant to provide a convenient way to get Botometer data from a screen name, no rate limiting is implemented
for Twitter/Mashape. 
 
## SOFTWARE:
 * Ubuntu 14.04 LTS (Any OS with Java/Ant installed should work just fine.)
 * Java SDK 1.8.0_151
 * Apache Ant 1.9.3

## DOCUMENTATION:
 * https://botometer.iuni.iu.edu/#!/
 * https://botometer.iuni.iu.edu/#!/publications
 * https://market.mashape.com/OSoMe/botometer
 * http://twitter4j.org/en/

## EXECUTION:
 * All possible targets are in the ./build.xml file.
 * From a command line type "$ ant" to fully build/deploy/execute the code.
