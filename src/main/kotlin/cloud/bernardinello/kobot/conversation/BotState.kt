package cloud.bernardinello.kobot.conversation

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(name = "start", value = StartState::class),
        JsonSubTypes.Type(name = "end", value = EndState::class),
        JsonSubTypes.Type(name = "send-mex", value = SendMexState::class),
        JsonSubTypes.Type(name = "wait-for-input", value = WaitForInputState::class),
        JsonSubTypes.Type(name = "jdbc-read", value = JdbcReadState::class),
        JsonSubTypes.Type(name = "jdbc-write", value = JdbcWriteState::class)
    ]
)
abstract class BotState(val id: String, val type: String) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BotState::class.java)
    }

    init {
        if (id == "")
            throw BotConfigException("State field 'id' can't be ''")
    }
}

class StartState(id: String) : BotState(id, type = "start")
class EndState(id: String) : BotState(id, type = "end")
class SendMexState(id: String, val message: String) : BotState(id, type = "send-mex") {
    init {
        if (message == "")
            throw BotConfigException("State field 'message' can't be empty")
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(name = "static", value = StaticExpectedValues::class),
        JsonSubTypes.Type(name = "any", value = AnyExpectedValues::class),
        JsonSubTypes.Type(name = "session", value = SessionExpectedValues::class)
    ]
)
abstract class ExpectedValues(
    val type: String
)

data class StaticExpectedValues(val values: List<String>, @JsonProperty("on-mismatch") val onMismatch: String) :
    ExpectedValues(type = "static") {
    init {
        if (values.isEmpty())
            throw BotConfigException("A static expected-values type can't have [] values")
        if (onMismatch.isEmpty())
            throw BotConfigException("A static expected-values on-mismatch can't be empty")
    }
}

class AnyExpectedValues : ExpectedValues(type = "any")

class SessionExpectedValues(val key: String, @JsonProperty("on-mismatch") val onMismatch: String) :
    ExpectedValues(type = "session") {
    init {
        if (key == "")
            throw BotConfigException("A session expected-values type can't have empty key")
        if (onMismatch.isEmpty())
            throw BotConfigException("A session expected-values on-mismatch can't be empty")
    }
}

class WaitForInputState(
    id: String,
    @JsonProperty("expected-type") val expectedType: String,
    @JsonProperty("expected-values") val expectedValues: ExpectedValues,
    @JsonProperty("session-field") val sessionField: String = ""
) : BotState(id, type = "wait-for-input") {
    init {
        val availableExpectedTypes = listOf("number", "string").sorted()

        if (expectedType !in availableExpectedTypes)
            throw BotConfigException("Invalid expected-type '$expectedType'. Valid values are: $availableExpectedTypes")
    }
}

class JdbcReadState(
    id: String,
    @JsonProperty("query") val query: String,
    @JsonProperty("session-field") val sessionField: String
) : BotState(id, type = "jdbc-read") {
    init {
        if (sessionField == "")
            throw BotConfigException("Invalid session-field: '$sessionField' provided for state: '$id'")
        try {
            log.trace("Parsing query: $query")
            CCJSqlParserUtil.parse(query) as Select
        } catch (e: ClassCastException) {
            throw BotConfigException("Invalid query: '$query' provided for state: '$id' is not a select")
        } catch (e: Exception) {
            log.trace("{}", e)
            throw BotConfigException("Invalid query: '$query' provided for state: '$id'")
        }
    }
}

class JdbcWriteState(
    id: String,
    @JsonProperty("query") val query: String
) : BotState(id, type = "jdbc-write") {
    init {
        val s: Statement = try {
            log.trace("Parsing query: $query")
            CCJSqlParserUtil.parse(query)
        } catch (e: Exception) {
            throw BotConfigException("Invalid query: '$query' provided for state: '$id'")
        }

        try {
            s as Update
        } catch (e: ClassCastException) {
            log.trace("Update cast failed, trying as Insert")
            try {
                s as Insert
            } catch (e: ClassCastException) {
                throw BotConfigException("Invalid query: '$query' provided for state: '$id' is not an insert or update")
            }
        }
    }

}