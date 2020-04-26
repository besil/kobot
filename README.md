# Kobot - Conversational bot
The goal of this project is to provide an open source framework for creating conversational 
bots using only a simple descriptive and human-readable configuration

* [Quickstart](https://github.com/besil/kobot#quickstart-guide)
* [User guide](https://github.com/besil/kobot#user-guide)
    * [Configuration](https://github.com/besil/kobot#configuration)
    * [Kobot conversation](https://github.com/besil/kobot#kobot-conversation)
    * [Session data](https://github.com/besil/kobot#session-data)
    * [State types](https://github.com/besil/kobot#state-types)
        * [start/end](https://github.com/besil/kobot#startend-state)
        * [send-mex](https://github.com/besil/kobot#send-mex)
        * [wait-for-input](https://github.com/besil/kobot#wait-for-input)
        * [jdbc-read](https://github.com/besil/kobot#jdbc-read)
* [Developer guide](https://github.com/besil/kobot#developer-guide)

## Quickstart guide
### Prerequisites
* [Download](https://github.com/besil/kobot/releases/) or build the jar
* Have a telegram bot token and name ([telegram docs](https://core.telegram.org/bots))
* Have Java 8+ installed

### Define the configuration
Under the *config* dir, create two files:
* *application.properties*
* *conversation.json*

The *config/application.properties* contains bot telegram definition and the
relative path to the conversation json descriptor
```properties
bot.name=<your bot name>
bot.token=<your bot token>
conversation.path=config/conversation.json
```

The *conversation.json* file contains your actual configuration.
Try with this:

```json
{
  "states": [
    {"id":  "start", "type":  "start"},
    {"id":  "greet", "type":  "send-mex", "message":  "Welcome! Pick an option"},
    {
      "id":  "wait-input",
      "type":  "wait-for-input",
      "expected-type": "string",
      "on-mismatch": "Choice not recognized. Please, pick one of the following only",
      "expected-values": {
        "type": "static",
        "values": ["hello", "world"]
      },
      "session-field": "customer-key"
    },
    {"id":  "ciao", "type":  "send-mex", "message":  "You wrote: !{customer-key}. Hello to you!"},
    {"id":  "mondo", "type":  "send-mex", "message":  "You wrote: !{customer-key}. Did you say word? Hello world!!"},
    {"id":  "question", "type":  "send-mex", "message":  "Do you like me?"},
    {
      "id":  "get-answer",
      "type":  "wait-for-input",
      "expected-type": "string",
      "on-mismatch": "nah, that wasn't a valid option",
      "expected-values": {
        "type": "static",
        "values": ["yes", "no"]
      }
    },
    {"id":  "sorry", "type":  "send-mex", "message":  "oh, I'm sorry... I'll try to do better"},
    {"id":  "wow", "type":  "send-mex", "message":  "WOW thanks! You are awesome too! <3"},
    {"id":  "bye", "type":  "send-mex", "message":  "Bye! See ya"},
    {"id":  "end", "type":  "end"}
  ],
  "relationships": [
    {"from":  "start", "to":  "greet"},
    {"from":  "greet", "to":  "wait-input"},
    {"from" : "wait-input", "to":  "ciao", "on-input":  ["hello"]},
    {"from" : "wait-input", "to":  "mondo", "on-input":  ["world"]},
    {"from":  "ciao", "to":  "question"},
    {"from":  "mondo", "to":  "question"},
    {"from":  "question", "to":  "get-answer"},
    {"from":  "get-answer", "to":  "sorry", "on-input":  ["no"]},
    {"from":  "get-answer", "to":  "wow", "on-input":  ["yes"]},
    {"from":  "sorry", "to":  "bye"},
    {"from":  "wow", "to":  "bye"},
    {"from":  "bye", "to":  "end"}
  ]
}
```

Run the bot with
```shell script
java -jar kobot.jar
```

Now go to telegram and enjoy your first conversation with the bot!

## User guide
A kobot conversation is represented as a simple json with two keys: *states* and *relationships*.

A *state* defines an action the bot will perform, while the *relationships* connect the actions together, forming
the actual conversation.
You can see some examples of conversation under the 
[conversations](https://github.com/besil/kobot/tree/master/src/test/resources/conversations) folder.

### Configuration
In order to use Kobot, you have to create a:
```text
application.properties
conversation.json
```
inside **config** folder where you run the jar.

The **application.properties** contains application specific properties, such as database connection.
In order to configure it, use the following as example:
```properties
logging.level.root=INFO
logging.level.org.springframework=INFO
logging.level.org.telegram=INFO
logging.level.cloud.bernardinello.kobot=INFO

bot.name=<telegram bot name>
bot.token=<telegram bot token>
conversation.path=config/conversation.json

spring.datasource.url=jdbc:h2:mem:store
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=h2
spring.datasource.password=h2
```

### Kobot conversation
You can find some examples of conversations in the [conversations folder](https://github.com/besil/kobot/tree/master/src/test/resources/conversations)

Every state must have a unique custom *id* property and a *type* properties.

There are different types, more will be developed:
  * [*start*](https://github.com/besil/kobot#startend-state) or [*end*](https://github.com/besil/kobot#startend-state), which indicate the beginning and the end of the conversation
  * [*send-mex*](https://github.com/besil/kobot#send-mex)
  * [*wait-for-input*](https://github.com/besil/kobot#wait-for-input)
  * [*jdbc-read*](https://github.com/besil/kobot#jdbc-read)

All states must be connected in a path from the start node to the end node.

### Session data
Each state can access **session data**, which are user-specific data saved during the conversation.
For example, you can save the input from user in the session and use it later to query a db or invoking an API.
Session data referring to key **session-key** can be accessed using 
```
!{session-key}
```
You will find examples in each state on how to access this information.

A special session key provided by kobot is the **!{chatId}**, which indicates the user chat unique identifier.

### State types
#### start/end State
```json
{"id":"start", "type": "start"}
```
```json
{"id":"end", "type": "end"}
```

These states indicates the beginning and the end of a conversation.
Every conversation must have them and no other states can be before the start state or after the end state.

#### send-mex
```json
{"id":"send", "type": "send-mex", "message": "Welcome! Pick an option"}
```
```json
{"id":"send", "type": "send-mex", "message": "Hi! Your chat is !{chatId}"}
```

This state makes kobot send the *message* to the user.
You can use **session-data** for dynamic message: you can use the **!{key}**, but the key must be present in session.
**chatId** is a special key, provided by the framework and indicates the unique user chat identifier.

#### wait-for-input
```json
{
  "id": "wait-input",
  "type": "wait-for-input",
  "expected-type": "string",
  "on-mismatch": "Choice not recognized. Please, pick one of the following only",
  "expected-values": {
    "type": "static",
    "values": [ "hello", "world"]
  }
}
```
```json
{
  "id": "show-retrieved",
  "type": "wait-for-input",
  "expected-type": "string",
  "on-mismatch": "Not a valid option! Try again",
  "expected-values": {
    "type": "session",
    "key": "db-data"
  },
  "session-field": "choice"
}
```
**wait-for-input** requires a *expected-type* field: this can be
* string
* number

*on-mismatch* is the message provided when the input doesn't match the expected values.

*session-field* is an optional key.
If provided, the input will be stored in session at the provided variable name

*expected-values* type can be :
* static
    * requires an array of values, which will be provided as input choices to the user and matched against given input
* session
    * requires a key, which session-values will be provided as input choices to the user

#### jdbc-read  
```json
{
  "id": "get-content",
  "type": "jdbc-read",
  "query": "select bar from foobar where foo=!{choice}",
  "session-field": "db-data"
}
```
this state requires a mandatory *query* and *session-field*.
In order to perform queries, you have to [configure the database access](https://github.com/besil/kobot#configuration)

The query is a SQL query, which can use session data in order to parametrize the query.
The query must be a plain select and must fetch only a single column.

If multiple results will be fetched, data will be stored in a list.
If a single value is fetched, it will provided as a scalar value.

*session-field* indicates the key of the session where data will be stored.
  

### Relationships
Bot relationships defines transitions between states.
A relationships defines the link between two states
```json
{"from": "s1", "to": "s2"}
```

In a wait-for-input state, you can go to different states based on the input from user: imagine a Yes/No situation.

For relationsips from a wait-for-input states with static expected-values, you have to specify
the message on the relationship. For example:   
```json
{"from" : "wait-input", "to":  "ciao", "on-input":  ["hello"]}
```
```json
{"from" : "wait-input", "to":  "mondo", "on-input":  ["world, foo"]}
```
In this case, when in *wait-input* state and after receiving input *hello*, kobot will go to *ciao* state.

If *world* or *foo* is received, the *mondo* state will be reached.
Please note that on-input values must exactly match the static expected-values provided in the state definition.

You will receive an error on startup in case

## Developer guide

### Build locally
```shell script
git clone https://github.com/besil/kobot.git
cd kobot
mvn clean package
``` 

### Docker
Put your *telegram.json* and *conversation.json* inside the **config** folder

A tipical *application.properties* could be
```properties
logging.level.root=INFO
logging.level.org.springframework=INFO
logging.level.org.telegram=INFO
logging.level.cloud.bernardinello.kobot=TRACE

bot.name=<bot name>
bot.token=<bot token>
conversation.path=config/conversation.json

#spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.ddl-auto=create-drop
spring.datasource.url=jdbc:h2:mem:store
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=h2
spring.datasource.password=h2
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.open-in-view=true
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console/
```

Then run
```shell script
bash scripts/build-docker.sh
bash scripts/run-docker.sh
``` 

You can access H2 database on [http://localhost:8080/h2-console](http://localhost:8080/h2-console)

#### License
Apache Open Source
#### Written in Kotlin with love

