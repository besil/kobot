# Kobot - Conversational bot

## Goal
The goal of this project is to provide an open source framework 
for creating conversational bots using only a simple descriptive
and human-readable configuration

## Quickstart guide
### Prerequisites
* Download or build the jar (kobot-1.0-SNAPSHOT-exec.jar)
* Have a telegram bot token and name (talk to Botfather in order to get it)
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
A conversation description is a simple json with two keys: *states* and *relationships*.

A *state* defines an action the bot will perform, while the *relationships* connect the actions together, forming
the actual conversation.
You can see some examples of conversation under the [conversations]() folder.

### States
Every state must have a unique *id* property and a *type* properties.
Types can be:
  * *start* or *end*, which indicate the beginning and the end of the conversation
  * *send-mex*
  * *wait-for-input*
  * *jdbc-read*
  

### Relationships


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

You can access H2 database on [http://localhost:8080/h2-console]

#### License
Apache Open Source
#### Written in Kotlin with love

