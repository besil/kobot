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
      "expected-values": {
        "type": "static",
        "on-mismatch": "Choice not recognized. Please, pick one of the following only",
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
      "expected-values": {
        "type": "static",
        "on-mismatch": "nah, that wasn't a valid option",
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
java -jar kobot.jar -config conversation.json telegram.json
```

Now go to telegram and enjoy your first conversation with the bot!


## Developer guide

### Build locally
```shell script
git clone https://github.com/besil/kobot.git
cd kobot
mvn clean package
``` 

### Docker
Put your *telegram.json* and *conversation.json* inside the **config** folder

Then run
```shell script
bash scripts/build-docker.sh
bash scripts/run-docker.sh
``` 

#### License
Apache Open Source
#### Written in Kotlin with love

