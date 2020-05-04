package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.services.conversation.ConversationServiceException
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.PlainSelect
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    value = [
        JsonSubTypes.Type(name = "start", value = StartState::class),
        JsonSubTypes.Type(name = "end", value = EndState::class),
        JsonSubTypes.Type(name = "send-mex", value = SendMexState::class),
        JsonSubTypes.Type(name = "wait-for-input", value = WaitForInputState::class),
        JsonSubTypes.Type(name = "jdbc-read", value = JdbcReadState::class),
        JsonSubTypes.Type(name = "jdbc-write", value = JdbcWriteState::class),
        JsonSubTypes.Type(name = "http", value = HttpState::class)
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

class StaticExpectedValues(val values: List<String>) :
    ExpectedValues(type = "static") {
    init {
        if (values.isEmpty())
            throw BotConfigException("A static expected-values type can't have [] values")
    }
}

class AnyExpectedValues : ExpectedValues(type = "any")

class SessionExpectedValues(val key: String) :
    ExpectedValues(type = "session") {
    init {
        if (key == "")
            throw BotConfigException("A session expected-values type can't have empty key")
//        if (onMismatch.isEmpty())
//            throw BotConfigException("A session expected-values on-mismatch can't be empty")
    }
}

class WaitForInputState(
    id: String,
    @JsonProperty("expected-type") val expectedType: String,
    @JsonProperty("expected-values") val expectedValues: ExpectedValues,
    @JsonProperty("on-mismatch") val onMismatch: String,
    @JsonProperty("session-field") val sessionField: String = ""
) : BotState(id, type = "wait-for-input") {
    init {
        val availableExpectedTypes = listOf("number", "string").sorted()
        if (expectedType !in availableExpectedTypes)
            throw BotConfigException("Invalid expected-type '$expectedType'. Valid values are: $availableExpectedTypes")
        if (onMismatch.isEmpty())
            throw BotConfigException("A static expected-values on-mismatch can't be empty")
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
        if (query == "")
            throw BotConfigException("Invalid query: '$query' provided for state: '$id'")

        try {
            log.trace("Parsing query: $query")
            if (query.startsWith("select *"))
                throw ConversationServiceException(
                    "Invalid query: '$query' provided for state: '$id' must have a single column return"
                )

            val columns = extractSelectNames(query)
            log.trace("Columns list is: {}", columns)
            if (columns.size != 1) {
                log.trace("Invalid column list")
                throw ConversationServiceException(
                    "Invalid query: '$query' provided for state: '$id' must have a single column return"
                )
            }
        } catch (e: ClassCastException) {
            log.trace("Query is not a select!")
            throw BotConfigException("Invalid query: '$query' provided for state: '$id' is not a select")
        } catch (e: JSQLParserException) {
            log.trace("Query is bad written!")
            throw BotConfigException("Invalid query - SQL error: '$query' provided for state: '$id'")
        } catch (e: ConversationServiceException) {
            throw e
        } catch (e: Exception) {
            log.trace("{}", e)
            throw BotConfigException("Invalid query: '$query' provided for state: '$id'")
        }
    }

    fun extractSelectNames(s: String): List<String> {
        val regex = """!\{(.*?)\}""".toRegex()
        val sql = s.replace(regex, "?")
        log.trace("Looking select columns from: $sql")
        val selectStatement = CCJSqlParserUtil.parse(sql) as Select
        log.trace("Select body: {}", selectStatement.selectBody)
        log.trace("Is plain select? {}", selectStatement.selectBody is PlainSelect)

        val select = selectStatement.selectBody as PlainSelect
        log.trace("{}", select.selectItems)
        log.trace("{}", select.selectItems.map { it.toString() })
        return select.selectItems.map { it.toString() }
    }
}

class JdbcWriteState(
    id: String,
    @JsonProperty("query") val query: String
) : BotState(id, type = "jdbc-write") {
    init {
        val s: Statement = try {
            log.trace("Parsing query: $query")
            log.trace("Substitute session parameters")
            val regex = """!\{(.*?)\}""".toRegex()
            val sql = query.replace(regex, "?")
            CCJSqlParserUtil.parse(sql)
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

data class HttpRequestHeaders(
    @JsonProperty("content-type", required = false) val contentType: String = "",
    @JsonProperty("accept", required = false) val accept: String = ""
)

data class HttpRequestParam(val key: String, val value: String)

data class HttpRequestDetails(
    val method: String,
    val url: String,
    @JsonProperty("query-params") val queryParams: List<HttpRequestParam> = listOf(),
    @JsonProperty("body-params") val bodyParams: List<HttpRequestParam> = listOf(),
    val headers: HttpRequestHeaders = HttpRequestHeaders()
) {
    init {
        if (method.isEmpty())
            throw BotConfigException("http method can't be empty")
        if (method !in setOf("get", "GET", "post", "POST", "put", "PUT", "delete", "DELETE"))
            throw BotConfigException("'$method' is not a valid http method. Supported methods are: [get, post, put, delete]")

        if (url.isEmpty())
            throw BotConfigException("url can't be empty")
        try {

            val regex = """!\{(.*?)\}""".toRegex()
            val checkUrl = url.replace(regex, "sessiondata")
            URL(checkUrl).toURI()
        } catch (e: Exception) {
            when (e) {
                is MalformedURLException,
                is URISyntaxException -> throw BotConfigException("url '$url' is not a valid url")
            }
        }
    }
}

class HttpState(
    id: String,
    val request: HttpRequestDetails,
    @JsonProperty("extraction-key") val extractionKey: String,
    @JsonProperty("session-field") val sessionField: String
) : BotState(id, "http") {
    init {
        if (extractionKey.isEmpty())
            throw BotConfigException("extraction-key can't be empty")
        if (sessionField.isEmpty())
            throw BotConfigException("session-field can't be empty")
    }
}