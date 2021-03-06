package jp.ne.opt.redshiftfake

import java.sql.{ResultSet, SQLWarning, Connection, Statement}

import jp.ne.opt.redshiftfake.parse.{DDLParser, UnloadCommandParser, CopyCommandParser}
import jp.ne.opt.redshiftfake.s3.S3Service

/**
 * Enum used to hold which overload of createStatement is called.
 */
sealed abstract class StatementType
object StatementType {
  case object Plain extends StatementType
  case class ResultSetTypeConcurrency(resultSetType: Int, resultSetConcurrency: Int) extends StatementType
  case class ResultSetTypeConcurrencyHoldability(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) extends StatementType
}

/**
 * Fake Statement.
 */
class FakeStatement(
  underlying: Statement,
  connection: Connection,
  statementType: StatementType,
  s3Service: S3Service) extends Statement with CopyInterceptor with UnloadInterceptor {

  def setMaxFieldSize(max: Int): Unit = underlying.setMaxFieldSize(max)
  def getMoreResults: Boolean = underlying.getMoreResults
  def getMoreResults(current: Int): Boolean = underlying.getMoreResults
  def clearWarnings(): Unit = underlying.clearWarnings()
  def getGeneratedKeys: ResultSet = underlying.getGeneratedKeys
  def closeOnCompletion(): Unit = underlying.closeOnCompletion()
  def cancel(): Unit = underlying.cancel()
  def getResultSet: ResultSet = underlying.getResultSet
  def isPoolable: Boolean = underlying.isPoolable
  def setPoolable(poolable: Boolean): Unit = underlying.setPoolable(poolable)
  def setCursorName(name: String): Unit = underlying.setCursorName(name)
  def getUpdateCount: Int = underlying.getUpdateCount
  def addBatch(sql: String): Unit = underlying.addBatch(sql)
  def getMaxRows: Int = underlying.getMaxRows
  def getResultSetType: Int = underlying.getResultSetType
  def setMaxRows(max: Int): Unit = underlying.setMaxRows(max)
  def getFetchSize: Int = underlying.getFetchSize
  def getResultSetHoldability: Int = underlying.getResultSetHoldability
  def setFetchDirection(direction: Int): Unit = underlying.setFetchDirection(direction)
  def getFetchDirection: Int = underlying.getFetchDirection
  def getResultSetConcurrency: Int = underlying.getResultSetConcurrency
  def clearBatch(): Unit = underlying.clearBatch()
  def close(): Unit = underlying.close()
  def isClosed: Boolean = underlying.isClosed
  def getQueryTimeout: Int = underlying.getQueryTimeout
  def getWarnings: SQLWarning = underlying.getWarnings
  def setFetchSize(rows: Int): Unit = underlying.setFetchSize(rows)
  def setQueryTimeout(seconds: Int): Unit = underlying.setQueryTimeout(seconds)
  def setEscapeProcessing(enable: Boolean): Unit = underlying.setEscapeProcessing(enable)
  def getConnection: Connection = underlying.getConnection
  def getMaxFieldSize: Int = underlying.getMaxFieldSize
  def isCloseOnCompletion: Boolean = underlying.isCloseOnCompletion
  def unwrap[T](iface: Class[T]): T = underlying.unwrap(iface)
  def isWrapperFor(iface: Class[_]): Boolean = underlying.isWrapperFor(iface)

  // Unsupported
  def executeBatch(): Array[Int] = underlying.executeBatch()

  //========================
  // Intercept
  //========================
  def execute(sql: String): Boolean = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.execute(sanitized), false)
  }

  def execute(sql: String, autoGeneratedKeys: Int): Boolean = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.execute(sanitized, autoGeneratedKeys), false)
  }

  def execute(sql: String, columnIndexes: Array[Int]): Boolean = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.execute(sanitized, columnIndexes), false)
  }

  def execute(sql: String, columnNames: Array[String]): Boolean = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.execute(sanitized, columnNames), false)
  }

  def executeQuery(sql: String): ResultSet = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.executeQuery(sanitized), underlying.executeQuery("select 1 as one"))
  }
  def executeUpdate(sql: String): Int = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.executeUpdate(sanitized), 0)
  }
  def executeUpdate(sql: String, autoGeneratedKeys: Int): Int = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.executeUpdate(sanitized, autoGeneratedKeys), 0)
  }
  def executeUpdate(sql: String, columnIndexes: Array[Int]): Int = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.executeUpdate(sanitized, columnIndexes), 0)
  }
  def executeUpdate(sql: String, columnNames: Array[String]): Int = {
    val sanitized = DDLParser.sanitize(sql)
    switchExecute(sanitized)(underlying.executeUpdate(sanitized, columnNames), 0)
  }

  private[this] def switchExecute[A](sql: String)(default: => A, switched: => A): A = {
    CopyCommandParser.parse(sql).map { command =>
      executeCopy(connection, command, s3Service)
      switched
    }.orElse(UnloadCommandParser.parse(sql).map { command =>
      executeUnload(connection, command, s3Service)
      switched
    }).getOrElse(default)
  }
}
